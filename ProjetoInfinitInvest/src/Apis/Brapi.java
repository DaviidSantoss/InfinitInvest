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
	// Tokens, URLs base, timeouts e limites usados pelas APIs externas.

	private static final String EODHD_TOKEN = "Seu_Token";
	private static final String BASE = "https://eodhd.com/api";
	private static final String TOKEN = "Seu_Token";

	private static final int CONNECT_TIMEOUT = 8000;
	private static final int READ_TIMEOUT = 12000;
	private static final int MAX_CANDIDATES = 40;

	@SuppressWarnings("resource")
	private static final ExecutorService DETAIL_EXECUTOR = Executors.newFixedThreadPool(10);

	@SuppressWarnings("unused")
	private static final Cache<String, AssetInfo> CACHE = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1500).build();

	// ==========================================================
	// CACHE DE ATIVOS (DISCO)
	// ==========================================================
	// Guarda metadados de ativos (ticker, nome, tipo, preço) por 24h para acelerar autocomplete.

	private static final Path CACHE_FILE = Paths.get("cache_ativos.json");
	private static final long CACHE_EXPIRATION_HOURS = 24;

	private static final ConcurrentHashMap<String, AssetInfo> DISK_CACHE = new ConcurrentHashMap<>();

	// ==========================================================
	// CACHE DE DIVIDENDOS 12M (DISCO)
	// ==========================================================
	// Guarda o JSON completo dos últimos 12 meses por ticker para reduzir chamadas na EODHD.

	private static final Path DIV_CACHE_FILE = Paths.get("cache_dividendos_12m.json");
	private static final Duration DIV_TTL_OK = Duration.ofDays(30);
	private static final Duration DIV_TTL_EMPTY = Duration.ofDays(3); // se vier vazio, tenta novamente mais cedo

	private static final ConcurrentHashMap<String, DivEntry> DIV_DISK_CACHE = new ConcurrentHashMap<>();

	// ==========================================================
	// INIT (CARREGA CACHES)
	// ==========================================================
	// Carrega os caches de disco ao iniciar a classe.

	static {
		carregarCacheDoDisco();
		carregarCacheDividendosDoDisco();
	}

	// ==========================================================
	// TIPOS E MODELOS
	// ==========================================================
	// Estruturas básicas usadas pela API (tipo de ativo e informações do ativo).

	public enum AssetType {
		ACOES, FIIS, CRYPTO, ETF, TREASURY, UNKNOWN
	}

	public static class AssetInfo {
		public final String ticker;
		public final String name;
		public final AssetType type;
		public final Double price;

		public AssetInfo(String ticker, String name, AssetType type, Double price) {
			this.ticker = ticker;
			this.name = name;
			this.type = type;
			this.price = price;
		}

		public AssetInfo(String ticker, String name, AssetType type) {
			this(ticker, name, type, null);
		}

		@Override
		public String toString() {
			return ticker + (name != null && !name.isEmpty() ? " - " + name : "");
		}
	}

	// ==========================================================
	// BUSCA PRINCIPAL (AUTOCOMPLETE)
	// ==========================================================
	// Decide qual fonte usar (cripto/ETF/ações/FIIs/tesouro) conforme tipo selecionado.

	public static List<String> buscarAtivosPorTipo(AssetType tipo, String query) {

		if (tipo == null)
			tipo = AssetType.UNKNOWN;

		try {
			return switch (tipo) {
			case CRYPTO -> buscarCripto(query);
			case ETF -> buscarEtfs(query);
			case FIIS, ACOES, UNKNOWN -> buscarAcoesOuFiis(tipo, query);
			case TREASURY -> List.of(); // Tesouro não passa por esse fluxo
			};
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	// ==========================================================
	// PREÇO RÁPIDO
	// ==========================================================
	// Retorna apenas o preço atual usando a mesma base de dados do buscarAssetInfo.

	public static Double buscarAssetPrice(String ticker) {
		try {
			AssetInfo info = buscarAssetInfo(ticker);
			return info != null ? info.price : null;
		} catch (Exception e) {
			return null;
		}
	}

	// ==========================================================
	// BUSCAS ESPECÍFICAS (CRIPTO / ETF / TESOURO / AÇÕES-FIIS)
	// ==========================================================
	// Implementações de busca para cada classe de ativo.

	@SuppressWarnings("resource")
	private static List<String> buscarCripto(String query) {
		List<String> out = new ArrayList<>();
		try {
			String url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=brl&order=market_cap_desc&per_page=60&page=1";

			HttpClient cli = HttpClient.newHttpClient();
			HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).build();

			HttpResponse<String> res = cli.send(req, HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() != 200)
				return out;

			JSONArray arr = new JSONArray(res.body());
			String q = query.toLowerCase();

			for (int i = 0; i < arr.length(); i++) {
				JSONObject o = arr.getJSONObject(i);

				String name = o.getString("name");
				String sym = o.getString("symbol").toUpperCase();
				double price = o.getDouble("current_price");

				if (name.toLowerCase().contains(q) || sym.toLowerCase().contains(q)) {
					out.add(name + " (" + sym + ") - R$ " + price);
				}
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	@SuppressWarnings("deprecation")
	private static List<String> buscarEtfs(String query) {
		List<String> out = new ArrayList<>();
		try {
			URL url = new URL("https://brapi.dev/api/quote/list?stockType=fund&token=" + TOKEN);
			JSONObject json = fetchJson(url);

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

	@SuppressWarnings("deprecation")
	private static List<String> buscarAcoesOuFiis(AssetType tipo, String query) throws Exception {

		if (query == null || query.isBlank())
			return new ArrayList<>();

		String key = query.trim().toUpperCase();

		List<String> cacheResults = DISK_CACHE.values().stream().filter(ai -> ai.ticker.contains(key) || ai.name.toUpperCase().contains(key)).map(AssetInfo::toString).collect(Collectors.toList());

		if (!cacheResults.isEmpty())
			return cacheResults;

		String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
		URL url = new URL("https://brapi.dev/api/available?search=" + encoded + "&token=" + TOKEN);

		JSONObject json = fetchJson(url);
		JSONArray arr = json.optJSONArray("stocks");
		if (arr == null)
			return new ArrayList<>();

		List<String> out = new CopyOnWriteArrayList<>();
		List<CompletableFuture<Void>> futures = new ArrayList<>();

		int limit = Math.min(arr.length(), MAX_CANDIDATES);

		for (int i = 0; i < limit; i++) {
			String ticker = arr.getString(i);

			futures.add(CompletableFuture.supplyAsync(() -> buscarAssetInfo(ticker), DETAIL_EXECUTOR).thenAccept(info -> {
				if (info != null) {
					DISK_CACHE.put(info.ticker, info);
					out.add(info.toString());
				}
			}));
		}

		CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
		salvarCacheNoDisco();

		return out;
	}

	// ==========================================================
	// DIVIDENDOS / DY MENSAL (12M)
	// ==========================================================
	// Calcula DY mensal (%) usando histórico 12m da EODHD cacheado em disco por 30 dias.

	public static double buscarDividendYieldMensal12mPercent(String tickerB3, double precoAtual) {

		try {
			if (tickerB3 == null || tickerB3.isBlank() || precoAtual <= 0)
				return 0.0;
			if (EODHD_TOKEN == null || EODHD_TOKEN.isBlank())
				return 0.0;

			String t = tickerB3.trim().toUpperCase();
			LocalDate hoje = LocalDate.now();

			// 1) cache em disco (30 dias)
			JSONArray cachedDivs = getDividendos12mFromDiskIfFresh(t);
			if (cachedDivs != null) {
				double soma12m = somarDivsNoPeriodo(cachedDivs, hoje);
				if (soma12m <= 0)
					return 0.0;
				return ((soma12m / precoAtual) / 12.0) * 100.0;
			}

			// 2) resolve símbolo EODHD
			String symbol = toEodhdSymbol(t);

			// 3) busca dividendos (últimos 12 meses)
			LocalDate from = hoje.minusMonths(12);

			String urlStr = BASE + "/div/" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) + "?from=" + from + "&to=" + hoje + "&api_token="
					+ URLEncoder.encode(EODHD_TOKEN, StandardCharsets.UTF_8) + "&fmt=json";

			JSONArray arr = fetchJsonArray(new URL(urlStr));
			if (arr == null || arr.isEmpty()) {
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

			// 4) salva histórico completo em disco por 30 dias
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
		double soma12mPorAcao = 0.0;

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

			soma12mPorAcao += value;
		}

		return soma12mPorAcao;
	}

	// ==========================================================
	// CACHE DE DIVIDENDOS (DISCO) - LOAD / SAVE / GET / PUT
	// ==========================================================
	// Lê e grava o histórico 12m por ticker, aplicando TTL de 30 dias (ou 3 dias para vazio).

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
	// BUSCA INFO COMPLETA (AÇÃO/FII/ETF)
	// ==========================================================
	// Consulta brapi.dev para retornar nome, tipo e preço do ativo.

	@SuppressWarnings({ "deprecation", "unused" })
	public static AssetInfo buscarAssetInfo(String ticker) {
		try {
			String encoded = URLEncoder.encode(ticker, StandardCharsets.UTF_8);
			URL url = new URL("https://brapi.dev/api/quote/" + encoded + "?fundamental=true&token=" + TOKEN);

			JSONObject json = fetchJson(url);
			JSONArray res = json.optJSONArray("results");
			if (res == null || res.isEmpty())
				return null;

			JSONObject r = res.getJSONObject(0);

			String name = r.optString("longName", r.optString("shortName", ticker));
			double price = r.optDouble("regularMarketPrice", 0.0);

			AssetType type;
			boolean isEtf = r.optBoolean("isEtf", false);
			String typeField = r.optString("assetType", "").toLowerCase();

			if (isEtf || typeField.contains("etf"))
				type = AssetType.ETF;
			else if (ticker.endsWith("11"))
				type = AssetType.FIIS;
			else
				type = AssetType.ACOES;

			return new AssetInfo(ticker.toUpperCase(), name, type, price);

		} catch (Exception e) {
			return null;
		}
	}

	// ==========================================================
	// HTTP HELPERS (JSON / ARRAY / TEXT)
	// ==========================================================
	// Funções base de rede, incluindo suporte a gzip e respostas em array.

	private static JSONObject fetchJson(URL url) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setRequestProperty("Accept-Encoding", "gzip");
		conn.connect();

		InputStream input = "gzip".equals(conn.getContentEncoding()) ? new GZIPInputStream(conn.getInputStream()) : conn.getInputStream();

		String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		conn.disconnect();

		if (content.startsWith("{"))
			return new JSONObject(content);
		if (content.startsWith("["))
			return new JSONObject().put("data", new JSONArray(content));

		throw new Exception("JSON inválido");
	}

	private static JSONArray fetchJsonArray(URL url) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Accept-Encoding", "gzip");

		int code = conn.getResponseCode();

		InputStream raw = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
		if (raw == null)
			throw new Exception("HTTP " + code + " sem corpo de resposta");

		InputStream in = "gzip".equalsIgnoreCase(conn.getContentEncoding()) ? new GZIPInputStream(raw) : raw;

		String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		conn.disconnect();

		if (body.startsWith("["))
			return new JSONArray(body);

		if (body.startsWith("{")) {
			JSONObject obj = new JSONObject(body);
			JSONArray data = obj.optJSONArray("data");
			if (data != null)
				return data;
		}

		throw new Exception("JSON inválido (esperado array). Body: " + body);
	}

	@SuppressWarnings("unused")
	private static String fetchText(URL url) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setRequestProperty("Accept-Encoding", "gzip");
		conn.setRequestProperty("User-Agent", "InfinitInvest/1.0");
		conn.connect();

		int status = conn.getResponseCode();

		InputStream raw = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
		if (raw == null) {
			conn.disconnect();
			throw new Exception("HTTP " + status + " sem body");
		}

		InputStream input = "gzip".equalsIgnoreCase(conn.getContentEncoding()) ? new GZIPInputStream(raw) : raw;

		String content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		conn.disconnect();

		if (status < 200 || status >= 300)
			throw new Exception("HTTP " + status);

		return content;
	}

	// ==========================================================
	// CACHE DE ATIVOS (DISCO) - LOAD / SAVE
	// ==========================================================
	// Persiste o cache do autocomplete e evita consultas repetidas.

	private static void carregarCacheDoDisco() {
		try {
			if (!Files.exists(CACHE_FILE))
				return;

			FileTime lm = Files.getLastModifiedTime(CACHE_FILE);
			if (Duration.between(lm.toInstant(), Instant.now()).toHours() > CACHE_EXPIRATION_HOURS)
				return;

			JSONObject json = new JSONObject(Files.readString(CACHE_FILE));

			json.keySet().forEach(k -> {
				JSONObject o = json.getJSONObject(k);
				DISK_CACHE.put(k, new AssetInfo(o.getString("ticker"), o.optString("name", ""), AssetType.valueOf(o.optString("type", "UNKNOWN")), o.has("price") ? o.getDouble("price") : null));
			});

		} catch (Exception ignored) {
		}
	}

	private static void salvarCacheNoDisco() {
		try {
			JSONObject json = new JSONObject();
			DISK_CACHE.forEach((key, ai) -> {
				JSONObject o = new JSONObject();
				o.put("ticker", ai.ticker);
				o.put("name", ai.name);
				o.put("type", ai.type.name());
				if (ai.price != null)
					o.put("price", ai.price);
				json.put(key, o);
			});

			Files.writeString(CACHE_FILE, json.toString(2), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		} catch (Exception ignored) {
		}
	}

	// ==========================================================
	// UTILITÁRIOS (LEGADO / FUTURO)
	// ==========================================================
	// Helpers que você usava antes e pode manter para expansão.

	@SuppressWarnings("unused")
	private static double somarCashDividends12m(JSONObject first) {
		JSONObject dividendsData = first.optJSONObject("dividendsData");
		if (dividendsData == null)
			return 0.0;

		JSONArray cashDividends = dividendsData.optJSONArray("cashDividends");
		if (cashDividends == null || cashDividends.isEmpty())
			return 0.0;

		LocalDate hoje = LocalDate.now();
		LocalDate cutoff = hoje.minusMonths(12);

		double soma = 0.0;
		for (int i = 0; i < cashDividends.length(); i++) {
			JSONObject d = cashDividends.optJSONObject(i);
			if (d == null)
				continue;

			double rate = d.optDouble("rate", 0.0);
			if (rate <= 0)
				continue;

			String dateStr = d.optString("paymentDate", null);
			if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr))
				dateStr = d.optString("approvedOn", null);
			if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr))
				dateStr = d.optString("lastDatePrior", null);
			if (dateStr == null || dateStr.isBlank() || "null".equalsIgnoreCase(dateStr))
				continue;

			LocalDate data = parseToLocalDate(dateStr);
			if (data == null)
				continue;

			if (data.isBefore(cutoff) || data.isAfter(hoje.plusDays(1)))
				continue;

			soma += rate;
		}
		return soma;
	}

	private static LocalDate parseToLocalDate(String s) {
		try {
			return LocalDate.parse(s);
		} catch (Exception ignore) {
		}

		try {
			return java.time.OffsetDateTime.parse(s).toLocalDate();
		} catch (Exception ignore) {
		}

		try {
			return Instant.parse(s).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
		} catch (Exception ignore) {
		}

		return null;
	}

	@SuppressWarnings("unused")
	private static java.time.OffsetDateTime parseOffsetDateTimeSafe(String iso) {
		try {
			return java.time.OffsetDateTime.parse(iso);
		} catch (Exception ignored) {
			try {
				java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(iso);
				return ldt.atOffset(java.time.ZoneOffset.ofHours(-3));
			} catch (Exception ignored2) {
				return null;
			}
		}
	}

	// ==========================================================
	// SHUTDOWN
	// ==========================================================
	// Finaliza o executor de threads internas.

	public static void shutdown() {
		DETAIL_EXECUTOR.shutdownNow();
	}
}
