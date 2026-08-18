package br.com.gauderio.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerenciamento do banco de dados SQLite do Gauderio-ERP.
 *
 * O banco fica separado da pasta de instalação do programa.
 * Isso permite atualizar/reinstalar o aplicativo sem perder os dados.
 */
public final class Database {

    private static final String APP_DIRECTORY = "Gauderio-ERP";

    private static final Path DATA_DIRECTORY = resolveDataDirectory();

    private static final Path DB_PATH =
            DATA_DIRECTORY.resolve("gauderio.db");

    private static final String DB_URL =
            "jdbc:sqlite:" + DB_PATH;

    private static Connection conexao;

    private Database() {
    }

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException ignored) {
            // O driver SQLite normalmente é carregado automaticamente.
        }
    }

    /**
     * Define onde os dados da aplicação serão armazenados.
     *
     * No Windows:
     *
     * C:\Users\Usuario\AppData\Local\Gauderio-ERP
     *
     * Caso LOCALAPPDATA não esteja disponível, utiliza:
     *
     * C:\Users\Usuario\.gauderio-erp
     */
    private static Path resolveDataDirectory() {

        String localAppData = System.getenv("LOCALAPPDATA");

        if (localAppData != null && !localAppData.isBlank()) {
            return Paths.get(localAppData, APP_DIRECTORY);
        }

        return Paths.get(
                System.getProperty("user.home"),
                ".gauderio-erp"
        );
    }

    /**
     * Garante que a pasta de dados exista.
     */
    private static void ensureDataDirectory() throws SQLException {

        try {

            Files.createDirectories(DATA_DIRECTORY);

        } catch (IOException ex) {

            throw new SQLException(
                    "Não foi possível criar a pasta de dados do Gauderio-ERP.",
                    ex
            );
        }
    }

    /**
     * Obtém uma conexão com o banco SQLite.
     */
    public static Connection getConnection() throws SQLException {

        ensureDataDirectory();

        if (conexao == null || conexao.isClosed()) {

            conexao = DriverManager.getConnection(DB_URL);

            try (Statement st = conexao.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
        }

        return conexao;
    }

    /**
     * Inicializa o banco de dados.
     *
     * Cria as tabelas caso ainda não existam.
     */
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
                    criado_em  TEXT NOT NULL
                )
                """,

                """
                CREATE TABLE IF NOT EXISTS movimentacoes_saldo (
                    id        INTEGER PRIMARY KEY AUTOINCREMENT,
                    id_conta  INTEGER NOT NULL
                              REFERENCES contas(id)
                              ON DELETE CASCADE,
                    tipo      TEXT    NOT NULL,
                    descricao TEXT    NOT NULL,
                    valor     REAL    NOT NULL,
                    data      TEXT    NOT NULL
                )
                """
        };

        try (
                Connection conn = getConnection();
                Statement st = conn.createStatement()
        ) {

            for (String sql : schemas) {
                st.execute(sql);
            }

            semearCategorias(st);

        } catch (SQLException ex) {

            throw new RuntimeException(
                    "Não foi possível inicializar o banco de dados: "
                            + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * Insere categorias padrão somente quando o banco está vazio.
     */
    private static void semearCategorias(Statement st)
            throws SQLException {

        try (
                ResultSet rs =
                        st.executeQuery(
                                "SELECT COUNT(*) FROM categorias"
                        )
        ) {

            rs.next();

            if (rs.getInt(1) > 0) {
                return;
            }
        }

        st.executeUpdate(
                """
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
                """
        );
    }

    /**
     * Retorna o caminho atual do banco.
     */
    public static String getDbPath() {
        return DB_PATH.toString();
    }
}