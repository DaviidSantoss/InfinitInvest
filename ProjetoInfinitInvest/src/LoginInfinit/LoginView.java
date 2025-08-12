package LoginInfinit;

import ControllerInfinit.ScreenManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView extends StackPane {

	ScreenManager manager;

	@SuppressWarnings("static-access")
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

		// ====================
		// Defindo a label e sua vbox.
		// ====================
		Label clique = new Label("   Clique aqui.");
		clique.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
		clique.getStyleClass().add("cadastro");
		clique.setTranslateX(-36);
		clique.setTranslateY(0);
	



		VBox vBox = new VBox(clique);
		vBox.setSpacing(100);

		vBox.setAlignment(Pos.BOTTOM_CENTER);
		vBox.setTranslateY(-71);
		vBox.setMouseTransparent(false);
		raiz.add(vBox, 3, 3);

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

		// ==============================
		// Defindo a criação da tela após clicar no botão.
		// ==============================
		clique.setOnMouseClicked(e -> {
			CadastroForm cadastro = new CadastroForm().build(); // constrói a tela
			Scene novaCena = new Scene(cadastro, 1920, 1080);
			stage.setScene(novaCena);
			manager.mostrarCadastro();

		});

	}
}
