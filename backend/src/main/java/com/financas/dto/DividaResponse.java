package com.financas.dto;

import com.financas.model.TipoDivida;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DividaResponse(
        Long id,
        String pessoa,
        TipoDivida tipo,
        String banco,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        boolean quitada,
        LocalDate dataQuitacao,
        LocalDate dataRecebimento,
        LocalDate dataPagamento
) {
}
