package com.financas.service;

import com.financas.dto.BancoDividasResponse;
import com.financas.dto.DividaRequest;
import com.financas.dto.DividaResponse;
import com.financas.dto.PessoaDividasResponse;
import com.financas.dto.TipoDividasResponse;
import com.financas.model.Divida;
import com.financas.model.Lancamento;
import com.financas.model.TipoDivida;
import com.financas.model.TipoLancamento;
import com.financas.repository.DividaRepository;
import com.financas.repository.LancamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * "Me devem": valores que outras pessoas devem ao usuário. Não entram no saldo.
 * <ul>
 *   <li>DEBITO (dinheiro do usuário): ao quitar, vira uma ENTRADA (Reembolso) no mês em que foi paga.</li>
 *   <li>CREDITO (cartão, crédito, empréstimo): "recebi" e "paguei a instituição" são marcados à parte,
 *       sem tocar no saldo; com os dois marcados, fica quitada.</li>
 * </ul>
 */
@Service
public class DividaService {

    static final String CATEGORIA_REEMBOLSO = "Reembolso";

    private final DividaRepository dividas;
    private final LancamentoRepository lancamentos;
    private final Clock clock;

    public DividaService(DividaRepository dividas, LancamentoRepository lancamentos, Clock clock) {
        this.dividas = dividas;
        this.lancamentos = lancamentos;
        this.clock = clock;
    }

    /**
     * Agrupa por pessoa → tipo (crédito antes de débito) → instituição. Pessoa e instituição não
     * diferenciam maiúsculas; quem deve mais aparece primeiro. Sem instituição fica por último.
     */
    public List<PessoaDividasResponse> listarPorPessoa() {
        return agrupar(dividas.findAllByOrderByDataDescIdDesc(), d -> chave(d.getPessoa())).stream()
                .map(lista -> new PessoaDividasResponse(
                        lista.get(lista.size() - 1).getPessoa(), // nome do registro mais antigo
                        somar(lista, d -> !d.isRecebida()),
                        agruparPorTipo(lista)))
                .sorted(Comparator.comparing(PessoaDividasResponse::totalAberto).reversed()
                        .thenComparing(PessoaDividasResponse::pessoa, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static List<TipoDividasResponse> agruparPorTipo(List<Divida> daPessoa) {
        List<TipoDividasResponse> tipos = new ArrayList<>();
        for (TipoDivida tipo : TipoDivida.values()) {
            List<Divida> doTipo = daPessoa.stream().filter(d -> d.getTipo() == tipo).toList();
            if (!doTipo.isEmpty()) {
                tipos.add(new TipoDividasResponse(
                        tipo,
                        somar(doTipo, d -> !d.isRecebida()),
                        somar(doTipo, d -> d.isCredito() && d.getDataPagamento() == null),
                        agruparPorBanco(doTipo)));
            }
        }
        return tipos;
    }

    private static List<BancoDividasResponse> agruparPorBanco(List<Divida> lista) {
        return agrupar(lista, d -> d.getBanco() == null ? "" : chave(d.getBanco())).stream()
                .map(grupo -> new BancoDividasResponse(
                        grupo.get(grupo.size() - 1).getBanco(),
                        somar(grupo, d -> !d.isRecebida()),
                        grupo.stream()
                                .sorted(Comparator.comparing(Divida::isQuitada) // abertas primeiro,
                                        .thenComparing(Divida::getData))        // vencimento mais próximo antes
                                .map(DividaService::paraResponse)
                                .toList()))
                .sorted(Comparator.comparing((BancoDividasResponse b) -> b.banco() == null) // sem instituição por último
                        .thenComparing(BancoDividasResponse::totalAberto, Comparator.reverseOrder())
                        .thenComparing(b -> b.banco() == null ? "" : b.banco(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static List<List<Divida>> agrupar(List<Divida> lista, Function<Divida, String> chave) {
        Map<String, List<Divida>> grupos = new LinkedHashMap<>();
        for (Divida divida : lista) {
            grupos.computeIfAbsent(chave.apply(divida), k -> new ArrayList<>()).add(divida);
        }
        return new ArrayList<>(grupos.values());
    }

    private static BigDecimal somar(List<Divida> lista, Predicate<Divida> filtro) {
        return lista.stream().filter(filtro).map(Divida::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Cria a dívida; com {@code parcelas} > 1, cria uma por mês (vencimento + i meses) com
     * "(i/N)" na descrição. As parcelas são independentes: cada uma é recebida/editada à parte.
     */
    @Transactional
    public List<DividaResponse> criar(DividaRequest request) {
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        List<Divida> novas = new ArrayList<>();
        for (int i = 0; i < parcelas; i++) {
            Divida divida = new Divida();
            preencher(divida, request);
            if (parcelas > 1) {
                divida.setDescricao(divida.getDescricao() + " (" + (i + 1) + "/" + parcelas + ")");
                divida.setData(request.data().plusMonths(i));
            }
            novas.add(divida);
        }
        return dividas.saveAll(novas).stream().map(DividaService::paraResponse).toList();
    }

    /**
     * Edita uma dívida (ou uma parcela; {@code parcelas} é ignorado). Se já estiver quitada (débito), a entrada gerada também é corrigida. O tipo só muda enquanto
     * nada foi marcado — senão a entrada de reembolso ou as datas ficariam sem sentido.
     */
    @Transactional
    public DividaResponse atualizar(Long id, DividaRequest request) {
        Divida divida = buscar(id);
        boolean temMarcacao = divida.isQuitada() || divida.getDataRecebimento() != null || divida.getDataPagamento() != null;
        if (tipoDe(request) != divida.getTipo() && temMarcacao) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Desfaça o recebimento/pagamento antes de trocar entre crédito e débito");
        }
        preencher(divida, request);
        if (divida.isQuitada() && divida.getLancamentoId() != null) {
            lancamentos.findById(divida.getLancamentoId()).ifPresent(entrada -> {
                entrada.setDescricao(descricaoDaEntrada(divida));
                entrada.setValor(divida.getValor());
                lancamentos.save(entrada);
            });
        }
        return paraResponse(dividas.save(divida));
    }

    /** Exclui só a dívida; uma entrada já gerada continua (o dinheiro entrou de fato). */
    public void excluir(Long id) {
        dividas.delete(buscar(id));
    }

    /** Só débito: quita e cria a entrada de reembolso com a data de hoje. */
    @Transactional
    public DividaResponse quitar(Long id) {
        Divida divida = buscar(id);
        if (divida.isCredito()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dívida de crédito: marque o recebimento e o pagamento separadamente");
        }
        if (divida.isQuitada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa dívida já foi quitada");
        }
        LocalDate hoje = LocalDate.now(clock);

        Lancamento entrada = new Lancamento();
        entrada.setTipo(TipoLancamento.ENTRADA);
        entrada.setCategoria(CATEGORIA_REEMBOLSO);
        entrada.setInstituicao(divida.getBanco());
        entrada.setDescricao(descricaoDaEntrada(divida));
        entrada.setValor(divida.getValor());
        entrada.setData(hoje);
        entrada.setPago(true);
        Lancamento salva = lancamentos.save(entrada);

        divida.setQuitada(true);
        divida.setDataQuitacao(hoje);
        divida.setLancamentoId(salva.getId());
        return paraResponse(dividas.save(divida));
    }

    /** Desfaz a quitação: no débito remove a entrada gerada; no crédito limpa as duas marcações. */
    @Transactional
    public DividaResponse reabrir(Long id) {
        Divida divida = buscar(id);
        if (!divida.isQuitada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa dívida não está quitada");
        }
        if (divida.getLancamentoId() != null && lancamentos.existsById(divida.getLancamentoId())) {
            lancamentos.deleteById(divida.getLancamentoId());
        }
        divida.setDataRecebimento(null);
        divida.setDataPagamento(null);
        divida.setQuitada(false);
        divida.setDataQuitacao(null);
        divida.setLancamentoId(null);
        return paraResponse(dividas.save(divida));
    }

    /** Crédito: a pessoa pagou o usuário. Não mexe no saldo. */
    public DividaResponse receber(Long id) {
        Divida divida = buscarCredito(id);
        if (divida.getDataRecebimento() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O recebimento já foi marcado");
        }
        divida.setDataRecebimento(LocalDate.now(clock));
        atualizarQuitacao(divida);
        return paraResponse(dividas.save(divida));
    }

    public DividaResponse desfazerRecebimento(Long id) {
        Divida divida = buscarCredito(id);
        if (divida.getDataRecebimento() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O recebimento não está marcado");
        }
        divida.setDataRecebimento(null);
        atualizarQuitacao(divida);
        return paraResponse(dividas.save(divida));
    }

    /** Crédito: o usuário pagou a instituição (fatura, parcela...). Não mexe no saldo. */
    public DividaResponse pagar(Long id) {
        Divida divida = buscarCredito(id);
        if (divida.getDataPagamento() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O pagamento já foi marcado");
        }
        divida.setDataPagamento(LocalDate.now(clock));
        atualizarQuitacao(divida);
        return paraResponse(dividas.save(divida));
    }

    public DividaResponse desfazerPagamento(Long id) {
        Divida divida = buscarCredito(id);
        if (divida.getDataPagamento() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O pagamento não está marcado");
        }
        divida.setDataPagamento(null);
        atualizarQuitacao(divida);
        return paraResponse(dividas.save(divida));
    }

    /** Crédito fica quitado com recebimento e pagamento marcados; a quitação é a data mais recente. */
    private static void atualizarQuitacao(Divida divida) {
        LocalDate recebido = divida.getDataRecebimento();
        LocalDate pago = divida.getDataPagamento();
        boolean quitada = recebido != null && pago != null;
        divida.setQuitada(quitada);
        divida.setDataQuitacao(quitada ? (recebido.isAfter(pago) ? recebido : pago) : null);
    }

    private Divida buscar(Long id) {
        return dividas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dívida não encontrada"));
    }

    private Divida buscarCredito(Long id) {
        Divida divida = buscar(id);
        if (!divida.isCredito()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Só dívidas de crédito têm recebimento e pagamento separados");
        }
        return divida;
    }

    private static TipoDivida tipoDe(DividaRequest request) {
        return request.tipo() == null ? TipoDivida.DEBITO : request.tipo();
    }

    private static void preencher(Divida divida, DividaRequest request) {
        divida.setPessoa(request.pessoa().trim());
        divida.setTipo(tipoDe(request));
        divida.setBanco(request.banco() == null || request.banco().isBlank() ? null : request.banco().trim());
        divida.setDescricao(request.descricao().trim());
        divida.setValor(request.valor());
        divida.setData(request.data());
    }

    private static String chave(String texto) {
        return texto.trim().toLowerCase(Locale.ROOT);
    }

    static String descricaoDaEntrada(Divida divida) {
        return divida.getPessoa() + " — " + divida.getDescricao();
    }

    private static DividaResponse paraResponse(Divida divida) {
        return new DividaResponse(
                divida.getId(),
                divida.getPessoa(),
                divida.getTipo(),
                divida.getBanco(),
                divida.getDescricao(),
                divida.getValor(),
                divida.getData(),
                divida.isQuitada(),
                divida.getDataQuitacao(),
                divida.getDataRecebimento(),
                divida.getDataPagamento());
    }
}
