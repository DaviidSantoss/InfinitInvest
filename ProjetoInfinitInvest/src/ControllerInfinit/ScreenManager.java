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

	// ======================
	// Método para exibir CadastroForm.
	// ======================
	public static void mostrarCadastro() {

		CadastroForm cadastroForm = new CadastroForm().build();

		CadastroController controller = new CadastroController(cadastroForm);
		cadastroForm.setController(controller);

		Scene scene = new Scene(cadastroForm, 1920, 1080);

		primaryStage.setScene(scene);
	}

	// ======================
	// Método para exibir LoginView.
	// ======================
	public static void mostrarLogin(Stage stage) throws IOException, SQLException {
		LoginView loginView = new LoginView(stage);

		LoginBackground fundo = (LoginBackground) loginView.getChildren().get(0);
		LoginForm loginForm = (LoginForm) fundo.getChildren().get(0);

		LoginController controller = new LoginController(loginForm);
		loginForm.setController(controller);

		Scene scene = new Scene(loginView);
		stage.setScene(scene);
		stage.setMaximized(true);
	}

	// ======================
	// Método para exibir TelaPrincipal.
	// =====================
	public static void mostrarTelaPrincipal() {
		teste testecena = new teste();
		Scene cenateste = new Scene(testecena, 1920, 1080);
		primaryStage.setScene(cenateste);
		primaryStage.setMaximized(true);
	}
}