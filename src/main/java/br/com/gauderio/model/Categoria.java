package br.com.gauderio.model;

/** Categoria associada a receitas ou despesas. */
public class Categoria {

    public static final String TIPO_RECEITA = "RECEITA";
    public static final String TIPO_DESPESA = "DESPESA";

    private int id;
    private String nome;
    private String tipo;

    public Categoria() {
    }

    public Categoria(String nome, String tipo) {
        this.nome = nome;
        this.tipo = tipo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    @Override
    public String toString() {
        return nome;
    }
}