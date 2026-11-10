package com.financas.dto;

import java.math.BigDecimal;
import java.util.List;

/** Dívidas agrupadas por pessoa: em aberto primeiro, depois as quitadas. */
public record PessoaDividasResponse(
        String pessoa,
        BigDecimal totalAberto,
        List<DividaResponse> dividas
) {
}
