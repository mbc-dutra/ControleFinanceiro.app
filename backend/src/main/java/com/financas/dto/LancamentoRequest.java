package com.financas.dto;

import com.financas.model.TipoLancamento;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param instituicao  banco/cartão/conta (opcional; em branco = nenhuma)
 * @param data         vencimento (saída) ou data prevista (entrada)
 * @param pago         null = decide pela data (data futura = previsto)
 * @param repetirMeses só na criação: cria uma cópia por mês a partir de {@code data}. null = 1
 */
public record LancamentoRequest(
        @NotBlank(message = "Descrição é obrigatória") String descricao,
        @NotNull(message = "Tipo é obrigatório") TipoLancamento tipo,
        @NotBlank(message = "Categoria é obrigatória") String categoria,
        @Size(max = 40, message = "Nome da instituição muito longo") String instituicao,
        @NotNull(message = "Valor é obrigatório") @Positive(message = "Valor deve ser maior que zero") BigDecimal valor,
        @NotNull(message = "Data é obrigatória") LocalDate data,
        Boolean pago,
        @Min(value = 1, message = "Repetir deve ser entre 1 e 36 meses")
        @Max(value = 36, message = "Repetir deve ser entre 1 e 36 meses") Integer repetirMeses
) {
}
