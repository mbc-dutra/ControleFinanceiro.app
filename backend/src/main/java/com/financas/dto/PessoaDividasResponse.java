package com.financas.dto;

import java.math.BigDecimal;
import java.util.List;

/** Dívidas agrupadas por pessoa → tipo (crédito/débito) → instituição. */
public record PessoaDividasResponse(
        String pessoa,
        BigDecimal totalAberto,
        List<TipoDividasResponse> tipos
) {
}
