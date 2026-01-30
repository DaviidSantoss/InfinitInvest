package ControllerInfinit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import Apis.Brapi;
import Apis.Brapi.AssetInfo;
import Apis.LogoKit;
import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO.SessaoTemp;
import MainInfinit.ListaDeAtivos;
import javafx.application.Platform;

public class AtualizadorCarteira {

	// ==========================================================
	// CONTEXTO DO ATUALIZADOR
	// ==========================================================
	// Serviço central de atualização da carteira.
	// Agenda rotinas de refresh (ações/fiis/etfs, cripto em BRL, tesouro e renda fixa),
	// sincronizando API, banco e a lista visual (JavaFX).

	private final ListaDeAtivos listaView;

	private final ExecutorService executor = Executors.newCachedThreadPool();
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private final AtomicBoolean atualizandoAtivos = new AtomicBoolean(false);
	private final AtomicBoolean atualizandoCriptos = new AtomicBoolean(false);
	private final AtomicBoolean atualizandoTesouro = new AtomicBoolean(false);
	private final AtomicBoolean atualizandoRendaFixa = new AtomicBoolean(false);

	public AtualizadorCarteira(ListaDeAtivos listaView) {
		this.listaView = listaView;
	}

	// ==========================================================
	// LIFECYCLE
	// ==========================================================
	// Inicia e encerra as rotinas de atualização.

	public void start() {
		// roda uma vez imediatamente
		scheduler.execute(this::atualizarAtivosDoUsuario);
		scheduler.execute(this::atualizarCriptosEmBRL);
		scheduler.execute(this::atualizarTesouroDiretoRendimento);
		scheduler.execute(this::atualizarRendaFixaRendimento);

		// agenda recorrente
		scheduler.scheduleAtFixedRate(this::atualizarAtivosDoUsuario, 5, 5, TimeUnit.MINUTES);
		scheduler.scheduleAtFixedRate(this::atualizarCriptosEmBRL, 10, 60, TimeUnit.SECONDS);

		// Tesouro e RF não precisam rodar toda hora
		scheduler.scheduleAtFixedRate(this::atualizarTesouroDiretoRendimento, 15, 6, TimeUnit.HOURS);
		scheduler.scheduleAtFixedRate(this::atualizarRendaFixaRendimento, 20, 6, TimeUnit.HOURS);
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

	// ==========================================================
	// ATUALIZAÇÕES: AÇÕES / FIIs / ETFs (+ DY mensal)
	// ==========================================================

	private void atualizarAtivosDoUsuario() {
		if (!atualizandoAtivos.compareAndSet(false, true))
			return;

		try {
			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null) {
				scheduler.schedule(this::atualizarAtivosDoUsuario, 1, TimeUnit.SECONDS);
				return;
			}

			Dao daoList = new Dao();
			@SuppressWarnings("static-access")
			var ativos = daoList.listarAtivosPorUsuario(usuarioId);

			for (var ativo : ativos) {
				String cat = (ativo.getCategoria() == null ? "" : ativo.getCategoria().trim());

				if ("Renda Fixa".equalsIgnoreCase(cat) || "Criptomoedas".equalsIgnoreCase(cat) || "Tesouro Direto".equalsIgnoreCase(cat)) {
					continue;
				}

				executor.submit(() -> {
					try {
						String ticker = ativo.getAtivo();
						if (ticker == null || ticker.isBlank())
							return;

						ticker = ticker.trim().toUpperCase();

						String tickerApi = normalizarTickerB3ParaApi(ticker);

						AssetInfo info = Brapi.buscarAssetInfo(tickerApi);
						if (info == null || info.price == null || info.price <= 0)
							return;

						double precoMedio = ativo.getPrecoMedio();
						double precoAtual = info.price;

						if (precoMedio > 0) {
							double ratio = precoAtual / precoMedio;
							if (ratio < 0.05 || ratio > 20.0)
								return;
						}

						double variacao = (precoMedio > 0) ? ((precoAtual - precoMedio) / precoMedio) * 100.0 : 0.0;
						double saldo = precoAtual * ativo.getQuantidade();

						double dyMensalPercent = 0.0;
						try {
							dyMensalPercent = Brapi.buscarDividendYieldMensal12mPercent(ticker, precoAtual);
						} catch (Exception ex) {
							ex.printStackTrace();
							dyMensalPercent = 0.0;
						}

						// Dao dentro da task evita concorrência com a mesma conexão/estado
						Dao dao = new Dao();
						dao.atualizarAtivo(ativo.getId(), ativo.getQuantidade(), precoMedio, precoAtual, variacao, saldo);

						String logoUrl = ativo.getIconUrl();
						if (logoUrl == null || logoUrl.isBlank()) {
							logoUrl = LogoKit.byStockTicker(ticker);
						}
						final String logoUrlFinal = logoUrl;

						final String categoriaFinal = ativo.getCategoria();
						final String tickerFinal = ticker;
						final double qtdFinal = ativo.getQuantidade();
						final double dyMensalFinal = dyMensalPercent;

						Platform.runLater(() -> listaView.atualizarOuAdicionarAtivo(categoriaFinal, tickerFinal, logoUrlFinal, qtdFinal, precoMedio, precoAtual, variacao, dyMensalFinal, saldo));

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});
			}

		} finally {
			atualizandoAtivos.set(false);
		}
	}

	// ==========================================================
	// ATUALIZAÇÕES: CRIPTOS (SEMPRE EM BRL)
	// ==========================================================

	private void atualizarCriptosEmBRL() {
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
						String symbol = ativo.getAtivo();
						if (symbol == null || symbol.isBlank())
							return;
						symbol = symbol.trim().toUpperCase();

						double precoMedio = ativo.getPrecoMedio();

						PriceBRLResult r = buscarPrecoCriptoEmBRL(symbol);
						if (r == null || r.precoBRL == null || r.precoBRL <= 0)
							return;

						double precoAtual = r.precoBRL;
						double quantidade = ativo.getQuantidade();

						if (precoMedio > 0) {
							double ratio = precoAtual / precoMedio;
							if (ratio < 0.05 || ratio > 20.0)
								return;
						}

						double saldo = precoAtual * quantidade;
						double variacao = (precoMedio > 0) ? ((precoAtual - precoMedio) / precoMedio) * 100.0 : 0.0;

						Dao dao = new Dao();
						dao.atualizarAtivo(ativo.getId(), quantidade, precoMedio, precoAtual, variacao, saldo);

						String logo = ativo.getIconUrl();

						final String symbolFinal = symbol;
						final double pmFinal = precoMedio;
						final double paFinal = precoAtual;
						final double qtdFinal = quantidade;
						final double varFinal = variacao;
						final double saldoFinal = saldo;

						Platform.runLater(() -> listaView.atualizarOuAdicionarCriptomoeda(logo, symbolFinal, qtdFinal, pmFinal, paFinal, varFinal, saldoFinal));

					} catch (Exception ex) {
						ex.printStackTrace();
					}
				});
			}

		} finally {
			atualizandoCriptos.set(false);
		}
	}

	// ==========================================================
	// ATUALIZAÇÕES: TESOURO DIRETO
	// ==========================================================

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
					atualizandoTesouro.set(false);
				}
			});

		} catch (Exception ex) {
			ex.printStackTrace();
			atualizandoTesouro.set(false);
		}
	}

	// ==========================================================
	// ATUALIZAÇÕES: RENDA FIXA
	// ==========================================================

	private void atualizarRendaFixaRendimento() {
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
								saldoNovo = saldoAnterior * Math.pow(1.0 + taxaAnual, tAnos);
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
							saldoNovo = saldoAnterior * Math.pow(1.0 + taxaAnual, tAnos);
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
	// Normalização de ticker e rotinas de preço cripto em BRL.

	private String normalizarTickerB3ParaApi(String ticker) {
		if (ticker == null)
			return "";
		String t = ticker.trim().toUpperCase();
		if (t.matches("^[A-Z]{4}\\d{1,2}$"))
			return t + ".SA";
		return t;
	}

	private static class PriceBRLResult {
		final Double precoBRL;
		@SuppressWarnings("unused")
		final String origem;

		PriceBRLResult(Double precoBRL, String origem) {
			this.precoBRL = precoBRL;
			this.origem = origem;
		}
	}

	private PriceBRLResult buscarPrecoCriptoEmBRL(String symbol) {
		String s = symbol.trim().toUpperCase();

		String[] brlCands = new String[] { s + "BRL", s + "-BRL", s + "/BRL" };
		for (String cand : brlCands) {
			Double p = safePrice(cand);
			if (p != null && p > 0)
				return new PriceBRLResult(p, "BRL direto: " + cand);
		}

		String[] usdCands = new String[] { s + "USD", s + "-USD", s + "/USD" };
		Double usd = null;
		String usdTicker = null;
		for (String cand : usdCands) {
			Double p = safePrice(cand);
			if (p != null && p > 0) {
				usd = p;
				usdTicker = cand;
				break;
			}
		}
		if (usd == null)
			return null;

		Double usdbrl = buscarUsdBrl();
		if (usdbrl == null || usdbrl <= 0)
			return null;

		double brl = usd * usdbrl;
		return new PriceBRLResult(brl, "USD convertido: " + usdTicker + " * USDBRL=" + usdbrl);
	}

	private Double safePrice(String ticker) {
		try {
			return Brapi.buscarAssetPrice(ticker);
		} catch (Exception e) {
			return null;
		}
	}

	private Double buscarUsdBrl() {
		String[] fxCands = new String[] { "USDBRL", "USD-BRL", "USDBRL=X", "BRL=X" };
		for (String fx : fxCands) {
			Double p = safePrice(fx);
			if (p != null && p > 0)
				return p;
		}
		return null;
	}
}
