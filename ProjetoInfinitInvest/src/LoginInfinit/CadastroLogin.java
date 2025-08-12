package LoginInfinit;

import java.io.IOException;
import java.sql.SQLException;

import ControllerInfinit.LoginController;
import ControllerInfinit.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;




public class CadastroLogin extends GridPane {
	
	// =================================================
	// Referências para o palco principal, controle de login e gerenciamento de telas.
	// =================================================

	Stage stage;
	LoginController loginController;
	ScreenManager telas;

	// ====================
	// Definindo a cor do BackGround.
	// ====================
	private String cores = "#212121";


	// ====================
	// Definindo o tamanho da caixa.
	// ====================
	public CadastroLogin() {
		this(0, 0);
	}

	@SuppressWarnings("static-access")
	public CadastroLogin(int altura, int largura) {
		
		// ====================
		// Definindo o background.
		// ====================
		BackgroundFill fundo = new BackgroundFill(Color.web(cores), new CornerRadii(0, 250, 250, 0, false), Insets.EMPTY);
		setBackground(new Background(fundo));

		// ====================
		// Adicionando o estilo css.
		// ====================
		getStylesheets().add(getClass().getResource("login.css").toExternalForm());
		

		// ====================
		// Fontes.
		// ====================
		Font opensansT = Font.loadFont(getClass().getResourceAsStream("fonts/Baskervville/Baskervville-Regular.ttf"),
				36);

		Font opensansL = Font.loadFont(getClass().getResourceAsStream("fonts/Baskervville/Baskervville-Regular.ttf"),
				18);

		// ===================================
		// Componentes visuais, imagens,labels, texto e botões.
		// ===================================

		Image foto = new Image(getClass().getResource("/LoginInfinit/imagens/logo.png").toExternalForm());
		ImageView verFoto = new ImageView(foto);

		verFoto.setFitWidth(300);
		verFoto.setFitHeight(250);
		verFoto.setPreserveRatio(true);

		VBox Vfoto = new VBox(verFoto);
		Vfoto.setAlignment(Pos.CENTER);
		Vfoto.setPadding(new Insets(130, 150, 0, 150));


		Label volta = new Label("Bem Vindo de Volta!");
		volta.setFont(opensansT);
		volta.setTextFill(Color.WHITE);
		


		HBox Tlabel = new HBox(volta);
		Tlabel.setAlignment(Pos.CENTER);


		Tlabel.setPadding(new Insets(350, 150, 0, 150));


		Label conectar = new Label("Para se conectar novamente, por favor ");
		conectar.setFont(opensansL);
		conectar.setTextFill(Color.WHITE);


		Label conectarN = new Label(" faça login clicando abaixo!");
		conectarN.setFont(opensansL);
		conectarN.setTextFill(Color.WHITE);


		VBox Llabel = new VBox(conectar, conectarN);
		Llabel.setAlignment(Pos.BOTTOM_CENTER);
		Llabel.setTranslateY(70);
		

		Button login = new Button("Login");
		login.getStyleClass().add("botao-login");
		login.setPrefWidth(270);
		login.setPrefHeight(30);


		HBox Cbotao = new HBox(login);
		Cbotao.setAlignment(Pos.CENTER);
		Cbotao.setTranslateY(450);
		Cbotao.setMargin(Cbotao, new Insets(10, 5, 10, 5));


		Label rodape = new Label("''Quem não investe, paga para ver os outros enriquecerem'.'");
		rodape.setFont(opensansL);
		rodape.setTextFill(Color.WHITE);
		rodape.setStyle(" -fx-font-size: 15px;" + "  -fx-font-style: italic;");


		HBox RLabel = new HBox(rodape);
		RLabel.setAlignment(Pos.BOTTOM_CENTER);
		RLabel.setTranslateY(600);
		RLabel.setSpacing(10);

		// ====================
		// Ação ao clicar no botão.
		// ====================
		login.setOnAction(e -> {

			Stage stage = (Stage) ((Button) e.getSource()).getScene().getWindow();
			try {
				ScreenManager.mostrarLogin(stage);
			} catch (IOException | SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});

		// ==================================
		// Adicionando os componenetes visuais a classe atual.
		// ==================================
		getChildren().addAll(Vfoto, Tlabel, Llabel, RLabel, Cbotao);
		
		// ======================
		// Definindo o alinhamento central.
		// ======================
		setAlignment(Pos.TOP_CENTER);
		setMinHeight(altura);
		setMinWidth(largura);

	}


}
