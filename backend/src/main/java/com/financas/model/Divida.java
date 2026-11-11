package com.financas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valor que outra pessoa deve ao usuário. Fica fora do saldo até ser quitado. */
@Entity
@Table(name = "dividas")
public class Divida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String pessoa;

    /** Nulo nos registros anteriores ao campo: valem como DEBITO (ver {@link #getTipo()}). */
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TipoDivida tipo;

    /** Banco/conta por onde o dinheiro vai voltar (texto livre, opcional). */
    @Column(length = 40)
    private String banco;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    /** Vencimento: até quando a pessoa deve pagar (em parcelas, o vencimento daquela parcela). */
    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private boolean quitada = false;

    private LocalDate dataQuitacao;

    /** Só no CREDITO: quando a pessoa pagou o usuário. */
    private LocalDate dataRecebimento;

    /** Só no CREDITO: quando o usuário pagou a instituição (fatura, parcela...). */
    private LocalDate dataPagamento;

    /** Entrada criada no controle do usuário quando a dívida foi quitada. */
    private Long lancamentoId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPessoa() { return pessoa; }
    public void setPessoa(String pessoa) { this.pessoa = pessoa; }

    public TipoDivida getTipo() { return tipo == null ? TipoDivida.DEBITO : tipo; }
    public void setTipo(TipoDivida tipo) { this.tipo = tipo; }

    public boolean isCredito() { return getTipo() == TipoDivida.CREDITO; }

    /** A pessoa já pagou? No débito isso é a própria quitação. */
    public boolean isRecebida() { return isCredito() ? dataRecebimento != null : quitada; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public boolean isQuitada() { return quitada; }
    public void setQuitada(boolean quitada) { this.quitada = quitada; }

    public LocalDate getDataQuitacao() { return dataQuitacao; }
    public void setDataQuitacao(LocalDate dataQuitacao) { this.dataQuitacao = dataQuitacao; }

    public LocalDate getDataRecebimento() { return dataRecebimento; }
    public void setDataRecebimento(LocalDate dataRecebimento) { this.dataRecebimento = dataRecebimento; }

    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }

    public Long getLancamentoId() { return lancamentoId; }
    public void setLancamentoId(Long lancamentoId) { this.lancamentoId = lancamentoId; }
}
