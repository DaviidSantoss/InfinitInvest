package BancoInfinit;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SessaoDAO {

	// ==========================================================
	// CONTEXTO DO DAO DE SESSÃO
	// ==========================================================
	// Gerencia a sessão do usuário em dois níveis:
	// - Persistente (tabela "sessao" no SQLite): mantém login entre execuções, se você quiser
	// - Em memória (SessaoTemp): acesso rápido ao usuário/ID logado durante a execução

	private static Connection conn;

	// ==========================================================
	// CONSTRUTORES / CONEXÃO
	// ==========================================================

	public SessaoDAO() throws IOException, SQLException {
		conn = Conexao.getInstance().getConnection();
	}

	public SessaoDAO(Connection conn) {
		SessaoDAO.conn = conn;
	}

	public Connection getConn() {
		return conn;
	}

	public void setConn(Connection conn) {
		SessaoDAO.conn = conn;
	}

	// ==========================================================
	// SESSÃO NO BANCO (TABELA "sessao")
	// ==========================================================

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

	public void limparSessao() throws SQLException {
		String deleteSql = "DELETE FROM sessao";
		try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
			ps.executeUpdate();
		}
	}

	public static Integer buscarSessao() throws SQLException {
		String selectSql = "SELECT usuario_id FROM sessao LIMIT 1";
		try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return rs.getInt("usuario_id");
			}
		}
		return null;
	}

	// ==========================================================
	// SESSÃO EM MEMÓRIA (RÁPIDA) — USO PELO APP
	// ==========================================================

	public static final class SessaoTemp {

		private static volatile Usuario usuarioLogado;
		private static volatile Integer usuarioId;

		private SessaoTemp() {
		}

		public static void setUsuarioLogado(Usuario usuario) {
			usuarioLogado = usuario;
			usuarioId = (usuario != null ? usuario.getId() : null);
		}

		public static void setUsuarioId(Integer id) {
			usuarioId = id;
		}

		public static Usuario getUsuarioLogado() {
			return usuarioLogado;
		}

		public static Integer getUsuarioId() {
			if (usuarioLogado != null)
				return usuarioLogado.getId();
			return usuarioId;
		}

		public static boolean isAtiva() {
			return usuarioLogado != null || usuarioId != null;
		}

		public static void limpar() {
			usuarioLogado = null;
			usuarioId = null;
		}
	}
}
