package LoginInfinit;

import ControllerInfinit.ScreenManager;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {


	// ============================
	// Método principal que "roda" todo o código.
	// ============================
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		ScreenManager.setPrimaryStage(primaryStage);
		
		ScreenManager manager = new ScreenManager();
		// =====================
		// Setando o nome e a maximização.
		// =====================
		primaryStage.setTitle("InfinitInvest");
		primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/Icone.png")));
		primaryStage.setMaximized(true);

		// =========================
		// executa o método mostrarCadastro();
		// =========================
		if (manager.verificarSessaoSalva()) {
			// Se existir sessão salva, vai direto pra tela principal
			ScreenManager.mostrarTelaPrincipal();
			;
		} else {
			// Caso contrário, mostra a tela de cadastro/login
			ScreenManager.mostrarCadastro();
		}

		primaryStage.show();
	}
}
