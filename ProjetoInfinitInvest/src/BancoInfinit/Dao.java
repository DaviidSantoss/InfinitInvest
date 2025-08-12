package BancoInfinit;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Dao {

	/* Criamos uma conexão. */
	private static Connection con;

	public Dao() throws IOException, SQLException {

		/* Iniciamos a conexão */
		con = Conexao.getInstance().getConnection();

	}

	/* Método para inserir um usário. */
	public void insert(Usuario usuario) {

		String sql = "INSERT INTO usuarios (nome, email, senha_hash) VALUES (?, ?, ?)";
		try {
			PreparedStatement pstmt = con.prepareStatement(sql);

			pstmt.setString(1, usuario.getNome());
			pstmt.setString(2, usuario.getEmail());
			pstmt.setString(3, usuario.getSenhaHash());

			pstmt.executeUpdate();
			pstmt.close();

		} catch (SQLException e) {

			System.out.println("Falha ai Inserir Usuário!.");
			e.printStackTrace();
		}

	}

	/* Método para deletar um usuário. */
	public void delete(int id) throws SQLException {

		/* String deletando a pessoa. */
		String sql = "DELETE FROM usuarios WHERE id = ?";

		PreparedStatement stmt = con.prepareStatement(sql);

		stmt.setInt(1, id);

		stmt.executeUpdate();
		stmt.close();
	}

	/* Método para exibir todos os usuários do banco. */
	public List<Usuario> getAll() throws SQLException {

		String sql = "select * from usuarios";

		/* Prepara o comando SQL com "?" para ser preenchido depois. */
		PreparedStatement stmt = con.prepareStatement(sql);

		/* Executa a consulta e guarda o resultado da busca. */
		ResultSet rs = stmt.executeQuery();

		/* Criamos uma nova lista do tipo Usuario. */
		List<Usuario> usuarios = new ArrayList<>();

		/*
		 * esse código nos diz o seguinte, Enquanto tiver resultados dentro de "rs" continue caso contrario termine o loop.
		 */
		while (rs.next()) {

			/* Pegamos o id que foi "gerado" da consulta do ResultSet */
			int id = rs.getInt("id");

			/* Pegamos o nome que foi "gerado" da consulta do ResultSet */
			String nameString = rs.getString("nome");
			
			
			String emailString = rs.getString("email");

			/*
			 * Criamos uma nova pessoa passando os parãmetros que foram pegos da consulta do ResultSet.
			 */
			Usuario user = new Usuario(id, nameString, emailString);

			/* adicionamos a nossa lista a pessoa que foi criada acima. */
			usuarios.add(user);

		}

		/* fechamos o prepareStatement e o ResultSet. */
		stmt.close();
		rs.close();

		/* por fim retornamos uma lista de pessoas. */
		return usuarios;
	}

	/* Método para mudar o nome. */
	public void update(int id, String newName) throws SQLException {

		/* comando sql. */
		String sql = "UPDATE person SET name = ? WHERE id = ?";

		/* Prepara o comando sql com ? para ser preenchido depois. */
		PreparedStatement stmt = con.prepareStatement(sql);

		/*
		 * Coloca o valor da variável id no primeiro ? da SQL, e garanta que esse valor é tratado como número inteiro.
		 */
		stmt.setString(1, newName);
		stmt.setInt(2, id);

		/* executa o comando sql acima, e logo em seguida fecha o PreparedStatement. */
		stmt.executeUpdate();
		stmt.close();

	}

	public static Usuario buscarPorEmail(String email) throws SQLException {
		String sql = "SELECT id, nome, email, senha_hash FROM usuarios WHERE email = ?";
		try (PreparedStatement ps = con.prepareStatement(sql)) {
			ps.setString(1, email);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new Usuario(rs.getInt("id"), rs.getString("nome"), rs.getString("email"), rs.getString("senha_hash"));
			}
		}
		return null;
	}


}
