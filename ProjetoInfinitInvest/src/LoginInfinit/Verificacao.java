package LoginInfinit;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class Verificacao extends BorderPane {

	// ======================================================
	// Componentes da interface: botão, campo de texto, mensagens, imagem e área clicável.
	// ======================================================
	private Button confirmar = new Button("Confirmar");
	private TextField CodField = new TextField();
	private Label reenvio = new Label("Reenviar código. ");
	private Label erroCodigo = new Label("Código incorreto. Reenvie e aguarde.");
	private Image foto = new Image(getClass().getResource("/LoginInfinit/imagens/seta-esquerda.png").toExternalForm());
	ImageView verFoto = new ImageView(foto);
	private StackPane clickableArea = new StackPane(verFoto);

	public Verificacao() {

		// ====================
		// Cor do backGround.
		// ====================
		String corbg = "#212121";

		// ====================
		// Setando o estilo css.
		// ====================
		getStylesheets().add(getClass().getResource("/LoginInfinit/Login.css").toExternalForm());

		// ====================
		// Background.
		// ====================
		BackgroundFill bg = new BackgroundFill(Color.web(corbg), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(bg));

		// ====================
		// Foto Logo.
		// ====================
		Image fotoLogo = new Image(getClass().getResource("/LoginInfinit/imagens/logo.png").toExternalForm());
		ImageView verlogo = new ImageView(fotoLogo);
		verlogo.setFitHeight(250);
		verlogo.setFitWidth(250);
		verlogo.setPreserveRatio(true);

		// ====================
		// Foto Seta Voltar.
		// ====================
		verFoto.setPreserveRatio(false);
		verFoto.setFitWidth(50);
		verFoto.setFitHeight(50);
		verFoto.setPickOnBounds(true);
		verFoto.setMouseTransparent(false);

		// ====================
		// Configuração Seta Voltar.
		// ====================

		clickableArea.setPrefSize(300, 300);
		clickableArea.setMinSize(150, 150);
		clickableArea.setMaxSize(250, 250);
		clickableArea.setAlignment(Pos.TOP_LEFT);
		clickableArea.setPadding(new Insets(20, 0, 0, 20));
		setTop(clickableArea);

		// ====================
		// Campo de verificação ..
		// ====================

		CodField.setPromptText("Codigo de Verificação");
		CodField.getStyleClass().add("campo-verific");

		// ====================
		// Botão.
		// ====================
		confirmar.getStyleClass().add("botao-login");
		confirmar.setStyle("-fx-padding: 10px 50px; ");

		// ====================
		// Labels.
		// ====================
		Label codlabel = new Label("Não recebeu o código? ");
		codlabel.getStyleClass().add("cadastro");

		reenvio.setStyle("-fx-font-weight: bold;-fx-underline: true; -fx-cursor: hand;");
		reenvio.getStyleClass().add("cadastro");


		erroCodigo.setStyle("-fx-text-fill: Red; -fx-font-size: 16px; -fx-font-weight: bold;");
		erroCodigo.setVisible(false);

		VBox erroBox = new VBox(erroCodigo);
		erroBox.setAlignment(Pos.BOTTOM_CENTER);

		HBox timeBox = new HBox(codlabel, reenvio);
		timeBox.setPadding(new Insets(0, 0, 0, 948));


		// ====================
		// VBox central com os elementos.
		// ====================
		VBox centerBox = new VBox(25);
		centerBox.setAlignment(Pos.TOP_CENTER);
		centerBox.setPadding(new Insets(60, 0, 0, 0));

		verlogo.setFitHeight(180);
		verlogo.setFitWidth(180);

		centerBox.getChildren().addAll(verlogo, CodField, confirmar, timeBox, erroBox);

		// ====================================
		// StackPane para centralizar tudo no centro real da tela.
		// ====================================
		StackPane centerWrapper = new StackPane(centerBox);
		centerWrapper.setAlignment(Pos.CENTER); // reforça centralização

		// =======================================================
		// Ajusta tamanho do 'centerWrapper' ao do container e define ele como centro do layout.
		// =======================================================
		centerWrapper.prefWidthProperty().bind(widthProperty());
		centerWrapper.prefHeightProperty().bind(heightProperty());
		setCenter(centerWrapper);
	}
	
	// =========================
	// Método para confirmação de cadastro.
	// =========================
	public void mostrarTelaSucesso(Runnable acaoDepois) {

		// Criar imagem de sucesso
		Image setaVerde = new Image(getClass().getResource("/LoginInfinit/imagens/seta-verde.png").toExternalForm());
		ImageView setaView = new ImageView(setaVerde);
		setaView.setFitWidth(100);
		setaView.setFitHeight(100);
		setaView.setPreserveRatio(true);

		// Criar label da mensagem
		Label mensagem = new Label("Cadastro confirmado! Vamos começar.");
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

	// ====================
	// Getters and Setters..
	// ====================

	public Button getConfirmar() {
		return confirmar;
	}

	public void setConfirmar(Button confirmar) {
		this.confirmar = confirmar;
	}

	public Label getReenvio() {
		return reenvio;
	}

	public void setReenvio(Label reenvio) {
		this.reenvio = reenvio;
	}

	public TextField getCodField() {
		return CodField;
	}

	public void setCodField(TextField codField) {
		CodField = codField;
	}

	public ImageView getVerFoto() {
		return verFoto;
	}

	public void setVerFoto(ImageView verFoto) {
		this.verFoto = verFoto;
	}

	public StackPane getClickableArea() {
		return clickableArea;
	}

	public void setClickableArea(StackPane clickableArea) {
		this.clickableArea = clickableArea;
	}

	public Label getErroCodigo() {
		return erroCodigo;
	}

	public void setErroCodigo(Label erroCodigo) {
		this.erroCodigo = erroCodigo;
	}

}
