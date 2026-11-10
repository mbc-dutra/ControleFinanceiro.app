package com.financas.service;

import com.financas.dto.CategoriaResumoResponse;
import com.financas.dto.ResumoResponse;
import com.financas.model.Lancamento;
import com.financas.model.TipoLancamento;
import com.financas.repository.LancamentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    @Mock
    LancamentoRepository repository;

    @InjectMocks
    LancamentoService service;

    private static final LocalDate INICIO_NOV = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIM_NOV = LocalDate.of(2026, 11, 30);

    @Test
    void resumoDoMesConsideraApenasLancamentosDoMes() {
        when(repository.findByDataBetween(INICIO_NOV, FIM_NOV)).thenReturn(List.of(
                lancamento(TipoLancamento.ENTRADA, "Renda", "3000.00", "2026-11-10"),
                lancamento(TipoLancamento.SAIDA, "Moradia", "1200.00", "2026-11-12")));

        ResumoResponse resumo = service.resumo(YearMonth.of(2026, 11));

        assertThat(resumo.entradas()).isEqualByComparingTo("3000.00");
        assertThat(resumo.saidas()).isEqualByComparingTo("1200.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("1800.00");
    }

    @Test
    void resumoSemMesConsideraTodosOsLancamentos() {
        when(repository.findAll()).thenReturn(List.of(
                lancamento(TipoLancamento.ENTRADA, "Renda", "3000.00", "2026-10-05"),
                lancamento(TipoLancamento.ENTRADA, "Renda", "3000.00", "2026-11-10"),
                lancamento(TipoLancamento.SAIDA, "Moradia", "1200.00", "2026-11-12")));

        ResumoResponse resumo = service.resumo(null);

        assertThat(resumo.entradas()).isEqualByComparingTo("6000.00");
        assertThat(resumo.saidas()).isEqualByComparingTo("1200.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("4800.00");
    }

    @Test
    void listarComMesUsaPrimeiroEUltimoDiaDoMes() {
        service.listar(null, YearMonth.of(2026, 2));

        verify(repository).findByDataBetweenOrderByDataDescIdDesc(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
    }

    @Test
    void gastosPorCategoriaDoMesCalculaPercentual() {
        when(repository.findByTipoAndDataBetween(TipoLancamento.SAIDA, INICIO_NOV, FIM_NOV)).thenReturn(List.of(
                lancamento(TipoLancamento.SAIDA, "Moradia", "750.00", "2026-11-05"),
                lancamento(TipoLancamento.SAIDA, "Alimentação", "250.00", "2026-11-06")));

        List<CategoriaResumoResponse> categorias = service.gastosPorCategoria(YearMonth.of(2026, 11));

        assertThat(categorias).hasSize(2);
        assertThat(categorias.get(0).categoria()).isEqualTo("Moradia");
        assertThat(categorias.get(0).percentual()).isEqualTo(75.0);
        assertThat(categorias.get(1).percentual()).isEqualTo(25.0);
    }

    private static Lancamento lancamento(TipoLancamento tipo, String categoria, String valor, String data) {
        Lancamento l = new Lancamento();
        l.setDescricao(categoria);
        l.setTipo(tipo);
        l.setCategoria(categoria);
        l.setValor(new BigDecimal(valor));
        l.setData(LocalDate.parse(data));
        return l;
    }
}
