package br.com.gauderio.model;

/** Conta bancária controlada pelo sistema (controle das contas do banco). */
public class ContaBancaria {

    private int id;
    private String nome;
    private String banco;
    private String agencia;
    private String numero;
    private double saldoInicial;

    public ContaBancaria() {
    }

    public ContaBancaria(String nome, String banco, String agencia, String numero, double saldoInicial) {
        this.nome = nome;
        this.banco = banco;
        this.agencia = agencia;
        this.numero = numero;
        this.saldoInicial = saldoInicial;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getBanco() { return banco; }
    public void setBanco(String banco) { this.banco = banco; }

    public String getAgencia() { return agencia; }
    public void setAgencia(String agencia) { this.agencia = agencia; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public double getSaldoInicial() { return saldoInicial; }
    public void setSaldoInicial(double saldoInicial) { this.saldoInicial = saldoInicial; }

    @Override
    public String toString() {
        return nome;
    }
}