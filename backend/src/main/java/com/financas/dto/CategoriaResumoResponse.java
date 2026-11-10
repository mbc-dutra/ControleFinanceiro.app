package com.financas.dto;

import java.math.BigDecimal;

public record CategoriaResumoResponse(
        String categoria,
        BigDecimal total,
        double percentual
) {
}
