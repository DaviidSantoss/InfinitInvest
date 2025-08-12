package LoginInfinit;


import ControllerInfinit.LoginController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class LoginForm extends VBox {

	// ==================== ====================
	// Controlador da lógica de login e botão para acionar o processo.
	// ==================== ====================
	private LoginController controller;
	private TextField emailField = new TextField();
	private PasswordField senhaField = new PasswordField();
	private Button logar = new Button("Loginss");
	private CheckBox checkBox = new CheckBox("Continuar Conectado.");



	// ====================
	// Defindo a cor do BackGround.
	// ====================
	private String cores = "#212121";

	// ====================
	// Definimos o tamanho da nossa caixa
	// ====================
	public LoginForm() {
		this(0, 0);
	}

	public LoginForm(int altura, int largura) {

		// ====================
		// Defindo o BackGround.
		// ====================
		BackgroundFill fundo = new BackgroundFill(Color.web(cores), new CornerRadii(20), Insets.EMPTY);
		setBackground(new Background(fundo));

		// ====================
		// Aplicando o estilo css e a fonte.
		// ====================
		getStylesheets().add(getClass().getResource("/LoginInfinit/Login.css").toExternalForm());
		Font opensans = Font.loadFont(getClass().getResourceAsStream("fonts/Baskervville/Baskervville-Regular.ttf"), 32);

		// ===================================
		// Componentes visuais, imagens,labels, texto e botões.
		// ===================================
		Image foto = new Image(getClass().getResource("/LoginInfinit/imagens/logo.png").toExternalForm());
		ImageView verFoto = new ImageView(foto);

		verFoto.setFitHeight(300);
		verFoto.setFitWidth(250);
		verFoto.setPreserveRatio(true);


		Label titulo = new Label("InfinitInvest");
		titulo.setFont(opensans);
		titulo.setTextFill(Color.WHITE);

		VBox painel = new VBox(verFoto, titulo);
		painel.setAlignment(Pos.CENTER);
		painel.setSpacing(0);


		emailField.setPrefWidth(500);
		emailField.setPromptText("Email"); // Texto de placeholder
		emailField.getStyleClass().add("campo-transparente");

		HBox linhasEmail = new HBox(10, emailField);
		linhasEmail.setAlignment(Pos.CENTER_LEFT);


		senhaField.setPrefWidth(500);

		senhaField.setPromptText("Senha");
		senhaField.getStyleClass().add("campo-transparente");

		Label cadastro = new Label("Não possui cadastro?");
		cadastro.getStyleClass().add("cadastro");
		cadastro.setTranslateX(-36);

		Label aqui = new Label("            ");
		aqui.getStyleClass().addAll("cadastro");
		aqui.setStyle("-fx-font-weight: bold;" + "-fx-cursor: hand;");

		HBox vcadastro = new HBox(cadastro, aqui);
		vcadastro.setAlignment(Pos.BASELINE_RIGHT);

		VBox linhasSenha = new VBox(10, senhaField, vcadastro);
		linhasSenha.setAlignment(Pos.CENTER);

		checkBox.getStyleClass().add("cadastro");
		checkBox.setTranslateX(40);
		checkBox.setTranslateY(10);

		logar.setPrefWidth(270);
		logar.setPrefHeight(30);
		logar.getStyleClass().add("botao-login");

		VBox Butao = new VBox(logar, checkBox);
		Butao.setAlignment(Pos.CENTER);
		Butao.setSpacing(10);
		Butao.setPadding(new Insets(0, 0, 60, 0));

		VBox conteudo = new VBox(linhasEmail, linhasSenha, Butao);
		conteudo.setSpacing(40);

		// =======================================================
		// Adicionando os elementos visuais a classe e setando seus respectivos espaçamentos.
		// =======================================================
		getChildren().addAll(painel, conteudo);
		setPadding(new Insets(10, 150, 150, 150)); // top, right, bottom, left

		// ====================
		// Define alinhamento central.
		// ====================
		setAlignment(Pos.TOP_CENTER);
		setMinHeight(altura);
		setMinWidth(largura);
	}

	// ====================
	// Metodos auxiliares.
	// ====================
	public Button getLogar() {
		return logar;
	}

	public void setLogar(Button logar) {
		this.logar = logar;
	}

	public void setController(LoginController controller) {
		this.controller = controller;
		this.controller.configuraracoes();
	}

	public TextField getEmailField() {
		return emailField;
	}

	public void setEmailField(TextField emailField) {
		this.emailField = emailField;
	}

	public PasswordField getSenhaField() {
		return senhaField;
	}

	public void setSenhaField(PasswordField senhaField) {
		this.senhaField = senhaField;
	}

	public CheckBox getCheckBox() {
		return checkBox;
	}

	public void setCheckBox(CheckBox checkBox) {
		this.checkBox = checkBox;
	}

}
