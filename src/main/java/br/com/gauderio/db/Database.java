package br.com.gauderio.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Banco de dados SQLite do Gauderio-ERP.
 * O arquivo {@code gauderio.db} é criado na pasta de trabalho do projeto,
 * permitindo visualizar/copiar os dados para backups e consultas futuras.
 */
public final class Database {

    private static final String DB_FILE = System.getProperty("user.dir")
            + File.separator + "gauderio.db";

    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    private static Connection conexao;

    private Database() {
    }

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignorado) {
            // Driver registrado via SPI quando disponível
        }
    }

    public static Connection getConnection() throws SQLException {
        if (conexao == null || conexao.isClosed()) {
            conexao = DriverManager.getConnection(DB_URL);
            try (Statement st = conexao.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
        }
        return conexao;
    }

    /** Cria as tabelas (se ainda não existirem) e semeia categorias padrão. */
    public static void init() {

        String[] schemas = {
                """
                CREATE TABLE IF NOT EXISTS contas (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome          TEXT    NOT NULL UNIQUE,
                    banco         TEXT    NOT NULL DEFAULT '',
                    agencia       TEXT    NOT NULL DEFAULT '',
                    numero        TEXT    NOT NULL DEFAULT '',
                    saldo_inicial REAL    NOT NULL DEFAULT 0
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS categorias (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT    NOT NULL UNIQUE,
                    tipo TEXT    NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS transacoes (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    tipo       TEXT    NOT NULL,
                    descricao  TEXT    NOT NULL,
                    valor      REAL    NOT NULL,
                    categoria  TEXT    NOT NULL DEFAULT '',
                    conta      TEXT    NOT NULL DEFAULT '',
                    data       TEXT    NOT NULL,
                    vencimento TEXT,
                    status     TEXT    NOT NULL DEFAULT 'PENDENTE',
                    recorrente INTEGER NOT NULL DEFAULT 0,
                    frequencia TEXT,
                    criado_em  TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS movimentacoes_saldo (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_conta  INTEGER NOT NULL REFERENCES contas(id) ON DELETE CASCADE,
                    tipo      TEXT    NOT NULL,
                    descricao TEXT    NOT NULL,
                    valor     REAL    NOT NULL,
                    data      TEXT    NOT NULL
                )
                """
        };

        try (Connection conn = getConnection(); Statement st = conn.createStatement()) {
            for (String sql : schemas) {
                st.execute(sql);
            }
            semearCategorias(st);
        } catch (SQLException ex) {
            throw new RuntimeException("Não foi possível inicializar o banco de dados: " + ex.getMessage(), ex);
        }
    }

    private static void semearCategorias(Statement st) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM categorias")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }
        st.executeUpdate("""
                INSERT INTO categorias (nome, tipo) VALUES
                    ('Salário',             'RECEITA'),
                    ('Vendas',              'RECEITA'),
                    ('Serviços',            'RECEITA'),
                    ('Investimentos',       'RECEITA'),
                    ('Outras receitas',     'RECEITA'),
                    ('Aluguel',             'DESPESA'),
                    ('Alimentação',         'DESPESA'),
                    ('Transporte',          'DESPESA'),
                    ('Moradia',             'DESPESA'),
                    ('Energia elétrica',    'DESPESA'),
                    ('Água',                'DESPESA'),
                    ('Telefone / Internet', 'DESPESA'),
                    ('Impostos',            'DESPESA'),
                    ('Lazer',               'DESPESA'),
                    ('Saúde',               'DESPESA'),
                    ('Educação',            'DESPESA'),
                    ('Outras despesas',     'DESPESA')
                """);
    }

    public static String getDbPath() {
        return DB_FILE;
    }
}