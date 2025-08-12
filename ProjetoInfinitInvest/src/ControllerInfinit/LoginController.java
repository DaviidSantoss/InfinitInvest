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


	private LoginForm login;
	Dao dao;
	SessaoDAO sessaoDAO = new SessaoDAO();


	// Construtor que recebe a view
	public LoginController(LoginForm login) throws IOException, SQLException {
		this.login = login;
		this.dao = new Dao(); // Inicialize aqui para evitar null
	}


	public void configuraracoes() {

		/* quando o botão login for clicado */
		login.getLogar().setOnMouseClicked(e -> {
			
			System.out.println("aaaaaaa");

			try {
				/* pega o email e senhas digitados */
				String emailDigitado = login.getEmailField().getText();
				String senhaDigitada = login.getSenhaField().getText();

				/* faz um select através do email digitado */
				Usuario usuario = Dao.buscarPorEmail(emailDigitado);

				/*
				 * si o usuário for não nulo adicione a senha armazenada buscada pelo select acima e armazena na variavel "String senhaArmazenada"
				 */
				if (usuario != null) {
					
					String senhaArmazenada = usuario.getSenhaHash();

					if (senhaDigitada.equals(senhaArmazenada)) {

						if (login.getCheckBox().isSelected()) {
							sessaoDAO.salvarSessao(usuario.getId());
						}

						Stage stageAtual = (Stage) login.getLogar().getScene().getWindow();

						teste testecena = new teste(); // agora é um StackPane
						Scene cenateste = new Scene(testecena);
						stageAtual.setScene(cenateste);
						stageAtual.setMaximized(true);
						
					} else {
						System.out.println("Senha incorreta");
					}
				} else {
					System.out.println("Usuário não encontrado");
				}

			} catch (SQLException ex) {
				ex.printStackTrace();
			}
	});

	}
}