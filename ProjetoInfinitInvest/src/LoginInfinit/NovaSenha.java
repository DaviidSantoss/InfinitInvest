package LoginInfinit;

import java.io.IOException;
import java.sql.SQLException;

import BancoInfinit.Dao;
import ControllerInfinit.ScreenManager;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

public class NovaSenha extends BorderPane {

	private String corFundo = "#212121";
	private Button cofirmar = new Button("Confirmar");
	private Dao dao = new Dao();
	private Stage primaryStage;
	private ImageView setaView;
	private StackPane container;

	public NovaSenha(Stage stage) {

		this.primaryStage = stage;

		BackgroundFill fundo = new BackgroundFill(Color.web(corFundo), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(fundo));

		getStylesheets().add(getClass().getResource("/LoginInfinit/Login.css").toExternalForm());

		Image fotoLogo = new Image(getClass().getResource("/LoginInfinit/imagens/logo.png").toExternalForm());
		ImageView verlogo = new ImageView(fotoLogo);
		verlogo.setFitHeight(250);
		verlogo.setFitWidth(250);
		verlogo.setPreserveRatio(true);

		Label EmailLabel = new Label("Digite seu email cadastrado. ");
		EmailLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: bold;");

		TextField Email = new TextField();
		Email.setPromptText("Email");
		Email.getStyleClass().add("campo-verific");

		PasswordField senhaField = new PasswordField();
		senhaField.setPromptText("Senha");
		senhaField.getStyleClass().add("campo-verific");
		senhaField.setVisible(false);
		senhaField.setManaged(false);

		PasswordField senhaField2 = new PasswordField();
		senhaField2.setPromptText("Cofirme a Senha");
		senhaField2.getStyleClass().add("campo-verific");
		senhaField2.setVisible(false);
		senhaField2.setManaged(false);

		Label EmailErro = new Label("Email não Cadastrado!");
		EmailErro.setStyle("-fx-text-fill: Red; -fx-font-size: 16px; -fx-font-weight: bold;");
		EmailErro.setVisible(false);
		EmailErro.setManaged(false);

		Label SenhaErro = new Label("Senhas Diferentes.");
		SenhaErro.setStyle("-fx-text-fill: Red; -fx-font-size: 16px; -fx-font-weight: bold;");
		SenhaErro.setVisible(false);
		SenhaErro.setManaged(false);

		Label SenhaMenor = new Label("Senhas Menores que 8 digitos.");
		SenhaMenor.setStyle("-fx-text-fill: Red; -fx-font-size: 16px; -fx-font-weight: bold;");
		SenhaMenor.setVisible(false);
		SenhaMenor.setManaged(false);
		
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
		container.setPadding(new Insets(15));
		container.setCursor(Cursor.HAND);
		container.setPickOnBounds(true);

		// Alinha no canto superior esquerdo com margens de 20px
		StackPane.setAlignment(setaView, Pos.TOP_LEFT);
		StackPane.setMargin(setaView, new Insets(20, 0, 0, 20));

		// Cria um StackPane de overlay para garantir que fique por cima de tudo
		StackPane overlay = new StackPane(container);
		overlay.setMouseTransparent(false);
		overlay.setPickOnBounds(false);

		// Adiciona o overlay como "top" do BorderPane (posição superior)
		setTop(overlay);

		// Clique da seta
		container.setOnMouseClicked(e -> {
			try {
				ScreenManager.mostrarLogin(stage);
			} catch (IOException | SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});



		cofirmar.setPrefWidth(270);
		cofirmar.setPrefHeight(30);
		cofirmar.getStyleClass().add("botao-login");

		cofirmar.setOnMouseClicked(e -> {

			String senha1 = senhaField.getText();
			String senha2 = senhaField2.getText();

			if (!senhaField.isVisible()) {

				// Primeiro clique: verificar apenas email
				if (dao.emailExiste(Email.getText().trim())) {
					EmailLabel.setVisible(false);
					Email.setVisible(false);
					EmailLabel.setManaged(false);
					Email.setManaged(false);

					// Mostrar campos de senha
					senhaField.setVisible(true);
					senhaField2.setVisible(true);
					senhaField.setManaged(true);
					senhaField2.setManaged(true);

					EmailErro.setVisible(false);
					EmailErro.setManaged(false);

				} else {
					EmailErro.setVisible(true);
					EmailErro.setManaged(true);
				}
			} else {

				// Segundo clique: verificar as senhas
				SenhaErro.setVisible(false);
				SenhaErro.setManaged(false);
				SenhaMenor.setVisible(false);
				SenhaMenor.setManaged(false);

				if (senha1.equals(senha2) && senha2.length() >= 8) {
					dao.updateSenha(Email.getText(), senha2);
					mostrarTelaSucesso(() -> {

						try {
							ScreenManager.mostrarLogin(primaryStage);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (SQLException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

					});
				} else {
					if (senha2.length() < 8) {
						SenhaMenor.setVisible(true);
						SenhaMenor.setManaged(true);
					}
					if (!senha1.equals(senha2)) {
						SenhaErro.setVisible(true);
						SenhaErro.setManaged(true);
					}
				}
			}
		});


		VBox formBox = new VBox(EmailLabel, Email, EmailErro, senhaField, senhaField2, SenhaErro, SenhaMenor, cofirmar);
		formBox.setSpacing(30);
		formBox.setAlignment(Pos.CENTER);

		VBox mainBox = new VBox(verlogo, formBox);
		mainBox.setSpacing(-10); // mais espaço entre logo e o resto
		mainBox.setAlignment(Pos.CENTER);

		setCenter(mainBox);
		getChildren().addAll(container);
	}

	public void mostrarTelaSucesso(Runnable acaoDepois) {

		// Criar imagem de sucesso
		Image setaVerde = new Image(getClass().getResource("/LoginInfinit/imagens/seta-verde.png").toExternalForm());
		ImageView setaView = new ImageView(setaVerde);
		setaView.setFitWidth(100);
		setaView.setFitHeight(100);
		setaView.setPreserveRatio(true);

		// Criar label da mensagem
		Label mensagem = new Label("Senha Atualizada com Sucesso.");
		mensagem.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

		// Colocar imagem e texto em uma VBox
		VBox sucessoBox = new VBox(20, setaView, mensagem);
		sucessoBox.setAlignment(Pos.CENTER);

		// StackPane para garantir centralização total
		StackPane centralizador = new StackPane(sucessoBox);
		centralizador.setAlignment(Pos.CENTER);

		// Ajustar para ocupar todo o espaço da tela
		centralizador.prefWidthProperty().bind(widthProperty());
		centralizador.prefHeightProperty().bind(heightProperty());

		// Limpar layout original e substituir pelo StackPane
		setTop(null);
		setCenter(centralizador);

		// Criar timer de 3 segundos
		PauseTransition pause = new PauseTransition(Duration.seconds(3));
		pause.setOnFinished(event -> {
			if (acaoDepois != null)
				acaoDepois.run();
		});
		pause.play();
	}

}
