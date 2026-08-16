package br.com.gauderio.dao;

import br.com.gauderio.db.Database;
import br.com.gauderio.model.ContaBancaria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Acesso às contas bancárias, incluindo cálculo do saldo atual (derivado). */
public class ContaDAO {

    private ContaBancaria map(ResultSet rs) throws SQLException {
        ContaBancaria c = new ContaBancaria();
        c.setId(rs.getInt("id"));
        c.setNome(rs.getString("nome"));
        c.setBanco(rs.getString("banco"));
        c.setAgencia(rs.getString("agencia"));
        c.setNumero(rs.getString("numero"));
        c.setSaldoInicial(rs.getDouble("saldo_inicial"));
        return c;
    }

    public List<ContaBancaria> findAll() {
        List<ContaBancaria> lista = new ArrayList<>();
        String sql = "SELECT * FROM contas ORDER BY nome";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao consultar contas", ex);
        }
        return lista;
    }

    public int insert(ContaBancaria c) {
        String sql = """
                INSERT INTO contas (nome, banco, agencia, numero, saldo_inicial)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getBanco());
            ps.setString(3, c.getAgencia());
            ps.setString(4, c.getNumero());
            ps.setDouble(5, c.getSaldoInicial());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao salvar conta (nome duplicado?)", ex);
        }
    }

    public void update(ContaBancaria c) {
        String sql = "UPDATE contas SET nome = ?, banco = ?, agencia = ?, numero = ?, saldo_inicial = ? WHERE id = ?";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getBanco());
            ps.setString(3, c.getAgencia());
            ps.setString(4, c.getNumero());
            ps.setDouble(5, c.getSaldoInicial());
            ps.setInt(6, c.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao atualizar conta", ex);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM contas WHERE id = ?";
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao excluir conta", ex);
        }
    }

    /**
     * Saldo atual da conta:
     * saldo inicial + movimentações manuais (entradas - saídas)
     * + receitas pagas - despesas pagas.
     */
    public double saldoAtual(String nomeConta) {
        String sql = """
                SELECT
                    COALESCE((SELECT saldo_inicial FROM contas WHERE nome = ?), 0)
                  + COALESCE((SELECT SUM(m.valor) FROM movimentacoes_saldo m
                              JOIN contas c ON c.id = m.id_conta
                              WHERE c.nome = ? AND m.tipo = 'ENTRADA'), 0)
                  - COALESCE((SELECT SUM(m.valor) FROM movimentacoes_saldo m
                              JOIN contas c ON c.id = m.id_conta
                              WHERE c.nome = ? AND m.tipo = 'SAIDA'), 0)
                  + COALESCE((SELECT SUM(valor) FROM transacoes
                              WHERE conta = ? AND tipo = 'RECEITA' AND status = 'PAGO'), 0)
                  - COALESCE((SELECT SUM(valor) FROM transacoes
                              WHERE conta = ? AND tipo = 'DESPESA' AND status = 'PAGO'), 0)
                """;
        try (Connection cn = Database.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nomeConta);
            ps.setString(2, nomeConta);
            ps.setString(3, nomeConta);
            ps.setString(4, nomeConta);
            ps.setString(5, nomeConta);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao calcular saldo", ex);
        }
    }

    /** Total consolidado de todas as contas cadastradas. */
    public double saldoGeral() {
        double total = 0;
        for (ContaBancaria c : findAll()) {
            total += saldoAtual(c.getNome());
        }
        return total;
    }
}