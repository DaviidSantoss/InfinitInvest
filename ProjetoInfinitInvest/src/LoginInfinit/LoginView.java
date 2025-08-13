package LoginInfinit;

import ControllerInfinit.ScreenManager;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class LoginView extends StackPane {

	ScreenManager manager;

	public LoginView(Stage stage) {
		
		// ===========================
		// Defindo o estilo css e o aplicando à classe.
		// ===========================
		String css = getClass().getResource("/LoginInfinit/Login.css").toExternalForm();
		this.getStylesheets().add(css);

		// ========================================
		// Layout principal da tela, com fundo personalizado para login.
		// ========================================
		GridPane raiz = new LoginBackground();


		// ==================================
		// Adicionamos o nosso node "raiz" a nossa classe atual.
		// ==================================
		this.getChildren().add(raiz);
		this.setPrefSize(1920, 1080);

		// ============================================
		// Faz o layout 'raiz' ajustar largura e altura conforme a janela principal.
		// ============================================
		raiz.prefWidthProperty().bind(stage.widthProperty());
		raiz.prefHeightProperty().bind(stage.heightProperty());

		// ============================================
		// Adiciona o layout 'raiz' à cena apenas se ainda não estiver adicionado.
		// ============================================
		if (raiz.getParent() == null) {
			this.getChildren().add(raiz);
		}


	}
}
