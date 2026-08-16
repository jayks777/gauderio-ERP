package br.com.gauderio.dao;

import br.com.gauderio.db.Database;
import br.com.gauderio.model.MovimentacaoSaldo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acesso às movimentações manuais de entrada/saída de saldo das contas. */
public class MovimentacaoDAO {

    public int insert(MovimentacaoSaldo m) {
        String sql = "INSERT INTO movimentacoes_saldo (id_conta, tipo, descricao, valor, data) VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getIdConta());
            ps.setString(2, m.getTipo());
            ps.setString(3, m.getDescricao());
            ps.setDouble(4, m.getValor());
            ps.setString(5, m.getData().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao registrar movimentação de saldo", ex);
        }
    }

    public List<MovimentacaoSaldo> porConta(int idConta) {
        List<MovimentacaoSaldo> lista = new ArrayList<>();
        String sql = "SELECT * FROM movimentacoes_saldo WHERE id_conta = ? ORDER BY data DESC, id DESC";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idConta);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MovimentacaoSaldo m = new MovimentacaoSaldo();
                    m.setId(rs.getInt("id"));
                    m.setIdConta(rs.getInt("id_conta"));
                    m.setTipo(rs.getString("tipo"));
                    m.setDescricao(rs.getString("descricao"));
                    m.setValor(rs.getDouble("valor"));
                    m.setData(LocalDate.parse(rs.getString("data")));
                    lista.add(m);
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao consultar movimentações", ex);
        }
        return lista;
    }

    public void delete(int id) {
        String sql = "DELETE FROM movimentacoes_saldo WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao excluir movimentação", ex);
        }
    }
}