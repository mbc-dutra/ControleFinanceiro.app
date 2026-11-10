package com.financas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DividaResponse(
        Long id,
        String pessoa,
        String descricao,
        BigDecimal valor,
        LocalDate data,
        boolean quitada,
        LocalDate dataQuitacao
) {
}
