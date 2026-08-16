package br.com.gauderio.dao;

import br.com.gauderio.db.Database;
import br.com.gauderio.model.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Acesso às categorias de receitas e despesas. */
public class CategoriaDAO {

    private Categoria map(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getInt("id"));
        c.setNome(rs.getString("nome"));
        c.setTipo(rs.getString("tipo"));
        return c;
    }

    public List<Categoria> findAll() {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM categorias ORDER BY tipo, nome";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(map(rs));
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao consultar categorias", ex);
        }
        return lista;
    }

    public List<Categoria> porTipo(String tipo) {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT * FROM categorias WHERE tipo = ? ORDER BY nome";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(map(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao consultar categorias", ex);
        }
        return lista;
    }

    public int insert(String nome, String tipo) {
        String sql = "INSERT INTO categorias (nome, tipo) VALUES (?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nome);
            ps.setString(2, tipo);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao criar categoria (já existe?)", ex);
        }
    }

    public void update(int id, String nome, String tipo) {
        String sql = "UPDATE categorias SET nome = ?, tipo = ? WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setString(2, tipo);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Erro ao atualizar categoria", ex);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM categorias WHERE id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Não foi possível excluir a categoria", ex);
        }
    }
}