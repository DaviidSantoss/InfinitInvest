package MainInfinit;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ListaDeAtivos extends VBox {

	private static final ExecutorService LOGO_EXEC = Executors.newFixedThreadPool(4);

	private final Map<String, CategoriaPane> categorias = new HashMap<>();

	private static final double ALTURA_LINHA = 70;
	private static final double LARGURA_COLUNA = 170;
	private static final double ESPACAMENTO_COLUNAS = 40;

	private static final double ESPACO_BASE = 5;
	private static final double DURACAO_ANIM = 120;

	private final double LARGURA_TOTAL = 7 * LARGURA_COLUNA + (ESPACAMENTO_COLUNAS * 6) + 120;

	private static final NumberFormat CRYPTO_CURRENCY = NumberFormat.getCurrencyInstance();
	static {
		CRYPTO_CURRENCY.setMinimumFractionDigits(2);
		CRYPTO_CURRENCY.setMaximumFractionDigits(8);
	}

	private static String formatCurrencyCripto(double value) {
		return CRYPTO_CURRENCY.format(value);
	}

	@SuppressWarnings("deprecation")
	private static final Locale BR = new Locale("pt", "BR");
	private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(BR);

	public ListaDeAtivos() {
		setFillWidth(true);
		setSpacing(ESPACO_BASE);
		setPadding(new Insets(0));
		setStyle("-fx-background-color: #212121;");
	}

	private CategoriaPane getOuCriarCategoria(String categoriaNome) {
		String nomeNormalizado = (categoriaNome == null ? "" : categoriaNome.trim());
		return categorias.computeIfAbsent(nomeNormalizado, nome -> {
			CategoriaPane categoria = new CategoriaPane(nome);
			getChildren().add(categoria);
			return categoria;
		});
	}

	// ========================================================
	// AÇÕES / FIIs
	// ========================================================

	public void adicionarAcoesEFiis(String categoria, String ticker, String logoUrl, double quantidade, double precoMedio, double precoAtual, double variacaoPercent, double saldo) {

		getOuCriarCategoria(categoria).adicionarLinhaAcoesEFiis(logoUrl, ticker, quantidade, precoMedio, precoAtual, variacaoPercent, 0.0, saldo);
	}


	public void atualizarOuAdicionarAtivo(String categoria, String ticker, String logoUrl, double quantidade, double precoMedio, double precoAtual, double variacaoPercent, double rendimentoMensal,
			double saldo) {

		String cat = (categoria == null ? "" : categoria.trim());

		if ("Tesouro Direto".equalsIgnoreCase(cat)) {
			atualizarOuAdicionarTesouroDireto(ticker, quantidade, variacaoPercent, saldo);
			return;
		}
		if ("Criptomoedas".equalsIgnoreCase(cat)) {
			atualizarOuAdicionarCriptomoeda(logoUrl, ticker, quantidade, precoMedio, precoAtual, variacaoPercent, saldo);
			return;
		}
		if ("Renda Fixa".equalsIgnoreCase(cat)) {
			atualizarOuAdicionarRendaFixa(ticker, quantidade, variacaoPercent, saldo);
			return;
		}

		getOuCriarCategoria(cat).atualizarOuAdicionarLinha(logoUrl, ticker, quantidade, precoMedio, precoAtual, variacaoPercent, rendimentoMensal, saldo);
	}


	// ========================================================
	// CRIPTOMOEDAS
	// ========================================================

	public void adicionarCriptomoeda(String logoUrl, String ticker, double quantidade, double precoMedio, double precoAtual, double variacaoPercent, double saldo) {
		getOuCriarCategoria("Criptomoedas").adicionarLinhaCripto(logoUrl, ticker, quantidade, precoMedio, precoAtual, variacaoPercent, saldo);
	}

	public void atualizarOuAdicionarCriptomoeda(String logoUrl, String ticker, double quantidade, double precoMedio, double precoAtual, double variacaoPercent, double saldo) {
		getOuCriarCategoria("Criptomoedas").atualizarOuAdicionarLinhaCripto(logoUrl, ticker, quantidade, precoMedio, precoAtual, variacaoPercent, saldo);
	}

	// ========================================================
	// TESOURO DIRETO
	// ========================================================

	public void adicionarTesouroDireto(String nomeAtivo, double quantidade, double variacao, double saldo) {
		getOuCriarCategoria("Tesouro Direto").adicionarLinhaTesouroDireto(nomeAtivo, quantidade, variacao, saldo);
	}

	public void atualizarOuAdicionarTesouroDireto(String nomeAtivo, double quantidade, double variacao, double saldo) {
		getOuCriarCategoria("Tesouro Direto").atualizarOuAdicionarLinhaTesouroDireto(nomeAtivo, quantidade, variacao, saldo);
	}

	// ========================================================
	// RENDA FIXA
	// ========================================================

	public void adicionarRendaFixa(String nomeAtivo, double rentabilidade, double variacao, double saldo) {
		getOuCriarCategoria("Renda Fixa").adicionarLinhaRendaFixa(nomeAtivo, rentabilidade, variacao, saldo);
	}

	public void atualizarOuAdicionarRendaFixa(String nomeAtivo, double rentabilidade, double variacao, double saldo) {
		for (var node : categorias.get("Renda Fixa").listaAtivos.getChildren()) {
			if (node instanceof GridPane grid) {
				if (nomeAtivo.equals(grid.getId())) {
					LineData ld = (LineData) grid.getUserData();
					ld.lblPreco.setText(formatVariacao(rentabilidade));
					ld.lblVariacao.setText(formatVariacao(variacao));
					ld.lblSaldo.setText(formatCurrency(saldo));
					return;
				}
			}
		}
		adicionarRendaFixa(nomeAtivo, rentabilidade, variacao, saldo);
	}

	// ========================================================
	// ATUALIZAÇÃO VISUAL DE PREÇO
	// ========================================================

	public void atualizarPrecoVisual(String ticker, double novoPreco) {
		for (var categoria : categorias.values()) {
			for (var node : categoria.listaAtivos.getChildren()) {
				if (node instanceof GridPane grid) {
					String id = grid.getId();
					if (id != null && id.equalsIgnoreCase(ticker)) {
						Object ud = grid.getUserData();
						if (ud instanceof LineData ld) {
							try {
								double quantidade = ld.quantidade;
								double precoReferencia = ld.precoReferencia;

								double precoAtual = novoPreco;
								double variacao = 0;
								if (precoReferencia != 0) {
									variacao = ((precoAtual - precoReferencia) / precoReferencia) * 100.0;
								}
								double saldo = precoAtual * quantidade;

								if (ld.lblPM == null) {
									ld.lblPreco.setText(formatCurrencyCripto(precoAtual));
								} else {
									ld.lblPreco.setText(formatCurrency(precoAtual));
								}
								ld.lblVariacao.setText(formatVariacao(variacao));
								ld.lblSaldo.setText(formatCurrency(saldo));
							} catch (Exception ex) {
								ex.printStackTrace();
							}
						}
						return;
					}
				}
			}
		}
	}

	// ========================================================
	// Helpers de formatação
	// ========================================================

	private static String formatCurrency(double value) {
		return CURRENCY.format(value);
	}

	private static String formatVariacao(double variacaoPercent) {
		String sinal = variacaoPercent > 0 ? "+ " : (variacaoPercent < 0 ? "- " : "");
		double abs = Math.abs(variacaoPercent);
		String formatted = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", abs).replace('.', ',');
		return sinal + formatted + "%";
	}

	private static String formatQuantidadeAcoes(double q) {
		return String.format(Locale.forLanguageTag("pt-BR"), "%.2f", q);
	}

	private static String formatQuantidadeCripto(double q) {
		String s = String.format(Locale.forLanguageTag("pt-BR"), "%.8f", q);
		return s.replace('.', ',');
	}

	private static String formatQuantidadeTesouro(double q) {
		String s = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", q);
		return s.replace('.', ',');
	}

	// ========================================================
	// CATEGORIA EXPANSÍVEL
	// ========================================================

	private class CategoriaPane extends VBox {
		private final VBox conteudo;
		private final VBox listaAtivos;
		private final ImageView arrow;
		private boolean expandido = false;
		private double alturaExpandida = 0;

		public CategoriaPane(String titulo) {

			setSpacing(0);
			this.setPrefWidth(LARGURA_TOTAL);
			this.setMinWidth(LARGURA_TOTAL);
			this.setMaxWidth(LARGURA_TOTAL);
			setStyle("-fx-background-color: transparent;");
			setAlignment(Pos.CENTER_LEFT);

			HBox header = new HBox(10);
			header.setAlignment(Pos.CENTER_LEFT);
			header.setPadding(new Insets(0, 20, 0, 20));
			header.setPrefHeight(ALTURA_LINHA);
			header.setPrefWidth(LARGURA_TOTAL);
			header.setStyle("-fx-background-color: #424242; -fx-background-radius: 8;");
			header.setCursor(Cursor.HAND);

			arrow = new ImageView(new Image("https://cdn-icons-png.flaticon.com/512/32/32195.png", 14, 14, true, true));
			arrow.setRotate(-90);

			Label lblTitulo = new Label(titulo);
			lblTitulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
			header.getChildren().addAll(arrow, lblTitulo);

			conteudo = new VBox(8);
			conteudo.setPrefWidth(LARGURA_TOTAL);
			conteudo.prefWidthProperty().bind(this.widthProperty());
			conteudo.setOpacity(0);
			conteudo.setManaged(false);
			conteudo.setVisible(false);

			HBox headerColunas = criarHeaderColunas(titulo);
			headerColunas.setPrefWidth(LARGURA_TOTAL);

			listaAtivos = new VBox(5);
			listaAtivos.setPrefWidth(LARGURA_TOTAL);

			conteudo.getChildren().addAll(headerColunas, listaAtivos);

			this.getChildren().addAll(header, conteudo);

			header.setOnMouseClicked(e -> toggleConteudo());
		}

		private HBox criarHeaderColunas(String titulo) {
			List<String> colunas;
			if ("Criptomoedas".equalsIgnoreCase(titulo)) {
				colunas = Arrays.asList("Ativo", "Quantidade", "Preço Atual", "Variação", "Saldo");
			} else if ("Renda Fixa".equalsIgnoreCase(titulo)) {
				colunas = Arrays.asList("Ativo", "Rentabilidade", "Variação", "Saldo");
			} else if ("Tesouro Direto".equalsIgnoreCase(titulo)) {
				colunas = Arrays.asList("Ativo", "Quantidade", "Variação", "Saldo");
			} else {
				colunas = Arrays.asList("Ativo", "Quantidade", "Preço Médio", "Preço Atual", "Variação", "Rendimento Mensal", "Saldo");
			}


			HBox linha = new HBox(ESPACAMENTO_COLUNAS);
			linha.setAlignment(Pos.CENTER_LEFT);
			linha.setPadding(new Insets(ALTURA_LINHA / 3, 35, ALTURA_LINHA / 3, 35));
			linha.setStyle("-fx-background-color: #1e1f21; -fx-background-radius: 6; -fx-border-color: #323335; -fx-border-radius: 6;");
			linha.setPrefWidth(LARGURA_TOTAL);

			for (String c : colunas) {
				Label lbl = new Label(c);
				lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #d0d0d0; -fx-font-weight: bold;");
				lbl.setPrefWidth(LARGURA_COLUNA);
				linha.getChildren().add(lbl);
			}
			return linha;
		}

		// ====================================================
		// Ações / FIIs / ETFs
		// ====================================================

		public void adicionarLinhaAcoesEFiis(String logoUrl, String ticker, double quantidade, double precoMedio, double precoAtual, double variacao, double rendimentoMensal, double saldo) {

			listaAtivos.getChildren().add(criarLinhaAtivo(logoUrl, ticker, quantidade, precoMedio, precoAtual, variacao, rendimentoMensal, saldo));
		}


		public void atualizarOuAdicionarLinha(
		        String logoUrl, String ticker,
		        double quantidade, double precoMedio, double precoAtual,
				double variacao, double rendimentoMensal, double saldo) {
			for (var node : listaAtivos.getChildren()) {
				if (node instanceof GridPane grid) {
					String id = grid.getId();
					if (id != null && id.equalsIgnoreCase(ticker)) {
						Object ud = grid.getUserData();
						if (ud instanceof LineData ld) {
							ld.quantidade = quantidade;
							ld.precoReferencia = precoMedio;

							ld.lblQtd.setText(formatQuantidadeAcoes(quantidade));
							ld.lblPM.setText(formatCurrency(precoMedio));
							ld.lblPreco.setText(formatCurrency(precoAtual));
							ld.lblVariacao.setText(formatVariacao(variacao));
							ld.lblSaldo.setText(formatCurrency(saldo));
							ld.lblRendMensal.setText(formatPercent(rendimentoMensal));
							return;
						}
					}
				}
			}
			adicionarLinhaAcoesEFiis(logoUrl, ticker, quantidade, precoMedio, precoAtual, variacao, rendimentoMensal, saldo);
		}

		// ====================================================
		// Criptomoedas
		// ====================================================

		public void adicionarLinhaCripto(String logoUrl, String ticker, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo) {
			listaAtivos.getChildren().add(criarLinhaCripto(logoUrl, ticker, quantidade, precoMedio, precoAtual, variacao, saldo));
		}

		public void atualizarOuAdicionarLinhaCripto(String logoUrl, String ticker, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo) {
			for (var node : listaAtivos.getChildren()) {
				if (node instanceof GridPane grid) {
					String id = grid.getId();
					if (id != null && id.equalsIgnoreCase(ticker)) {
						Object ud = grid.getUserData();
						if (ud instanceof LineData ld) {
							ld.quantidade = quantidade;
							ld.precoReferencia = precoMedio;

							ld.lblQtd.setText(formatQuantidadeCripto(quantidade));
							ld.lblPreco.setText(formatCurrencyCripto(precoAtual));
							ld.lblVariacao.setText(formatVariacao(variacao));
							ld.lblSaldo.setText(formatCurrency(saldo));
							return;
						}
					}
				}
			}
			adicionarLinhaCripto(logoUrl, ticker, quantidade, precoMedio, precoAtual, variacao, saldo);
		}

		// ====================================================
		// Tesouro Direto
		// ====================================================

		public void adicionarLinhaTesouroDireto(String nomeAtivo, double quantidade, double variacao, double saldo) {
			listaAtivos.getChildren().add(criarLinhaTesouroDireto(nomeAtivo, quantidade, variacao, saldo));
		}

		public void atualizarOuAdicionarLinhaTesouroDireto(String nomeAtivo, double quantidade, double variacao, double saldo) {
			for (var node : listaAtivos.getChildren()) {
				if (node instanceof GridPane grid && nomeAtivo.equals(grid.getId())) {
					LineData ld = (LineData) grid.getUserData();
					ld.quantidade = quantidade;

					ld.lblQtd.setText(formatQuantidadeTesouro(quantidade));
					ld.lblVariacao.setText(formatVariacao(variacao));
					ld.lblSaldo.setText(formatCurrency(saldo));
					return;
				}
			}
			adicionarLinhaTesouroDireto(nomeAtivo, quantidade, variacao, saldo);
		}

		public void adicionarLinhaRendaFixa(String nomeAtivo, double rentabilidade, double variacao, double saldo) {
			listaAtivos.getChildren().add(criarLinhaRendaFixa(nomeAtivo, rentabilidade, variacao, saldo));
		}

		// ====================================================
		// Linhas (UI)
		// ====================================================

		private GridPane criarLinhaCripto(String logoUrl, String ticker, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo) {
			GridPane grid = new GridPane();
			grid.setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 6;");
			grid.setPadding(new Insets(ALTURA_LINHA / 3, 35, ALTURA_LINHA / 3, 35));
			grid.setHgap(ESPACAMENTO_COLUNAS);

			grid.setPrefWidth(LARGURA_TOTAL);
			grid.setMaxWidth(LARGURA_TOTAL);
			grid.setId(ticker);

			for (int i = 0; i < 5; i++) {
				grid.getColumnConstraints().add(new ColumnConstraints(LARGURA_COLUNA));
			}

			Label lblTicker = new Label(ticker);
			lblTicker.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
			grid.add(lblTicker, 0, 0);

			Label lQtd = novoLabel(formatQuantidadeCripto(quantidade));
			Label lPreco = novoLabel(formatCurrencyCripto(precoAtual));
			Label lVar = novoLabel(formatVariacao(variacao));
			Label lSaldo = novoLabel(formatCurrency(saldo));

			grid.add(lQtd, 1, 0);
			grid.add(lPreco, 2, 0);
			grid.add(lVar, 3, 0);
			grid.add(lSaldo, 4, 0);

			LineData ld = new LineData();
			ld.lblQtd = lQtd;
			ld.lblPM = null;
			ld.lblPreco = lPreco;
			ld.lblVariacao = lVar;
			ld.lblSaldo = lSaldo;
			ld.quantidade = quantidade;
			ld.precoReferencia = precoMedio;
			grid.setUserData(ld);

			grid.setOnMouseEntered(e -> grid.setStyle("-fx-background-color: #333437; -fx-background-radius: 6;"));
			grid.setOnMouseExited(e -> grid.setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 6;"));

			return grid;
		}

		private GridPane criarLinhaRendaFixa(String nomeAtivo, double rentabilidade, double variacao, double saldo) {
			GridPane grid = new GridPane();
			grid.setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 6;");
			grid.setPadding(new Insets(ALTURA_LINHA / 3, 35, ALTURA_LINHA / 3, 35));
			grid.setHgap(ESPACAMENTO_COLUNAS);

			grid.setPrefWidth(LARGURA_TOTAL);
			grid.setMaxWidth(LARGURA_TOTAL);
			grid.setId(nomeAtivo);

			for (int i = 0; i < 4; i++) {
				grid.getColumnConstraints().add(new ColumnConstraints(LARGURA_COLUNA));
			}

			Label lblAtivo = novoLabel(nomeAtivo);
			Label lblRent = novoLabel(formatVariacao(rentabilidade));
			Label lblVar = novoLabel(formatVariacao(variacao));
			Label lblSaldo = novoLabel(formatCurrency(saldo));

			grid.add(lblAtivo, 0, 0);
			grid.add(lblRent, 1, 0);
			grid.add(lblVar, 2, 0);
			grid.add(lblSaldo, 3, 0);

			LineData ld = new LineData();
			ld.lblPreco = lblRent;
			ld.lblVariacao = lblVar;
			ld.lblSaldo = lblSaldo;
			grid.setUserData(ld);

			return grid;
		}

		private GridPane criarLinhaAtivo(String logoUrl, String ticker, double quantidade, double precoMedio, double precoAtual, double variacao, double rendimentoMensal, double saldo) {

			GridPane grid = new GridPane();
			grid.setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 6;");
			grid.setPadding(new Insets(ALTURA_LINHA / 3, 35, ALTURA_LINHA / 3, 35));
			grid.setHgap(ESPACAMENTO_COLUNAS);
			grid.setPrefWidth(LARGURA_TOTAL);
			grid.setMaxWidth(LARGURA_TOTAL);
			grid.setId(ticker);

			for (int i = 0; i < 7; i++) {
				grid.getColumnConstraints().add(new ColumnConstraints(LARGURA_COLUNA));
			}


			HBox ativoBox = new HBox(10);
			ativoBox.setAlignment(Pos.CENTER_LEFT);

			// ========= Logo (preenche sem distorcer: crop central) =========
			final double SIZE = 40;

			Rectangle clip = new Rectangle(SIZE, SIZE);
			clip.setArcWidth(10);
			clip.setArcHeight(10);

			ImageView logoView = new ImageView();
			logoView.setFitWidth(SIZE);
			logoView.setFitHeight(SIZE);
			logoView.setPreserveRatio(false); // a proporção vem do viewport

			Rectangle bg = new Rectangle(SIZE, SIZE);
			bg.setArcWidth(20);
			bg.setArcHeight(20);
			bg.setFill(Color.web("#3a3b3d"));

			logoView.setClip(clip);

			StackPane logoPane = new StackPane(bg, logoView);
			logoPane.setPrefSize(SIZE, SIZE);
			logoPane.setMinSize(SIZE, SIZE);
			logoPane.setMaxSize(SIZE, SIZE);

			// Carrega logo async e aplica viewport (crop central quadrado)
			if (logoUrl != null && !logoUrl.isBlank()) {
				final String urlFinal = logoUrl;

				LOGO_EXEC.submit(() -> {
				    Image img = Apis.LogoFetcher.loadImage(urlFinal);
				    if (img == null) return;

				    Image trimmed = trimBordasBrancas(img);

				    Platform.runLater(() -> {
				        logoView.setImage(trimmed);

				        double w = trimmed.getWidth();
				        double h = trimmed.getHeight();
				        if (w <= 0 || h <= 0) return;

				        double side = Math.min(w, h);
						double x = (w - side) / 2.1;
						double y = (h - side) / 2.1;

				        logoView.setViewport(new javafx.geometry.Rectangle2D(x, y, side, side));
				    });
				});

			} else {
				// opcional: log se quiser
				// System.out.println("[LOGO] URL vazia para ticker: " + ticker);
			}

			Label lblTicker = new Label(ticker);
			lblTicker.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

			// opcional: deixa alinhadinho no meio do logo
			lblTicker.setMinHeight(40); // mesmo SIZE do logo
			lblTicker.setAlignment(Pos.CENTER_LEFT);

			ativoBox.getChildren().addAll(logoPane, new Label(" - "), lblTicker);

			// >>>> FALTAVA ISSO NO SEU CÓDIGO COLADO <<<<
			grid.add(ativoBox, 0, 0);

			// ========= Demais colunas =========
			Label lQtd = novoLabel(formatQuantidadeAcoes(quantidade));
			Label lPM = novoLabel(formatCurrency(precoMedio));
			Label lPreco = novoLabel(formatCurrency(precoAtual));
			Label lVar = novoLabel(formatVariacao(variacao));
			Label lRendMensal = novoLabel(formatCurrency(rendimentoMensal));
			Label lSaldo = novoLabel(formatCurrency(saldo));

			grid.add(lQtd, 1, 0);
			grid.add(lPM, 2, 0);
			grid.add(lPreco, 3, 0);
			grid.add(lVar, 4, 0);
			grid.add(lRendMensal, 5, 0);
			grid.add(lSaldo, 6, 0);

			LineData ld = new LineData();
			ld.lblQtd = lQtd;
			ld.lblPM = lPM;
			ld.lblPreco = lPreco;
			ld.lblVariacao = lVar;
			ld.lblRendMensal = lRendMensal;
			ld.lblSaldo = lSaldo;
			ld.quantidade = quantidade;
			ld.precoReferencia = precoMedio;
			grid.setUserData(ld);

			grid.setOnMouseEntered(e -> grid.setStyle("-fx-background-color: #333437; -fx-background-radius: 6;"));
			grid.setOnMouseExited(e -> grid.setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 6;"));

			return grid;
		}

		private GridPane criarLinhaTesouroDireto(String nomeAtivo, double quantidade, double variacao, double saldo) {
			GridPane grid = new GridPane();
			grid.setId(nomeAtivo);
			grid.setStyle("-fx-background-color: #2a2b2d; -fx-background-radius: 6;");
			grid.setPadding(new Insets(ALTURA_LINHA / 3, 35, ALTURA_LINHA / 3, 35));
			grid.setHgap(ESPACAMENTO_COLUNAS);
			grid.setPrefWidth(LARGURA_TOTAL);
			grid.setMaxWidth(LARGURA_TOTAL);

			for (int i = 0; i < 4; i++) {
				grid.getColumnConstraints().add(new ColumnConstraints(LARGURA_COLUNA));
			}

			Label lblAtivo = novoLabel(nomeAtivo);
			Label lblQtd = novoLabel(formatQuantidadeTesouro(quantidade));
			Label lblVar = novoLabel(formatVariacao(variacao));
			Label lblSaldo = novoLabel(formatCurrency(saldo));

			grid.add(lblAtivo, 0, 0);
			grid.add(lblQtd, 1, 0);
			grid.add(lblVar, 2, 0);
			grid.add(lblSaldo, 3, 0);

			LineData ld = new LineData();
			ld.lblQtd = lblQtd;
			ld.lblVariacao = lblVar;
			ld.lblSaldo = lblSaldo;
			ld.quantidade = quantidade;
			grid.setUserData(ld);

			return grid;
		}

		private Label novoLabel(String texto) {
			Label lbl = new Label(texto);
			lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #ffffff;");
			return lbl;
		}

		private void toggleConteudo() {
			if (expandido) {
				Timeline anim = new Timeline(new KeyFrame(Duration.millis(DURACAO_ANIM), new KeyValue(conteudo.opacityProperty(), 0), new KeyValue(conteudo.prefHeightProperty(), 0)));
				anim.setOnFinished(ev -> {
					conteudo.setManaged(false);
					conteudo.setVisible(false);
					expandido = false;
				});
				anim.play();

				RotateTransition rotate = new RotateTransition(Duration.millis(DURACAO_ANIM), arrow);
				rotate.setToAngle(-90);
				rotate.play();

			} else {
				conteudo.setVisible(true);
				conteudo.setManaged(true);
				conteudo.applyCss();
				conteudo.layout();
				alturaExpandida = conteudo.prefHeight(-1);

				Timeline anim = new Timeline(new KeyFrame(Duration.millis(DURACAO_ANIM), new KeyValue(conteudo.opacityProperty(), 1), new KeyValue(conteudo.prefHeightProperty(), alturaExpandida)));
				anim.setOnFinished(ev -> expandido = true);
				anim.play();

				RotateTransition rotate = new RotateTransition(Duration.millis(DURACAO_ANIM), arrow);
				rotate.setToAngle(0);
				rotate.play();
			}
		}
	}

	private static class LineData {
		Label lblQtd;
		Label lblPM; // Ações: visível | Cripto: null
		Label lblPreco;
		Label lblVariacao;
		Label lblSaldo;
		Label lblRendMensal;

		double quantidade;
		double precoReferencia;
	}
	
	private static Image trimBordasBrancas(Image img) {
	    PixelReader pr = img.getPixelReader();
	    if (pr == null) return img;

	    int w = (int) img.getWidth();
	    int h = (int) img.getHeight();

	    // Ajuste fino:
		double white = 0.50; // quanto mais perto de 1, mais "branco" precisa ser pra cortar
	    double alphaMin = 0.05;

	    int top = 0, bottom = h - 1, left = 0, right = w - 1;

	    // Top
	    topLoop:
	    for (; top < h; top++) {
	        for (int x = 0; x < w; x++) {
	            Color c = pr.getColor(x, top);
	            if (!ehQuaseBranco(c, white, alphaMin)) break topLoop;
	        }
	    }

	    // Bottom
	    bottomLoop:
	    for (; bottom >= top; bottom--) {
	        for (int x = 0; x < w; x++) {
	            Color c = pr.getColor(x, bottom);
	            if (!ehQuaseBranco(c, white, alphaMin)) break bottomLoop;
	        }
	    }

	    // Left
	    leftLoop:
	    for (; left < w; left++) {
	        for (int y = top; y <= bottom; y++) {
	            Color c = pr.getColor(left, y);
	            if (!ehQuaseBranco(c, white, alphaMin)) break leftLoop;
	        }
	    }

	    // Right
	    rightLoop:
	    for (; right >= left; right--) {
	        for (int y = top; y <= bottom; y++) {
	            Color c = pr.getColor(right, y);
	            if (!ehQuaseBranco(c, white, alphaMin)) break rightLoop;
	        }
	    }

	    int newW = right - left + 1;
	    int newH = bottom - top + 1;

	    // Se não achou nada ou recorte ficou inválido, devolve original
	    if (newW <= 5 || newH <= 5) return img;

	    return new WritableImage(pr, left, top, newW, newH);
	}

	private static boolean ehQuaseBranco(Color c, double white, double alphaMin) {
	    if (c.getOpacity() < alphaMin) return true; // transparente conta como "vazio"
	    return c.getRed() >= white && c.getGreen() >= white && c.getBlue() >= white;
	}

	private static String formatPercent(double v) {
		// v já é "1.23" significando 1,23%
		String formatted = String.format(Locale.forLanguageTag("pt-BR"), "%.2f", v).replace('.', ',');
		return formatted + "%";
	}

}
