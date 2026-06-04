package ControllerInfinit;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import Apis.Brapi;
import Apis.LogoFetcher;
import Apis.LogoKit;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Popup;
import javafx.util.Duration;

public class PesquisaController {

	// ==========================================================
	// CONTEXTO DO CONTROLLER
	// ==========================================================
	// Controla a busca/autocomplete de ativos.
	// Faz debounce da digitação, consulta API, renderiza resultados com logo e preço
	// e abre o ativo no TradingView ao selecionar.

	private final PauseTransition debounce = new PauseTransition(Duration.millis(250));
	private final ExecutorService executor = Executors.newFixedThreadPool(2);

	private final Popup popup = new Popup();
	private final ListView<String> listView = new ListView<>();

	private Future<?> currentTask;

	// ==========================================================
	// API PÚBLICA
	// ==========================================================

	public void configurar(TextField searchField) {
		configurarPopup(searchField);
		configurarEventos(searchField);
	}

	// ==========================================================
	// POPUP / LISTA
	// ==========================================================

	private void configurarPopup(TextField searchField) {
		listView.setPrefWidth(350);
		listView.setPrefHeight(260);

		listView.setStyle("""
				    -fx-background-color: transparent;
				    -fx-control-inner-background: transparent;
				    -fx-padding: 0;
				""");

		listView.setCellFactory(lv -> new ResultadoCell());

		VBox root = new VBox(listView);
		root.setPadding(new Insets(10));
		root.setStyle("""
				    -fx-background-color: #1e1e1e;
				    -fx-background-radius: 12;
				    -fx-border-color: #555;
				    -fx-border-radius: 10;
				""");

		popup.getContent().add(root);
		popup.setAutoHide(true);

		listView.setOnMouseClicked(e -> {
			String selected = listView.getSelectionModel().getSelectedItem();
			if (selected != null) {
				abrirAtivo(selected);
				popup.hide();
			}
		});
	}

	// ==========================================================
	// EVENTOS / NAVEGAÇÃO
	// ==========================================================

	private void configurarEventos(TextField searchField) {
		searchField.textProperty().addListener((obs, old, val) -> {
			String q = (val == null) ? "" : val.trim();

			if (q.isBlank()) {
				listView.getItems().clear();
				popup.hide();
				return;
			}

			debounce.setOnFinished(e -> buscarEExibir(searchField, q));
			debounce.playFromStart();
		});

		searchField.setOnKeyPressed(e -> {
			if (!popup.isShowing())
				return;

			if (e.getCode() == KeyCode.DOWN) {
				listView.requestFocus();
				listView.getSelectionModel().selectFirst();
				e.consume();
				return;
			}

			if (e.getCode() == KeyCode.ENTER) {
				String selected = listView.getSelectionModel().getSelectedItem();
				if (selected == null && !listView.getItems().isEmpty()) {
					selected = listView.getItems().get(0);
				}
				if (selected != null) {
					abrirAtivo(selected);
					popup.hide();
				}
				e.consume();
			}

			if (e.getCode() == KeyCode.ESCAPE) {
				popup.hide();
			}
		});

		searchField.sceneProperty().addListener((o, oldScene, newScene) -> {
			if (newScene != null) {
				newScene.windowProperty().addListener((oo, oldW, newW) -> {
					if (newW != null) {
						newW.xProperty().addListener((a, b, c) -> reposicionarPopup(searchField));
						newW.yProperty().addListener((a, b, c) -> reposicionarPopup(searchField));
					}
				});
			}
		});
	}

	// ==========================================================
	// BUSCA
	// ==========================================================

	private void buscarEExibir(TextField searchField, String query) {
		if (currentTask != null)
			currentTask.cancel(true);

		currentTask = executor.submit(() -> {
			try {
				List<String> resultados = Brapi.buscarAtivosPorTipo(Brapi.tipoDoAtivo.DESCONHECIDO, query);

				String qUp = query.trim().toUpperCase();
				if (queryEhTickerCompleto(qUp)) {
					resultados = resultados.stream().filter(line -> extrairTickerDaLinha(line).equals(qUp)).toList();
				}

				List<String> top = new ArrayList<>();
				for (int i = 0; i < Math.min(12, resultados.size()); i++)
					top.add(resultados.get(i));

				Platform.runLater(() -> {
					listView.getItems().setAll(top);

					if (top.isEmpty()) {
						popup.hide();
						return;
					}

					mostrarPopupAbaixo(searchField);
				});

			} catch (Exception ex) {
				Platform.runLater(() -> popup.hide());
			}
		});
	}

	// ==========================================================
	// POSICIONAMENTO DO POPUP
	// ==========================================================

	private void mostrarPopupAbaixo(TextField searchField) {
		if (searchField.getScene() == null)
			return;

		reposicionarPopup(searchField);

		if (!popup.isShowing()) {
			popup.show(searchField.getScene().getWindow());
		}
	}

	private void reposicionarPopup(TextField searchField) {
		if (searchField.getScene() == null)
			return;

		Point2D p = searchField.localToScreen(0, searchField.getHeight() + 6);
		if (p == null)
			return;

		popup.setX(p.getX());
		popup.setY(p.getY());

		listView.setPrefWidth(searchField.getWidth());
	}

	// ==========================================================
	// AÇÃO: ABRIR NO TRADINGVIEW
	// ==========================================================

	private void abrirAtivo(String item) {
		String ticker = extrairTicker(item);

		String url;
		if (ticker.matches("^[A-Z]{4}\\d{1,2}$")) {
			url = "https://br.tradingview.com/chart/?symbol=BMFBOVESPA:" + ticker;
		} else if (ticker.endsWith(".SA")) {
			String t = ticker.replace(".SA", "");
			url = "https://br.tradingview.com/chart/?symbol=BMFBOVESPA:" + t;
		} else {
			url = "https://br.tradingview.com/search/?query=" + ticker;
		}

		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(new URI(url));
			}
		} catch (Exception ignored) {
		}
	}

	// ==========================================================
	// PARSING
	// ==========================================================

	private String extrairTicker(String s) {
		if (s == null)
			return "";

		String x = s.trim();

		int dash = x.indexOf(" - ");
		if (dash > 0) {
			String left = x.substring(0, dash).trim();
			if (left.contains("(") && left.contains(")")) {
				int a = left.lastIndexOf('(');
				int b = left.lastIndexOf(')');
				if (a >= 0 && b > a)
					return left.substring(a + 1, b).trim().toUpperCase();
			}
			return left.toUpperCase();
		}

		if (x.contains("(") && x.contains(")")) {
			int a = x.lastIndexOf('(');
			int b = x.lastIndexOf(')');
			if (a >= 0 && b > a)
				return x.substring(a + 1, b).trim().toUpperCase();
		}

		String[] parts = x.split("\\s+");
		return parts.length > 0 ? parts[0].toUpperCase() : x.toUpperCase();
	}

	private boolean queryEhTickerCompleto(String q) {
		if (q == null)
			return false;
		return q.trim().toUpperCase().matches("^[A-Z]{4}\\d{1,2}$");
	}

	private String extrairTickerDaLinha(String item) {
		if (item == null)
			return "";
		int dash = item.indexOf(" - ");
		String left = (dash > 0) ? item.substring(0, dash).trim() : item.trim();

		if (left.contains("(") && left.contains(")")) {
			int a = left.lastIndexOf('(');
			int b = left.lastIndexOf(')');
			if (a >= 0 && b > a)
				return left.substring(a + 1, b).trim().toUpperCase();
		}
		return left.toUpperCase();
	}

	// ==========================================================
	// CELL (logo com crop/zoom + preço async)
	// ==========================================================

	private static class ResultadoCell extends ListCell<String> {

		private final HBox root = new HBox(8);
		private final ImageView logo = new ImageView();
		private final VBox texts = new VBox(2);
		private final Text tTicker = new Text();
		private final Text tName = new Text();
		private final Text tPrice = new Text("—");

		private boolean hovering = false;

		// ajuste o “zoom” (quanto maior, mais ele corta borda branca)
		private static final double LOGO_ZOOM = 1.65;

		ResultadoCell() {
			logo.setFitWidth(40);
			logo.setFitHeight(40);
			logo.setPreserveRatio(true);
			logo.setSmooth(true);

			tTicker.setStyle("-fx-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
			tName.setStyle("-fx-fill: white; -fx-font-size: 14px;");
			tPrice.setStyle("-fx-fill: #d0d0d0; -fx-font-size: 12px;");

			Text sep = new Text(" - ");
			sep.setStyle("-fx-fill: white; -fx-font-size: 14px;");

			texts.getChildren().addAll(new HBox(6, tTicker, sep, tName), tPrice);
			root.getChildren().addAll(logo, texts);

			root.setPadding(new Insets(8));
			setStyle("-fx-background-color: transparent;");

			aplicarEstilo(false, false);

			this.hoverProperty().addListener((obs, old, hov) -> {
				hovering = hov;
				aplicarEstilo(hovering, isSelected());
			});

			selectedProperty().addListener((obs, wasSel, isSel) -> {
				aplicarEstilo(hovering, isSel);
			});
		}

		private void aplicarEstilo(boolean hover, boolean selected) {
			if (selected) {
				root.setStyle("""
							-fx-background-color: #4f4f4f;
							-fx-background-radius: 10;
							-fx-border-color: #777;
							-fx-border-radius: 10;
						""");
				return;
			}

			if (hover) {
				root.setStyle("""
							-fx-background-color: #4b4b4b;
							-fx-background-radius: 10;
							-fx-border-color: #666;
							-fx-border-radius: 10;
						""");
			} else {
				root.setStyle("""
							-fx-background-color: #424242;
							-fx-background-radius: 10;
							-fx-border-color: #555;
							-fx-border-radius: 10;
						""");
			}
		}

		private void aplicarCropCentral(Image img, double zoom) {
			if (img == null)
				return;

			double w = img.getWidth();
			double h = img.getHeight();
			if (w <= 0 || h <= 0)
				return;

			double vw = w / zoom;
			double vh = h / zoom;

			double x = (w - vw) / 2.0;
			double y = (h - vh) / 2.0;

			logo.setViewport(new Rectangle2D(x, y, vw, vh));
		}

		@Override
		protected void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);

			if (empty || item == null) {
				setGraphic(null);
				setText(null);
				return;
			}

			String ticker = item;
			String nome = "";
			int dash = item.indexOf(" - ");
			if (dash > 0) {
				ticker = item.substring(0, dash).trim();
				nome = item.substring(dash + 3).trim();
			}

			final String tickerFinal = ticker;
			final String itemFinal = item;

			tTicker.setText(tickerFinal.toUpperCase());
			tName.setText(nome);
			tPrice.setText("Carregando preço...");

			logo.setImage(null);
			logo.setViewport(null);

			String logoUrl = LogoKit.byStockTicker(tickerFinal);

			CompletableFuture.supplyAsync(() -> LogoFetcher.loadImage(logoUrl)).thenAccept(img -> Platform.runLater(() -> {
				if (getItem() != null && getItem().equals(itemFinal)) {
					if (img != null) {
						logo.setImage(img);
						aplicarCropCentral(img, LOGO_ZOOM);
					}
				}
			}));

			CompletableFuture.supplyAsync(() -> {
				String tk = tickerFinal.toUpperCase();
				int dash2 = tk.indexOf(" ");
				if (dash2 > 0)
					tk = tk.substring(0, dash2).trim();
				return Brapi.buscarPrecoAcoes(tk);
			}).thenAccept(p -> Platform.runLater(() -> {
				if (getItem() != null && getItem().equals(itemFinal)) {
					if (p == null || p <= 0)
						tPrice.setText("—");
					else
						tPrice.setText(String.format("R$ %.2f", p).replace(".", ","));
				}
			}));

			setGraphic(root);
		}
	}
}
