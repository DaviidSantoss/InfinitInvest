package ControllerInfinit;

import java.io.IOException;
import java.sql.SQLException;

import BancoInfinit.SessaoDAO;
import LoginInfinit.CadastroForm;
import LoginInfinit.LoginBackground;
import LoginInfinit.LoginForm;
import LoginInfinit.LoginView;
import LoginInfinit.NovaSenha;
import MainInfinit.Principal;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ScreenManager {

	SessaoDAO sessaoDAO;
	private static Stage primaryStage = new Stage();;

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



		// Obtém o fundo (LoginBackground)
		LoginBackground fundo = (LoginBackground) loginView.getChildren().get(0);

		// Obtém o formulário (LoginForm)
		LoginForm loginForm = (LoginForm) fundo.getChildren().get(0);

		// Cria o controller passando ambos
		LoginController controller = new LoginController(loginForm, fundo);
		loginForm.setController(controller);
		controller.configuraracoes(); // importante chamar aqui

		// Mostra na tela
		Scene scene = new Scene(loginView);
		stage.setScene(scene);
		stage.setMaximized(true);
	}



	// ======================
	// Método para exibir TelaPrincipal.
	// =====================
	public static void mostrarTelaPrincipal() throws SQLException {
		Principal testecena = new Principal();
		Scene cenateste = new Scene(testecena, 1920, 1080);
		primaryStage.setScene(cenateste);
		primaryStage.setMaximized(true);
	}

	public static void mostrarNovaSenha() {

		NovaSenha novaSenha = new NovaSenha(primaryStage);
		Scene scene = new Scene(novaSenha, 1920, 1080);
		primaryStage.setScene(scene);
		primaryStage.setMaximized(true);
		primaryStage.show();

	}
}