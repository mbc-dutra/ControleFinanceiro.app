package com.financas.service;

import com.financas.dto.DividaResponse;
import com.financas.dto.PessoaDividasResponse;
import com.financas.model.Divida;
import com.financas.model.Lancamento;
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
        assertThat(joao.dividas()).extracting(DividaResponse::descricao)
                .containsExactly("Pizza", "Cinema", "Show"); // abertas primeiro, quitada por último
    }

    @Test
    void quitarCriaEntradaDeReembolsoComDataDeHoje() {
        Divida pizza = divida(1L, "João", "Pizza", "50.00", false);
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

    private static Divida divida(Long id, String pessoa, String descricao, String valor, boolean quitada) {
        Divida d = new Divida();
        d.setId(id);
        d.setPessoa(pessoa);
        d.setDescricao(descricao);
        d.setValor(new BigDecimal(valor));
        d.setData(LocalDate.of(2026, 11, 1));
        d.setQuitada(quitada);
        return d;
    }
}
