package BancoInfinit;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Dao {

	// ===============
	// Criamos uma conexão.
	// ===============
	private static Connection con;

	public Dao() throws IOException, SQLException {

		// ===============
		// Iniciamos a conexão.
		// ===============
		con = Conexao.getInstance().getConnection();

	}

	// ====================
	// Método para inserir um usário.
	// ====================
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

	// =====================
	// Método para deletar um usuário.
	// =====================
	public void delete(int id) throws SQLException {

		String sql = "DELETE FROM usuarios WHERE id = ?";

		PreparedStatement stmt = con.prepareStatement(sql);

		stmt.setInt(1, id);

		stmt.executeUpdate();
		stmt.close();
	}

	// ==============================
	// Método para exibir todos os usuários do banco.
	// ==============================
	public List<Usuario> getAll() throws SQLException {

		String sql = "select * from usuarios";

		PreparedStatement stmt = con.prepareStatement(sql);

		ResultSet rs = stmt.executeQuery();

		List<Usuario> usuarios = new ArrayList<>();

		while (rs.next()) {

			int id = rs.getInt("id");

			String nameString = rs.getString("nome");
			
			String emailString = rs.getString("email");

			Usuario user = new Usuario(id, nameString, emailString);

			usuarios.add(user);
		}
		stmt.close();
		rs.close();

		return usuarios;
	}

	// ===================
	// Método para mudar o nome.
	// ===================
	public void update(int id, String newName) throws SQLException {

		String sql = "UPDATE person SET name = ? WHERE id = ?";

		PreparedStatement stmt = con.prepareStatement(sql);

		stmt.setString(1, newName);
		stmt.setInt(2, id);

		stmt.executeUpdate();
		stmt.close();

	}

	// ==========================
	// Método para buscar usuario por email.
	// ==========================
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
