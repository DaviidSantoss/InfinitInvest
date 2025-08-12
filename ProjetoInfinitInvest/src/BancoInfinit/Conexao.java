package BancoInfinit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Conexao {

	/* Define que o banco ainda não foi criado. */
	private static Conexao INSTANCE = null;

	/* Criamos uma conexão nulla. */
	private Connection conect = null;

	private Conexao() throws SQLException {

		try {

			/*
			 * sample.db armazena nosso banco de dados SQLite com todas as tabelas e dados
			 * inseridos.
			 */
			String res = "sample.db";

			/* Estabelecemos a conexão com o banco de dados */
			conect = DriverManager.getConnection("jdbc:sqlite:" + res);

			/*
			 * Cria um objeto Statement que será usado para executar comandos SQL no banco.
			 */
			Statement stmt = conect.createStatement();

			/* Cria um objeto que permite enviar comandos SQL para o banco */
			stmt = conect.createStatement();

			String sqlBanco = "src/ResInfinit/BancoUsuarios.sql";
					
			String sqlContent = new String(Files.readAllBytes(Paths.get(sqlBanco)));;

			/*
			 * Divide a String com o SQL em vários comandos, separados por ponto e vírgula
			 * seguido de quebra de linha.
			 */
			String[] commands = sqlContent.split(";\\s*\n");

			/* Para cada comando SQL separado. */
			for (String cmd : commands) {

				/*
				 * Verifica se o comando não está vazio (para evitar erros com linhas em branco)
				 */
				if (!cmd.trim().isEmpty()) {

					/*
					 * Executa o comando no banco de dados (como criar tabela, inserir dados, etc.)
					 */
					stmt.executeUpdate(cmd);
				}
			}


		} catch (Exception e) {

			System.out.println("Houve um erro ao criar o Banco.");
			System.out.println();
			e.printStackTrace();

		}
		
	}

	/* Criamos um método para "pegar" uma instancia. */
	public static Conexao getInstance() throws IOException, SQLException {

		/* Se ainda não tiver um banco criado, crie um agora */
		if (INSTANCE == null) {
			INSTANCE = new Conexao();
		}

		/* retorna um banco já existente ou que acabou de ser criado. */
		return INSTANCE;
	}

	/* Método para "pegarmos" a conexão. */
	public Connection getConnection() {
		return this.conect;
	}

	/* método para fechar uma conexão */
	public void closeConnection() {
		try {
			this.conect.close();
		} catch (SQLException e) {

			System.err.println("Houve um erro ao fechar a conexão!");
			e.printStackTrace();
		}
	
	}
}



