package BancoInfinit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import BancoInfinit.SessaoDAO.SessaoTemp;

public class Dao {

	// ====================
	// Inserir usuário
	// ====================
	public void insert(Usuario usuario) {
		String sql = "INSERT INTO usuarios (nome, email, senha_hash, fotoPerfil) VALUES (?, ?, ?, ?)";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

			pstmt.setString(1, usuario.getNome());
			pstmt.setString(2, usuario.getEmail());
			pstmt.setString(3, usuario.getSenhaHash());
			pstmt.setBytes(4, usuario.getFotoPerfil());

			pstmt.executeUpdate();
			System.out.println("Usuário inserido com sucesso!");

        } catch (SQLException e) {
			System.out.println("Falha ao inserir usuário!");
            e.printStackTrace();
        }
    }

	// ====================
	// Deletar usuário
	// ====================
	public void delete(int id) {
		String sql = "DELETE FROM usuarios WHERE id = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, id);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Falha ao deletar usuário!");
			e.printStackTrace();
		}
	}

	// ====================
	// Atualizar nome do usuário
	// ====================
	public void updateNome(int id, String novoNome) {
		String sql = "UPDATE usuarios SET nome = ? WHERE id = ?";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, novoNome);
			pstmt.setInt(2, id);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Falha ao atualizar nome!");
			e.printStackTrace();
		}
	}

	// ====================
	// Atualizar senha do usuário
	// ====================
	public void updateSenha(String email, String novaSenhaHash) {
		String sql = "UPDATE usuarios SET senha_hash = ? WHERE LOWER(email) = LOWER(?)";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, novaSenhaHash);
			pstmt.setString(2, email);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Falha ao atualizar senha!");
			e.printStackTrace();
		}
	}

	// ====================
	// Atualizar foto de perfil
	// ====================
	public void atualizarImagem(int usuarioId, byte[] imagemBytes) {
		String sql = "UPDATE usuarios SET fotoPerfil = ? WHERE id = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setBytes(1, imagemBytes);
			pstmt.setInt(2, usuarioId);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println("Falha ao atualizar a imagem do usuário!");
			e.printStackTrace();
		}
	}

	// ====================
	// Obter foto de perfil
	// ====================
	public byte[] getImagem(int usuarioId) {
		String sql = "SELECT fotoPerfil FROM usuarios WHERE id = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, usuarioId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getBytes("fotoPerfil");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	// ====================
	// Listar todos os usuários
	// ====================
	public List<Usuario> getAll() {
		List<Usuario> usuarios = new ArrayList<>();
		String sql = "SELECT * FROM usuarios";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
			while (rs.next()) {
				Usuario u = new Usuario(rs.getInt("id"), rs.getString("nome"), rs.getString("email"), rs.getString("senha_hash"));
				u.setFotoPerfil(rs.getBytes("fotoPerfil"));
				usuarios.add(u);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return usuarios;
	}

	// ====================
	// Buscar usuário por email
	// ====================
	public static Usuario buscarPorEmail(String email) {
		String sql = "SELECT * FROM usuarios WHERE LOWER(email) = LOWER(?)";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, email.trim());
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				Usuario u = new Usuario(rs.getInt("id"), rs.getString("nome"), rs.getString("email"), rs.getString("senha_hash"));
				u.setFotoPerfil(rs.getBytes("fotoPerfil"));
				return u;
			}
		} catch (SQLException e) {
			System.out.println("Falha ao buscar usuário por email!");
			e.printStackTrace();
		}
		return null;
	}

	// ====================
	// Listar todos os emails
	// ====================
	public void listarTodosEmails() {
		String sql = "SELECT email FROM usuarios";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
			System.out.println("=== EMAILS NO BANCO ===");
			while (rs.next()) {
				System.out.println("'" + rs.getString("email") + "'");
			}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

	public static Usuario buscarPorId(int id) throws SQLException {
		String sql = "SELECT * FROM usuarios WHERE id = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				Usuario usuario = new Usuario();
				usuario.setId(rs.getInt("id"));
				usuario.setNome(rs.getString("nome"));
				usuario.setEmail(rs.getString("email"));
				usuario.setSenhaHash(rs.getString("senha_hash"));
				usuario.setFotoPerfil(rs.getBytes("fotoPerfil"));
				return usuario;
			}
		}
		return null;
	}

	public boolean emailExiste(String email) {
		String sql = "SELECT COUNT(*) FROM usuarios WHERE email = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
			ps.setString(1, email);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				int count = rs.getInt(1);
				return count > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// =========================================================
	// ======== A T I V O S - NOVOS MÉTODOS =============
	// =========================================================

	public void insertAtivo(String categoria, String ativo, String iconUrl, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo) {
		Integer usuarioId = SessaoTemp.getUsuarioId();
		if (usuarioId == null)
			return;

		String sql = """
				INSERT INTO ativos (usuario_id, categoria, ativo, iconUrl, quantidade, preco_medio, preco_atual, variacao, saldo)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, usuarioId);
			stmt.setString(2, categoria);
			stmt.setString(3, ativo);
			stmt.setString(4, iconUrl);
			stmt.setDouble(5, quantidade);
			stmt.setDouble(6, precoMedio);
			stmt.setDouble(7, precoAtual);
			stmt.setDouble(8, variacao);
			stmt.setDouble(9, saldo);

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}



	public List<Ativo> listarAtivosUsuario(int usuarioId) {
		List<Ativo> ativos = new ArrayList<>();
		String sql = """
				SELECT
				  id, usuario_id, categoria, ativo,
				  iconUrl,
				  quantidade, preco_medio, preco_atual, variacao, saldo,
				  td_indexador, td_taxa_anual, td_principal, td_ultimo_dia
				FROM ativos
				WHERE usuario_id = ?
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, usuarioId);

			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next())
					ativos.add(mapAtivo(rs));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ativos;
	}



	// =========================================================
	// ======== I N I C I A L I Z A Ç Ã O D O B A N C O =====
	// =========================================================

	public static void initDatabase() {
		try (Connection conn = Conexao.getInstance().getConnection(); Statement stmt = conn.createStatement()) {

			// Tabela usuários
			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS usuarios (
					        id INTEGER PRIMARY KEY AUTOINCREMENT,
					        nome TEXT NOT NULL,
					        email TEXT NOT NULL UNIQUE,
					        senha_hash TEXT NOT NULL,
					        fotoPerfil BLOB
					    );
					""");

			// Tabela sessão
			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS sessao (
					        id INTEGER PRIMARY KEY AUTOINCREMENT,
					        usuario_id INTEGER NOT NULL
					    );
					""");

			// Tabela ativos — COMPATÍVEL COM AddAtivosController + Classe Ativo
			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS ativos (
					        id INTEGER PRIMARY KEY AUTOINCREMENT,
					        usuario_id INTEGER NOT NULL,
					        categoria TEXT NOT NULL,
					        ativo TEXT NOT NULL,
					        quantidade REAL NOT NULL,
					        preco_medio REAL NOT NULL,
					        preco_atual REAL NOT NULL,
					        variacao REAL NOT NULL,
					        saldo REAL NOT NULL,
					        iconUrl TEXT,
					        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
					    );
					""");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void migrarBancoSeNecessario() {
		try (Connection conn = Conexao.getInstance().getConnection(); Statement stmt = conn.createStatement()) {

			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS ativos (
					        id INTEGER PRIMARY KEY AUTOINCREMENT,
					        usuario_id INTEGER NOT NULL,
					        categoria TEXT NOT NULL,
					        ativo TEXT NOT NULL,
					        quantidade REAL NOT NULL,
					        preco_medio REAL NOT NULL,
					        preco_atual REAL NOT NULL,
					        variacao REAL NOT NULL,
					        saldo REAL NOT NULL
					    );
					""");

			// ✅ ESSENCIAL: iconUrl
			if (!colunaExiste(conn, "ativos", "iconUrl")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN iconUrl TEXT;");
			}

			// td_*
			if (!colunaExiste(conn, "ativos", "td_ultimo_dia")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN td_ultimo_dia TEXT;");
			}
			if (!colunaExiste(conn, "ativos", "td_indexador")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN td_indexador TEXT;");
			}
			if (!colunaExiste(conn, "ativos", "td_taxa_anual")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN td_taxa_anual REAL;");
			}
			if (!colunaExiste(conn, "ativos", "td_principal")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN td_principal REAL;");
			}
			// adiciona coluna iconUrl se não existir ✅
			if (!colunaExiste(conn, "ativos", "iconUrl")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN iconUrl TEXT;");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public void atualizarCamposTesouroMeta(int id, String indexador, double taxaAnual, double principal, String ultimoDiaIso) {
		String sql = "UPDATE ativos SET td_indexador = ?, td_taxa_anual = ?, td_principal = ?, td_ultimo_dia = ? WHERE id = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, indexador);
			stmt.setDouble(2, taxaAnual);
			stmt.setDouble(3, principal);
			stmt.setString(4, ultimoDiaIso);
			stmt.setInt(5, id);
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private static boolean colunaExiste(Connection conn, String tabela, String coluna) throws SQLException {
		String sql = "PRAGMA table_info(" + tabela + ")";
		try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
			while (rs.next()) {
				String nomeColuna = rs.getString("name");
				if (coluna.equalsIgnoreCase(nomeColuna))
					return true;
			}
		}
		return false;
	}

	public static void limparAtivos() {
		try (Connection conn = Conexao.getInstance().getConnection(); var stmt = conn.createStatement()) {


			stmt.execute("DELETE FROM ativos");
			System.out.println("✅ Banco de dados limpo com sucesso!");

		} catch (SQLException e) {
			System.err.println("❌ Erro ao limpar banco de dados!");
			e.printStackTrace();
		}
	}

	public static List<Ativo> listarAtivosPorUsuario(int usuarioId) {
		List<Ativo> ativos = new ArrayList<>();

		String sql = """
				SELECT
				  id, usuario_id, categoria, ativo,
				  iconUrl,
				  quantidade, preco_medio, preco_atual, variacao, saldo,
				  td_indexador, td_taxa_anual, td_principal, td_ultimo_dia
				FROM ativos
				WHERE usuario_id = ?
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, usuarioId);
			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				Ativo a = new Ativo(rs.getInt("id"), rs.getInt("usuario_id"), rs.getString("categoria"), rs.getString("ativo"), rs.getString("iconUrl"), rs.getDouble("quantidade"),
						rs.getDouble("preco_medio"), rs.getDouble("preco_atual"), rs.getDouble("variacao"), rs.getDouble("saldo"), rs.getString("td_indexador"), rs.getDouble("td_taxa_anual"),
						rs.getDouble("td_principal"), rs.getString("td_ultimo_dia"));
				ativos.add(a); // <<< ESSENCIAL
			}


		} catch (SQLException e) {
			e.printStackTrace();
		}

		return ativos;
	}


	public Ativo buscarAtivoPorUsuarioECategoria(int usuarioId, String ativo, String categoria) {
		String sql = "SELECT * FROM ativos WHERE usuario_id = ? AND ativo = ? AND categoria = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, usuarioId);
			stmt.setString(2, ativo);
			stmt.setString(3, categoria);

			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next())
					return mapAtivo(rs);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}



	public void atualizarAtivo(int id, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo) {
		String sql = "UPDATE ativos SET quantidade = ?, preco_medio = ?, preco_atual = ?, variacao = ?, saldo = ? WHERE id = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setDouble(1, quantidade);
			stmt.setDouble(2, precoMedio);
			stmt.setDouble(3, precoAtual);
			stmt.setDouble(4, variacao);
			stmt.setDouble(5, saldo);
			stmt.setInt(6, id);

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private static Ativo mapAtivo(ResultSet rs) throws SQLException {
		return new Ativo(rs.getInt("id"), rs.getInt("usuario_id"), rs.getString("categoria"), rs.getString("ativo"), rs.getString("iconUrl"), rs.getDouble("quantidade"), rs.getDouble("preco_medio"),
				rs.getDouble("preco_atual"), rs.getDouble("variacao"), rs.getDouble("saldo"), rs.getString("td_indexador"), rs.getDouble("td_taxa_anual"), rs.getDouble("td_principal"),
				rs.getString("td_ultimo_dia"));
	}




	public void insertRendaFixa(Integer usuarioId, String nomeAtivo, double rentabilidade, double variacao, double saldo) {

		String sql = """
				INSERT INTO ativos (usuario_id, categoria, ativo, quantidade, preco_medio, preco_atual, variacao, saldo)
				VALUES (?, 'Renda Fixa', ?, ?, 0, 0, ?, ?)
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, usuarioId);
			stmt.setString(2, nomeAtivo);

// quantidade = rentabilidade
			stmt.setDouble(3, rentabilidade);

			stmt.setDouble(4, variacao);
			stmt.setDouble(5, saldo);

			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void insertTesouroDireto(Integer usuarioId, String nomeAtivo, BigDecimal quantidade, BigDecimal variacao, BigDecimal saldo) {
		String sql = """
				    INSERT INTO ativos (
				        usuario_id, categoria, ativo,
				        quantidade, preco_medio, preco_atual,
				        variacao, saldo
				    )
				    VALUES (?, 'Tesouro Direto', ?, ?, 0, 0, ?, ?)
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setInt(1, usuarioId);
			stmt.setString(2, nomeAtivo);
			stmt.setBigDecimal(3, quantidade.setScale(2, RoundingMode.HALF_UP));
			stmt.setBigDecimal(4, variacao);
			stmt.setBigDecimal(5, saldo.setScale(2, RoundingMode.HALF_UP));

			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// Dao.java
	public java.util.Map<String, Double> obterTotaisPorCategoria(int usuarioId) {
		java.util.Map<String, Double> out = new java.util.HashMap<>();

		String sql = "SELECT categoria, SUM(saldo) AS total " + "FROM ativos WHERE usuario_id = ? GROUP BY categoria";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, usuarioId);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				String categoria = rs.getString("categoria");
				double total = rs.getDouble("total");
				out.put(categoria, total);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return out;
	}

	// Mantém compatibilidade com chamadas antigas
	public void insertAtivo(String categoria, String ativo, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo) {

		insertAtivo(categoria, ativo, null, quantidade, precoMedio, precoAtual, variacao, saldo);
	}

	public void atualizarIconUrl(int id, String iconUrl) {
		String sql = "UPDATE ativos SET iconUrl = ? WHERE id = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, iconUrl);
			stmt.setInt(2, id);
			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void atualizarCamposTesouroMetaPorNome(int usuarioId, String nomeAtivo, String indexador, double taxaAnual, double principal, String ultimoDiaIso) {
		String sql = """
				    UPDATE ativos
				       SET td_indexador = ?,
				           td_taxa_anual = ?,
				           td_principal  = ?,
				           td_ultimo_dia = ?
				     WHERE usuario_id = ?
				       AND categoria  = 'Tesouro Direto'
				       AND ativo      = ?
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, indexador);
			stmt.setDouble(2, taxaAnual);
			stmt.setDouble(3, principal);
			stmt.setString(4, ultimoDiaIso);
			stmt.setInt(5, usuarioId);
			stmt.setString(6, nomeAtivo);

			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void somarPrincipalETocarUltimoDiaPorNome(int usuarioId, String nomeAtivo, String indexador, double taxaAnual, double aporte, String ultimoDiaIso) {
		String sql = """
				    UPDATE ativos
				       SET td_indexador = ?,
				           td_taxa_anual = ?,
				           td_principal  = COALESCE(td_principal, 0) + ?,
				           td_ultimo_dia = ?
				     WHERE usuario_id = ?
				       AND categoria  = 'Tesouro Direto'
				       AND ativo      = ?
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, indexador);
			stmt.setDouble(2, taxaAnual);
			stmt.setDouble(3, aporte);
			stmt.setString(4, ultimoDiaIso);
			stmt.setInt(5, usuarioId);
			stmt.setString(6, nomeAtivo);

			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


}
