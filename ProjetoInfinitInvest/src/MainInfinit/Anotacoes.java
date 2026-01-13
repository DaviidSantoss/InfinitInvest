package MainInfinit;

import java.sql.SQLException;

import ControllerInfinit.BarraController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;


public class Anotacoes extends BorderPane {

	private String corFundo = "#212121";
	private BarraLateral barra;
	private BarraController barraController;

	// =============================
	// Clase que irá conter as anotações do usuário.
	// =============================
	public Anotacoes() {


		// =========
		// BackGround.
		// =========
		BackgroundFill fundo = new BackgroundFill(Color.web(corFundo), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(fundo));

		// ===================
		// Cria a barra dentro da classe.
		// ===================
		try {
			barra = new BarraLateral();
			barraController = new BarraController(barra);
			barra.setController(barraController);

			this.setLeft(barra);
			barraController.selecionarBotao(barra.getAnotacoes(), "/LoginInfinit/imagens/lanBlack.png");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// ===================
		// Códigos da classe.
		// ===================
		Label label = new Label("Essa é a tela de Anotações");
		label.setStyle("-fx-font-size: 16px; -fx-text-fill: red; -fx-font-weight: bold;");

		VBox labelBox = new VBox(label);
		labelBox.setAlignment(Pos.CENTER); // centraliza no VBox
		labelBox.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);

		setCenter(labelBox);

	}

}
