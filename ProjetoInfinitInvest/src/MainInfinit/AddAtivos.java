package MainInfinit;

import java.time.LocalDate;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class AddAtivos extends StackPane {

	// -----------------------------
	// ATRIBUTOS (TODOS OS CAMPOS)
	// -----------------------------
	private double xOffset = 0;
	private double yOffset = 0;

	private Stage dialog; // ✅ guarda referência do stage atual

	private Button AdicionarAtivios = new Button("+ Adicionar");

	private ToggleButton compraBtn = new ToggleButton("Compra");
	private ToggleButton vendaBtn = new ToggleButton("Venda");

	private ComboBox<String> tipoCombo = new ComboBox<>();
	private TextField ativoCombo = new TextField();

	private TextField precoField = new TextField("R$ 0,00");
	private TextField qtdField = new TextField("1");

	private Label totalLabel = new Label("Valor total: R$ 0,00");

	private DatePicker datePicker;
	public boolean precoSetByApi = false;

	// ✅ overlay (funciona em StackPane)
	private final StackPane overlaySucesso = new StackPane();



	// ---- CAMPOS RENDA FIXA ----
	private TextField emissorField;
	private ComboBox<String> indexadorCombo;
	private ComboBox<String> formaCombo;
	private ComboBox<String> tipoTituloCombo;
	private TextField taxaField;

	// -----------------------------
	// MÉTODO PRINCIPAL (SHOW)
	// -----------------------------
	public void show(Stage owner) {

		// ✅ guarda o stage na instância (pra usar no mostrarTelaSucesso)
		dialog = new Stage();
		dialog.initOwner(owner);
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initStyle(StageStyle.UNDECORATED);

		precoField.textProperty().addListener((obs, oldVal, newVal) -> {

			if (precoSetByApi) {
				precoSetByApi = false;
				return;
			}

			if (newVal.equals(oldVal))
				return;

			String clean = newVal.replace("R$", "").trim().replaceAll("[^0-9,]", "");

			if (clean.isEmpty()) {
				precoField.setText("R$ 0,00");
				return;
			}

			if (clean.contains(",")) {
				String[] parts = clean.split(",");
				if (parts.length > 1 && parts[1].length() > 2)
					clean = parts[0] + "," + parts[1].substring(0, 2);
			}

			precoField.setText("R$ " + clean);
		});

		// -----------------------------------------------------------------------
		// 1) BARRA SUPERIOR
		// -----------------------------------------------------------------------

		Label title = new Label("Adicionar Ativo");
		title.setFont(Font.font(16));
		title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

		Button closeBtn = new Button("✕");
		closeBtn.setCursor(Cursor.HAND);
		closeBtn.setOnAction(e -> dialog.close());
		closeBtn.setStyle("-fx-background-color: red; -fx-text-fill: white;");

		Region spacerTop = new Region();
		HBox.setHgrow(spacerTop, Priority.ALWAYS);

		HBox topBar = new HBox(10, title, spacerTop, closeBtn);
		topBar.setPadding(new Insets(14, 14, 14, 16));
		topBar.setStyle("-fx-background-color: #232529;");

		topBar.setOnMousePressed(e -> {
			xOffset = e.getSceneX();
			yOffset = e.getSceneY();
		});

		topBar.setOnMouseDragged(e -> {
			dialog.setX(e.getScreenX() - xOffset);
			dialog.setY(e.getScreenY() - yOffset);
		});

		// -----------------------------------------------------------------------
		// 2) TOGGLE COMPRA / VENDA
		// -----------------------------------------------------------------------

		compraBtn.setSelected(true);
		compraBtn.setPrefWidth(280);
		vendaBtn.setPrefWidth(280);

		compraBtn.getStyleClass().add("botao");
		vendaBtn.getStyleClass().add("botao");

		ToggleGroup tg = new ToggleGroup();
		compraBtn.setToggleGroup(tg);
		vendaBtn.setToggleGroup(tg);

		StackPane togglePane = new StackPane(compraBtn, vendaBtn);
		StackPane.setAlignment(compraBtn, Pos.TOP_LEFT);
		StackPane.setMargin(compraBtn, new Insets(10, 0, 0, 20));

		StackPane.setAlignment(vendaBtn, Pos.TOP_RIGHT);
		StackPane.setMargin(vendaBtn, new Insets(10, 10, 0, 20));

		// -----------------------------------------------------------------------
		// 3) GRID PRINCIPAL
		// -----------------------------------------------------------------------

		GridPane grid = new GridPane();
		grid.setPadding(new Insets(10));
		grid.setHgap(10);
		grid.setVgap(10);

		ColumnConstraints col1 = new ColumnConstraints();
		col1.setPercentWidth(50);
		col1.setHgrow(Priority.ALWAYS);

		ColumnConstraints col2 = new ColumnConstraints();
		col2.setPercentWidth(50);
		col2.setHgrow(Priority.ALWAYS);

		grid.getColumnConstraints().addAll(col1, col2);

		// -----------------------------------------------------------------------
		// 4) CAMPOS BASE (AÇÕES / FIIs / ETFs / Cripto)
		// -----------------------------------------------------------------------

		Label tipoLabel = new Label("Tipo de ativo");
		tipoLabel.getStyleClass().add("form-label");

		tipoCombo.getItems().addAll("Ações", "FIIs", "Criptomoeda", "ETFs", "Tesouro Direto", "Renda fixa");
		tipoCombo.setValue("Ações");
		tipoCombo.getStyleClass().add("combo-box");
		tipoCombo.setMinHeight(40);
		tipoCombo.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(tipoCombo, Priority.ALWAYS);

		Label ativoLabel = new Label("Ativo");
		ativoLabel.getStyleClass().add("form-label");
		ativoCombo.getStyleClass().add("combo-box");
		ativoCombo.setMinHeight(40);
		ativoCombo.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(ativoCombo, Priority.ALWAYS);

		Label dataLabel = new Label("Data");
		dataLabel.getStyleClass().add("form-label");

		this.datePicker = new DatePicker(LocalDate.now());
		datePicker.setMinHeight(40);
		datePicker.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(datePicker, Priority.ALWAYS);

		Label precoLabel = new Label("Valor em R$");
		precoLabel.getStyleClass().add("form-label");

		precoField.getStyleClass().add("combo-box");
		precoField.setMinHeight(40);
		precoField.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(precoField, Priority.ALWAYS);

		Label qtdLabel = new Label("Quantidade");
		qtdLabel.getStyleClass().add("form-label");

		qtdField.getStyleClass().add("combo-box");
		qtdField.setMinHeight(40);
		qtdField.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(qtdField, Priority.ALWAYS);

		totalLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: white; -fx-font-weight: bold;");

		// -----------------------------------------------------------------------
		// 5) CAMPOS DE RENDA FIXA
		// -----------------------------------------------------------------------

		Label emissorLabel = new Label("Emissor");
		emissorLabel.getStyleClass().add("form-label");

		this.emissorField = new TextField();
		emissorField.getStyleClass().add("combo-box");
		emissorField.setMinHeight(40);
		emissorField.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(emissorField, Priority.ALWAYS);

		Label indexadorLabel = new Label("Indexador");
		indexadorLabel.getStyleClass().add("form-label");

		this.indexadorCombo = new ComboBox<>();
		indexadorCombo.getItems().addAll("CDI", "CDI+", "IPCA+");
		indexadorCombo.getStyleClass().add("combo-box");
		indexadorCombo.setMinHeight(40);
		indexadorCombo.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(indexadorCombo, Priority.ALWAYS);

		Label formaLabel = new Label("Forma");
		formaLabel.getStyleClass().add("form-label");

		this.formaCombo = new ComboBox<>();
		formaCombo.getItems().addAll("Pré-Fixado", "Pós-Fixado");
		formaCombo.setMinHeight(40);
		formaCombo.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(formaCombo, Priority.ALWAYS);

		Label tipoTituloLabel = new Label("Tipo de Título");
		tipoTituloLabel.getStyleClass().add("form-label");

		this.tipoTituloCombo = new ComboBox<>();
		tipoTituloCombo.getItems().addAll("LCI", "LCA", "CDB", "LC", "LF", "RDB", "Debênture", "CRI", "CRA", "CCB");
		tipoTituloCombo.setMinHeight(40);
		tipoTituloCombo.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(tipoTituloCombo, Priority.ALWAYS);

		Label taxaLabel = new Label("Taxa do Indexador");
		taxaLabel.getStyleClass().add("form-label");

		this.taxaField = new TextField("0,00%");
		taxaField.getStyleClass().add("combo-box");
		taxaField.setMinHeight(40);
		taxaField.setMaxWidth(Double.MAX_VALUE);
		GridPane.setHgrow(taxaField, Priority.ALWAYS);

		// MÁSCARA DE TAXA
		taxaField.textProperty().addListener((obs, oldVal, newVal) -> {
			String clean = newVal.replace("%", "").replace(",", "").replaceAll("[^0-9]", "");

			if (clean.isEmpty()) {
				taxaField.setText("0,00%");
				return;
			}

			while (clean.length() < 3)
				clean = "0" + clean;

			String formatted = clean.substring(0, clean.length() - 2) + "," + clean.substring(clean.length() - 2);
			taxaField.setText(formatted + "%");
			taxaField.positionCaret(taxaField.getText().length());

			if ("Renda fixa".equalsIgnoreCase(tipoCombo.getValue()))
				totalLabel.setText("Rentabilidade: " + taxaField.getText());
		});

		// -----------------------------------------------------------------------
		// 6) BOTTOM BAR
		// -----------------------------------------------------------------------

		AdicionarAtivios.getStyleClass().add("botao");

		HBox bottomBar = new HBox();
		bottomBar.setPadding(new Insets(0, 16, 10, 0));
		bottomBar.setSpacing(10);
		bottomBar.setAlignment(Pos.CENTER_LEFT);

		Region flex = new Region();
		HBox.setHgrow(flex, Priority.ALWAYS);

		bottomBar.getChildren().addAll(totalLabel, flex, AdicionarAtivios);
		GridPane.setHgrow(bottomBar, Priority.ALWAYS);

		// -----------------------------------------------------------------------
		// 7) ADICIONAR CAMPOS AO GRID
		// -----------------------------------------------------------------------

		grid.add(tipoLabel, 0, 0);
		grid.add(tipoCombo, 0, 1);

		grid.add(dataLabel, 0, 2);
		grid.add(datePicker, 0, 3);

		grid.add(precoLabel, 0, 4);
		grid.add(precoField, 0, 5);

		grid.add(ativoLabel, 1, 0);
		grid.add(ativoCombo, 1, 1);

		grid.add(qtdLabel, 1, 2);
		grid.add(qtdField, 1, 3);

		// CAMPOS DE RENDA FIXA
		grid.add(emissorLabel, 1, 0);
		grid.add(emissorField, 1, 1);

		grid.add(indexadorLabel, 1, 2);
		grid.add(indexadorCombo, 1, 3);

		grid.add(tipoTituloLabel, 1, 4);
		grid.add(tipoTituloCombo, 1, 5);

		grid.add(taxaLabel, 1, 6);
		grid.add(taxaField, 1, 7);

		grid.add(formaLabel, 0, 6);
		grid.add(formaCombo, 0, 7);

		// BottomBar inicial para "Ações"
		grid.add(bottomBar, 0, 6, 2, 1);
		GridPane.setMargin(bottomBar, new Insets(0, 0, 10, 0));

		// Inicialmente ocultamos Renda Fixa
		emissorLabel.setVisible(false);
		emissorField.setVisible(false);
		indexadorLabel.setVisible(false);
		indexadorCombo.setVisible(false);
		formaLabel.setVisible(false);
		formaCombo.setVisible(false);
		tipoTituloLabel.setVisible(false);
		tipoTituloCombo.setVisible(false);
		taxaLabel.setVisible(false);
		taxaField.setVisible(false);

		// -----------------------------------------------------------------------
		// 8) LÓGICA DE TROCA ENTRE RENDA FIXA E VARIÁVEL
		// -----------------------------------------------------------------------

		tipoCombo.valueProperty().addListener((obs, oldVal, newVal) -> {

			boolean rf = "Renda fixa".equalsIgnoreCase(newVal);

			emissorLabel.setVisible(rf);
			emissorField.setVisible(rf);
			indexadorLabel.setVisible(rf);
			indexadorCombo.setVisible(rf);
			formaLabel.setVisible(rf);
			formaCombo.setVisible(rf);
			tipoTituloLabel.setVisible(rf);
			tipoTituloCombo.setVisible(rf);
			taxaLabel.setVisible(rf);
			taxaField.setVisible(rf);

			ativoLabel.setVisible(!rf);
			ativoCombo.setVisible(!rf);
			qtdLabel.setVisible(!rf);
			qtdField.setVisible(!rf);

			grid.getChildren().remove(bottomBar);

			if (rf) {
				grid.add(bottomBar, 0, 8, 2, 1);
				totalLabel.setText("Rentabilidade: " + taxaField.getText());
				dialog.setHeight(520);
			} else {
				grid.add(bottomBar, 0, 6, 2, 1);
				totalLabel.setText("Valor total: R$ 0,00");
				dialog.setHeight(430);
			}

			GridPane.setMargin(bottomBar, new Insets(0, 0, 10, 0));
		});

		// -----------------------------------------------------------------------
		// 9) ROOT + CENA + OVERLAY (mantém o X como antes)
		// -----------------------------------------------------------------------

		VBox content = new VBox(togglePane, grid);
		content.setStyle("-fx-background-color: #212121;");
		VBox.setVgrow(grid, Priority.ALWAYS);

		// ✅ exatamente como na versão antiga (isso garante o X)
		VBox root = new VBox(topBar, content);

		// ✅ overlay por cima do root
		overlaySucesso.setVisible(false);
		overlaySucesso.setManaged(false);
		overlaySucesso.setMouseTransparent(true); // ✅ não bloqueia cliques quando escondido

		StackPane sceneRoot = new StackPane(root, overlaySucesso);
		StackPane.setAlignment(overlaySucesso, Pos.CENTER);

		Scene scene = new Scene(sceneRoot, 630, 490);
		scene.getStylesheets().add(AddAtivos.class.getResource("/LoginInfinit/Login.css").toExternalForm());

		dialog.setScene(scene);
		dialog.showAndWait();


	}

	// ✅ agora funciona em StackPane (overlay por cima)
	public void mostrarTelaSucesso(Runnable acaoDepois) {

		Image setaVerde = new Image(getClass().getResource("/LoginInfinit/imagens/seta-verde.png").toExternalForm());
		ImageView setaView = new ImageView(setaVerde);
		setaView.setFitWidth(100);
		setaView.setFitHeight(100);
		setaView.setPreserveRatio(true);

		Label mensagem = new Label("Ativo Adicionado!");
		mensagem.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

		VBox sucessoBox = new VBox(20, setaView, mensagem);
		sucessoBox.setAlignment(Pos.CENTER);

		overlaySucesso.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
		overlaySucesso.getChildren().setAll(sucessoBox);
		overlaySucesso.setAlignment(Pos.CENTER);

		overlaySucesso.setMouseTransparent(false);
		overlaySucesso.setVisible(true);
		overlaySucesso.setManaged(true);


		PauseTransition pause = new PauseTransition(Duration.seconds(3));
		pause.setOnFinished(event -> {
			overlaySucesso.setVisible(false);
			overlaySucesso.setManaged(false);
			overlaySucesso.setMouseTransparent(true);

			if (acaoDepois != null)
				acaoDepois.run();
		});

		pause.play();
	}

	// -----------------------------
	// GETTERS PARA O CONTROLLER
	// -----------------------------

	public Button getAdicionarAtivios() {
		return AdicionarAtivios;
	}

	public ToggleButton getCompraBtn() {
		return compraBtn;
	}

	public ToggleButton getVendaBtn() {
		return vendaBtn;
	}

	public ComboBox<String> getTipoCombo() {
		return tipoCombo;
	}

	public TextField getAtivoCombo() {
		return ativoCombo;
	}

	public TextField getPrecoField() {
		return precoField;
	}

	public TextField getQtdField() {
		return qtdField;
	}

	public Label getTotalLabel() {
		return totalLabel;
	}

	public DatePicker getDatePicker() {
		return datePicker;
	}

	// RENDA FIXA
	public TextField getEmissorField() {
		return emissorField;
	}

	public ComboBox<String> getIndexadorCombo() {
		return indexadorCombo;
	}

	public ComboBox<String> getFormaCombo() {
		return formaCombo;
	}

	public ComboBox<String> getTipoTituloCombo() {
		return tipoTituloCombo;
	}

	public TextField getTaxaField() {
		return taxaField;
	}
}
