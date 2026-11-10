package com.financas.controller;

import com.financas.dto.ResumoResponse;
import com.financas.service.LancamentoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LancamentoController.class)
class LancamentoControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    LancamentoService service;

    @Test
    void resumoAceitaMesNoFormatoAnoMes() throws Exception {
        when(service.resumo(YearMonth.of(2026, 11))).thenReturn(new ResumoResponse(
                new BigDecimal("3000.00"), new BigDecimal("1200.00"), new BigDecimal("1800.00")));

        mvc.perform(get("/api/lancamentos/resumo").param("mes", "2026-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(1800.00));
    }

    @Test
    void mesInvalidoRetorna400ComMensagem() throws Exception {
        mvc.perform(get("/api/lancamentos/resumo").param("mes", "2026-13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Parâmetro 'mes' inválido. Use o formato AAAA-MM, ex.: 2026-11"));
    }
}
