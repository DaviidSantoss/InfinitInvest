package BancoInfinit;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SessaoDAO {

	private Connection conn;

	public SessaoDAO() throws IOException, SQLException {

		/* Iniciamos a conexão */
		conn = Conexao.getInstance().getConnection();

	}

	public SessaoDAO(Connection conn) {
		this.conn = conn;
	}

	/* SALVA E APAGA A SESSÃO. */
	public void salvarSessao(int usuarioId) throws SQLException {
		String deleteSql = "DELETE FROM sessao";
		try (PreparedStatement psDelete = conn.prepareStatement(deleteSql)) {
			psDelete.executeUpdate();
		}

		String insertSql = "INSERT INTO sessao (usuario_id) VALUES (?)";
		try (PreparedStatement psInsert = conn.prepareStatement(insertSql)) {
			psInsert.setInt(1, usuarioId);
			psInsert.executeUpdate();
		}
	}

	// Buscar usuário logado
	public Integer buscarSessao() throws SQLException {
		String selectSql = "SELECT usuario_id FROM sessao LIMIT 1";
		try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("usuario_id");
			}
		}
		return null; // Nenhuma sessão ativa
	}

	// Limpar sessão (logout)
	public void limparSessao() throws SQLException {
        String deleteSql = "DELETE FROM sessao";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.executeUpdate();
        }
	}

	public Connection getConn() {
		return conn;
	}

	public void setConn(Connection conn) {
		this.conn = conn;
	}

}
