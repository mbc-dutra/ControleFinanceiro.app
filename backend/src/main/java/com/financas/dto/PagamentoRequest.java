package com.financas.dto;

import jakarta.validation.constraints.NotNull;

public record PagamentoRequest(@NotNull(message = "Informe se está pago") Boolean pago) {
}
