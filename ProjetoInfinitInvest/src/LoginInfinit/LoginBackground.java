package LoginInfinit;

import ControllerInfinit.ScreenManager;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class LoginBackground extends GridPane {

	private String cor = "#3D3D3D";
	private ImageView setaView;
	private StackPane container;

	public LoginBackground() {
		// ====================
		// Fundo cinza
		// ====================
		BackgroundFill fundo = new BackgroundFill(Color.web(cor), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(fundo));

		// ====================
		// Grade (6x6)
		// ====================
		getColumnConstraints().addAll(co(), co(), co(), co(), co(), co());
		getRowConstraints().addAll(rc(), rc(), rc(), rc(), rc(), rc());

		// ====================
		// Cria o formulário de login (posição central)
		// ====================
		LoginForm caixa1 = new LoginForm();
		add(caixa1, 2, 1, 2, 4);

		// ====================
		// Cria seta (imagem)
		// ====================
		Image setaImg = new Image(getClass().getResource("/LoginInfinit/imagens/seta-esquerda.png").toExternalForm());
		setaView = new ImageView(setaImg);
		setaView.setFitWidth(50);
		setaView.setFitHeight(50);
		setaView.setPreserveRatio(true);

		// ====================
		// Cria container da seta (área clicável maior)
		// ====================
		container = new StackPane(setaView);
		container.setPadding(new Insets(15)); // aumenta área de clique
		container.setCursor(Cursor.HAND);
		container.setPickOnBounds(true);
		StackPane.setMargin(setaView, new Insets(-80, 0, 0, -170));

		container.setOnMouseClicked(e -> {

			ScreenManager.mostrarCadastro();

		});

		// ====================
		// Adiciona container à tela
		// ====================
		getChildren().add(container);
		container.toFront();
	}

	private ColumnConstraints co() {
		ColumnConstraints co = new ColumnConstraints();
		co.setPercentWidth(20);
		co.setFillWidth(true);
		return co;
	}

	private RowConstraints rc() {
		RowConstraints rc = new RowConstraints();
		rc.setPercentHeight(20);
		rc.setFillHeight(true);
		return rc;
	}

	public ImageView getSetaView() {
		return setaView;
	}

	public StackPane getContainer() {
		return container;
	}

	public void setContainer(StackPane container) {
		this.container = container;
	}
}
