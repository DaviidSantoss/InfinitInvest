package LoginInfinit;

import ControllerInfinit.ScreenManager;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class LoginView extends StackPane {

	ScreenManager manager;

	
	public LoginView(Stage stage) {
		
		LoginBackground background = new LoginBackground();
		getChildren().add(background);

		// ===========================
		// Defindo o estilo css e o aplicando à classe.
		// ===========================
		String css = getClass().getResource("/LoginInfinit/Login.css").toExternalForm();
		this.getStylesheets().add(css);



		// ============================================
		// Faz o layout 'raiz' ajustar largura e altura conforme a janela principal.
		// ============================================
		this.setPrefSize(1920, 1080);
		background.prefWidthProperty().bind(stage.widthProperty());
		background.prefHeightProperty().bind(stage.heightProperty());


	}
}
