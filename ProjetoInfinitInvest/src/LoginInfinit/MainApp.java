package LoginInfinit;

import java.sql.SQLException;

import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO;
import BancoInfinit.SessaoDAO.SessaoTemp;
import BancoInfinit.Usuario;
import ControllerInfinit.LoginController;
import ControllerInfinit.ScreenManager;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@SuppressWarnings("static-access")
	@Override
	public void start(Stage primaryStage) throws SQLException {
		ScreenManager.setPrimaryStage(primaryStage);
//		Dao.limparAtivos();
		Dao.initDatabase();
		Dao.migrarBancoSeNecessario();

		primaryStage.setTitle("InfinitInvest");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/Icone.png")));
		primaryStage.setMaximized(true);

		// ======================================================
		// 🔹 RESTAURA A SESSÃO DO USUÁRIO CASO HAJA UMA SALVA
		// ======================================================
		try {
			SessaoDAO sessaoDAO = new SessaoDAO();
			Integer usuarioId = sessaoDAO.buscarSessao();

			if (usuarioId != null) {
				Usuario usuario = Dao.buscarPorId(usuarioId);
				if (usuario != null) {
					SessaoTemp.setUsuarioLogado(usuario);
					System.out.println("✅ Sessão restaurada automaticamente para: " + usuario.getNome());
				} else {
					System.out.println("⚠️ Sessão encontrada, mas usuário não existe mais.");
				}
			} else {
				System.out.println("ℹ️ Nenhuma sessão ativa encontrada no banco.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// ======================================================
		// 🔹 Define qual tela mostrar
		// ======================================================
		if (LoginController.verificarSessaoSalva()) {
			// Sessão salva → vai direto pra tela principal
			ScreenManager.mostrarTelaPrincipal();
		} else {
			// Nenhuma sessão → mostra tela de login/cadastro
			ScreenManager.mostrarCadastro();
		}

		primaryStage.show();
	}
}
