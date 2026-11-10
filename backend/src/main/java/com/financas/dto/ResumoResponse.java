package com.financas.dto;

import java.math.BigDecimal;

/**
 * Valores realizados (já pagos/recebidos) separados dos previstos.
 *
 * @param entradas      entradas já recebidas
 * @param saidas        saídas já pagas
 * @param saldo         entradas - saidas (só o que já aconteceu)
 * @param aReceber      entradas previstas, ainda não recebidas
 * @param aPagar        saídas previstas, ainda não pagas
 * @param saldoPrevisto saldo + aReceber - aPagar
 */
public record ResumoResponse(
        BigDecimal entradas,
        BigDecimal saidas,
        BigDecimal saldo,
        BigDecimal aReceber,
        BigDecimal aPagar,
        BigDecimal saldoPrevisto
) {
}
