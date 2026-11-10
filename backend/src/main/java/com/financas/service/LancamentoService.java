package com.financas.service;

import com.financas.dto.CategoriaResumoResponse;
import com.financas.dto.LancamentoRequest;
import com.financas.dto.LancamentoResponse;
import com.financas.dto.ResumoResponse;
import com.financas.model.Lancamento;
import com.financas.model.TipoLancamento;
import com.financas.repository.LancamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LancamentoService {

    private final LancamentoRepository repository;
    private final Clock clock;

    public LancamentoService(LancamentoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** Em todos os métodos de consulta, {@code mes == null} significa "todos os meses". */
    public List<LancamentoResponse> listar(TipoLancamento tipo, YearMonth mes) {
        List<Lancamento> lancamentos;
        if (mes == null) {
            lancamentos = tipo == null
                    ? repository.findAllByOrderByDataAscIdAsc()
                    : repository.findByTipoOrderByDataAscIdAsc(tipo);
        } else {
            lancamentos = tipo == null
                    ? repository.findByDataBetweenOrderByDataAscIdAsc(mes.atDay(1), mes.atEndOfMonth())
                    : repository.findByTipoAndDataBetweenOrderByDataAscIdAsc(tipo, mes.atDay(1), mes.atEndOfMonth());
        }
        return lancamentos.stream().map(LancamentoService::paraResponse).toList();
    }

    /**
     * Cria o lançamento e, se {@code repetirMeses > 1}, uma cópia por mês seguinte (mesmo dia;
     * dia 31 vira o último dia em meses mais curtos). Cópias com data futura nascem como previstas.
     */
    @Transactional
    public List<LancamentoResponse> criar(LancamentoRequest request) {
        int vezes = request.repetirMeses() == null ? 1 : request.repetirMeses();
        LocalDate hoje = LocalDate.now(clock);
        boolean pagoPrimeiro = request.pago() != null ? request.pago() : !request.data().isAfter(hoje);

        List<Lancamento> novos = new ArrayList<>();
        for (int i = 0; i < vezes; i++) {
            LocalDate data = request.data().plusMonths(i);
            Lancamento lancamento = new Lancamento();
            preencher(lancamento, request, data);
            lancamento.setPago(i == 0 ? pagoPrimeiro : pagoPrimeiro && !data.isAfter(hoje));
            novos.add(lancamento);
        }
        return repository.saveAll(novos).stream().map(LancamentoService::paraResponse).toList();
    }

    /** Edita um único lançamento ({@code repetirMeses} é ignorado). */
    public LancamentoResponse atualizar(Long id, LancamentoRequest request) {
        Lancamento lancamento = buscar(id);
        preencher(lancamento, request, request.data());
        if (request.pago() != null) {
            lancamento.setPago(request.pago());
        }
        return paraResponse(repository.save(lancamento));
    }

    public LancamentoResponse marcarPago(Long id, boolean pago) {
        Lancamento lancamento = buscar(id);
        lancamento.setPago(pago);
        return paraResponse(repository.save(lancamento));
    }

    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado");
        }
        repository.deleteById(id);
    }

    public ResumoResponse resumo(YearMonth mes) {
        List<Lancamento> todos = mes == null
                ? repository.findAll()
                : repository.findByDataBetween(mes.atDay(1), mes.atEndOfMonth());

        BigDecimal entradas = somar(todos, TipoLancamento.ENTRADA, true);
        BigDecimal saidas = somar(todos, TipoLancamento.SAIDA, true);
        BigDecimal aReceber = somar(todos, TipoLancamento.ENTRADA, false);
        BigDecimal aPagar = somar(todos, TipoLancamento.SAIDA, false);
        BigDecimal saldo = entradas.subtract(saidas);

        return new ResumoResponse(entradas, saidas, saldo, aReceber, aPagar,
                saldo.add(aReceber).subtract(aPagar));
    }

    /** Considera todas as saídas do período, pagas ou previstas. */
    public List<CategoriaResumoResponse> gastosPorCategoria(YearMonth mes) {
        List<Lancamento> saidas = mes == null
                ? repository.findByTipo(TipoLancamento.SAIDA)
                : repository.findByTipoAndDataBetween(TipoLancamento.SAIDA, mes.atDay(1), mes.atEndOfMonth());

        Map<String, BigDecimal> totaisPorCategoria = new LinkedHashMap<>();
        BigDecimal totalGeral = BigDecimal.ZERO;

        for (Lancamento lancamento : saidas) {
            totaisPorCategoria.merge(lancamento.getCategoria(), lancamento.getValor(), BigDecimal::add);
            totalGeral = totalGeral.add(lancamento.getValor());
        }

        BigDecimal totalFinal = totalGeral;
        return totaisPorCategoria.entrySet().stream()
                .map(entry -> new CategoriaResumoResponse(
                        entry.getKey(),
                        entry.getValue(),
                        totalFinal.signum() == 0
                                ? 0.0
                                : entry.getValue()
                                        .multiply(BigDecimal.valueOf(100))
                                        .divide(totalFinal, 1, RoundingMode.HALF_UP)
                                        .doubleValue()))
                .sorted(Comparator.comparing(CategoriaResumoResponse::total).reversed())
                .toList();
    }

    private Lancamento buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lançamento não encontrado"));
    }

    private static void preencher(Lancamento lancamento, LancamentoRequest request, LocalDate data) {
        lancamento.setDescricao(request.descricao().trim());
        lancamento.setTipo(request.tipo());
        lancamento.setCategoria(request.categoria());
        lancamento.setValor(request.valor());
        lancamento.setData(data);
    }

    private static BigDecimal somar(List<Lancamento> lancamentos, TipoLancamento tipo, boolean pago) {
        return lancamentos.stream()
                .filter(l -> l.getTipo() == tipo && l.isPago() == pago)
                .map(Lancamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static LancamentoResponse paraResponse(Lancamento lancamento) {
        return new LancamentoResponse(
                lancamento.getId(),
                lancamento.getDescricao(),
                lancamento.getTipo(),
                lancamento.getCategoria(),
                lancamento.getValor(),
                lancamento.getData(),
                lancamento.isPago());
    }
}
