package br.com.gauderio.dao;

import br.com.gauderio.db.Database;
import br.com.gauderio.model.Transacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acesso aos lançamentos (receitas, despesas, recorrentes e futuros/boletos). */
public class TransacaoDAO {

    private Transacao map(ResultSet rs) throws SQLException {
        Transacao t = new Transacao();
        t.setId(rs.getInt("id"));
        t.setTipo(rs.getString("tipo"));
        t.setDescricao(rs.getString("descricao"));
        t.setValor(rs.getDouble("valor"));
        t.setCategoria(rs.getString("categoria"));
        t.setConta(rs.getString("conta"));
        t.setData(LocalDate.parse(rs.getString("data")));
        String ven = rs.getString("vencimento");
        t.setVencimento(ven == null ? null : LocalDate.parse(ven));
        t.setStatus(rs.getString("status"));
        t.setRecorrente(rs.getInt("recorrente") == 1);
        t.setFrequencia(rs.getString("frequencia"));
        return t;
    }

    private List<Transacao> consultar(String sql, Object... params) {
        List<Transacao> lista = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Erro ao consultar lançamentos", ex);
        }
        return lista;
    }

    public List<Transacao> findAll() {
        return consultar("SELECT * FROM transacoes ORDER BY data DESC, id DESC");
    }

    public List<Transacao> porTipo(String tipo) {
        return consultar("SELECT * FROM transacoes WHERE tipo = ? ORDER BY data DESC, id DESC", tipo);
    }

    public List<Transacao> pendentes() {
        return consultar("""
                SELECT * FROM transacoes
                WHERE status = 'PENDENTE'
                ORDER BY COALESCE(vencimento, data) ASC, id ASC
                """);
    }

    public List<Transacao> pagasEntre(LocalDate inicio, LocalDate fim) {
        return consultar("""
                SELECT * FROM transacoes
                WHERE status = 'PAGO' AND data BETWEEN ? AND ?
                ORDER BY data DESC, id DESC
                """, inicio.toString(), fim.toString());
    }

    public List<Transacao> ultimos(int limite) {
        return consultar("SELECT * FROM transacoes ORDER BY data DESC, id DESC LIMIT " + limite);
    }

    public int insert(Transacao t) {
        String sql = """
                INSERT INTO transacoes
                    (tipo, descricao, valor, categoria, conta, data, vencimento, status, recorrente, frequencia)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencher(ps, t);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao salvar lançamento", ex);
        }
    }

    public void insertAll(List<Transacao> lancamentos) {
        if (lancamentos == null || lancamentos.isEmpty()) {
            return;
        }
        String sql = """
                INSERT INTO transacoes
                    (tipo, descricao, valor, categoria, conta, data, vencimento, status, recorrente, frequencia)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c = Database.getConnection()) {
            boolean antigo = c.getAutoCommit();
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (Transacao t : lancamentos) {
                    preencher(ps, t);
                    ps.addBatch();
                }
                ps.executeBatch();
                c.commit();
            } catch (SQLException ex) {
                c.rollback();
                throw ex;
            } finally {
                c.setAutoCommit(antigo);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao salvar lançamentos em lote", ex);
        }
    }

    private void preencher(PreparedStatement ps, Transacao t) throws SQLException {
        ps.setString(1, t.getTipo());
        ps.setString(2, t.getDescricao());
        ps.setDouble(3, t.getValor());
        ps.setString(4, t.getCategoria() == null ? "" : t.getCategoria());
        ps.setString(5, t.getConta() == null ? "" : t.getConta());
        ps.setString(6, t.getData().toString());
        ps.setString(7, t.getVencimento() == null ? null : t.getVencimento().toString());
        ps.setString(8, t.getStatus());
        ps.setInt(9, t.isRecorrente() ? 1 : 0);
        ps.setString(10, t.getFrequencia());
    }

    public void update(Transacao t) {
        String sql = """
                UPDATE transacoes SET
                    tipo = ?, descricao = ?, valor = ?, categoria = ?, conta = ?,
                    data = ?, vencimento = ?, status = ?, recorrente = ?, frequencia = ?
                WHERE id = ?
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            preencher(ps, t);
            ps.setInt(11, t.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao atualizar lançamento", ex);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM transacoes WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao excluir lançamento", ex);
        }
    }

    public void setStatus(int id, String status) {
        String sql = "UPDATE transacoes SET status = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao atualizar status", ex);
        }
    }

    /** Marca como paga/recebida e grava a data em que o dinheiro efetivamente transacionou. */
    public void marcarPaga(int id, LocalDate dataPagamento) {
        String sql = "UPDATE transacoes SET status = ?, data = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, Transacao.STATUS_PAGO);
            ps.setString(2, dataPagamento.toString());
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao marcar como paga", ex);
        }
    }

    /** Total pendente vencido de um tipo (a pagar ou a receber). */
    public double somaVencido(String tipo) {
        String sql = """
                SELECT COALESCE(SUM(valor), 0) FROM transacoes
                WHERE status = 'PENDENTE' AND tipo = ? AND COALESCE(vencimento, data) < ?
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.setString(2, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao somar pendências vencidas", ex);
        }
    }

    /** Lançamentos pendentes com vencimento entre hoje e a data informada (limite de linhas). */
    public List<Transacao> proximosVencimentos(LocalDate fim, int limite) {
        return consultar("""
                SELECT * FROM transacoes
                WHERE status = 'PENDENTE' AND COALESCE(vencimento, data) BETWEEN ? AND ?
                ORDER BY COALESCE(vencimento, data) ASC, id ASC
                LIMIT ?
                """, LocalDate.now().toString(), fim.toString(), limite);
    }

    /** Soma dos lançamentos PAGOS de um tipo dentro do período. */
    public double somaPagaEntre(String tipo, LocalDate inicio, LocalDate fim) {
        String sql = """
                SELECT COALESCE(SUM(valor), 0) FROM transacoes
                WHERE status = 'PAGO' AND tipo = ? AND data BETWEEN ? AND ?
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo);
            ps.setString(2, inicio.toString());
            ps.setString(3, fim.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao somar lançamentos", ex);
        }
    }

    /** Soma pendente (a receber ou a pagar), considerando inclusive vencidos. */
    public double somaPendente(String tipo) {
        String sql = """
                SELECT COALESCE(SUM(valor), 0) FROM transacoes
                WHERE status = 'PENDENTE' AND tipo = ?
                """;
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao somar pendências", ex);
        }
    }
}