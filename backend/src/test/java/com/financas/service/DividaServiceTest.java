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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DividaServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 10);
    private static final Clock RELOGIO = Clock.fixed(
            HOJE.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant(), ZoneId.of("America/Sao_Paulo"));

    @Mock
    DividaRepository dividas;

    @Mock
    LancamentoRepository lancamentos;

    DividaService service;

    @BeforeEach
    void setUp() {
        service = new DividaService(dividas, lancamentos, RELOGIO);
    }

    @Test
    void agrupaPorPessoaSemDiferenciarMaiusculasESomaSoAsAbertas() {
        when(dividas.findAllByOrderByDataDescIdDesc()).thenReturn(List.of(
                divida(1L, "joão", "Pizza", "50.00", false),   // mais recente, digitado em minúsculas
                divida(2L, "Ana", "Uber", "30.00", false),
                divida(3L, "joão ", "Cinema", "40.00", false),
                divida(4L, "João", "Show", "200.00", true)));  // mais antigo: o nome exibido vem daqui

        List<PessoaDividasResponse> pessoas = service.listarPorPessoa();

        assertThat(pessoas).extracting(PessoaDividasResponse::pessoa).containsExactly("João", "Ana");
        PessoaDividasResponse joao = pessoas.get(0);
        assertThat(joao.totalAberto()).isEqualByComparingTo("90.00");
        assertThat(joao.tipos()).extracting(TipoDividasResponse::tipo).containsExactly(TipoDivida.DEBITO);
        assertThat(joao.tipos().get(0).bancos().get(0).dividas()).extracting(DividaResponse::descricao)
                .containsExactly("Pizza", "Cinema", "Show"); // abertas primeiro, quitada por último
    }

    @Test
    void separaAsDividasDaPessoaPorBancoSemDiferenciarMaiusculas() {
        when(dividas.findAllByOrderByDataDescIdDesc()).thenReturn(List.of(
                divida(1L, "Aline", "nubank", "Pix", "20.00", false),
                divida(2L, "Aline", null, "Lanche", "500.00", false),
                divida(3L, "Aline", "Inter", "Mercado", "30.00", false),
                divida(4L, "Aline", "NUBANK", "Deposito", "100.00", false),  // mais antigo: nome exibido
                divida(5L, "Aline", "Inter", "Show", "80.00", true)));

        PessoaDividasResponse aline = service.listarPorPessoa().get(0);

        assertThat(aline.totalAberto()).isEqualByComparingTo("650.00");
        List<BancoDividasResponse> bancos = aline.tipos().get(0).bancos();
        assertThat(bancos).extracting(BancoDividasResponse::banco)
                .containsExactly("NUBANK", "Inter", null); // maior total primeiro, sem banco por último
        BancoDividasResponse nubank = bancos.get(0);
        assertThat(nubank.totalAberto()).isEqualByComparingTo("120.00");
        assertThat(nubank.dividas()).extracting(DividaResponse::descricao).containsExactly("Pix", "Deposito");
        BancoDividasResponse inter = bancos.get(1);
        assertThat(inter.totalAberto()).isEqualByComparingTo("30.00"); // a quitada não soma
        assertThat(inter.dividas()).extracting(DividaResponse::descricao).containsExactly("Mercado", "Show");
    }

    @Test
    void bancoEmBrancoViraSemBanco() {
        devolverOQueSalvar();

        DividaResponse sem = service.criar(new DividaRequest("Aline", null, "   ", "Pix", new BigDecimal("10.00"), HOJE, null)).get(0);
        DividaResponse com = service.criar(new DividaRequest("Aline", null, " Nubank ", "Pix", new BigDecimal("10.00"), HOJE, null)).get(0);

        assertThat(sem.banco()).isNull();
        assertThat(com.banco()).isEqualTo("Nubank");
        assertThat(sem.tipo()).isEqualTo(TipoDivida.DEBITO); // sem tipo = débito
    }

    @Test
    void parcelasCriamUmaDividaPorMesComNumeroNaDescricao() {
        devolverOQueSalvar();

        List<DividaResponse> criadas = service.criar(new DividaRequest("Aline", TipoDivida.CREDITO, "Shopee",
                "Fone", new BigDecimal("25.00"), LocalDate.of(2027, 1, 31), 3));

        assertThat(criadas).extracting(DividaResponse::descricao)
                .containsExactly("Fone (1/3)", "Fone (2/3)", "Fone (3/3)");
        assertThat(criadas).extracting(DividaResponse::data).containsExactly(
                LocalDate.of(2027, 1, 31), LocalDate.of(2027, 2, 28), LocalDate.of(2027, 3, 31));
        assertThat(criadas).allSatisfy(d -> {
            assertThat(d.valor()).isEqualByComparingTo("25.00");
            assertThat(d.tipo()).isEqualTo(TipoDivida.CREDITO);
            assertThat(d.banco()).isEqualTo("Shopee");
        });
    }

    @Test
    void abertasAparecemPorVencimentoMaisProximo() {
        Divida maio = divida(1L, "Aline", "Parcela 3", "10.00", false);
        maio.setData(LocalDate.of(2027, 5, 10));
        Divida marco = divida(2L, "Aline", "Parcela 1", "10.00", false);
        marco.setData(LocalDate.of(2027, 3, 10));
        Divida quitada = divida(3L, "Aline", "Antiga", "10.00", true);
        quitada.setData(LocalDate.of(2027, 1, 10));
        when(dividas.findAllByOrderByDataDescIdDesc()).thenReturn(List.of(maio, marco, quitada));

        List<DividaResponse> lista = service.listarPorPessoa().get(0).tipos().get(0).bancos().get(0).dividas();

        assertThat(lista).extracting(DividaResponse::descricao).containsExactly("Parcela 1", "Parcela 3", "Antiga");
    }

    @Test
    void separaCreditoDeDebitoDentroDaPessoa() {
        Divida shopee = credito(1L, "Aline", "Shopee 3/10", "20.00");
        Divida cartao = credito(2L, "Aline", "Cartão", "70.00");
        cartao.setDataRecebimento(HOJE);                    // já recebeu, falta pagar a fatura
        Divida parcela = credito(3L, "Aline", "Parcela", "40.00");
        parcela.setDataPagamento(HOJE);                     // já paguei a parcela, ela ainda não me pagou
        Divida deposito = divida(4L, "Aline", "Deposito", "100.00", false);
        deposito.setTipo(null);                             // registro antigo, sem tipo
        when(dividas.findAllByOrderByDataDescIdDesc()).thenReturn(List.of(shopee, cartao, parcela, deposito));

        PessoaDividasResponse aline = service.listarPorPessoa().get(0);

        assertThat(aline.totalAberto()).isEqualByComparingTo("160.00"); // 20 + 40 + 100
        assertThat(aline.tipos()).extracting(TipoDividasResponse::tipo)
                .containsExactly(TipoDivida.CREDITO, TipoDivida.DEBITO);
        TipoDividasResponse credito = aline.tipos().get(0);
        assertThat(credito.totalAberto()).isEqualByComparingTo("60.00");  // ainda não me pagaram
        assertThat(credito.totalAPagar()).isEqualByComparingTo("90.00");  // ainda não paguei a instituição
        TipoDividasResponse debito = aline.tipos().get(1);
        assertThat(debito.totalAberto()).isEqualByComparingTo("100.00");
        assertThat(debito.totalAPagar()).isEqualByComparingTo("0");
    }

    @Test
    void creditoSoFicaQuitadoComRecebimentoEPagamentoESemMexerNoSaldo() {
        Divida shopee = credito(1L, "Aline", "Shopee", "20.00");
        when(dividas.findById(1L)).thenReturn(Optional.of(shopee));
        when(dividas.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DividaResponse recebida = service.receber(1L);
        assertThat(recebida.dataRecebimento()).isEqualTo(HOJE);
        assertThat(recebida.quitada()).isFalse();

        DividaResponse paga = service.pagar(1L);
        assertThat(paga.dataPagamento()).isEqualTo(HOJE);
        assertThat(paga.quitada()).isTrue();
        assertThat(paga.dataQuitacao()).isEqualTo(HOJE);

        DividaResponse desfeita = service.desfazerPagamento(1L);
        assertThat(desfeita.quitada()).isFalse();
        assertThat(desfeita.dataQuitacao()).isNull();
        assertThat(desfeita.dataRecebimento()).isEqualTo(HOJE);

        verify(lancamentos, never()).save(any());
    }

    @Test
    void creditoNaoUsaQuitarEDebitoNaoUsaReceber() {
        when(dividas.findById(1L)).thenReturn(Optional.of(credito(1L, "Aline", "Shopee", "20.00")));
        when(dividas.findById(2L)).thenReturn(Optional.of(divida(2L, "Aline", "Pix", "20.00", false)));

        assertThatThrownBy(() -> service.quitar(1L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.receber(2L)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.pagar(2L)).isInstanceOf(ResponseStatusException.class);
        verify(lancamentos, never()).save(any());
    }

    @Test
    void naoTrocaOTipoDepoisDeMarcarRecebimento() {
        Divida shopee = credito(1L, "Aline", "Shopee", "20.00");
        shopee.setDataRecebimento(HOJE);
        when(dividas.findById(1L)).thenReturn(Optional.of(shopee));

        DividaRequest paraDebito = new DividaRequest("Aline", TipoDivida.DEBITO, null, "Shopee", new BigDecimal("20.00"), HOJE, null);

        assertThatThrownBy(() -> service.atualizar(1L, paraDebito)).isInstanceOf(ResponseStatusException.class);
        verify(dividas, never()).save(any());
    }

    @Test
    void quitarCriaEntradaDeReembolsoComDataDeHoje() {
        Divida pizza = divida(1L, "João", "Pizza", "50.00", false);
        pizza.setBanco("Nubank");
        when(dividas.findById(1L)).thenReturn(Optional.of(pizza));
        when(lancamentos.save(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            l.setId(99L);
            return l;
        });
        when(dividas.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DividaResponse resposta = service.quitar(1L);

        ArgumentCaptor<Lancamento> captor = ArgumentCaptor.forClass(Lancamento.class);
        verify(lancamentos).save(captor.capture());
        Lancamento entrada = captor.getValue();
        assertThat(entrada.getTipo()).isEqualTo(TipoLancamento.ENTRADA);
        assertThat(entrada.getCategoria()).isEqualTo("Reembolso");
        assertThat(entrada.getDescricao()).isEqualTo("João — Pizza");
        assertThat(entrada.getInstituicao()).isEqualTo("Nubank"); // herda a instituição da dívida
        assertThat(entrada.getValor()).isEqualByComparingTo("50.00");
        assertThat(entrada.getData()).isEqualTo(HOJE);
        assertThat(entrada.isPago()).isTrue();

        assertThat(resposta.quitada()).isTrue();
        assertThat(resposta.dataQuitacao()).isEqualTo(HOJE);
        assertThat(pizza.getLancamentoId()).isEqualTo(99L);
    }

    @Test
    void naoQuitaDuasVezes() {
        when(dividas.findById(1L)).thenReturn(Optional.of(divida(1L, "João", "Pizza", "50.00", true)));

        assertThatThrownBy(() -> service.quitar(1L)).isInstanceOf(ResponseStatusException.class);
        verify(lancamentos, never()).save(any());
    }

    @Test
    void reabrirRemoveAEntradaGerada() {
        Divida pizza = divida(1L, "João", "Pizza", "50.00", true);
        pizza.setLancamentoId(99L);
        when(dividas.findById(1L)).thenReturn(Optional.of(pizza));
        when(lancamentos.existsById(99L)).thenReturn(true);
        when(dividas.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DividaResponse resposta = service.reabrir(1L);

        verify(lancamentos).deleteById(99L);
        assertThat(resposta.quitada()).isFalse();
        assertThat(pizza.getLancamentoId()).isNull();
    }

    @SuppressWarnings("unchecked")
    private void devolverOQueSalvar() {
        when(dividas.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Divida divida(Long id, String pessoa, String descricao, String valor, boolean quitada) {
        return divida(id, pessoa, null, descricao, valor, quitada);
    }

    private static Divida credito(Long id, String pessoa, String descricao, String valor) {
        Divida d = divida(id, pessoa, null, descricao, valor, false);
        d.setTipo(TipoDivida.CREDITO);
        return d;
    }

    private static Divida divida(Long id, String pessoa, String banco, String descricao, String valor, boolean quitada) {
        Divida d = new Divida();
        d.setBanco(banco);
        d.setId(id);
        d.setPessoa(pessoa);
        d.setDescricao(descricao);
        d.setValor(new BigDecimal(valor));
        d.setData(LocalDate.of(2026, 11, 1));
        d.setQuitada(quitada);
        return d;
    }
}
