package ControllerInfinit;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import Apis.Brapi;
import Apis.Brapi.AssetInfo;
import Apis.Brapi.AssetType;
import Apis.LogoKit;
import Apis.TituloTesouro;
import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO.SessaoTemp;
import MainInfinit.AddAtivos;
import MainInfinit.ListaDeAtivos;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class AddAtivosController {

	// ==========================================================
	// CONTEXTO DO CONTROLLER
	// ==========================================================
	// Orquestra a tela de adicionar ativos.
	// Conecta UI, APIs externas, banco de dados e lista visual.

	private final AddAtivos view;
	private final ListaDeAtivos listaView;

	private final ContextMenu suggestionsMenu = new ContextMenu();
	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private final PauseTransition pause = new PauseTransition(Duration.millis(320));

	private final java.util.concurrent.atomic.AtomicBoolean atualizandoAtivos = new java.util.concurrent.atomic.AtomicBoolean(false);
	private final java.util.concurrent.atomic.AtomicBoolean atualizandoCriptos = new java.util.concurrent.atomic.AtomicBoolean(false);
	private final java.util.concurrent.atomic.AtomicBoolean atualizandoTesouro = new java.util.concurrent.atomic.AtomicBoolean(false);
	private final java.util.concurrent.atomic.AtomicBoolean atualizandoRendaFixa = new java.util.concurrent.atomic.AtomicBoolean(false);

	private AssetType tipoSelecionado = AssetType.ACOES;
	private TituloTesouro tesouroSelecionado;

	@SuppressWarnings("deprecation")
	private static final Locale BR = new Locale("pt", "BR");
	private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(BR);
	private static final NumberFormat CRYPTO_CURRENCY = NumberFormat.getCurrencyInstance(BR);
	static {
		CRYPTO_CURRENCY.setMinimumFractionDigits(2);
		CRYPTO_CURRENCY.setMaximumFractionDigits(8); // cripto precisa disso
	}

	// ==========================================================
	// CONSTRUTOR
	// ==========================================================
	// Inicializa listeners da UI e agendas de atualização automática.
	// É o ponto de entrada da tela AddAtivos.

	public AddAtivosController(AddAtivos view, ListaDeAtivos listaView) {
		this.view = view;
		this.listaView = listaView;

		// UI
		configurarTipoCombo();
		configurarBuscaAtivos();
		configurarCampoQuantidade();
		configurarCampoPreco();
		configurarBotaoAdicionar();

		// refresh imediato ao abrir a tela
		Platform.runLater(() -> {
			atualizarAtivosDoUsuario();
			atualizarCriptos();
			atualizarTesouroDiretoRendimento();
			atualizarRendaFixaRendimento();
		});

		// Schedulers
		scheduler.scheduleAtFixedRate(this::atualizarAtivosDoUsuario, 0, 5, TimeUnit.MINUTES);
		scheduler.scheduleAtFixedRate(this::atualizarCriptos, 10, 60, TimeUnit.SECONDS);

		// Observação: você tem dois schedules pro Tesouro.
		// Mantive como estava (pra não mudar comportamento), mas o de 6h é redundante se o de 60s estiver ativo.
		scheduler.scheduleAtFixedRate(this::atualizarTesouroDiretoRendimento, 5, 60, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(this::atualizarTesouroDiretoRendimento, 15, 6, TimeUnit.HOURS);

		scheduler.scheduleAtFixedRate(this::atualizarRendaFixaRendimento, 20, 6, TimeUnit.HOURS);
	}

	// ==========================================================
	// LIFECYCLE
	// ==========================================================
	// Libera threads e schedulers quando a tela for fechada.

	public void dispose() {
		try {
			scheduler.shutdownNow();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		try {
			executor.shutdownNow();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	// ==========================================================
	// ATUALIZAÇÕES AUTOMÁTICAS
	// ==========================================================
	// Atualiza periodicamente preços e métricas dos ativos do usuário.
	// Sincroniza API, banco e lista visual.

	private void atualizarAtivosDoUsuario() {

		if (!atualizandoAtivos.compareAndSet(false, true))
			return;

		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			Dao daoList = new Dao();
			@SuppressWarnings("static-access")
			var ativos = daoList.listarAtivosPorUsuario(usuarioId);

			for (var ativo : ativos) {

				String cat = (ativo.getCategoria() == null ? "" : ativo.getCategoria().trim());

				// esses são tratados por rotinas específicas
				if ("Renda Fixa".equalsIgnoreCase(cat)
						|| "Criptomoedas".equalsIgnoreCase(cat) || "Tesouro Direto".equalsIgnoreCase(cat)) {
					continue;
				}

				executor.submit(() -> {
					try {
						String ticker = ativo.getAtivo();
						if (ticker == null || ticker.isBlank())
							return;

						ticker = ticker.trim().toUpperCase();

						AssetInfo info = Brapi.buscarAssetInfo(ticker);
						if (info == null || info.price == null || info.price <= 0)
							return;

						// ⚠️ Dao dentro da task evita concorrência perigosa
						Dao dao = new Dao();

						double precoMedio = ativo.getPrecoMedio();
						double precoAtual = info.price;
						double variacao = precoMedio != 0 ? ((precoAtual - precoMedio) / precoMedio) * 100 : 0;
						double saldo = precoAtual * ativo.getQuantidade();

						dao.atualizarAtivo(ativo.getId(), ativo.getQuantidade(), precoMedio, precoAtual, variacao, saldo);

						String logoUrl = (ativo.getIconUrl() != null && !ativo.getIconUrl().isBlank()) ? ativo.getIconUrl() : Apis.LogoKit.byStockTicker(ticker);

						if ((ativo.getIconUrl() == null || ativo.getIconUrl().isBlank()) && logoUrl != null && !logoUrl.isBlank()) {
							dao.atualizarIconUrl(ativo.getId(), logoUrl);
						}

						double dyMensalPercent = 0.0;
						try {
							dyMensalPercent = Brapi.buscarDividendYieldMensal12mPercent(ticker, precoAtual);
						} catch (Exception ex) {
							ex.printStackTrace();
						}

						final double dyMensalFinal = dyMensalPercent;

						Platform.runLater(() -> listaView.atualizarOuAdicionarAtivo(ativo.getCategoria(), ativo.getAtivo(), logoUrl, ativo.getQuantidade(), precoMedio, precoAtual, variacao,
								dyMensalFinal, saldo));

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			atualizandoAtivos.set(false);
		}
	}

	// Atualiza periodicamente apenas criptomoedas.
	// Usa API específica e recalcula saldo e variação.

	private void atualizarCriptos() {

		if (!atualizandoCriptos.compareAndSet(false, true))
			return;

		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			Dao daoList = new Dao();
			@SuppressWarnings("static-access")
			var ativos = daoList.listarAtivosPorUsuario(usuarioId);

			for (var ativo : ativos) {

				if (!"Criptomoedas".equalsIgnoreCase(ativo.getCategoria()))
					continue;

				executor.submit(() -> {
					try {
						// ⚠️ Dao dentro da task evita concorrência perigosa
						Dao dao = new Dao();

						String symbol = ativo.getAtivo();
						double precoMedio = ativo.getPrecoMedio();

						String tickerApi = resolverTickerCriptoParaPreco(symbol, precoMedio);
						Double preco = Brapi.buscarAssetPrice(tickerApi);

						if (preco == null || preco <= 0)
							return;

						double precoAtual = preco;
						double quantidade = ativo.getQuantidade();
						double saldo = precoAtual * quantidade;

						if (precoMedio > 0) {
							double ratio = precoAtual / precoMedio;

							// preço mudou mais de 80% em 1 minuto? provavelmente ticker errado/moeda errada
							if (ratio < 0.2 || ratio > 5.0) {
								return;
							}
						}

						double variacao = (precoMedio > 0) ? ((precoAtual - precoMedio) / precoMedio) * 100 : 0.0;

						dao.atualizarAtivo(ativo.getId(), quantidade, precoMedio, precoAtual, variacao, saldo);

						String logo = ativo.getIconUrl();
						Platform.runLater(() -> listaView.atualizarOuAdicionarCriptomoeda(logo, symbol, quantidade, precoMedio, precoAtual, variacao, saldo));

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// ✅ sem isso, trava após a 1ª execução
			atualizandoCriptos.set(false);
		}
	}

	// ==========================================================
	// CONFIGURAÇÕES DE UI
	// ==========================================================
	// Configura listeners e comportamento da interface.

	@SuppressWarnings("unused")
	private void configurarTipoCombo() {
		view.getTipoCombo().valueProperty().addListener((obs, oldVal, newVal) -> {

			tipoSelecionado = switch (newVal) {
			case "Ações" -> AssetType.ACOES;
			case "FIIs" -> AssetType.FIIS;
			case "Criptomoeda" -> AssetType.CRYPTO;
			case "ETFs" -> AssetType.ETF;
			case "Tesouro Direto" -> AssetType.TREASURY;
			case "Renda fixa" -> AssetType.UNKNOWN;
			default -> AssetType.UNKNOWN;
			};

			view.getAtivoCombo().clear();
			suggestionsMenu.hide();

			TextField qtd = view.getQtdField();

			switch (tipoSelecionado) {
			case CRYPTO -> qtd.setText("0,00000001"); // Cripto: ultra fracionado
			case TREASURY -> qtd.setText("0,01"); // Tesouro: mínimo 1% do título
			default -> qtd.setText("1"); // Ações, FIIs, ETFs
			}
		});
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void configurarBuscaAtivos() {
		TextField textField = view.getAtivoCombo();

		textField.textProperty().addListener((obs, oldVal, newVal) -> {
			suggestionsMenu.hide();
			if (newVal == null || newVal.trim().length() < 2)
				return;
			pause.playFromStart();
		});

		pause.setOnFinished(e -> {

			String query = view.getAtivoCombo().getText().trim();
			if (query.length() < 2)
				return;

			Task<List<?>> task = new Task<>() {
				@Override
				protected List<?> call() {
					if (tipoSelecionado == AssetType.TREASURY) {
						return Brapi.buscarTreasuries(query); // List<TituloTesouro>
					}
					return Brapi.buscarAtivosPorTipo(tipoSelecionado, query); // List<String>
				}
			};

			task.setOnSucceeded(evt -> {
				var results = task.getValue();
				if (results == null || results.isEmpty())
					return;

				suggestionsMenu.getItems().clear();

				if (tipoSelecionado == AssetType.TREASURY) {

					List<TituloTesouro> titulos = (List<TituloTesouro>) results;

					for (TituloTesouro t : titulos) {

						Label lbl = new Label(t.toString());
						lbl.setPrefWidth(320);

						CustomMenuItem item = new CustomMenuItem(lbl, true);

						item.setOnAction(ev -> {

							tesouroSelecionado = t;

							view.getAtivoCombo().setText(t.getNome() + " " + t.getAnoVencimento());
							view.getPrecoField().setText(CURRENCY.format(t.getPreco()));

							if (view.getTaxaField() != null) {
								view.getTaxaField().setText(String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", t.getTaxaAnual() * 100));
							}

							atualizarTotal();
							suggestionsMenu.hide();
						});

						suggestionsMenu.getItems().add(item);
					}

				} else {

					for (String s : (List<String>) results) {

						Label lbl = new Label(s);
						lbl.setPrefWidth(300);

						CustomMenuItem item = new CustomMenuItem(lbl, true);

						item.setOnAction(a -> {

							if (tipoSelecionado == AssetType.CRYPTO) {

								String[] parts = s.split(" - R\\$ ");
								String nomeESimbolo = parts[0];
								double preco = Double.parseDouble(parts[1].replace(",", "."));

								String symbol = nomeESimbolo.substring(nomeESimbolo.indexOf("(") + 1, nomeESimbolo.indexOf(")"));

								view.getAtivoCombo().setText(symbol);
								view.getPrecoField().setText(CRYPTO_CURRENCY.format(preco));
								atualizarTotal();

							} else {
								String ticker = s.split(" - ")[0];
								view.getAtivoCombo().setText(ticker);
								buscarPrecoAtual(ticker);
							}

							suggestionsMenu.hide();
						});

						suggestionsMenu.getItems().add(item);
					}
				}

				suggestionsMenu.show(view.getAtivoCombo(), Side.BOTTOM, 0, 0);
			});

			executor.submit(task);
		});
	}

	@SuppressWarnings("unused")
	private void configurarCampoQuantidade() {

		TextField qtdField = view.getQtdField();

		qtdField.textProperty().addListener((obs, oldVal, newVal) -> {

			if (tipoSelecionado == AssetType.ACOES || tipoSelecionado == AssetType.FIIS || tipoSelecionado == AssetType.ETF) {
				atualizarTotal();
				return;
			}

			if (newVal == null || newVal.isEmpty()) {
				return; // permite apagar tudo
			}

			newVal = newVal.replace(".", ",");

			if (!newVal.matches("[0-9,]*")) {
				qtdField.setText(oldVal);
				return;
			}

			if (newVal.chars().filter(c -> c == ',').count() > 1) {
				qtdField.setText(oldVal);
				return;
			}

			int limiteCasas = (tipoSelecionado == AssetType.TREASURY) ? 2 : 8;

			if (newVal.contains(",")) {
				String[] parts = newVal.split(",", -1);
				if (parts.length == 2 && parts[1].length() > limiteCasas) {
					newVal = parts[0] + "," + parts[1].substring(0, limiteCasas);
					qtdField.setText(newVal);
					qtdField.positionCaret(newVal.length());
					return;
				}
			}

			atualizarTotal();
		});
	}

	@SuppressWarnings("unused")
	private void configurarCampoPreco() {
		TextField precoField = view.getPrecoField();

		precoField.textProperty().addListener((obs, oldVal, newVal) -> {
			atualizarTotal();
		});
	}

	// ==========================================================
	// CÁLCULO
	// ==========================================================
	// Calcula o valor total do investimento.
	// Multiplica preço atual pela quantidade.

	private void atualizarTotal() {
		try {
			double preco = parsePreco(view.getPrecoField().getText());
			double qtd = parseQtd(view.getQtdField().getText());
			double total = preco * qtd;

			view.getTotalLabel().setText("Valor total: " + CURRENCY.format(total));

		} catch (Exception e) {
			view.getTotalLabel().setText("Valor total: R$ 0,00");
		}
	}

	// ==========================================================
	// BOTÃO ADICIONAR
	// ==========================================================
	// Decide qual fluxo de adição executar.
	// Direciona para ações, cripto, tesouro ou renda fixa.

	@SuppressWarnings("unused")
	private void configurarBotaoAdicionar() {

		view.getAdicionarAtivios().setOnAction(e -> {

			String categoria = view.getTipoCombo().getValue();

			if ("Renda fixa".equalsIgnoreCase(categoria)) {
				processarAdicaoRendaFixa();
				return;
			} else if ("Tesouro Direto".equalsIgnoreCase(categoria)) {
				processarAdicaoTesouroDireto();
				return;
			}

			try {
				String ticker = view.getAtivoCombo().getText().trim();
				double quantidade = parseQtd(view.getQtdField().getText());
				double precoAtual = parsePreco(view.getPrecoField().getText());

				Integer usuarioId = SessaoTemp.getUsuarioId();
				if (usuarioId == null)
					return;

				if (tipoSelecionado == AssetType.CRYPTO) {
					processarAdicaoCripto(usuarioId, ticker, quantidade, precoAtual);
				} else if (tipoSelecionado == AssetType.ACOES || tipoSelecionado == AssetType.FIIS || tipoSelecionado == AssetType.ETF) {
					processarAdicaoAcoes(usuarioId, categoria, ticker, quantidade, precoAtual);
				}

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		});
	}

	// ==========================================================
	// RENDA FIXA
	// ==========================================================
	// Processa adição de renda fixa.
	// Consolida títulos por nome canônico.

	private void processarAdicaoRendaFixa() {

		String emissor = (view.getEmissorField() != null ? view.getEmissorField().getText().trim() : "");
		String tipoTitulo = (view.getTipoTituloCombo() != null && view.getTipoTituloCombo().getValue() != null ? view.getTipoTituloCombo().getValue().trim() : "");
		String forma = (view.getFormaCombo() != null && view.getFormaCombo().getValue() != null ? view.getFormaCombo().getValue().trim() : "");
		String indexador = (view.getIndexadorCombo() != null && view.getIndexadorCombo().getValue() != null ? view.getIndexadorCombo().getValue().trim() : "");

		String taxaRaw = (view.getTaxaField() != null ? view.getTaxaField().getText() : "");
		String taxaStr = taxaRaw.replace("%", "").replace(",", ".").trim();

		double taxaParsed = 0.0;
		try {
			if (!taxaStr.isEmpty()) {
				taxaParsed = Double.parseDouble(taxaStr) / 100.0;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			taxaParsed = 0.0;
		}

		double valorInicial = parsePreco(view.getPrecoField().getText());

		String taxaFormatada = String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", taxaParsed * 100).replace('.', ',');
		String nomeAtivo = String.join(" - ", tipoTitulo == null ? "" : tipoTitulo, emissor == null ? "" : emissor, indexador == null ? "" : indexador, forma == null ? "" : forma,
				taxaFormatada == null ? "" : taxaFormatada).replaceAll("\\s*-\\s*$", "");

		final String nomeAtivoFinal = nomeAtivo;
		final double taxaFinal = taxaParsed;
		final double valorInicialFinal = valorInicial;
		final String hojeStr = java.time.LocalDate.now().toString();
		final double aporte = valorInicialFinal;
		final String indexadorFinal = indexador;
		final String formaFinal = forma;

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				try {
					Integer usuarioId = SessaoTemp.getUsuarioId();
					if (usuarioId == null)
						return null;

					Dao dao = new Dao();

					var ativoExistente = dao.buscarAtivoPorUsuarioECategoria(usuarioId, nomeAtivoFinal, "Renda Fixa");

					if (ativoExistente != null) {

						double saldoAntigo = ativoExistente.getSaldo();
						double novoSaldo = saldoAntigo + valorInicialFinal;

						final double rentabilidadeExistente = ativoExistente.getQuantidade();
						final double variacaoExistente = ativoExistente.getVariacao();

						dao.atualizarAtivo(ativoExistente.getId(), rentabilidadeExistente, 0.0, 0.0, variacaoExistente, novoSaldo);

						dao.somarPrincipalRendaFixaETocarUltimoDiaPorNome(usuarioId, nomeAtivoFinal, indexadorFinal, formaFinal, taxaFinal, aporte, hojeStr);

						final double novoSaldoFinalLocal = novoSaldo;

						Platform.runLater(() -> {
							listaView.atualizarOuAdicionarRendaFixa(nomeAtivoFinal, rentabilidadeExistente, variacaoExistente, novoSaldoFinalLocal);
							view.mostrarTelaSucesso(() -> {
							});
						});

					} else {

						dao.insertRendaFixa(usuarioId, nomeAtivoFinal, taxaFinal, 0.0, valorInicialFinal);
						dao.atualizarCamposRendaFixaMetaPorNome(usuarioId, nomeAtivoFinal, indexadorFinal, formaFinal, taxaFinal, aporte, hojeStr);

						Platform.runLater(() -> {
							listaView.adicionarRendaFixa(nomeAtivoFinal, taxaFinal, 0.0, valorInicialFinal);
							view.mostrarTelaSucesso(() -> {
							});
						});
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
		};

		executor.submit(task);
	}

	// ==========================================================
	// TESOURO DIRETO
	// ==========================================================
	// Processa adição de tesouro direto (frações e cálculo com BigDecimal).
	// Atualiza saldo, quantidade e metas do TD.

	private void processarAdicaoTesouroDireto() {

		final TituloTesouro t = tesouroSelecionado;
		if (t == null)
			return;

		String qtdTexto = view.getQtdField().getText();
		if (qtdTexto == null || qtdTexto.trim().isEmpty())
			return;

		qtdTexto = qtdTexto.trim().replace(",", ".");

		BigDecimal quantidade;
		try {
			quantidade = new BigDecimal(qtdTexto).setScale(2, RoundingMode.HALF_UP);
		} catch (NumberFormatException e) {
			return;
		}

		BigDecimal saldo = quantidade.multiply(BigDecimal.valueOf(t.getPreco())).setScale(2, RoundingMode.HALF_UP);

		final String hojeStr = java.time.LocalDate.now().toString();
		final double aporte = saldo.doubleValue();
		double variacao = 0.0;

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				try {
					Integer usuarioId = SessaoTemp.getUsuarioId();
					if (usuarioId == null)
						return null;

					Dao dao = new Dao();
					String nomeAtivo = t.getNome() + " " + t.getAnoVencimento();

					var existente = dao.buscarAtivoPorUsuarioECategoria(usuarioId, nomeAtivo, "Tesouro Direto");

					if (existente != null) {

						BigDecimal qtdExistente = BigDecimal.valueOf(existente.getQuantidade());
						BigDecimal saldoExistente = BigDecimal.valueOf(existente.getSaldo());

						BigDecimal qtdTotal = qtdExistente.add(quantidade).setScale(2, RoundingMode.HALF_UP);
						BigDecimal saldoTotal = saldoExistente.add(saldo).setScale(2, RoundingMode.HALF_UP);

						dao.atualizarAtivo(existente.getId(), qtdTotal.doubleValue(), 0.0, 0.0, variacao, saldoTotal.doubleValue());

						double principalAntigo = existente.getTdPrincipal();
						double principalNovo = principalAntigo + aporte;

						String indexador = t.getIndexador();
						double taxaAnual = t.getTaxaAnual();

						dao.atualizarCamposTesouroMeta(existente.getId(), indexador, taxaAnual, principalNovo, hojeStr);

						Platform.runLater(() -> {
							listaView.atualizarOuAdicionarTesouroDireto(nomeAtivo, qtdTotal.doubleValue(), variacao, saldoTotal.doubleValue());
							view.mostrarTelaSucesso(() -> atualizarTesouroDiretoRendimento());
						});

					} else {

						BigDecimal variacaoBd = BigDecimal.ZERO;

						dao.insertTesouroDireto(usuarioId, nomeAtivo, quantidade.setScale(2, RoundingMode.HALF_UP), variacaoBd, saldo.setScale(2, RoundingMode.HALF_UP));

						String indexador = t.getIndexador();
						double taxaAnual = t.getTaxaAnual();
						dao.atualizarCamposTesouroMetaPorNome(usuarioId, nomeAtivo, indexador, taxaAnual, aporte, hojeStr);

						Platform.runLater(() -> {
							listaView.adicionarTesouroDireto(nomeAtivo, quantidade.doubleValue(), variacaoBd.doubleValue(), saldo.doubleValue());
							view.mostrarTelaSucesso(() -> atualizarTesouroDiretoRendimento());
						});
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				return null;
			}
		};

		executor.submit(task);
	}

	// ==========================================================
	// AÇÕES / FIIS / ETFs
	// ==========================================================
	// Processa compra e recalcula preço médio.
	// Atualiza banco, lista visual e registra lançamento.

	private void processarAdicaoAcoes(Integer usuarioId, String categoria, String ticker, double qtdNova, double precoAtual) {

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				try {
					Dao dao = new Dao();
					var ativoExistente = dao.buscarAtivoPorUsuarioECategoria(usuarioId, ticker, categoria);

					if (ativoExistente != null) {

						double qtdAntiga = ativoExistente.getQuantidade();
						double pmAntigo = ativoExistente.getPrecoMedio();
						double qtdTotal = qtdAntiga + qtdNova;

						double novoPM = ((pmAntigo * qtdAntiga) + (precoAtual * qtdNova)) / qtdTotal;
						double saldo = precoAtual * qtdTotal;
						double variacao = ((precoAtual - novoPM) / novoPM) * 100;

						dao.atualizarAtivo(ativoExistente.getId(), qtdTotal, novoPM, precoAtual, variacao, saldo);

						String logoUrl = buildLogoUrl(ticker);

						double dyMensalPercent = 0.0;
						try {
							dyMensalPercent = Brapi.buscarDividendYieldMensal12mPercent(ticker, precoAtual);
						} catch (Exception ex) {
							ex.printStackTrace();
						}

						String hojeIso = java.time.LocalDate.now().toString();
						dao.inserirLancamento(usuarioId, categoria, ticker, qtdNova, precoAtual, hojeIso);

						final double dyMensalFinal = dyMensalPercent;

						Platform.runLater(() -> {
							listaView.atualizarOuAdicionarAtivo(categoria, ticker, logoUrl, qtdTotal, novoPM, precoAtual, variacao, dyMensalFinal, saldo);
							view.mostrarTelaSucesso(() -> atualizarAtivosDoUsuario());
						});

					} else {

						double saldo = precoAtual * qtdNova;
						String logoUrl = Apis.LogoKit.byStockTicker(ticker);

						String hojeIso = java.time.LocalDate.now().toString();
						dao.inserirLancamento(usuarioId, categoria, ticker, qtdNova, precoAtual, hojeIso);

						dao.insertAtivo(categoria, ticker, logoUrl, qtdNova, precoAtual, precoAtual, 0, saldo);

						Platform.runLater(() -> {
							listaView.adicionarAcoesEFiis(categoria, ticker, logoUrl, qtdNova, precoAtual, precoAtual, 0.0, saldo);
							view.mostrarTelaSucesso(() -> atualizarAtivosDoUsuario());
						});
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}

				return null;
			}
		};

		executor.submit(task);
	}

	// ==========================================================
	// CRIPTOMOEDAS
	// ==========================================================
	// Processa compra, permite frações e recalcula preço médio.

	private void processarAdicaoCripto(Integer usuarioId, String symbol, double qtdNova, double precoAtual) {

		final String sym = symbol.toUpperCase();

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				try {
					Dao dao = new Dao();
					var ativoExistente = dao.buscarAtivoPorUsuarioECategoria(usuarioId, sym, "Criptomoedas");

					if (ativoExistente != null) {

						double qtdAntiga = ativoExistente.getQuantidade();
						double pmAntigo = ativoExistente.getPrecoMedio();
						double qtdTotal = qtdAntiga + qtdNova;

						double novoPM = ((pmAntigo * qtdAntiga) + (precoAtual * qtdNova)) / qtdTotal;
						double saldo = precoAtual * qtdTotal;
						double variacao = ((precoAtual - novoPM) / novoPM) * 100;

						dao.atualizarAtivo(ativoExistente.getId(), qtdTotal, novoPM, precoAtual, variacao, saldo);

						String logoUrl = LogoKit.byStockTicker(sym);
						if ((ativoExistente.getIconUrl() == null || ativoExistente.getIconUrl().isBlank()) && logoUrl != null && !logoUrl.isBlank()) {
							dao.atualizarIconUrl(ativoExistente.getId(), logoUrl);
						}

						Platform.runLater(() -> {
							listaView.atualizarOuAdicionarCriptomoeda(logoUrl, sym, qtdTotal, novoPM, precoAtual, variacao, saldo);
							view.mostrarTelaSucesso(() -> atualizarCriptos());
						});

					} else {

						double saldo = precoAtual * qtdNova;
						String logoUrl = LogoKit.byStockTicker(sym);

						dao.insertAtivo("Criptomoedas", sym, logoUrl, qtdNova, precoAtual, precoAtual, 0, saldo);

						final String logoFinal = logoUrl;

						Platform.runLater(() -> {
							listaView.adicionarCriptomoeda(logoFinal, sym, qtdNova, precoAtual, precoAtual, 0.0, saldo);
							view.mostrarTelaSucesso(() -> atualizarCriptos());
						});
					}

				} catch (Exception ex) {
					// ❌ não engole erro — isso te salva no debug
					ex.printStackTrace();
				}
				return null;
			}
		};

		executor.submit(task);
	}

	// ==========================================================
	// RENDIMENTO: TESOURO DIRETO
	// ==========================================================
	// Recalcula saldo/variação baseado em indexador + tempo desde último dia.
	// Atualiza banco e a lista visual.

	private void atualizarTesouroDiretoRendimento() {

		if (!atualizandoTesouro.compareAndSet(false, true))
			return;

		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			Dao daoList = new Dao();
			@SuppressWarnings("static-access")
			var ativos = daoList.listarAtivosPorUsuario(usuarioId);

			java.time.LocalDate hoje = java.time.LocalDate.now();

			executor.submit(() -> {
				try {
					Double selicDiaPercent = null;

					for (var ativo : ativos) {
						if (!"Tesouro Direto".equalsIgnoreCase(ativo.getCategoria()))
							continue;

						String indexador = ativo.getTdIndexador();
						double taxaAnual = ativo.getTdTaxaAnual();
						double principal = ativo.getTdPrincipal();
						String ultimoDiaStr = ativo.getTdUltimoDia();

						if (indexador == null || indexador.isBlank())
							continue;

						Dao dao = new Dao();

						if (ultimoDiaStr == null || ultimoDiaStr.isBlank()) {
							dao.atualizarCamposTesouroMeta(ativo.getId(), indexador, taxaAnual, principal, hoje.toString());
							continue;
						}

						java.time.LocalDate ultimoDia;
						try {
							ultimoDia = java.time.LocalDate.parse(ultimoDiaStr);
						} catch (Exception e) {
							dao.atualizarCamposTesouroMeta(ativo.getId(), indexador, taxaAnual, principal, hoje.toString());
							continue;
						}

						long dias = java.time.temporal.ChronoUnit.DAYS.between(ultimoDia, hoje);
						if (dias <= 0)
							continue;

						double saldoAnterior = ativo.getSaldo();
						double saldoNovo = saldoAnterior;
						double tAnos = dias / 365.0;

						switch (indexador.toUpperCase()) {

						case "SELIC": {
							if (selicDiaPercent == null) {
								selicDiaPercent = new Apis.SelicApi().getUltimaTaxaSelic();
							}
							double f = Math.pow(1.0 + (selicDiaPercent / 100.0), dias);
							saldoNovo = saldoAnterior * f;
							break;
							}

						case "PREFIXADO": {
							saldoNovo = saldoAnterior * Math.pow(1.0 + taxaAnual, tAnos);
							break;
						}

						case "IPCA+":
						case "RENDA+":
						case "EDUCA+": {
							double ipcaAa = Apis.IpcaService.getIpcaAtual();
							if (ipcaAa < 0)
								ipcaAa = 0;

							double fIpca = Math.pow(1.0 + (ipcaAa / 100.0), tAnos);
							double fReal = Math.pow(1.0 + taxaAnual, tAnos);

							saldoNovo = saldoAnterior * (fIpca * fReal);
							break;
						}

						default:
							continue;
						}

						double variacao = (principal > 0) ? ((saldoNovo - principal) / principal) * 100.0 : 0.0;

						double qtd = ativo.getQuantidade();
						double puEstimado = (qtd > 0) ? (saldoNovo / qtd) : 0.0;

						dao.atualizarAtivo(ativo.getId(), qtd, 0.0, puEstimado, variacao, saldoNovo);
						dao.atualizarCamposTesouroMeta(ativo.getId(), indexador, taxaAnual, principal, hoje.toString());

						final String nomeAtivoFinal = ativo.getAtivo();
						final double qtdFinal = qtd;
						final double variacaoFinal = variacao;
						final double saldoNovoFinal = saldoNovo;

						Platform.runLater(() -> listaView.atualizarOuAdicionarTesouroDireto(nomeAtivoFinal, qtdFinal, variacaoFinal, saldoNovoFinal));
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					// ✅ sem isso, trava após a 1ª execução
					atualizandoTesouro.set(false);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			atualizandoTesouro.set(false);
		}
	}

	// ==========================================================
	// RENDIMENTO: RENDA FIXA
	// ==========================================================
	// Recalcula saldo/variação baseado em indexador e tempo desde último dia.
	// Atualiza banco e a lista visual.

	private void atualizarRendaFixaRendimento() {
		System.out.println(">>> atualizarRendaFixaRendimento rodou");

		if (!atualizandoRendaFixa.compareAndSet(false, true))
			return;

		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			Dao daoList = new Dao();
			@SuppressWarnings("static-access")
			var ativos = daoList.listarAtivosPorUsuario(usuarioId);

			java.time.LocalDate hoje = java.time.LocalDate.now();

			executor.submit(() -> {
				try {
					Double cdiDiaPercent = null;
					Double ipcaAaPercent = null;

					for (var ativo : ativos) {

						if (!"Renda Fixa".equalsIgnoreCase(ativo.getCategoria()))
							continue;

						String indexador = ativo.getRfIndexador();
						String forma = ativo.getRfForma();
						double taxaAnual = ativo.getRfTaxaAnual();
						double principal = ativo.getRfPrincipal();
						String ultimoDiaStr = ativo.getRfUltimoDia();

						System.out.println("RF DEBUG -> ativo=" + ativo.getAtivo() + " ultimoDia=" + ativo.getRfUltimoDia() + " indexador=" + ativo.getRfIndexador() + " taxaAnual="
								+ ativo.getRfTaxaAnual() + " saldo=" + ativo.getSaldo());

						Dao dao = new Dao();

						if (ultimoDiaStr == null || ultimoDiaStr.isBlank()) {
							dao.atualizarCamposRendaFixaMeta(ativo.getId(), indexador, forma, taxaAnual, principal, hoje.toString());
							continue;
						}

						java.time.LocalDate ultimoDia;
						try {
							ultimoDia = java.time.LocalDate.parse(ultimoDiaStr);
						} catch (Exception e) {
							dao.atualizarCamposRendaFixaMeta(ativo.getId(), indexador, forma, taxaAnual, principal, hoje.toString());
							continue;
						}

						long dias = java.time.temporal.ChronoUnit.DAYS.between(ultimoDia, hoje);
						if (dias <= 0)
							continue;

						double saldoAnterior = ativo.getSaldo();
						double saldoNovo = saldoAnterior;

						double tAnos = dias / 365.0;

						System.out.println("RF DEBUG -> dias=" + dias);

						if (indexador == null)
							indexador = "";

						switch (indexador.toUpperCase()) {

						case "CDI": {
							if (cdiDiaPercent == null) {
								saldoNovo = saldoAnterior * Math.pow(1.0 + taxaAnual, tAnos);
								break;
							}
							double fator = Math.pow(1.0 + (cdiDiaPercent / 100.0), dias);
							saldoNovo = saldoAnterior * fator;
							break;
						}

						case "CDI+": {
							double spreadAa = ativo.getRfSpreadAnual();
							double percentCdi = ativo.getRfPercentIndexador();

							if (cdiDiaPercent == null) {
								double fator = Math.pow(1.0 + taxaAnual, tAnos);
								saldoNovo = saldoAnterior * fator;
								break;
							}

							double fatorCdi = Math.pow(1.0 + ((cdiDiaPercent * percentCdi) / 100.0), dias);
							double fatorSpread = Math.pow(1.0 + spreadAa, tAnos);
							saldoNovo = saldoAnterior * (fatorCdi * fatorSpread);
								break;
							}

						case "IPCA+": {
							double taxaRealAa = taxaAnual;
							if (ipcaAaPercent == null) {
								ipcaAaPercent = Apis.IpcaService.getIpcaAtual();
								if (ipcaAaPercent == null || ipcaAaPercent < 0)
									ipcaAaPercent = 0.0;
							}
							double fatorIpca = Math.pow(1.0 + (ipcaAaPercent / 100.0), tAnos);
							double fatorReal = Math.pow(1.0 + taxaRealAa, tAnos);
							saldoNovo = saldoAnterior * (fatorIpca * fatorReal);
							break;
						}

						default: {
								double fator = Math.pow(1.0 + taxaAnual, tAnos);
								saldoNovo = saldoAnterior * fator;
								break;
							}
						}

						double variacao = (principal > 0) ? ((saldoNovo - principal) / principal) * 100.0 : 0.0;
						double rentabilidade = variacao;

						dao.atualizarAtivo(ativo.getId(), rentabilidade, 0.0, 0.0, variacao, saldoNovo);
						dao.atualizarCamposRendaFixaMeta(ativo.getId(), indexador, forma, taxaAnual, principal, hoje.toString());

						final String nomeAtivoFinal = ativo.getAtivo();
						final double rentFinal = rentabilidade;
						final double varFinal = variacao;
						final double saldoFinal = saldoNovo;

						Platform.runLater(() -> listaView.atualizarOuAdicionarRendaFixa(nomeAtivoFinal, rentFinal, varFinal, saldoFinal));
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					atualizandoRendaFixa.set(false);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
			atualizandoRendaFixa.set(false);
		}
	}

	// ==========================================================
	// HELPERS
	// ==========================================================
	// Parsing, API helpers e utilitários de logo/ticker.

	private double parsePreco(String txt) {
		try {
			if (txt == null)
				return 0.0;

			txt = txt.replace('\u00A0', ' ').trim();
			txt = txt.replace("R$", "").trim();
			txt = txt.replace(".", "").replace(",", ".").trim();
			txt = txt.replace(" ", "");

			if (txt.isBlank())
				return 0.0;

			return Double.parseDouble(txt);
		} catch (Exception e) {
			return 0.0;
		}
	}

	private double parseQtd(String txt) {
		if (txt == null || txt.isBlank())
			return 0;
		return Double.parseDouble(txt.replace(",", "."));
	}

	@SuppressWarnings("unused")
	private void buscarPrecoAtual(String ticker) {

		Task<AssetInfo> task = new Task<>() {
			@Override
			protected AssetInfo call() {
				return Brapi.buscarAssetInfo(ticker);
			}
		};

		task.setOnSucceeded(evt -> {

			AssetInfo info = task.getValue();

			if (info != null && info.price != null) {
				view.precoSetByApi = true;
				view.getPrecoField().setText(CURRENCY.format(info.price));
				atualizarTotal();
				listaView.atualizarPrecoVisual(ticker, info.price);
			}
		});

		executor.submit(task);
	}

	private static final String LOGOKIT_TOKEN = "pk_fre0d0771b214e45db3dbb";

	private static String buildLogoUrl(String ticker) {
		if (ticker == null || ticker.isBlank())
			return null;

		String t = ticker.trim().toUpperCase();

		if (t.matches("^[A-Z]{4}\\d{1,2}$")) {
			t = t + ".SA";
		}

		return "https://img.logokit.com/ticker/" + t + "?token=" + LOGOKIT_TOKEN + "&size=64" + "&fallback=monogram";
	}

	private String resolverTickerCriptoParaPreco(String symbol, double precoMedio) {
		String s = symbol.trim().toUpperCase();

		String[] cands = new String[] { s, s + "BRL", s + "-BRL", s + "USD", s + "-USD"
		};

		Double melhor = null;
		String melhorTicker = s;
		double melhorScore = Double.POSITIVE_INFINITY;

		for (String cand : cands) {
			Double p = Brapi.buscarAssetPrice(cand);
			if (p == null || p <= 0)
				continue;

			if (precoMedio <= 0)
				return cand;

			double ratio = p / precoMedio;
			double score = Math.abs(Math.log(ratio));
			if (score < melhorScore) {
				melhorScore = score;
				melhor = p;
				melhorTicker = cand;
			}
		}

		return melhor != null ? melhorTicker : s;
	}
}
