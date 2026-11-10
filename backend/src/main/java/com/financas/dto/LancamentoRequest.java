package com.financas.dto;

import com.financas.model.TipoLancamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoRequest(
        @NotBlank(message = "Descrição é obrigatória") String descricao,
        @NotNull(message = "Tipo é obrigatório") TipoLancamento tipo,
        @NotBlank(message = "Categoria é obrigatória") String categoria,
        @NotNull(message = "Valor é obrigatório") @Positive(message = "Valor deve ser maior que zero") BigDecimal valor,
        @NotNull(message = "Data é obrigatória") LocalDate data
) {
}
