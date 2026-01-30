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
import ControllerInfinit.ResumoCarteira;

public class Dao {

	// ==========================================================
	// CONTEXTO DO DAO
	// ==========================================================
	// Centraliza operações de banco (SQLite) para:
	// - Usuários (CRUD + imagem)
	// - Ativos (CRUD + metas Tesouro/RF)
	// - Inicialização/migração do schema
	// - Anotações (1 por usuário)
	// - Lançamentos (histórico + recálculo do ativo)

	// ==========================================================
	// USUÁRIOS: CREATE / READ / UPDATE / DELETE
	// ==========================================================

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

	@SuppressWarnings("resource")
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

	@SuppressWarnings("resource")
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

	@SuppressWarnings("resource")
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

	// ==========================================================
	// ATIVOS: INSERT / UPDATE / SELECT / MAP
	// ==========================================================

	public void insertAtivo(String categoria, String ativo, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo) {
		insertAtivo(categoria, ativo, null, quantidade, precoMedio, precoAtual, variacao, saldo);
	}

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

	public List<Ativo> listarAtivosUsuario(int usuarioId) {
		List<Ativo> ativos = new ArrayList<>();

		String sql = """
				SELECT
				  id, usuario_id, categoria, ativo,
				  iconUrl,
				  quantidade, preco_medio, preco_atual, variacao, saldo,
				  td_indexador, td_taxa_anual, td_principal, td_ultimo_dia,
				  rf_indexador, rf_forma, rf_taxa_anual, rf_principal, rf_ultimo_dia,
				  rf_percent_indexador, rf_spread_anual
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

	public static List<Ativo> listarAtivosPorUsuario(int usuarioId) {
		List<Ativo> ativos = new ArrayList<>();

		String sql = """
				SELECT
				  id, usuario_id, categoria, ativo,
				  iconUrl,
				  quantidade, preco_medio, preco_atual, variacao, saldo,
				  td_indexador, td_taxa_anual, td_principal, td_ultimo_dia,
				  rf_indexador, rf_forma, rf_taxa_anual, rf_principal, rf_ultimo_dia,
				  rf_percent_indexador, rf_spread_anual
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

	private static Ativo mapAtivo(ResultSet rs) throws SQLException {
		return new Ativo(rs.getInt("id"), rs.getInt("usuario_id"), rs.getString("categoria"), rs.getString("ativo"), rs.getString("iconUrl"), rs.getDouble("quantidade"), rs.getDouble("preco_medio"),
				rs.getDouble("preco_atual"), rs.getDouble("variacao"), rs.getDouble("saldo"),

				rs.getString("td_indexador"), rs.getDouble("td_taxa_anual"), rs.getDouble("td_principal"), rs.getString("td_ultimo_dia"),

				rs.getString("rf_indexador"), rs.getString("rf_forma"), rs.getDouble("rf_taxa_anual"), rs.getDouble("rf_principal"), rs.getString("rf_ultimo_dia"),

				rs.getDouble("rf_percent_indexador"), rs.getDouble("rf_spread_anual"));
	}

	// ==========================================================
	// ATIVOS: CATEGORIAS ESPECIAIS (INSERÇÕES)
	// ==========================================================

	public void insertRendaFixa(Integer usuarioId, String nomeAtivo, double rentabilidade, double variacao, double saldo) {

		String sql = """
				INSERT INTO ativos (usuario_id, categoria, ativo, quantidade, preco_medio, preco_atual, variacao, saldo)
				VALUES (?, 'Renda Fixa', ?, ?, 0, 0, ?, ?)
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setInt(1, usuarioId);
			stmt.setString(2, nomeAtivo);
			stmt.setDouble(3, rentabilidade); // quantidade = rentabilidade
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

	// ==========================================================
	// METAS: TESOURO DIRETO / RENDA FIXA (POR ID)
	// ==========================================================

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

	public void atualizarCamposRendaFixaMeta(int id, String indexador, String forma, double taxaAnual, double principal, String ultimoDiaIso) {
		String sql = """
				    UPDATE ativos
				       SET rf_indexador = ?,
				           rf_forma = ?,
				           rf_taxa_anual = ?,
				           rf_principal = ?,
				           rf_ultimo_dia = ?
				     WHERE id = ?
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, indexador);
			stmt.setString(2, forma);
			stmt.setDouble(3, taxaAnual);
			stmt.setDouble(4, principal);
			stmt.setString(5, ultimoDiaIso);
			stmt.setInt(6, id);

			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ==========================================================
	// METAS: TESOURO DIRETO / RENDA FIXA (POR NOME)
	// ==========================================================

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

	public void atualizarCamposRendaFixaMetaPorNome(int usuarioId, String nomeAtivo, String indexador, String forma, double taxaAnual, double principal, String ultimoDiaIso) {
		String sql = """
				    UPDATE ativos
				       SET rf_indexador = ?,
				           rf_forma = ?,
				           rf_taxa_anual = ?,
				           rf_principal = ?,
				           rf_ultimo_dia = ?
				     WHERE usuario_id = ?
				       AND categoria  = 'Renda Fixa'
				       AND ativo      = ?
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, indexador);
			stmt.setString(2, forma);
			stmt.setDouble(3, taxaAnual);
			stmt.setDouble(4, principal);
			stmt.setString(5, ultimoDiaIso);
			stmt.setInt(6, usuarioId);
			stmt.setString(7, nomeAtivo);

			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void somarPrincipalRendaFixaETocarUltimoDiaPorNome(int usuarioId, String nomeAtivo, String indexador, String forma, double taxaAnual, double aporte, String ultimoDiaIso) {
		String sql = """
				    UPDATE ativos
				       SET rf_indexador = ?,
				           rf_forma = ?,
				           rf_taxa_anual = ?,
				           rf_principal = COALESCE(rf_principal, 0) + ?,
				           rf_ultimo_dia = ?
				     WHERE usuario_id = ?
				       AND categoria  = 'Renda Fixa'
				       AND ativo      = ?
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, indexador);
			stmt.setString(2, forma);
			stmt.setDouble(3, taxaAnual);
			stmt.setDouble(4, aporte);
			stmt.setString(5, ultimoDiaIso);
			stmt.setInt(6, usuarioId);
			stmt.setString(7, nomeAtivo);

			stmt.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ==========================================================
	// RESUMO DA CARTEIRA / AGREGAÇÕES
	// ==========================================================

	@SuppressWarnings("resource")
	public java.util.Map<String, Double> obterTotaisPorCategoria(int usuarioId) {
		java.util.Map<String, Double> out = new java.util.HashMap<>();

		String sql = "SELECT categoria, SUM(saldo) AS total FROM ativos WHERE usuario_id = ? GROUP BY categoria";

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

	public ResumoCarteira obterResumoCarteira(int usuarioId) {
		List<Ativo> ativos = listarAtivosUsuario(usuarioId);

		double patrimonio = 0.0;
		double investido = 0.0;

		double somaPesoRent = 0.0; // somatório (retorno * valor investido)
		double somaPesos = 0.0; // somatório valor investido

		for (Ativo a : ativos) {
			String cat = a.getCategoria() == null ? "" : a.getCategoria().trim();

			double saldo = a.getSaldo();
			patrimonio += saldo;

			double invAtivo = 0.0;
			double retornoAtivo = 0.0;

			if ("Tesouro Direto".equalsIgnoreCase(cat)) {
				double principal = a.getTdPrincipal();
				if (principal > 0) {
					invAtivo = principal;
				} else {
					invAtivo = Math.max(0.0, saldo - a.getVariacao());
				}

				if (invAtivo > 0)
					retornoAtivo = (saldo - invAtivo) / invAtivo;

			} else if ("Renda Fixa".equalsIgnoreCase(cat)) {
				invAtivo = Math.max(0.0, saldo - a.getVariacao());
				if (invAtivo > 0)
					retornoAtivo = (saldo - invAtivo) / invAtivo;

			} else {
				invAtivo = a.getQuantidade() * a.getPrecoMedio();
				if (invAtivo > 0)
					retornoAtivo = (saldo - invAtivo) / invAtivo;
			}

			investido += invAtivo;

			if (invAtivo > 0) {
				somaPesoRent += retornoAtivo * invAtivo;
				somaPesos += invAtivo;
			}
		}

		double lucro = patrimonio - investido;

		double variacaoPct = (investido > 0) ? (lucro / investido) : 0.0;
		double rentPond = (somaPesos > 0) ? (somaPesoRent / somaPesos) : 0.0;

		double ganhoCapital = lucro;

		return new ResumoCarteira(patrimonio, investido, lucro, ganhoCapital, variacaoPct, rentPond);
	}

	// ==========================================================
	// ANOTAÇÕES (1 POR USUÁRIO)
	// ==========================================================

	@SuppressWarnings("resource")
	public String carregarAnotacoes(int usuarioId) {
		String sql = "SELECT conteudo FROM anotacoes WHERE usuario_id = ?";
		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, usuarioId);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				String txt = rs.getString("conteudo");
				return (txt == null) ? "" : txt;
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";
	}

	public void salvarAnotacoes(int usuarioId, String conteudo) {
		if (conteudo == null)
			conteudo = "";

		String sql = """
				    INSERT INTO anotacoes (usuario_id, conteudo, updated_at)
				    VALUES (?, ?, datetime('now'))
				    ON CONFLICT(usuario_id)
				    DO UPDATE SET conteudo = excluded.conteudo,
				                  updated_at = datetime('now');
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			ps.setInt(1, usuarioId);
			ps.setString(2, conteudo);
			ps.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ==========================================================
	// LANÇAMENTOS: DTO + CRUD
	// ==========================================================

	public static class Lancamento {
		public final int id;
		public final int usuarioId;
		public final String categoria;
		public final String ativo;
		public final double quantidade;
		public final double precoUnitario;
		public final String dataLancamentoIso; // yyyy-MM-dd
		public final double total;

		public Lancamento(int id, int usuarioId, String categoria, String ativo, double quantidade, double precoUnitario, String dataLancamentoIso, double total) {
			this.id = id;
			this.usuarioId = usuarioId;
			this.categoria = categoria;
			this.ativo = ativo;
			this.quantidade = quantidade;
			this.precoUnitario = precoUnitario;
			this.dataLancamentoIso = dataLancamentoIso;
			this.total = total;
		}
	}

	public void inserirLancamento(int usuarioId, String categoria, String ativo, double quantidade, double precoUnit, String dataIso) {
		String sql = """
				INSERT INTO lancamentos (usuario_id, categoria, ativo, quantidade, preco_unit, data_lancamento, total)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""";
		double total = quantidade * precoUnit;

		try (var conn = Conexao.getInstance().getConnection(); var ps = conn.prepareStatement(sql)) {

			ps.setInt(1, usuarioId);
			ps.setString(2, categoria);
			ps.setString(3, ativo);
			ps.setDouble(4, quantidade);
			ps.setDouble(5, precoUnit);
			ps.setString(6, dataIso);
			ps.setDouble(7, total);
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Lancamento> listarLancamentos(int usuarioId) {
		var out = new ArrayList<Lancamento>();
		String sql = """
				SELECT id, usuario_id, categoria, ativo, quantidade, preco_unit, data_lancamento, total
				FROM lancamentos
				WHERE usuario_id = ?
				ORDER BY date(data_lancamento) DESC, id DESC
				""";

		try (var conn = Conexao.getInstance().getConnection(); var ps = conn.prepareStatement(sql)) {

			ps.setInt(1, usuarioId);
			try (var rs = ps.executeQuery()) {
				while (rs.next()) {
					out.add(new Lancamento(rs.getInt("id"), rs.getInt("usuario_id"), rs.getString("categoria"), rs.getString("ativo"), rs.getDouble("quantidade"), rs.getDouble("preco_unit"),
							rs.getString("data_lancamento"), rs.getDouble("total")));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return out;
	}

	public void deletarLancamento(int usuarioId, int lancamentoId) {
		String sql = "DELETE FROM lancamentos WHERE id = ? AND usuario_id = ?";
		try (var conn = Conexao.getInstance().getConnection(); var ps = conn.prepareStatement(sql)) {

			ps.setInt(1, lancamentoId);
			ps.setInt(2, usuarioId);
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void atualizarLancamento(int usuarioId, Lancamento l) {
		String sql = """
				UPDATE lancamentos
				SET quantidade = ?,
				preco_unit = ?,
				data_lancamento = ?,
				total = ?
				WHERE id = ? AND usuario_id = ?
				""";

		try (var conn = Conexao.getInstance().getConnection(); var ps = conn.prepareStatement(sql)) {

			ps.setDouble(1, l.quantidade);
			ps.setDouble(2, l.precoUnitario);
			ps.setString(3, l.dataLancamentoIso);
			ps.setDouble(4, l.total);
			ps.setInt(5, l.id);
			ps.setInt(6, usuarioId);
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ==========================================================
	// LANÇAMENTOS -> RECÁLCULO DO ATIVO CONSOLIDADO
	// ==========================================================

	/**
	 * Recalcula quantidade total + preco_medio baseado em TODOS os lançamentos do ativo. Evita inconsistência quando editar/excluir lançamentos antigos.
	 */
	public void recalcularAtivoPorLancamentos(int usuarioId, String categoria, String ativo) {

		String sqlAgg = """
				SELECT COALESCE(SUM(quantidade),0) as qtd,
				       COALESCE(SUM(quantidade * preco_unit),0) as custo
				FROM lancamentos
				WHERE usuario_id = ? AND categoria = ? AND ativo = ?
				""";

		try (var conn = Conexao.getInstance().getConnection()) {

			double qtdTotal = 0.0;
			double custoTotal = 0.0;

			try (var ps = conn.prepareStatement(sqlAgg)) {
				ps.setInt(1, usuarioId);
				ps.setString(2, categoria);
				ps.setString(3, ativo);
				try (var rs = ps.executeQuery()) {
					if (rs.next()) {
						qtdTotal = rs.getDouble("qtd");
						custoTotal = rs.getDouble("custo");
					}
				}
			}

			if (qtdTotal <= 0.0) {
				try (var psDel = conn.prepareStatement("DELETE FROM ativos WHERE usuario_id = ? AND categoria = ? AND ativo = ?")) {
					psDel.setInt(1, usuarioId);
					psDel.setString(2, categoria);
					psDel.setString(3, ativo);
					psDel.executeUpdate();
				}
				return;
			}

			double precoMedio = custoTotal / qtdTotal;

			double precoAtual = precoMedio;
			String sqlPreco = "SELECT preco_atual FROM ativos WHERE usuario_id = ? AND categoria = ? AND ativo = ? LIMIT 1";
			try (var ps = conn.prepareStatement(sqlPreco)) {
				ps.setInt(1, usuarioId);
				ps.setString(2, categoria);
				ps.setString(3, ativo);
				try (var rs = ps.executeQuery()) {
					if (rs.next())
						precoAtual = rs.getDouble("preco_atual");
				}
			}

			double saldo = precoAtual * qtdTotal;
			double variacao = (precoMedio != 0) ? ((precoAtual - precoMedio) / precoMedio) * 100.0 : 0.0;

			String sqlUp = """
					UPDATE ativos
					SET quantidade = ?,
					    preco_medio = ?,
					    variacao = ?,
					    saldo = ?
					WHERE usuario_id = ? AND categoria = ? AND ativo = ?
					""";

			try (var ps = conn.prepareStatement(sqlUp)) {
				ps.setDouble(1, qtdTotal);
				ps.setDouble(2, precoMedio);
				ps.setDouble(3, variacao);
				ps.setDouble(4, saldo);
				ps.setInt(5, usuarioId);
				ps.setString(6, categoria);
				ps.setString(7, ativo);

				int updated = ps.executeUpdate();

				if (updated == 0) {
					String sqlIns = """
							  INSERT INTO ativos (usuario_id, categoria, ativo, quantidade, preco_medio, preco_atual, variacao, saldo, iconUrl)
							  VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL)
							""";
					try (var ps2 = conn.prepareStatement(sqlIns)) {
						ps2.setInt(1, usuarioId);
						ps2.setString(2, categoria);
						ps2.setString(3, ativo);
						ps2.setDouble(4, qtdTotal);
						ps2.setDouble(5, precoMedio);
						ps2.setDouble(6, precoAtual);
						ps2.setDouble(7, variacao);
						ps2.setDouble(8, saldo);
						ps2.executeUpdate();
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ==========================================================
	// BANCO: INIT / MIGRAÇÃO / UTILITÁRIOS
	// ==========================================================

	public static void initDatabase() {
		try (Connection conn = Conexao.getInstance().getConnection(); Statement stmt = conn.createStatement()) {

			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS usuarios (
					        id INTEGER PRIMARY KEY AUTOINCREMENT,
					        nome TEXT NOT NULL,
					        email TEXT NOT NULL UNIQUE,
					        senha_hash TEXT NOT NULL,
					        fotoPerfil BLOB
					    );
					""");

			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS sessao (
					        id INTEGER PRIMARY KEY AUTOINCREMENT,
					        usuario_id INTEGER NOT NULL
					    );
					""");

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

			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS lancamentos (
					        id INTEGER PRIMARY KEY AUTOINCREMENT,
					        usuario_id INTEGER NOT NULL,
					        categoria TEXT NOT NULL,
					        ativo TEXT NOT NULL,
					        quantidade REAL NOT NULL,
					        preco_unit REAL NOT NULL,
					        data_lancamento TEXT NOT NULL,
					        total REAL NOT NULL,
					        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
					    );
					""");

			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS anotacoes (
					        usuario_id INTEGER PRIMARY KEY,
					        conteudo TEXT NOT NULL DEFAULT '',
					        updated_at TEXT,
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

			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS lancamentos (
					        id INTEGER PRIMARY KEY AUTOINCREMENT,
					        usuario_id INTEGER NOT NULL,
					        categoria TEXT NOT NULL,
					        ativo TEXT NOT NULL,
					        quantidade REAL NOT NULL,
					        preco_unit REAL NOT NULL,
					        data_lancamento TEXT NOT NULL,
					        total REAL NOT NULL
					    );
					""");

			stmt.execute("""
					    CREATE TABLE IF NOT EXISTS anotacoes (
					        usuario_id INTEGER PRIMARY KEY,
					        conteudo TEXT NOT NULL DEFAULT '',
					        updated_at TEXT,
					        FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
					    );
					""");

			if (!colunaExiste(conn, "ativos", "iconUrl")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN iconUrl TEXT;");
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
			if (!colunaExiste(conn, "ativos", "td_ultimo_dia")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN td_ultimo_dia TEXT;");
			}

			if (!colunaExiste(conn, "ativos", "rf_indexador")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN rf_indexador TEXT;");
			}
			if (!colunaExiste(conn, "ativos", "rf_forma")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN rf_forma TEXT;");
			}
			if (!colunaExiste(conn, "ativos", "rf_taxa_anual")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN rf_taxa_anual REAL;");
			}
			if (!colunaExiste(conn, "ativos", "rf_principal")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN rf_principal REAL;");
			}
			if (!colunaExiste(conn, "ativos", "rf_ultimo_dia")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN rf_ultimo_dia TEXT;");
			}

			if (!colunaExiste(conn, "ativos", "rf_percent_indexador")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN rf_percent_indexador REAL;");
			}
			if (!colunaExiste(conn, "ativos", "rf_spread_anual")) {
				stmt.execute("ALTER TABLE ativos ADD COLUMN rf_spread_anual REAL;");
			}

		} catch (Exception e) {
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
}
