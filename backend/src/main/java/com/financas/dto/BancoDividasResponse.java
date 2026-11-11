package com.financas.dto;

import java.math.BigDecimal;
import java.util.List;

/** Dívidas de uma pessoa num mesmo banco. {@code banco} nulo = sem banco informado. */
public record BancoDividasResponse(
        String banco,
        BigDecimal totalAberto,
        List<DividaResponse> dividas
) {
}
