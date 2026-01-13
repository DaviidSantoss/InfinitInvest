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
	private final PauseTransition pause = new PauseTransition(Duration.millis(320));

	private AssetType tipoSelecionado = AssetType.ACOES;
	private TituloTesouro tesouroSelecionado;


	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();



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

		configurarTipoCombo();
		configurarBuscaAtivos();
		configurarCampoQuantidade();
		configurarCampoPreco();
		configurarBotaoAdicionar();

		scheduler.scheduleAtFixedRate(this::atualizarTesouroDiretoRendimento, 5, 60, TimeUnit.SECONDS);
		scheduler.scheduleAtFixedRate(this::atualizarAtivosDoUsuario, 0, 5, TimeUnit.MINUTES);
		scheduler.scheduleAtFixedRate(this::atualizarCriptos, 10, 60, TimeUnit.SECONDS);

		scheduler.scheduleAtFixedRate(this::atualizarTesouroDiretoRendimento, 15, 6, TimeUnit.HOURS);


	}

	// ==========================================================
	// ATUALIZAÇÕES AUTOMÁTICAS
	// ==========================================================

	// Atualiza periodicamente preços de ações, FIIs e ETFs.
	// Sincroniza API, banco e lista visual do usuário.

	private void atualizarAtivosDoUsuario() {
		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			Dao dao = new Dao();
			@SuppressWarnings("static-access")
			var ativos = dao.listarAtivosPorUsuario(usuarioId);

			for (var ativo : ativos) {

				String cat = (ativo.getCategoria() == null ? "" : ativo.getCategoria().trim());

				if ("Renda Fixa".equalsIgnoreCase(cat)
				        || "Criptomoedas".equalsIgnoreCase(cat)
				        || "Tesouro Direto".equalsIgnoreCase(cat)) {
				    continue;
				}


				executor.submit(() -> {
					try {
						String ticker = ativo.getAtivo();
						AssetInfo info = Brapi.buscarAssetInfo(ticker);

						if (ticker == null || ticker.isBlank())
							return;
						ticker = ticker.trim().toUpperCase();

						if (info == null || info.price == null || info.price <= 0)
							return;

						double precoMedio = ativo.getPrecoMedio();
						double precoAtual = info.price;
						double variacao = precoMedio != 0 ? ((precoAtual - precoMedio) / precoMedio) * 100 : 0;
						double saldo = precoAtual * ativo.getQuantidade();

						

						dao.atualizarAtivo(ativo.getId(), ativo.getQuantidade(), precoMedio, precoAtual, variacao, saldo);
						String logoUrl = (ativo.getIconUrl() != null && !ativo.getIconUrl().isBlank()) ? ativo.getIconUrl() : Apis.LogoKit.byStockTicker(ticker);

						if ((ativo.getIconUrl() == null || ativo.getIconUrl().isBlank()) && logoUrl != null && !logoUrl.isBlank()) {
							dao.atualizarIconUrl(ativo.getId(), logoUrl);
						}

						Platform.runLater(() -> listaView.atualizarOuAdicionarAtivo(ativo.getCategoria(), ativo.getAtivo(), logoUrl, ativo.getQuantidade(), precoMedio, precoAtual, variacao, saldo));


					} catch (Exception ignored) {
					}
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// Atualiza periodicamente apenas criptomoedas.
	// Usa API específica de cripto e recalcula saldo e variação.

	private void atualizarCriptos() {
		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			Dao dao = new Dao();
			@SuppressWarnings("static-access")
			var ativos = dao.listarAtivosPorUsuario(usuarioId);

			for (var ativo : ativos) {

				if (!"Criptomoedas".equalsIgnoreCase(ativo.getCategoria()))
					continue;

				executor.submit(() -> {

					try {

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
								System.out.println("[CRYPTO-SKIP] provável preço inválido. symbol=" + symbol + " tickerApi=" + tickerApi + " pm=" + precoMedio + " preco=" + precoAtual);
								return;
							}
						}


						// 🔒 cálculo em única atribuição → efetivamente final
						double variacao = (precoMedio > 0) ? ((precoAtual - precoMedio) / precoMedio) * 100 : 0.0;

						dao.atualizarAtivo(ativo.getId(), quantidade, precoMedio, precoAtual, variacao, saldo);

						String logo = ativo.getIconUrl();
						Platform.runLater(() -> listaView.atualizarOuAdicionarCriptomoeda(logo, symbol, quantidade, precoMedio, precoAtual, variacao, saldo));

					} catch (Exception ignored) {
					}
				});
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	// ==========================================================
	// CONFIGURAÇÕES DE UI
	// ==========================================================

	// Controla mudança do tipo de ativo selecionado.
	// Ajusta comportamento da UI conforme a categoria.

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

			case CRYPTO -> {
				// Cripto: ultra fracionado
				qtd.setText("0,00000001");
			}

			case TREASURY -> {
				// Tesouro: mínimo 1% do título
				qtd.setText("0,01");
			}

			default -> {
				// Ações, FIIs, ETFs
				qtd.setText("1");
			}
			}

		});

	}

	// Configura busca com autocomplete de ativos.
	// Dispara chamadas à API conforme o tipo selecionado.

	@SuppressWarnings("unchecked")
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

				            // guarda o título selecionado
				            tesouroSelecionado = t;

				            // nome no campo
				            view.getAtivoCombo().setText(
				                t.getNome() + " " + t.getAnoVencimento()
				            );

				            // preço unitário
				            view.getPrecoField().setText(
				                CURRENCY.format(t.getPreco())
				            );

				            // taxa anual (se existir campo)
				            if (view.getTaxaField() != null) {
				                view.getTaxaField().setText(
				                    String.format(Locale.forLanguageTag("pt-BR"),
				                        "%.2f%%", t.getTaxaAnual() * 100
				                    )
				                );
				            }

				            atualizarTotal();
				            suggestionsMenu.hide();
				        });

				        suggestionsMenu.getItems().add(item);
				    }

				} else {

				    // 🔹 fluxo antigo (ações, fiis, cripto)
				    for (String s : (List<String>) results) {

				        Label lbl = new Label(s);
				        lbl.setPrefWidth(300);

				        CustomMenuItem item = new CustomMenuItem(lbl, true);

				        item.setOnAction(a -> {

				            if (tipoSelecionado == AssetType.CRYPTO) {

				                String[] parts = s.split(" - R\\$ ");
				                String nomeESimbolo = parts[0];
				                double preco = Double.parseDouble(parts[1].replace(",", "."));

				                String symbol = nomeESimbolo.substring(
				                    nomeESimbolo.indexOf("(") + 1,
				                    nomeESimbolo.indexOf(")")
				                );

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

	// Controla edição do campo quantidade.
	// Permite frações para cripto e recalcula total.

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

			// Normaliza: troca ponto por vírgula
			newVal = newVal.replace(".", ",");

			// Permite apenas dígitos e UMA vírgula
			if (!newVal.matches("[0-9,]*")) {
				qtdField.setText(oldVal);
				return;
			}

			// Impede múltiplas vírgulas
			if (newVal.chars().filter(c -> c == ',').count() > 1) {
				qtdField.setText(oldVal);
				return;
			}

			// Limita a 8 casas decimais se houver vírgula
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


			// Atualiza total SEM modificar o texto do usuário
			atualizarTotal();
		});
	}

	private void configurarCampoPreco() {
		TextField precoField = view.getPrecoField();

		precoField.textProperty().addListener((obs, oldVal, newVal) -> {
			// recalcula sempre que o preço mudar (API ou digitação)
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
	// Direciona para ações, cripto ou renda fixa.

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

	@SuppressWarnings("unused")
	private void processarAdicaoRendaFixa() {

		// --- LEITURA E VALIDAÇÃO NA THREAD DE UI (antes do Task) ---
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
		} catch (Exception ignored) {
			taxaParsed = 0.0;
		}

		double valorInicial = parsePreco(view.getPrecoField().getText());

		// monta nome canônico (inclui indexador)
		String taxaFormatada = String.format(Locale.forLanguageTag("pt-BR"), "%.2f%%", taxaParsed * 100).replace('.', ',');
		String nomeAtivo = String.join(" - ", tipoTitulo == null ? "" : tipoTitulo, emissor == null ? "" : emissor, indexador == null ? "" : indexador, forma == null ? "" : forma,
				taxaFormatada == null ? "" : taxaFormatada).replaceAll("\\s*-\\s*$", "");

		// Variáveis finais para uso seguro dentro do Task/lambda
		final String nomeAtivoFinal = nomeAtivo;
		final double taxaFinal = taxaParsed;
		final double valorInicialFinal = valorInicial;
		final String indexadorFinal = indexador;
		final String formaFinal = forma;
		final String tipoTituloFinal = tipoTitulo;
		final String emissorFinal = emissor;

		Task<Void> task = new Task<>() {
			@Override
			protected Void call() {
				try {
					Integer usuarioId = SessaoTemp.getUsuarioId();
					if (usuarioId == null)
						return null;

					Dao dao = new Dao();

					// procura por título existente usando o nome canônico
					var ativoExistente = dao.buscarAtivoPorUsuarioECategoria(usuarioId, nomeAtivoFinal, "Renda Fixa");

					if (ativoExistente != null) {
						// soma o saldo ao existente (mantém rentabilidade armazenada em 'quantidade')
						double saldoAntigo = ativoExistente.getSaldo();
						double novoSaldo = saldoAntigo + valorInicialFinal;

						double rentabilidadeExistente = ativoExistente.getQuantidade(); // quantidade guarda rentabilidade no modelo atual
						double variacaoExistente = ativoExistente.getVariacao();

						// atualiza no banco (mantendo o padrão: quantidade = rentabilidade)
						dao.atualizarAtivo(ativoExistente.getId(), rentabilidadeExistente, 0.0, 0.0, variacaoExistente, novoSaldo);

						// atualiza UI
						Platform.runLater(() -> listaView.atualizarOuAdicionarRendaFixa(nomeAtivoFinal, rentabilidadeExistente, variacaoExistente, novoSaldo));

					} else {
						// não existe: insere novo (quantidade guarda a rentabilidade)
						dao.insertRendaFixa(usuarioId, nomeAtivoFinal, taxaFinal, 0.0, valorInicialFinal);

						Platform.runLater(() -> listaView.adicionarRendaFixa(nomeAtivoFinal, taxaFinal, 0.0, valorInicialFinal));
					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
		};

		executor.submit(task);
	}

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
			return; // ou alerta de valor inválido
		}

		BigDecimal saldo = quantidade.multiply(BigDecimal.valueOf(t.getPreco())).setScale(2, RoundingMode.HALF_UP);

		final String hojeStr = java.time.LocalDate.now().toString();
		final double aporte = saldo.doubleValue(); // principal (base)

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

						// >>> NOVO: acumula principal e salva "último dia"
						double principalAntigo = existente.getTdPrincipal(); // precisa existir no model
						double principalNovo = principalAntigo + aporte;

						String indexador = t.getIndexador(); // precisa existir em TituloTesouro
						double taxaAnual = t.getTaxaAnual(); // você já usa isso no autocomplete

						dao.atualizarCamposTesouroMeta(existente.getId(), indexador, taxaAnual, principalNovo, hojeStr);

						Platform.runLater(() -> listaView.atualizarOuAdicionarTesouroDireto(nomeAtivo, qtdTotal.doubleValue(), variacao, saldoTotal.doubleValue()));

					} else {

						BigDecimal variacao = BigDecimal.ZERO;

						dao.insertTesouroDireto(
								usuarioId, nomeAtivo, quantidade.setScale(2, RoundingMode.HALF_UP), variacao, saldo.setScale(2, RoundingMode.HALF_UP));

						// >>> NOVO: salva meta TD (indexador, taxa, principal, ultimo dia)
						String indexador = t.getIndexador();
						double taxaAnual = t.getTaxaAnual();
						dao.atualizarCamposTesouroMetaPorNome(usuarioId, nomeAtivo, indexador, taxaAnual, aporte, hojeStr);

						Platform.runLater(() -> listaView.adicionarTesouroDireto(nomeAtivo, quantidade.doubleValue(), variacao.doubleValue(), saldo.doubleValue()));

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
	// AÇÕES / FIIS
	// ==========================================================

	// Processa compra de ações, FIIs e ETFs.
	// Atualiza preço médio, quantidade e saldo.

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

						Platform.runLater(() -> listaView.atualizarOuAdicionarAtivo(categoria, ticker, logoUrl, qtdTotal, novoPM, precoAtual, variacao, saldo));

					} else {

						double saldo = precoAtual * qtdNova;
						String logoUrl = Apis.LogoKit.byStockTicker(ticker);

						dao.insertAtivo(categoria, ticker, logoUrl, qtdNova, precoAtual, precoAtual, 0, saldo);

						Platform.runLater(() -> listaView.adicionarAcoesEFiis(categoria, ticker, logoUrl, qtdNova, precoAtual, precoAtual, 0.0, saldo));
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

	// Processa compra de criptomoedas.
	// Permite frações e recalcula preço médio.

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

						Platform.runLater(() -> listaView.atualizarOuAdicionarCriptomoeda(logoUrl, sym, qtdTotal, novoPM, precoAtual, variacao, saldo));

					} else {

						double saldo = precoAtual * qtdNova;

						String logoUrl = LogoKit.byStockTicker(sym); // ou null se você não quiser logo pra cripto

						dao.insertAtivo("Criptomoedas", sym, logoUrl, qtdNova, precoAtual, precoAtual, 0, saldo);

						final String logoFinal = logoUrl;
						Platform.runLater(() -> listaView.adicionarCriptomoeda(logoFinal, sym, qtdNova, precoAtual, precoAtual, 0.0, saldo));

					}

				} catch (Exception ex) {
				}
				return null;
			}
		};

		executor.submit(task);
	}


	private void atualizarTesouroDiretoRendimento() {
		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)

				return;

			Dao dao = new Dao();
			@SuppressWarnings("static-access")
			var ativos = dao.listarAtivosPorUsuario(usuarioId);

			// Data atual do PC
			java.time.LocalDate hoje = java.time.LocalDate.now();

			System.out.println("[TD] rodando rendimento. hoje=" + hoje);

			executor.submit(() -> {
				try {
					// Selic diária: sua API já pega o último valor da SGS 11 (taxa ao dia). :contentReference[oaicite:0]{index=0}
					// Cache simples pra não bater várias vezes no mesmo loop:
					Double selicDiaPercent = null;

					for (var ativo : ativos) {
						if (!"Tesouro Direto".equalsIgnoreCase(ativo.getCategoria()))
							continue;

						// Esses getters você vai criar no seu model Ativo:
						String indexador = ativo.getTdIndexador(); // "SELIC", "PREFIXADO", "IPCA+"
						double taxaAnual = ativo.getTdTaxaAnual(); // ex: 0.1188 (11,88% a.a.)
						double principal = ativo.getTdPrincipal(); // base
						String ultimoDiaStr = ativo.getTdUltimoDia(); // "2026-01-08"

						if (indexador == null || indexador.isBlank()) {
							System.out.println("[TD-SKIP] sem indexador: ativo=" + ativo.getAtivo());
							continue;
						}
						if (ultimoDiaStr == null || ultimoDiaStr.isBlank()) {
							System.out.println("[TD-SKIP] sem ultimoDia: ativo=" + ativo.getAtivo() + " -> setando hoje");
							dao.atualizarCamposTesouroMeta(ativo.getId(), indexador, taxaAnual, principal, hoje.toString());
							continue;
						}


						java.time.LocalDate ultimoDia;
						try {
							ultimoDia = java.time.LocalDate.parse(ultimoDiaStr);
						} catch (Exception e) {
							// se vier lixo, zera para hoje e não calcula
							dao.atualizarCamposTesouroMeta(ativo.getId(), indexador, taxaAnual, principal, hoje.toString());
							continue;
						}

						long dias = java.time.temporal.ChronoUnit.DAYS.between(ultimoDia, hoje);
						if (dias <= 0)
							continue;

						double saldoAnterior = ativo.getSaldo();
						double saldoNovo = saldoAnterior;

						// tempo em anos (aprox). Para MVP tá ok.
						double tAnos = dias / 365.0;

						switch (indexador.toUpperCase()) {

						case "SELIC": {
							if (selicDiaPercent == null) {
								selicDiaPercent = new Apis.SelicApi().getUltimaTaxaSelic(); // ex: 0.0456 (percent ao dia)
							}
							double f = Math.pow(1.0 + (selicDiaPercent / 100.0), dias);
							// “marcação na curva” simples: aplica no saldo atual
							saldoNovo = saldoAnterior * f;
							break;
						}

						case "PREFIXADO": {
							// ValorBruto(t) = Valor * (1 + taxa)^tempo
							saldoNovo = saldoAnterior * Math.pow(1.0 + taxaAnual, tAnos);
							break;
						}

						case "IPCA+":
						case "RENDA+":
						case "EDUCA+": {
							double ipcaAa = Apis.IpcaService.getIpcaAtual(); // % a.a.
							if (ipcaAa < 0)
								ipcaAa = 0;

							// fator do período (dias)
							double fIpca = Math.pow(1.0 + (ipcaAa / 100.0), tAnos);
							double fReal = Math.pow(1.0 + taxaAnual, tAnos); // taxa real a.a.

							// aplica no saldo atual (coerente com "render por período")
							double fatorPeriodo = fIpca * fReal;
							saldoNovo = saldoAnterior * fatorPeriodo;
							break;
						}


						default:
							continue;
						}

						System.out.println("[TD] ativo=" + ativo.getAtivo() + " ultimoDia=" + ultimoDiaStr + " dias=" + dias + " idx=" + indexador + " taxaAnual=" + taxaAnual + " principal="
								+ principal + " saldoAntes=" + saldoAnterior);

						// variação vs principal
						double variacao = (principal > 0) ? ((saldoNovo - principal) / principal) * 100.0 : 0.0;

						double qtd = ativo.getQuantidade();
						double puEstimado = (qtd > 0) ? (saldoNovo / qtd) : 0.0;

						System.out.printf("[TD] %s | idx=%s | dias=%d | principal=%.2f | saldoAntes=%.2f | saldoNovo=%.2f | var=%.4f%% | pu=%.6f%n", ativo.getAtivo(), indexador, dias, principal,
								saldoAnterior, saldoNovo, variacao, puEstimado);

						// Atualiza banco:
						dao.atualizarAtivo(ativo.getId(), qtd, 0.0, puEstimado, variacao, saldoNovo);
						dao.atualizarCamposTesouroMeta(ativo.getId(), indexador, taxaAnual, principal, hoje.toString());

						// ====== snapshot FINAL para o runLater ======
						final String nomeAtivoFinal = ativo.getAtivo();
						final double qtdFinal = qtd;
						final double variacaoFinal = variacao;
						final double saldoNovoFinal = saldoNovo;

						Platform.runLater(() -> listaView.atualizarOuAdicionarTesouroDireto(nomeAtivoFinal, qtdFinal, variacaoFinal, saldoNovoFinal));

					}

				} catch (Exception ex) {
					ex.printStackTrace();
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// =============
	// HELPERS
	// ==============

	// Converte texto monetário em double.
	// Normaliza formato brasileiro.

	private double parsePreco(String txt) {
		try {
			if (txt == null)
				return 0.0;

			// remove NBSP (espaço invisível do NumberFormat) + espaços normais
			txt = txt.replace('\u00A0', ' ').trim();

			// remove símbolo e qualquer lixo que não seja número/separador
			txt = txt.replace("R$", "").trim();

			// remove milhares e padroniza decimal
			txt = txt.replace(".", "").replace(",", ".").trim();

			// depois de limpar, ainda pode sobrar espaço → remove tudo
			txt = txt.replace(" ", "");

			if (txt.isBlank())
				return 0.0;

			return Double.parseDouble(txt);
		} catch (Exception e) {
			return 0.0;
		}
	}


	// Converte quantidade digitada em double.
	// Suporta vírgula como separador decimal.

	private double parseQtd(String txt) {
		if (txt == null || txt.isBlank())
			return 0;
		return Double.parseDouble(txt.replace(",", "."));
	}


	// Busca preço atual via API para ações/FIIs.
	// Atualiza UI e lista visual.

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

	public void dispose() {
		try {
			scheduler.shutdownNow();
		} catch (Exception ignored) {
		}

		try {
			executor.shutdownNow();
		} catch (Exception ignored) {
		}
	}

	private static final String LOGOKIT_TOKEN = "pk_fre0d0771b214e45db3dbb";

	private static String buildLogoUrl(String ticker) {
		if (ticker == null || ticker.isBlank())
			return null;

		String t = ticker.trim().toUpperCase();

		// Heurística pra B3: 4 letras + 1 ou 2 dígitos (BBAS3, PETR4, BBDC4, etc)
		if (t.matches("^[A-Z]{4}\\d{1,2}$")) {
			t = t + ".SA"; // ou ":BZ" se você preferir
		}

		// fallback=monogram evita "imagem quebrada" quando não existir logo
		return "https://img.logokit.com/ticker/" + t + "?token=" + LOGOKIT_TOKEN + "&size=64" + "&fallback=monogram";
	}

	private String resolverTickerCriptoParaPreco(String symbol, double precoMedio) {
		String s = symbol.trim().toUpperCase();

		// candidatos comuns (ajuste conforme sua API)
		String[] cands = new String[] { s, // BTC
				s + "BRL", // BTCBRL
				s + "-BRL", // BTC-BRL
				s + "USD", // BTCUSD
				s + "-USD" // BTC-USD
		};

		Double melhor = null;
		String melhorTicker = s;
		double melhorScore = Double.POSITIVE_INFINITY;

		for (String cand : cands) {
			Double p = Brapi.buscarAssetPrice(cand);
			if (p == null || p <= 0)
				continue;

			// se ainda não tem PM (deveria ter), aceita o primeiro válido
			if (precoMedio <= 0)
				return cand;

			// score = quão perto está do PM (em razão)
			double ratio = p / precoMedio;
			double score = Math.abs(Math.log(ratio)); // 0 = perfeito, alto = muito diferente
			if (score < melhorScore) {
				melhorScore = score;
				melhor = p;
				melhorTicker = cand;
			}
		}

		return melhor != null ? melhorTicker : s;
	}



}
