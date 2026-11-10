package com.financas.service;

import com.financas.dto.DividaRequest;
import com.financas.dto.DividaResponse;
import com.financas.dto.PessoaDividasResponse;
import com.financas.model.Divida;
import com.financas.model.Lancamento;
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

/**
 * "Me devem": valores que outras pessoas devem ao usuário. Não entram no saldo; ao quitar,
 * vira uma ENTRADA (categoria Reembolso) no mês em que foi paga.
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

    /** Agrupa por pessoa (sem diferenciar maiúsculas); quem deve mais aparece primeiro. */
    public List<PessoaDividasResponse> listarPorPessoa() {
        Map<String, List<Divida>> porPessoa = new LinkedHashMap<>();
        for (Divida divida : dividas.findAllByOrderByDataDescIdDesc()) {
            porPessoa.computeIfAbsent(chave(divida.getPessoa()), k -> new ArrayList<>()).add(divida);
        }

        return porPessoa.values().stream()
                .map(lista -> new PessoaDividasResponse(
                        lista.get(lista.size() - 1).getPessoa(), // nome do registro mais antigo
                        lista.stream().filter(d -> !d.isQuitada())
                                .map(Divida::getValor).reduce(BigDecimal.ZERO, BigDecimal::add),
                        lista.stream()
                                .sorted(Comparator.comparing(Divida::isQuitada)) // abertas primeiro
                                .map(DividaService::paraResponse)
                                .toList()))
                .sorted(Comparator.comparing(PessoaDividasResponse::totalAberto).reversed()
                        .thenComparing(PessoaDividasResponse::pessoa, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public DividaResponse criar(DividaRequest request) {
        Divida divida = new Divida();
        preencher(divida, request);
        return paraResponse(dividas.save(divida));
    }

    /** Se já estiver quitada, a entrada gerada também é corrigida. */
    @Transactional
    public DividaResponse atualizar(Long id, DividaRequest request) {
        Divida divida = buscar(id);
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

    @Transactional
    public DividaResponse quitar(Long id) {
        Divida divida = buscar(id);
        if (divida.isQuitada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa dívida já foi quitada");
        }
        LocalDate hoje = LocalDate.now(clock);

        Lancamento entrada = new Lancamento();
        entrada.setTipo(TipoLancamento.ENTRADA);
        entrada.setCategoria(CATEGORIA_REEMBOLSO);
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

    /** Desfaz a quitação e remove a entrada que ela gerou. */
    @Transactional
    public DividaResponse reabrir(Long id) {
        Divida divida = buscar(id);
        if (!divida.isQuitada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa dívida não está quitada");
        }
        if (divida.getLancamentoId() != null && lancamentos.existsById(divida.getLancamentoId())) {
            lancamentos.deleteById(divida.getLancamentoId());
        }
        divida.setQuitada(false);
        divida.setDataQuitacao(null);
        divida.setLancamentoId(null);
        return paraResponse(dividas.save(divida));
    }

    private Divida buscar(Long id) {
        return dividas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dívida não encontrada"));
    }

    private static void preencher(Divida divida, DividaRequest request) {
        divida.setPessoa(request.pessoa().trim());
        divida.setDescricao(request.descricao().trim());
        divida.setValor(request.valor());
        divida.setData(request.data());
    }

    private static String chave(String pessoa) {
        return pessoa.trim().toLowerCase(Locale.ROOT);
    }

    static String descricaoDaEntrada(Divida divida) {
        return divida.getPessoa() + " — " + divida.getDescricao();
    }

    private static DividaResponse paraResponse(Divida divida) {
        return new DividaResponse(
                divida.getId(),
                divida.getPessoa(),
                divida.getDescricao(),
                divida.getValor(),
                divida.getData(),
                divida.isQuitada(),
                divida.getDataQuitacao());
    }
}
