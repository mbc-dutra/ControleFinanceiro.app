package com.financas.dto;

import com.financas.model.TipoLancamento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoResponse(
        Long id,
        String descricao,
        TipoLancamento tipo,
        String categoria,
        String instituicao,
        BigDecimal valor,
        LocalDate data,
        boolean pago
) {
}
