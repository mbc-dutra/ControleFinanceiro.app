package com.financas.dto;

import java.math.BigDecimal;

public record ResumoResponse(
        BigDecimal entradas,
        BigDecimal saidas,
        BigDecimal saldo
) {
}
