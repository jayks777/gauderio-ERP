package br.com.gauderio.model;

import java.time.LocalDate;

/** Movimentação manual de entrada ou saída de saldo de uma conta bancária. */
public class MovimentacaoSaldo {

    public static final String ENTRADA = "ENTRADA";
    public static final String SAIDA = "SAIDA";

    private int id;
    private int idConta;
    private String tipo;
    private String descricao;
    private double valor;
    private LocalDate data;

    public MovimentacaoSaldo() {
    }

    public MovimentacaoSaldo(int idConta, String tipo, String descricao, double valor, LocalDate data) {
        this.idConta = idConta;
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdConta() { return idConta; }
    public void setIdConta(int idConta) { this.idConta = idConta; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
}