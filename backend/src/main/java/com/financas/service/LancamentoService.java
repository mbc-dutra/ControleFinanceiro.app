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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LancamentoService {

    private final LancamentoRepository repository;

    public LancamentoService(LancamentoRepository repository) {
        this.repository = repository;
    }

    /** Em todos os métodos de consulta, {@code mes == null} significa "todos os meses". */
    public List<LancamentoResponse> listar(TipoLancamento tipo, YearMonth mes) {
        List<Lancamento> lancamentos;
        if (mes == null) {
            lancamentos = tipo == null
                    ? repository.findAllByOrderByDataDescIdDesc()
                    : repository.findByTipoOrderByDataDescIdDesc(tipo);
        } else {
            lancamentos = tipo == null
                    ? repository.findByDataBetweenOrderByDataDescIdDesc(mes.atDay(1), mes.atEndOfMonth())
                    : repository.findByTipoAndDataBetweenOrderByDataDescIdDesc(tipo, mes.atDay(1), mes.atEndOfMonth());
        }
        return lancamentos.stream().map(this::paraResponse).toList();
    }

    public LancamentoResponse criar(LancamentoRequest request) {
        Lancamento lancamento = new Lancamento();
        lancamento.setDescricao(request.descricao());
        lancamento.setTipo(request.tipo());
        lancamento.setCategoria(request.categoria());
        lancamento.setValor(request.valor());
        lancamento.setData(request.data());
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

        BigDecimal entradas = somar(todos, TipoLancamento.ENTRADA);
        BigDecimal saidas = somar(todos, TipoLancamento.SAIDA);

        return new ResumoResponse(entradas, saidas, entradas.subtract(saidas));
    }

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

    private static BigDecimal somar(List<Lancamento> lancamentos, TipoLancamento tipo) {
        return lancamentos.stream()
                .filter(l -> l.getTipo() == tipo)
                .map(Lancamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private LancamentoResponse paraResponse(Lancamento lancamento) {
        return new LancamentoResponse(
                lancamento.getId(),
                lancamento.getDescricao(),
                lancamento.getTipo(),
                lancamento.getCategoria(),
                lancamento.getValor(),
                lancamento.getData());
    }
}
