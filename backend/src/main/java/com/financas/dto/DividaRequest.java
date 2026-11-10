package com.financas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DividaRequest(
        @NotBlank(message = "Nome da pessoa é obrigatório") @Size(max = 80, message = "Nome muito longo") String pessoa,
        @NotBlank(message = "Descrição é obrigatória") String descricao,
        @NotNull(message = "Valor é obrigatório") @Positive(message = "Valor deve ser maior que zero") BigDecimal valor,
        @NotNull(message = "Data é obrigatória") LocalDate data
) {
}
