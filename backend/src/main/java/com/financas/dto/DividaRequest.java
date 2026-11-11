package com.financas.dto;

import com.financas.model.TipoDivida;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param tipo     ausente = DEBITO
 * @param data     vencimento (da 1ª parcela, se houver parcelas)
 * @param parcelas só na criação: cria uma dívida por mês, "descrição (i/N)", cada uma com {@code valor}. null = 1
 */
public record DividaRequest(
        @NotBlank(message = "Nome da pessoa é obrigatório") @Size(max = 80, message = "Nome muito longo") String pessoa,
        TipoDivida tipo,
        @Size(max = 40, message = "Nome da instituição muito longo") String banco,
        @NotBlank(message = "Descrição é obrigatória") String descricao,
        @NotNull(message = "Valor é obrigatório") @Positive(message = "Valor deve ser maior que zero") BigDecimal valor,
        @NotNull(message = "Vencimento é obrigatório") LocalDate data,
        @Min(value = 1, message = "Parcelas deve ser entre 1 e 48")
        @Max(value = 48, message = "Parcelas deve ser entre 1 e 48") Integer parcelas
) {
}
