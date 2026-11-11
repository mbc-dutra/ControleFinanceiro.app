package com.financas.service;

import com.financas.dto.CategoriaResumoResponse;
import com.financas.dto.LancamentoRequest;
import com.financas.dto.LancamentoResponse;
import com.financas.dto.ResumoResponse;
import com.financas.model.Lancamento;
import com.financas.model.TipoLancamento;
import com.financas.repository.LancamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    /** "Hoje" fixo nos testes: 10/11/2026. */
    private static final Clock HOJE_10_NOV = Clock.fixed(
            LocalDate.of(2026, 11, 10).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant(),
            ZoneId.of("America/Sao_Paulo"));

    private static final LocalDate INICIO_NOV = LocalDate.of(2026, 11, 1);
    private static final LocalDate FIM_NOV = LocalDate.of(2026, 11, 30);

    @Mock
    LancamentoRepository repository;

    LancamentoService service;

    @BeforeEach
    void setUp() {
        service = new LancamentoService(repository, HOJE_10_NOV);
    }

    // ---------- resumo: realizado x previsto ----------

    @Test
    void salarioFuturoFicaComoAReceberENaoEntraNoSaldo() {
        when(repository.findByDataBetween(INICIO_NOV, FIM_NOV)).thenReturn(List.of(
                lancamento(TipoLancamento.ENTRADA, "Renda", "3000.00", "2026-11-30", false),
                lancamento(TipoLancamento.SAIDA, "Moradia", "1200.00", "2026-11-05", true),
                lancamento(TipoLancamento.SAIDA, "Contas", "200.00", "2026-11-20", false)));

        ResumoResponse resumo = service.resumo(YearMonth.of(2026, 11));

        assertThat(resumo.entradas()).isEqualByComparingTo("0");
        assertThat(resumo.saidas()).isEqualByComparingTo("1200.00");
        assertThat(resumo.saldo()).isEqualByComparingTo("-1200.00");
        assertThat(resumo.aReceber()).isEqualByComparingTo("3000.00");
        assertThat(resumo.aPagar()).isEqualByComparingTo("200.00");
        assertThat(resumo.saldoPrevisto()).isEqualByComparingTo("1600.00");
    }

    @Test
    void resumoSemMesConsideraTodosOsLancamentos() {
        when(repository.findAll()).thenReturn(List.of(
                lancamento(TipoLancamento.ENTRADA, "Renda", "3000.00", "2026-10-05", true),
                lancamento(TipoLancamento.ENTRADA, "Renda", "3000.00", "2026-11-10", true),
                lancamento(TipoLancamento.SAIDA, "Moradia", "1200.00", "2026-11-12", true)));

        ResumoResponse resumo = service.resumo(null);

        assertThat(resumo.saldo()).isEqualByComparingTo("4800.00");
        assertThat(resumo.saldoPrevisto()).isEqualByComparingTo("4800.00");
    }

    // ---------- criação: status padrão e repetição ----------

    @Test
    void semInformarPagoDataFuturaNasceComoPrevistaEDataPassadaComoPaga() {
        devolverOQueSalvar();

        LancamentoResponse futuro = service.criar(request("2026-11-30", null, null)).get(0);
        LancamentoResponse hoje = service.criar(request("2026-11-10", null, null)).get(0);

        assertThat(futuro.pago()).isFalse();
        assertThat(hoje.pago()).isTrue();
    }

    @Test
    void repetirPorMesesCriaUmaCopiaPorMesAjustandoODia31() {
        devolverOQueSalvar();

        List<LancamentoResponse> criados = service.criar(request("2027-01-31", null, 4));

        assertThat(criados).extracting(LancamentoResponse::data).containsExactly(
                LocalDate.of(2027, 1, 31),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 31),
                LocalDate.of(2027, 4, 30));
        assertThat(criados).allMatch(l -> !l.pago());
    }

    @Test
    void instituicaoVaiParaTodasAsCopiasEEmBrancoViraNenhuma() {
        devolverOQueSalvar();

        List<LancamentoResponse> comInstituicao = service.criar(new LancamentoRequest("Fatura", TipoLancamento.SAIDA,
                "Contas", " Nubank ", new BigDecimal("300.00"), LocalDate.of(2026, 12, 10), null, 3));
        List<LancamentoResponse> emBranco = service.criar(new LancamentoRequest("Luz", TipoLancamento.SAIDA,
                "Contas", "  ", new BigDecimal("90.00"), LocalDate.of(2026, 12, 10), null, null));

        assertThat(comInstituicao).extracting(LancamentoResponse::instituicao).containsOnly("Nubank").hasSize(3);
        assertThat(emBranco.get(0).instituicao()).isNull();
    }

    @Test
    void repetirAPartirDeHojeMarcaSoOPrimeiroComoPago() {
        devolverOQueSalvar();

        List<LancamentoResponse> criados = service.criar(request("2026-11-05", true, 3));

        assertThat(criados).extracting(LancamentoResponse::pago).containsExactly(true, false, false);
    }

    // ---------- edição ----------

    @Test
    void marcarPagoAlteraSoOStatus() {
        Lancamento salario = lancamento(TipoLancamento.ENTRADA, "Renda", "3000.00", "2026-11-30", false);
        when(repository.findById(7L)).thenReturn(Optional.of(salario));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LancamentoResponse resposta = service.marcarPago(7L, true);

        assertThat(resposta.pago()).isTrue();
        assertThat(resposta.valor()).isEqualByComparingTo("3000.00");
    }

    @Test
    void atualizarSemInformarPagoMantemOStatusAtual() {
        Lancamento conta = lancamento(TipoLancamento.SAIDA, "Contas", "200.00", "2026-11-20", true);
        when(repository.findById(3L)).thenReturn(Optional.of(conta));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LancamentoResponse resposta = service.atualizar(3L, new LancamentoRequest(
                "Luz", TipoLancamento.SAIDA, "Contas", null, new BigDecimal("230.00"), LocalDate.of(2026, 11, 25), null, null));

        assertThat(resposta.valor()).isEqualByComparingTo("230.00");
        assertThat(resposta.pago()).isTrue();
    }

    // ---------- listagem e categorias ----------

    @Test
    void listarComMesUsaPrimeiroEUltimoDiaDoMes() {
        service.listar(null, YearMonth.of(2026, 2));

        verify(repository).findByDataBetweenOrderByDataAscIdAsc(
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
    }

    @Test
    void gastosPorCategoriaDoMesCalculaPercentual() {
        when(repository.findByTipoAndDataBetween(TipoLancamento.SAIDA, INICIO_NOV, FIM_NOV)).thenReturn(List.of(
                lancamento(TipoLancamento.SAIDA, "Moradia", "750.00", "2026-11-05", true),
                lancamento(TipoLancamento.SAIDA, "Alimentação", "250.00", "2026-11-06", false)));

        List<CategoriaResumoResponse> categorias = service.gastosPorCategoria(YearMonth.of(2026, 11));

        assertThat(categorias).extracting(CategoriaResumoResponse::categoria).containsExactly("Moradia", "Alimentação");
        assertThat(categorias).extracting(CategoriaResumoResponse::percentual).containsExactly(75.0, 25.0);
    }

    // ---------- auxiliares ----------

    @SuppressWarnings("unchecked")
    private void devolverOQueSalvar() {
        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static LancamentoRequest request(String data, Boolean pago, Integer repetirMeses) {
        return new LancamentoRequest("Salário", TipoLancamento.ENTRADA, "Renda", null,
                new BigDecimal("3000.00"), LocalDate.parse(data), pago, repetirMeses);
    }

    private static Lancamento lancamento(TipoLancamento tipo, String categoria, String valor, String data, boolean pago) {
        Lancamento l = new Lancamento();
        l.setDescricao(categoria);
        l.setTipo(tipo);
        l.setCategoria(categoria);
        l.setValor(new BigDecimal(valor));
        l.setData(LocalDate.parse(data));
        l.setPago(pago);
        return l;
    }
}
