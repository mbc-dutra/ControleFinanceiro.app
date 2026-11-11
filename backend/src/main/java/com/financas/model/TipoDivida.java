package com.financas.model;

/**
 * DEBITO: o dinheiro emprestado era do usuário; ao receber, vira entrada (Reembolso) no saldo.
 * CREDITO: saiu de cartão/crédito/empréstimo do usuário; a pessoa só está pagando essa conta,
 * então receber e pagar a instituição são marcados à parte e nada entra no saldo.
 */
public enum TipoDivida {
    CREDITO,
    DEBITO
}
