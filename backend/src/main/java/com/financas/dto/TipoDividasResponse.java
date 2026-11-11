package com.financas.dto;

import com.financas.model.TipoDivida;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dívidas de uma pessoa de um mesmo tipo, separadas por instituição.
 * {@code totalAberto}: o que a pessoa ainda não pagou. {@code totalAPagar}: no crédito, o que o
 * usuário ainda não pagou à instituição (zero no débito).
 */
public record TipoDividasResponse(
        TipoDivida tipo,
        BigDecimal totalAberto,
        BigDecimal totalAPagar,
        List<BancoDividasResponse> bancos
) {
}
