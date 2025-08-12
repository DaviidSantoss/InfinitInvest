package ControllerInfinit;

import java.io.IOException;
import java.sql.SQLException;

import BancoInfinit.SessaoDAO;
import LoginInfinit.CadastroForm;
import LoginInfinit.LoginBackground;
import LoginInfinit.LoginForm;
import LoginInfinit.LoginView;
import MainInfinit.teste;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ScreenManager {

	SessaoDAO sessaoDAO;
	private static Stage primaryStage;

	public static void setPrimaryStage(Stage stage) {
		primaryStage = stage;
	}

	public static void mostrarCadastro() {
		// Constrói a view
		CadastroForm cadastroForm = new CadastroForm().build();

		// Cria o controller e vincula à view
		CadastroController controller = new CadastroController(cadastroForm);
		cadastroForm.setController(controller);

		// Cria uma nova cena com a tela de cadastro
		Scene scene = new Scene(cadastroForm, 1920, 1080);

		// Define a cena na janela principal
		primaryStage.setScene(scene);
	}

	public static void mostrarLogin(Stage stage) throws IOException, SQLException {
		LoginView loginView = new LoginView(stage);

		// Pegue o LoginForm dentro do LoginBackground dentro do LoginView
		// Como o layout é complexo, recomendo que você adicione um método getter no
		// LoginBackground para obter o LoginForm
		LoginBackground fundo = (LoginBackground) loginView.getChildren().get(0);
		LoginForm loginForm = (LoginForm) fundo.getChildren().get(0);

		LoginController controller = new LoginController(loginForm);
		loginForm.setController(controller);

		Scene scene = new Scene(loginView);
		stage.setScene(scene);
		stage.setMaximized(true);
	}

	public static void mostrarTelaPrincipal() {
		teste testecena = new teste(); // teste agora é só um layout, não cria Stage
		Scene cenateste = new Scene(testecena, 1920, 1080);
		primaryStage.setScene(cenateste);
		primaryStage.setMaximized(true);
	}

	@SuppressWarnings("unused")
	public boolean verificarSessaoSalva() {
		try {
			SessaoDAO sessaoDAO = new SessaoDAO(); // já cria a conexão internamente
	        Integer usuarioId = sessaoDAO.buscarSessao();
			return usuarioId != null; // true se houver sessão salva
		} catch (SQLException | IOException e) {
	        e.printStackTrace();
	        return false;
	    }
	}


}