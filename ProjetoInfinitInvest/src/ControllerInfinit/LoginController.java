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

	// ==========================================================
	// CONTEXTO DO CONTROLLER
	// ==========================================================
	// Controla o fluxo de login.
	// Valida credenciais, gerencia sessão (memória + persistência) e navega para a tela principal.

	private final LoginForm login;
	@SuppressWarnings("unused")
	private final LoginBackground background;
	@SuppressWarnings("unused")
	private final Dao dao;

	private final SessaoDAO sessaoDAO = new SessaoDAO();

	// ==========================================================
	// CONSTRUTOR
	// ==========================================================

	public LoginController(LoginForm login, LoginBackground background) throws IOException, SQLException {
		this.login = login;
		this.background = background;
		this.dao = new Dao();
	}

	// ==========================================================
	// CONFIGURAÇÕES DE UI
	// ==========================================================

	public void configuraracoes() throws IOException {

		login.getLogar().setOnMouseClicked(e -> {
			try {
				String emailDigitado = login.getEmailField().getText();
				String senhaDigitada = login.getSenhaField().getText();

				Usuario usuario = Dao.buscarPorEmail(emailDigitado);

				if (usuario != null) {

					String senhaArmazenada = usuario.getSenhaHash();

					if (senhaDigitada.equals(senhaArmazenada)) {

						// sessão em memória
						SessaoTemp.setUsuarioLogado(usuario);

						// sessão persistente (manter conectado)
						Preferences prefs = Preferences.userNodeForPackage(LoginController.class);

						if (login.getCheckBox().isSelected()) {

							sessaoDAO.salvarSessao(usuario.getId());

							prefs.put("emailUsuario", emailDigitado.trim());
							prefs.putBoolean("manterConectado", true);

						} else {

							sessaoDAO.limparSessao();

							prefs.remove("emailUsuario");
							prefs.putBoolean("manterConectado", false);
						}

						// troca de tela
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

	// ==========================================================
	// SESSÃO
	// ==========================================================
	// Verifica se existe sessão persistida no banco (manter conectado).

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
