package LoginInfinit;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;

public class LoginBackground extends GridPane {

	// ====================
	// Definindo a cor do BackGround.
	// ====================
	private String cor = "#3D3D3D";

	public LoginBackground() {

		// ====================
		// Definindo o background.
		// ====================
		BackgroundFill fundo = new BackgroundFill(Color.web(cor), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(fundo));

		// ========================================
		// Cria o formulário de login, define a grade da tela e o posiciona.
		// ========================================
		LoginForm caixa1 = new LoginForm();
		getColumnConstraints().addAll(co(), co(), co(), co(), co(), co());
		getRowConstraints().addAll(rc(), rc(), rc(), rc(), rc(), rc());
		add(caixa1, 2, 1, 2, 4);
	}

	// ====================
	// Método para criar as colunas.
	// ====================
	@SuppressWarnings("unused")
	private ColumnConstraints co() {
		ColumnConstraints co = new ColumnConstraints();

		co.setPercentWidth(20);
		co.setFillWidth(true);
		return co;

	}

	// ====================
	// Método para criar as linhas.
	// ====================
	private RowConstraints rc() {

		RowConstraints rc = new RowConstraints();

		rc.setPercentHeight(20);
		rc.setFillHeight(true);
		return rc;

	}


}

