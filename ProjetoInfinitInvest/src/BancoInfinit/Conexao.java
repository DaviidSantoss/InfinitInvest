package BancoInfinit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexao {
	private static Conexao instance;
	private static final String URL = "jdbc:sqlite:src/ResInfinit/sample.db"; // caminho do banco

	private Conexao() {
		// Cria tabelas só uma vez, quando inicializar
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

	public static Conexao getInstance() {
		if (instance == null) {
			instance = new Conexao();
		}
		return instance;
	}

	// Sempre retorna uma nova conexão
	@SuppressWarnings("resource")
	public Connection getConnection() {
		try {
			return DriverManager.getConnection(URL);
		} catch (SQLException e) {
			throw new RuntimeException("Erro ao abrir conexão com o banco!", e);
		}
	}
}
