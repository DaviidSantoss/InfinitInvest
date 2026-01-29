package MainInfinit;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO.SessaoTemp;
import ControllerInfinit.BarraController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class lancamentos extends BorderPane {

	// ====================
	// Dependências da classe.
	// ====================
	private String corFundo = "#212121";
	private BarraLateral barra;
	private BarraController barraController;

	// ====================
	// UI da lista
	// ====================
	private final VBox root = new VBox(10);
	private final VBox lista = new VBox(6);

	private static final double LARGURA_COLUNA = 170;
	private static final double ESPACAMENTO_COLUNAS = 40;
	private static final double ALTURA_LINHA = 60;

	@SuppressWarnings("deprecation")
	private static final Locale BR = new Locale("pt", "BR");
	private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(BR);

	private static final DateTimeFormatter UI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter DB_DATE = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

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
		// Conteúdo
		// ====================
		montarUI();
		setCenter(root);

		// ====================
		// Carrega lançamentos
		// ====================
		Platform.runLater(this::carregarLancamentos);
	}

	private void montarUI() {
		root.setPadding(new Insets(25));
		root.setStyle("-fx-background-color: #212121;");
		root.setAlignment(Pos.TOP_LEFT);

		Label titulo = new Label("Lançamentos");
		titulo.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

		HBox header = criarHeaderColunas();

		lista.setPadding(new Insets(0));
		lista.setStyle("-fx-background-color: transparent;");

		root.getChildren().addAll(titulo, header, lista);
	}

	private HBox criarHeaderColunas() {
		HBox linha = new HBox(ESPACAMENTO_COLUNAS);
		linha.setAlignment(Pos.CENTER_LEFT);
		linha.setPadding(new Insets(ALTURA_LINHA / 3, 35, ALTURA_LINHA / 3, 35));
		linha.setStyle("-fx-background-color: #1e1f21; -fx-background-radius: 6; -fx-border-color: #323335; -fx-border-radius: 6;");

		linha.getChildren().addAll(headerLabel("Ativo"), headerLabel("Quantidade"), headerLabel("Preço Unitário"), headerLabel("Data Lançamento"), headerLabel("Total"), headerLabel("") // coluna do
																																															// "..."
		);

		return linha;
	}

	private Label headerLabel(String txt) {
		Label lbl = new Label(txt);
		lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #d0d0d0; -fx-font-weight: bold;");
		lbl.setPrefWidth(LARGURA_COLUNA);
		return lbl;
	}

	private void carregarLancamentos() {
		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			Dao dao = new Dao();
			var lancs = dao.listarLancamentos(usuarioId);

			lista.getChildren().clear();
			for (var l : lancs) {
				lista.getChildren().add(criarLinhaLancamento(l));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private GridPane criarLinhaLancamento(Dao.Lancamento l) {
		GridPane grid = new GridPane();
		grid.setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 6;");
		grid.setPadding(new Insets(ALTURA_LINHA / 3, 35, ALTURA_LINHA / 3, 35));
		grid.setHgap(ESPACAMENTO_COLUNAS);
		grid.setId("lanc_" + l.id);

		// 6 colunas (última é o menu)
		for (int i = 0; i < 6; i++) {
			grid.getColumnConstraints().add(new ColumnConstraints(LARGURA_COLUNA));
		}

		Label lblAtivo = cellLabel(l.ativo);
		Label lblQtd = cellLabel(formatQtd(l.quantidade));
		Label lblPu = cellLabel(CURRENCY.format(l.precoUnitario));
		Label lblData = cellLabel(formatDataUi(l.dataLancamentoIso));
		Label lblTotal = cellLabel(CURRENCY.format(l.total));

		Label menu = new Label("⋯");
		menu.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: bold;");
		menu.setCursor(Cursor.HAND);
		menu.setMinWidth(30);

		grid.add(lblAtivo, 0, 0);
		grid.add(lblQtd, 1, 0);
		grid.add(lblPu, 2, 0);
		grid.add(lblData, 3, 0);
		grid.add(lblTotal, 4, 0);
		grid.add(menu, 5, 0);

		// hover
		grid.setOnMouseEntered(e -> grid.setStyle("-fx-background-color: #333437; -fx-background-radius: 6;"));
		grid.setOnMouseExited(e -> grid.setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 6;"));

		// menu “...”
		ContextMenu cm = new ContextMenu();
		MenuItem miEditar = new MenuItem("Editar");
		MenuItem miExcluir = new MenuItem("Excluir");
		cm.getItems().addAll(miEditar, miExcluir);

		menu.setOnMouseClicked(e -> cm.show(menu, e.getScreenX(), e.getScreenY()));

		miExcluir.setOnAction(e -> excluirLancamento(l));
		miEditar.setOnAction(e -> entrarModoEdicao(grid, l));

		return grid;
	}

	private Label cellLabel(String txt) {
		Label lbl = new Label(txt);
		lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffffff;");
		return lbl;
	}

	private void excluirLancamento(Dao.Lancamento l) {
		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			Dao dao = new Dao();
			dao.deletarLancamento(usuarioId, l.id);

			// ✅ recalcula o agregado do ativo (quantidade + PM) com base nos lançamentos restantes
			dao.recalcularAtivoPorLancamentos(usuarioId, l.categoria, l.ativo);

			// refresh UI
			carregarLancamentos();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void entrarModoEdicao(GridPane grid, Dao.Lancamento lOld) {
		// troca labels por textfields nas colunas editáveis:
		// quantidade, preço unitário, data (dd/MM/yyyy), total (auto)
		try {
			TextField tfQtd = new TextField(formatQtd(lOld.quantidade));
			TextField tfPu = new TextField(formatMoneyNoSymbol(lOld.precoUnitario));
			TextField tfData = new TextField(formatDataUi(lOld.dataLancamentoIso));
			Label lblTotal = new Label(CURRENCY.format(lOld.total));
			lblTotal.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffffff;");

			tfQtd.setStyle(inputStyle());
			tfPu.setStyle(inputStyle());
			tfData.setStyle(inputStyle());

			// remove nodes antigos das colunas 1..4 (exceto 0 e 5)
			grid.getChildren().removeIf(n -> {
				Integer col = GridPane.getColumnIndex(n);
				return col != null && col >= 1 && col <= 4;
			});

			grid.add(tfQtd, 1, 0);
			grid.add(tfPu, 2, 0);
			grid.add(tfData, 3, 0);
			grid.add(lblTotal, 4, 0);

			// total recalcula ao digitar
			Runnable recalcTotal = () -> {
				try {
					double qtd = parseNumber(tfQtd.getText());
					double pu = parseMoney(tfPu.getText());
					double total = qtd * pu;
					lblTotal.setText(CURRENCY.format(total));
				} catch (Exception ex) {
					lblTotal.setText("R$ 0,00");
				}
			};
			tfQtd.textProperty().addListener((o, a, b) -> recalcTotal.run());
			tfPu.textProperty().addListener((o, a, b) -> recalcTotal.run());

			// salvar no ENTER (em qualquer campo)
			javafx.event.EventHandler<javafx.scene.input.KeyEvent> handlerSave = ev -> {
				if (ev.getCode() == KeyCode.ENTER) {
					salvarEdicaoLancamento(lOld, tfQtd, tfPu, tfData);
				} else if (ev.getCode() == KeyCode.ESCAPE) {
					carregarLancamentos(); // cancela e recarrega
				}
			};


			tfQtd.setOnKeyPressed(handlerSave);
			tfPu.setOnKeyPressed(handlerSave);
			tfData.setOnKeyPressed(handlerSave);

			tfQtd.requestFocus();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private void salvarEdicaoLancamento(Dao.Lancamento lOld, TextField tfQtd, TextField tfPu, TextField tfData) {
		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			double qtd = parseNumber(tfQtd.getText());
			double pu = parseMoney(tfPu.getText());
			String dataIso = parseDataToIso(tfData.getText()); // dd/MM/yyyy -> yyyy-MM-dd
			double total = qtd * pu;

			Dao dao = new Dao();

			Dao.Lancamento lNew = new Dao.Lancamento(lOld.id, usuarioId, lOld.categoria, lOld.ativo, qtd, pu, dataIso, total);

			dao.atualizarLancamento(usuarioId, lNew);

			// ✅ recalcula o agregado do ativo (quantidade + PM) com base nos lançamentos
			dao.recalcularAtivoPorLancamentos(usuarioId, lOld.categoria, lOld.ativo);

			carregarLancamentos();

		} catch (Exception ex) {
			ex.printStackTrace();
			// se quiser: mostrar alerta na tela
		}
	}

	// =========================
	// Helpers
	// =========================

	private String inputStyle() {
		return "-fx-background-color: #1e1f21; -fx-text-fill: white; -fx-border-color: #3a3b3d; -fx-border-radius: 6; -fx-background-radius: 6;";
	}

	private static String formatQtd(double q) {
		// ações normalmente inteiro, mas você pode manter 2 casas se quiser
		String s = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", q).replace('.', ',');
		// opcional: corta ,00
		if (s.endsWith(",00"))
			s = s.substring(0, s.length() - 3);
		return s;
	}

	private static String formatMoneyNoSymbol(double v) {
		// "23,00" (sem R$)
		String s = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", v).replace('.', ',');
		return s;
	}

	private static String formatDataUi(String iso) {
		try {
			LocalDate d = LocalDate.parse(iso, DB_DATE);
			return d.format(UI_DATE);
		} catch (Exception e) {
			return "";
		}
	}

	private static String parseDataToIso(String ddMMyyyy) {
		LocalDate d = LocalDate.parse(ddMMyyyy.trim(), UI_DATE);
		return d.format(DB_DATE);
	}

	private static double parseNumber(String txt) {
		if (txt == null)
			return 0;
		txt = txt.trim().replace(".", "").replace(",", ".");
		if (txt.isBlank())
			return 0;
		return Double.parseDouble(txt);
	}

	private static double parseMoney(String txt) {
		if (txt == null)
			return 0;
		txt = txt.replace("R$", "").replace("\u00A0", " ").trim();
		txt = txt.replace(".", "").replace(",", ".").replace(" ", "");
		if (txt.isBlank())
			return 0;
		return Double.parseDouble(txt);
	}
}
