package LoginInfinit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ControllerInfinit.CadastroController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class CadastroForm extends GridPane {

	


	// ====================
	// Campos de entrada
	// ====================
	private TextField nomeField;
	private TextField emailField;
	private PasswordField senhaField;
	private Button cadastrar = new Button("Registrar-se");

	// ====================
	// Labels
	// ====================
	private Label emailerro = new Label("Email inválido");
	private Label nomerro = new Label("Nome inválido");
	private Label senhaerro = new Label("Senha muito curta. Use pelo menos 8 caracteres.");



	// ====================
	// Imagens dos ícones
	// ====================
	private final Image fotoemail = new Image(
			getClass().getResource("/LoginInfinit/imagens/email.png").toExternalForm());
	private final Image fotosenha = new Image(
			getClass().getResource("/LoginInfinit/imagens/senha.png").toExternalForm());
	private final Image fotonome = new Image(getClass().getResource("/LoginInfinit/imagens/name.png").toExternalForm());

	// ====================
	// Getters
	// ====================

	public String getNome() {
		return nomeField.getText();
	}

	public String getEmail() {
		return emailField.getText();
	}

	public TextField getEmailField() {
		return emailField;
	}


	public String getSenha() {
		return senhaField.getText();
	}

	// ====================
	// Setters
	// ====================
	public void setNome(String nome) {
		nomeField.setText(nome);
	}

	public void setEmail(String email) {
		emailField.setText(email);
	}

	public void setSenha(String senha) {
		senhaField.setText(senha);
	}

	public Label emailErro() {
		return emailerro;
	}

	public Label nomeerro() {
		return nomerro;
	}

	public Label senhaerro() {
		return senhaerro;
	}

	// ====================
	// Método de construção (Builder)
	// ====================
	public CadastroForm build() {


		// Layout e estilo
		setPrefSize(1920, 1080);
		getStylesheets().add(getClass().getResource("/LoginInfinit/Login.css").toExternalForm());

		Font opensansT = Font.loadFont(getClass().getResourceAsStream("fonts/Baskervville/Baskervville-Regular.ttf"),
				60);
		String cor = "#3D3D3D";
		setGridLinesVisible(false);

		getColumnConstraints().addAll(co(), co(), co(), co(), co(), co());
		getRowConstraints().addAll(rc(), rc(), rc(), rc(), rc(), rc());

		BackgroundFill fundo = new BackgroundFill(Color.web(cor), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(fundo));

		// Componente lateral (imagem ou outro conteúdo)
		CadastroLogin caixa1 = new CadastroLogin();
		add(caixa1, 0, 0, 3, 6);

		// ====================
		// Labels erros configuração
		// ====================
		emailerro.setVisible(false);
		nomerro.setVisible(false);
		senhaerro.setVisible(false);


		// ====================
		// Título
		// ====================
		Label cad = new Label("Cadastre-se");
		cad.setFont(opensansT);
		cad.setTextFill(Color.WHITE);
		HBox cadBox = new HBox(cad);
		cadBox.setTranslateX(1300);
		cadBox.setTranslateY(230);

		// ====================
		// Campos e erros
		// ====================
		nomeField = new TextField();
		nomeField.setPromptText("Nome");
		nomeField.setPrefWidth(300);
		nomeField.getStyleClass().add("custom-text-field");

		emailField = new TextField();
		emailField.setPromptText("Email");
		emailField.setPrefWidth(300);
		emailField.getStyleClass().add("custom-text-field");

		senhaField = new PasswordField();
		senhaField.setPromptText("Senha");
		senhaField.setPrefWidth(300);
		senhaField.getStyleClass().add("custom-text-field");




		HBox nomeBox = criarCampoComIcone(fotonome, nomeField);
		HBox emailBox = criarCampoComIcone(fotoemail, emailField);
		HBox senhaBox = criarCampoComIcone(fotosenha, senhaField);



		VBox camposComIcones = new VBox(40, nomeBox, nomerro, emailBox, emailerro, senhaBox, senhaerro);
		camposComIcones.setSpacing(25);
		camposComIcones.setTranslateX(1300);
		camposComIcones.setTranslateY(430);

		// ====================
		// Botão de cadastro
		// ====================

		cadastrar.setPrefWidth(270);
		cadastrar.setPrefHeight(30);
		cadastrar.getStyleClass().add("botao-cadastro");


		HBox botao = new HBox(cadastrar);
		botao.setTranslateX(1300);
		botao.setTranslateY(630);
		botao.setAlignment(Pos.CENTER);

		// ====================
		// Adiciona à tela
		// ====================
		getChildren().addAll(cadBox, camposComIcones, botao);

		return this;
	}

	// ====================
	// Funções auxiliares
	// ====================

	private HBox criarCampoComIcone(Image icone, TextField campo) {
		ImageView imagem = new ImageView(icone);
		imagem.setFitWidth(16);
		imagem.setFitHeight(16);
		imagem.getStyleClass().add("text-field-icon");

		HBox box = new HBox(10, imagem, campo);
		box.setAlignment(Pos.CENTER_LEFT);
		box.getStyleClass().add("custom-field-box");
		return box;
	}

	public boolean validaremail(String email) {
		String regex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(email);
		return matcher.matches();
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

	public Button getButtonCadastro() {
		return cadastrar;
	}

	public void setController(CadastroController controller) {
		controller.configurarAcoes();
	}

}
