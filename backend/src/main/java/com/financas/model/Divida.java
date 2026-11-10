package com.financas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    /** Quando a dívida surgiu. */
    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private boolean quitada = false;

    private LocalDate dataQuitacao;

    /** Entrada criada no controle do usuário quando a dívida foi quitada. */
    private Long lancamentoId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPessoa() { return pessoa; }
    public void setPessoa(String pessoa) { this.pessoa = pessoa; }

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

    public Long getLancamentoId() { return lancamentoId; }
    public void setLancamentoId(Long lancamentoId) { this.lancamentoId = lancamentoId; }
}
