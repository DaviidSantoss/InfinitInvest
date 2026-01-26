package BancoInfinit;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SessaoDAO {

	@SuppressWarnings("resource")
	private static Connection conn;

	public SessaoDAO() throws IOException, SQLException {

		// =============
		// Iniciando a conexão
		// =============
		conn = Conexao.getInstance().getConnection();

	}

	@SuppressWarnings("static-access")
	public SessaoDAO(Connection conn) {
		this.conn = conn;
	}

	// =======================
	// Método para salvar e pegar a sessão.
	// =======================
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

	// =======================
	// Método para Buscar usuário logado
	// =======================
	@SuppressWarnings("resource")
	public static Integer buscarSessao() throws SQLException {
		String selectSql = "SELECT usuario_id FROM sessao LIMIT 1";
		try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt("usuario_id");
			}
		}
		return null;
	}

	// =======================
	// Método para Limpar sessão (logout)
	// =======================
	public void limparSessao() throws SQLException {
        String deleteSql = "DELETE FROM sessao";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.executeUpdate();
        }
	}

	public Connection getConn() {
		return conn;
	}

	@SuppressWarnings("static-access")
	public void setConn(Connection conn) {
		this.conn = conn;
	}

	public final class SessaoTemp {

		// Mantém o usuário autenticado apenas em memória
		private static volatile Usuario usuarioLogado;
		private static volatile Integer usuarioId;

		private SessaoTemp() {
		}

		/** Define o usuário logado para a sessão atual (memória). */
		public static void setUsuarioLogado(Usuario usuario) {
			SessaoTemp.usuarioLogado = usuario;
			SessaoTemp.usuarioId = (usuario != null ? usuario.getId() : null);
		}


		public static void setUsuarioId(Integer id) {
			SessaoTemp.usuarioId = id;
		}

		/** Retorna o usuário logado (ou null se não houver). */
		public static Usuario getUsuarioLogado() {
			return usuarioLogado;
		}

		/** Retorna o ID do usuário logado (ou null se não houver). */
		public static Integer getUsuarioId() {
			if (usuarioLogado != null)
				return usuarioLogado.getId();
			return usuarioId;
		}


		/** Diz se existe sessão ativa em memória. */
		public static boolean isAtiva() {
			return usuarioLogado != null;
		}

		/** Limpa a sessão em memória (use no logout). */
		public static void limpar() {
			usuarioLogado = null;
			usuarioId = null;
		}

	}

}
