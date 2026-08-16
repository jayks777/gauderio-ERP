package br.com.gauderio.model;

import java.time.LocalDate;

/**
 * Lançamento financeiro: receita ou despesa.
 * Pode ser recorrente (gera parcelas futuras) e/ou futura
 * (boleto a pagar / venda a receber).
 */
public class Transacao {

    public static final String TIPO_RECEITA = "RECEITA";
    public static final String TIPO_DESPESA = "DESPESA";

    public static final String STATUS_PENDENTE = "PENDENTE";
    public static final String STATUS_PAGO = "PAGO";

    private int id;
    private String tipo;
    private String descricao;
    private double valor;
    private String categoria;
    private String conta;
    private LocalDate data;
    private LocalDate vencimento;
    private String status;
    private boolean recorrente;
    private String frequencia;

    public Transacao() {
    }

    public Transacao(String tipo, String descricao, double valor, String categoria, String conta,
                     LocalDate data, LocalDate vencimento, String status,
                     boolean recorrente, String frequencia) {
        this.tipo = tipo;
        this.descricao = descricao;
        this.valor = valor;
        this.categoria = categoria;
        this.conta = conta;
        this.data = data;
        this.vencimento = vencimento;
        this.status = status;
        this.recorrente = recorrente;
        this.frequencia = frequencia;
    }

    /** Data de referência para cobrança/vencimento (grita o vencimento quando informado). */
    public LocalDate getDataReferencia() {
        return vencimento != null ? vencimento : data;
    }

    /** Status exibido: lançamento pendente com vencimento no passado vira VENCIDO. */
    public String getStatusExibido() {
        if (STATUS_PENDENTE.equals(status) && getDataReferencia().isBefore(LocalDate.now())) {
            return "VENCIDO";
        }
        return status;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getConta() { return conta; }
    public void setConta(String conta) { this.conta = conta; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalDate getVencimento() { return vencimento; }
    public void setVencimento(LocalDate vencimento) { this.vencimento = vencimento; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isRecorrente() { return recorrente; }
    public void setRecorrente(boolean recorrente) { this.recorrente = recorrente; }

    public String getFrequencia() { return frequencia; }
    public void setFrequencia(String frequencia) { this.frequencia = frequencia; }
}