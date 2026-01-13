package ControllerInfinit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.prefs.Preferences;

import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO;
import BancoInfinit.SessaoDAO.SessaoTemp;
import BancoInfinit.Usuario;
import LoginInfinit.LoginBackground;
import LoginInfinit.LoginForm;
import MainInfinit.Principal;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginController {

	private LoginForm login;
	@SuppressWarnings("unused")
	private LoginBackground background;
	@SuppressWarnings("unused")
	private Dao dao;
	private SessaoDAO sessaoDAO = new SessaoDAO();

	public LoginController(LoginForm login, LoginBackground background) throws IOException, SQLException {
		this.login = login;
		this.background = background;
		this.dao = new Dao();
	}

	public void configuraracoes() throws IOException {

		login.getLogar().setOnMouseClicked(e -> {
			try {
				String emailDigitado = login.getEmailField().getText();
				String senhaDigitada = login.getSenhaField().getText();

				Usuario usuario = Dao.buscarPorEmail(emailDigitado);

				if (usuario != null) {
					String senhaArmazenada = usuario.getSenhaHash();

					if (senhaDigitada.equals(senhaArmazenada)) {

						// 🔹 Sempre seta o usuário logado na sessão em memória
						SessaoTemp.setUsuarioLogado(usuario);

						if (login.getCheckBox().isSelected()) {
							// 🔹 Se o usuário quiser manter logado, salva no banco
							sessaoDAO.salvarSessao(usuario.getId());

							Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
							prefs.put("emailUsuario", login.getEmailField().getText().trim());
							prefs.putBoolean("manterConectado", true);
						} else {
							// 🔹 Caso contrário, limpa a sessão persistente
							sessaoDAO.limparSessao();
							Preferences prefs = Preferences.userNodeForPackage(LoginController.class);
							prefs.remove("emailUsuario");
							prefs.putBoolean("manterConectado", false);
						}

						// 🔹 Troca de tela
						Stage stageAtual = (Stage) login.getLogar().getScene().getWindow();
						Principal principal = new Principal();
						Scene cenaPrincipal = new Scene(principal);
						stageAtual.setScene(cenaPrincipal);
						stageAtual.setMaximized(true);
						login.getErro().setVisible(false);

						System.out.println("✅ Login realizado com sucesso para: " + usuario.getNome());

					} else {
						System.out.println("❌ Senha incorreta");
						login.getErro().setVisible(true);
					}
				} else {
					System.out.println("❌ Usuário não encontrado");
					login.getErro().setVisible(true);
				}

			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		});

	}

	public static boolean verificarSessaoSalva() {
		try {
			SessaoDAO sessaoDAO = new SessaoDAO();
			@SuppressWarnings("static-access")
			Integer usuarioId = sessaoDAO.buscarSessao();
			return usuarioId != null;
		} catch (SQLException | IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
