package MainInfinit;

import java.sql.SQLException;

import ControllerInfinit.BarraController;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;


public class lancamentos extends BorderPane {

	// ====================
	// Dependências da classe.
	// ====================
	private String corFundo = "#212121";
	private BarraLateral barra;
	private BarraController barraController;

	public lancamentos() {

		// ====================
		// BackGround.
		// ====================
		BackgroundFill fundo = new BackgroundFill(Color.web(corFundo), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(fundo));

		// ====================
		// Cria a barra lateral.
		// ====================
		try {
			barra = new BarraLateral();
			barraController = new BarraController(barra);
			barra.setController(barraController);

			this.setLeft(barra);
			barraController.selecionarBotao(barra.getLancamentos(), "/LoginInfinit/imagens/lanBlack.png");

		} catch (SQLException e) {
			e.printStackTrace();
		}

		// ====================
		// Códigos da Classe.
		// ====================


	}



}
