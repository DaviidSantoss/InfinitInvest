package Apis;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

public class Brapi {

	// ==========================================================
	// CONFIGURAÇÕES E CONSTANTES
	// ==========================================================

	private static final String EODHDToken = "697bd52def20f8.31683743";
	private static final String EODHDurl = "https://eodhd.com/api";
	private static final String brapiToken = "usX4AiGRdvqLmqXjMjD2Fx";

	private static final int TEMPO_LIMITE_CONEXAO = 8000;
	private static final int TEMPO_LIMITE_LEITURA = 12000;
	private static final int QUANTIDADE_CANDIDATOS = 40;

	private static final ExecutorService EXECUTOR_DETALHES = Executors.newFixedThreadPool(10);

	private static final Cache<String, informacoesDoAtivo> CACHE = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1500).build();

	// ==========================================================
	// CACHE DE ATIVOS (DISCO)
	// ==========================================================

	private static final Path ARQUIVO_CACHE = Paths.get("cache_ativos.json");

	private static final long TEMPO_EXPIRACAO_CACHE = 24;

	private static final ConcurrentHashMap<String, informacoesDoAtivo> DISCO_CACHE = new ConcurrentHashMap<>();

	// ==========================================================
	// CACHE DIVIDENDOS
	// ==========================================================

	private static final Path DIV_CACHE_FILE = Paths.get("cache_dividendos_12m.json");

	private static final Duration DIV_TTL_OK = Duration.ofDays(30);

	private static final Duration DIV_TTL_EMPTY = Duration.ofDays(3);

	private static final ConcurrentHashMap<String, DivEntry> DIV_DISK_CACHE = new ConcurrentHashMap<>();

	// ==========================================================
	// INIT
	// ==========================================================

	static {
		carregarCacheDoDisco();
		carregarCacheDividendosDoDisco();
	}

	// ==========================================================
	// ENUMS / MODELOS
	// ==========================================================

	public enum tipoDoAtivo {
		ACOES, FIIS, CRYPTO, ETF, TESOURO, DESCONHECIDO
	}

	public static class informacoesDoAtivo {

		public final String sigla;
		public final String nome;
		public final tipoDoAtivo tipo;
		public final Double preco;

		public informacoesDoAtivo(String sigla, String nome, tipoDoAtivo tipo, Double preco) {
			this.sigla = sigla;
			this.nome = nome;
			this.tipo = tipo;
			this.preco = preco;
		}

		public informacoesDoAtivo(String sigla, String nome, tipoDoAtivo tipo) {
			this(sigla, nome, tipo, null);
		}

		@Override
		public String toString() {
			return sigla + (nome != null && !nome.isEmpty() ? " - " + nome : "");
		}
	}

	// ==========================================================
	// AUTOCOMPLETE PRINCIPAL
	// ==========================================================

	public static List<String> buscarAtivosPorTipo(tipoDoAtivo tipo, String query) {

		if (tipo == null)
			tipo = tipoDoAtivo.DESCONHECIDO;

		try {

			return switch (tipo) {

			case CRYPTO -> buscarCripto(query);

			case ETF -> buscarEtfs(query);

			case FIIS, ACOES, DESCONHECIDO -> buscarAcoesOuFiis(tipo, query);

			case TESOURO -> List.of();
			};

		} catch (Exception e) {

			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	// ==========================================================
	// PREÇO RÁPIDO
	// ==========================================================

	public static Double buscarPrecoAcoes(String sigla) {

		try {

			informacoesDoAtivo info = buscarAssetInfo(sigla);

			return info != null ? info.preco : null;

		} catch (Exception e) {
			return null;
		}
	}

	// ==========================================================
	// CRIPTO
	// ==========================================================

	private static List<String> buscarCripto(String query) {

		List<String> out = new ArrayList<>();

		try {

			String url = "https://api.coingecko.com/api/v3/coins/markets" + "?vs_currency=brl" + "&order=market_cap_desc" + "&per_page=60&page=1";

			HttpClient cliente = HttpClient.newHttpClient();

			HttpRequest requisicoe = HttpRequest.newBuilder().uri(URI.create(url)).build();

			HttpResponse<String> resposta = cliente.send(requisicoe, HttpResponse.BodyHandlers.ofString());

			if (resposta.statusCode() != 200)
				return out;

			JSONArray arr = new JSONArray(resposta.body());

			String q = query.toLowerCase();

			for (int i = 0; i < arr.length(); i++) {

				JSONObject o = arr.getJSONObject(i);

				String name = o.getString("name");
				String sym = o.getString("symbol").toUpperCase();

				double price = o.getDouble("current_price");

				if (name.toLowerCase().contains(q) || sym.toLowerCase().contains(q)) {

					out.add(name + " (" + sym + ")" + " - R$ " + price);
				}
			}

		} catch (Exception ignored) {
		}

		return out;
	}

	// ==========================================================
	// ETFS
	// ==========================================================

	private static List<String> buscarEtfs(String query) {

		List<String> out = new ArrayList<>();

		try {

			URL url = new URL("https://brapi.dev/api/quote/list" + "?stockType=fund" + "&token=" + brapiToken);

			JSONObject json = (JSONObject) buscarJson(url);

			JSONArray arr = json.optJSONArray("stocks");

			if (arr == null)
				return out;

			String q = query.toLowerCase();

			for (int i = 0; i < arr.length(); i++) {

				JSONObject o = arr.getJSONObject(i);

				String stock = o.optString("stock", "");

				String name = o.optString("name", "");

				if (!stock.endsWith("11"))
					continue;

				if (stock.toLowerCase().contains(q) || name.toLowerCase().contains(q)) {

					out.add(stock + " - " + name);
				}
			}

		} catch (Exception ignored) {
		}

		return out;
	}

	// ==========================================================
	// TESOURO
	// ==========================================================

	public static List<TituloTesouro> buscarTreasuries(String query) {

		if (query == null || query.isBlank())
			return List.of();

		String q = query.toLowerCase().trim();

		List<TituloTesouro> titulos = TesouroDataLoader.buscarTitulos(1900);

		return titulos.stream().filter(t -> {

			if (t.getNome().toLowerCase().contains(q))
				return true;

			if (String.valueOf(t.getAnoVencimento()).contains(q))
				return true;

			String combinado = (t.getNome() + " " + t.getAnoVencimento()).toLowerCase();

			return combinado.contains(q);

		}).limit(40).toList();
	}

	// ==========================================================
	// AÇÕES / FIIS
	// ==========================================================

	private static List<String> buscarAcoesOuFiis(tipoDoAtivo tipo, String query) throws Exception {

		if (query == null || query.isBlank())
			return new ArrayList<>();

		String key = query.trim().toUpperCase();

		List<String> cacheResults = DISCO_CACHE.values().stream().filter(ai -> ai.sigla.contains(key) || ai.nome.toUpperCase().contains(key)).map(informacoesDoAtivo::toString)
				.collect(Collectors.toList());

		if (!cacheResults.isEmpty())
			return cacheResults;

		String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

		URL url = new URL("https://brapi.dev/api/available" + "?search=" + encoded + "&token=" + brapiToken);

		JSONObject json = (JSONObject) buscarJson(url);

		JSONArray arr = json.optJSONArray("stocks");

		if (arr == null)
			return new ArrayList<>();

		List<String> out = new CopyOnWriteArrayList<>();

		List<CompletableFuture<Void>> futures = new ArrayList<>();

		int limit = Math.min(arr.length(), QUANTIDADE_CANDIDATOS);

		for (int i = 0; i < limit; i++) {

			String ticker = arr.getString(i);

			futures.add(

					CompletableFuture.supplyAsync(() -> buscarAssetInfo(ticker), EXECUTOR_DETALHES).thenAccept(info -> {

						if (info != null) {

							DISCO_CACHE.put(info.sigla, info);

							out.add(info.toString());
						}
					}));
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

		salvarCacheNoDisco();

		return out;
	}

	// ==========================================================
	// DIVIDEND YIELD MENSAL
	// ==========================================================

	public static double buscarDividendYieldMensal12mPercent(String tickerB3, double precoAtual) {

		try {

			if (tickerB3 == null || tickerB3.isBlank() || precoAtual <= 0) {
				return 0.0;
			}

			if (EODHDToken == null || EODHDToken.isBlank()) {
				return 0.0;
			}

			String t = tickerB3.trim().toUpperCase();

			LocalDate hoje = LocalDate.now();

			JSONArray cachedDivs = getDividendos12mFromDiskIfFresh(t);

			if (cachedDivs != null) {

				double soma12m = somarDivsNoPeriodo(cachedDivs, hoje);

				if (soma12m <= 0)
					return 0.0;

				return ((soma12m / precoAtual) / 12.0) * 100.0;
			}

			String symbol = toEodhdSymbol(t);

			LocalDate from = hoje.minusMonths(12);

			String urlStr = EODHDurl + "/div/" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) + "?from=" + from + "&to=" + hoje + "&api_token="
					+ URLEncoder.encode(EODHDToken, StandardCharsets.UTF_8) + "&fmt=json";

			JSONArray arr = (JSONArray) buscarJson(new URL(urlStr));

			if (arr == null || arr.length() == 0) {

				putDividendos12mOnDisk(t, new JSONArray(), false);

				return 0.0;
			}

			double soma12mPorAcao = somarDivsNoPeriodo(arr, hoje);

			if (soma12mPorAcao <= 0) {

				putDividendos12mOnDisk(t, new JSONArray(), false);

				return 0.0;
			}

			double yieldMensalPercent = ((soma12mPorAcao / precoAtual) / 12.0) * 100.0;

			yieldMensalPercent = Math.max(0.0, yieldMensalPercent);

			putDividendos12mOnDisk(t, arr, true);

			return yieldMensalPercent;

		} catch (Exception e) {
			return 0.0;
		}
	}

	private static String toEodhdSymbol(String b3Ticker) {
		return b3Ticker + ".SA";
	}

	private static double somarDivsNoPeriodo(JSONArray arr, LocalDate hoje) {

		double soma = 0.0;

		for (int i = 0; i < arr.length(); i++) {

			JSONObject d = arr.optJSONObject(i);

			if (d == null)
				continue;

			String dateStr = d.optString("date", "");

			double value = d.optDouble("value", 0.0);

			if (value <= 0)
				continue;

			LocalDate data;

			try {

				data = LocalDate.parse(dateStr);

			} catch (Exception ignore) {
				continue;
			}

			long days = ChronoUnit.DAYS.between(data, hoje);

			if (days < 0 || days > 366)
				continue;

			soma += value;
		}

		return soma;
	}

	// ==========================================================
	// DIVIDEND CACHE
	// ==========================================================

	private static class DivEntry {

		final long fetchedAtEpochMs;
		final boolean hadData;
		final JSONArray data;

		DivEntry(long fetchedAtEpochMs, boolean hadData, JSONArray data) {

			this.fetchedAtEpochMs = fetchedAtEpochMs;

			this.hadData = hadData;

			this.data = data;
		}
	}

	private static void carregarCacheDividendosDoDisco() {

		try {

			if (!Files.exists(DIV_CACHE_FILE))
				return;

			JSONObject root = new JSONObject(Files.readString(DIV_CACHE_FILE, StandardCharsets.UTF_8));

			for (String ticker : root.keySet()) {

				JSONObject o = root.getJSONObject(ticker);

				long fetchedAt = o.optLong("fetchedAt", 0L);

				boolean hadData = o.optBoolean("hadData", false);

				JSONArray data = o.optJSONArray("data");

				if (data == null)
					data = new JSONArray();

				DIV_DISK_CACHE.put(ticker, new DivEntry(fetchedAt, hadData, data));
			}

		} catch (Exception ignored) {
		}
	}

	private static void salvarCacheDividendosNoDisco() {

		try {

			JSONObject root = new JSONObject();

			DIV_DISK_CACHE.forEach((ticker, entry) -> {

				JSONObject o = new JSONObject();

				o.put("fetchedAt", entry.fetchedAtEpochMs);

				o.put("hadData", entry.hadData);

				o.put("data", entry.data != null ? entry.data : new JSONArray());

				root.put(ticker, o);
			});

			Files.writeString(DIV_CACHE_FILE, root.toString(2), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		} catch (Exception ignored) {
		}
	}

	private static JSONArray getDividendos12mFromDiskIfFresh(String ticker) {

		DivEntry e = DIV_DISK_CACHE.get(ticker);

		if (e == null)
			return null;

		Duration ttl = e.hadData ? DIV_TTL_OK : DIV_TTL_EMPTY;

		Instant fetchedAt = Instant.ofEpochMilli(e.fetchedAtEpochMs);

		if (Duration.between(fetchedAt, Instant.now()).compareTo(ttl) < 0) {
			return e.data;
		}

		return null;
	}

	private static void putDividendos12mOnDisk(String ticker, JSONArray arr, boolean hadData) {

		DIV_DISK_CACHE.put(ticker, new DivEntry(System.currentTimeMillis(), hadData, arr != null ? arr : new JSONArray()));

		salvarCacheDividendosNoDisco();
	}

	// ==========================================================
	// BUSCAR INFO ATIVO
	// ==========================================================

	public static informacoesDoAtivo buscarAssetInfo(String sigla) {

		try {

			informacoesDoAtivo cached = CACHE.getIfPresent(sigla);

			if (cached != null)
				return cached;

			String encoded = URLEncoder.encode(sigla, StandardCharsets.UTF_8);

			URL url = new URL("https://brapi.dev/api/quote/" + encoded + "?fundamental=true" + "&token=" + brapiToken);

			JSONObject json = (JSONObject) buscarJson(url);

			JSONArray res = json.optJSONArray("results");

			if (res == null || res.length() == 0) {
				return null;
			}

			JSONObject r = res.getJSONObject(0);

			String nome = r.optString("longName", r.optString("shortName", sigla));

			double preco = r.optDouble("regularMarketPrice", 0.0);

			boolean isEtf = r.optBoolean("isEtf", false);

			String tipoDoCampo = r.optString("assetType", "").toLowerCase();

			tipoDoAtivo tipo;

			if (isEtf || tipoDoCampo.contains("etf")) {

				tipo = tipoDoAtivo.ETF;

			} else if (sigla.endsWith("11")) {

				tipo = tipoDoAtivo.FIIS;

			} else {

				tipo = tipoDoAtivo.ACOES;
			}

			informacoesDoAtivo info = new informacoesDoAtivo(sigla.toUpperCase(), nome, tipo, preco);

			CACHE.put(sigla, info);

			return info;

		} catch (Exception e) {
			return null;
		}
	}

	// ==========================================================
	// HTTP HELPERS
	// ==========================================================

	private static Object buscarJson(URL url) throws Exception {

		HttpURLConnection conexao = (HttpURLConnection) url.openConnection();

		conexao.setConnectTimeout(TEMPO_LIMITE_CONEXAO);
		conexao.setReadTimeout(TEMPO_LIMITE_LEITURA);

		conexao.setRequestProperty("Accept", "application/json");
		conexao.setRequestProperty("Accept-Encoding", "gzip");

		int codigoResposta = conexao.getResponseCode();

		InputStream fluxoBruto = (codigoResposta >= 200 && codigoResposta < 300) ? conexao.getInputStream() : conexao.getErrorStream();

		if (fluxoBruto == null) {
			throw new Exception("HTTP " + codigoResposta + " sem body");
		}

		InputStream fluxoDecodificado = "gzip".equalsIgnoreCase(conexao.getContentEncoding()) ? new GZIPInputStream(fluxoBruto) : fluxoBruto;

		String corpoResposta = new String(fluxoDecodificado.readAllBytes(), StandardCharsets.UTF_8).trim();

		conexao.disconnect();

		if (corpoResposta.startsWith("{")) {
			return new JSONObject(corpoResposta);
		}

		if (corpoResposta.startsWith("[")) {
			return new JSONArray(corpoResposta);
		}

		throw new Exception("JSON inválido");
	}
	// ==========================================================
	// CACHE DISCO
	// ==========================================================

	private static void carregarCacheDoDisco() {

		try {

			if (!Files.exists(ARQUIVO_CACHE))
				return;

			FileTime ultimaModificacao = Files.getLastModifiedTime(ARQUIVO_CACHE);

			if (Duration.between(ultimaModificacao.toInstant(), Instant.now()).toHours() > TEMPO_EXPIRACAO_CACHE) {
				return;
			}

			JSONObject json = new JSONObject(Files.readString(ARQUIVO_CACHE));

			json.keySet().forEach(k -> {

				JSONObject o = json.getJSONObject(k);

				DISCO_CACHE.put(k,
						new informacoesDoAtivo(o.getString("sigla"), o.optString("nome", ""), tipoDoAtivo.valueOf(o.optString("tipo", "DESCONHECIDO")),
								o.has("preco") ? o.getDouble("preco") : null));
			});

		} catch (Exception ignored) {
		}
	}

	private static void salvarCacheNoDisco() {

		try {

			JSONObject json = new JSONObject();

			DISCO_CACHE.forEach((key, ai) -> {

				JSONObject o = new JSONObject();

				o.put("ticker", ai.sigla);
				o.put("name", ai.nome);
				o.put("type", ai.tipo.name());

				if (ai.preco != null)
					o.put("price", ai.preco);

				json.put(key, o);
			});

			Files.writeString(ARQUIVO_CACHE, json.toString(2), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		} catch (Exception ignored) {
		}
	}

	// ==========================================================
	// SHUTDOWN
	// ==========================================================

	public static void shutdown() {
		EXECUTOR_DETALHES.shutdownNow();
	}
}