package BancoInfinit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexao {

	// ==========================================================
	// CONTEXTO DA CONEXÃO
	// ==========================================================
	// Singleton simples para centralizar o acesso ao SQLite.
	// Na inicialização, garante que as tabelas essenciais existam.
	// Cada chamada de getConnection() devolve uma conexão nova.

	private static Conexao instance;

	private static final String URL = "jdbc:sqlite:src/ResInfinit/sample.db";

	private Conexao() {
		criarTabelasSeNecessario();
	}

	// ==========================================================
	// SINGLETON
	// ==========================================================
	public static Conexao getInstance() {
		if (instance == null) {
			instance = new Conexao();
		}
		return instance;
	}

	// ==========================================================
	// CONEXÃO
	// ==========================================================
	@SuppressWarnings("resource")
	public Connection getConnection() {
		try {
			return DriverManager.getConnection(URL);
		} catch (SQLException e) {
			throw new RuntimeException("Erro ao abrir conexão com o banco!", e);
		}
	}

	// ==========================================================
	// SETUP DO BANCO
	// ==========================================================
	private void criarTabelasSeNecessario() {
		try (Connection conn = DriverManager.getConnection(URL); Statement stmt = conn.createStatement()) {

			String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios (" + "id INTEGER PRIMARY KEY AUTOINCREMENT," + "nome TEXT NOT NULL," + "email TEXT NOT NULL UNIQUE," + "senha_hash TEXT NOT NULL,"
					+ "fotoPerfil BLOB" + ");";

			String sqlSessao = "CREATE TABLE IF NOT EXISTS sessao (" + "id INTEGER PRIMARY KEY AUTOINCREMENT," + "usuario_id INTEGER NOT NULL" + ");";

			stmt.execute(sqlUsuarios);
			stmt.execute(sqlSessao);

		} catch (SQLException e) {
			System.out.println("Houve um erro ao criar ou conectar ao Banco.");
			e.printStackTrace();
		}
	}
}
