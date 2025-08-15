package ControllerInfinit;

import java.io.IOException;
import java.sql.SQLException;

import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO;
import BancoInfinit.Usuario;
import LoginInfinit.LoginForm;
import MainInfinit.teste;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {

	/* COMENTAR CÓDIGO E VER SE PRECISA DE ALGUA ULTIMA LAPIDAÇÃO */

	private LoginForm login;
	Dao dao;
	SessaoDAO sessaoDAO = new SessaoDAO();


	// ====================
	// Construtor que recebe a view.
	// ====================
	public LoginController(LoginForm login) throws IOException, SQLException {
		this.login = login;
		this.dao = new Dao();
	}


	public void configuraracoes() {

		// ======================
		// Quando o botão login for clicado.
		// ======================
		login.getLogar().setOnMouseClicked(e -> {


			try {
				String emailDigitado = login.getEmailField().getText();
				String senhaDigitada = login.getSenhaField().getText();

				Usuario usuario = Dao.buscarPorEmail(emailDigitado);

				if (usuario != null) {
					
					String senhaArmazenada = usuario.getSenhaHash();

					if (senhaDigitada.equals(senhaArmazenada)) {

						if (login.getCheckBox().isSelected()) {
							sessaoDAO.salvarSessao(usuario.getId());
						}

						Stage stageAtual = (Stage) login.getLogar().getScene().getWindow();

						teste testecena = new teste();
						Scene cenateste = new Scene(testecena);
						stageAtual.setScene(cenateste);
						stageAtual.setMaximized(true);
						login.getErro().setVisible(false);
						
					} else {
						System.out.println("Senha incorreta");
						login.getErro().setVisible(true);
					}
				} else {
					System.out.println("Usuário não encontrado");
					login.getErro().setVisible(true);
				}

			} catch (SQLException ex) {
				ex.printStackTrace();
			}
			
			
	});
	}

	// ======================
	// Método para verificar sessão salva.
	// ======================
	public static boolean verificarSessaoSalva() {
		try {
			SessaoDAO sessaoDAO = new SessaoDAO();
			Integer usuarioId = sessaoDAO.buscarSessao();
			return usuarioId != null;
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}