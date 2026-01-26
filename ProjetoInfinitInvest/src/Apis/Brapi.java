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

	private static final String EODHD_TOKEN = System.getenv("EODHD_TOKEN");
	private static final String BASE = "https://eodhd.com/api";
	private static final String TOKEN = "usX4AiGRdvqLmqXjMjD2Fx";
	private static final int CONNECT_TIMEOUT = 3000;
	private static final int READ_TIMEOUT = 3000;
	private static final int MAX_CANDIDATES = 40;

	@SuppressWarnings("resource")
	private static final ExecutorService DETAIL_EXECUTOR = Executors.newFixedThreadPool(10);

	@SuppressWarnings("unused")
	private static final Cache<String, AssetInfo> CACHE = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(1500).build();

	private static final Path CACHE_FILE = Paths.get("cache_ativos.json");
	private static final long CACHE_EXPIRATION_HOURS = 24;

	// Cache em disco removido de tudo que se refere a logos
	private static final ConcurrentHashMap<String, AssetInfo> DISK_CACHE = new ConcurrentHashMap<>();

	static {
		carregarCacheDoDisco();
	}

	// =============================================
	// Tipo de Ativo
	// =============================================
	public enum AssetType {
		ACOES, FIIS, CRYPTO, ETF, TREASURY, UNKNOWN
	}

	// =============================================
	// Dados do Ativo (SEM LOGO)
	// =============================================
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

	// =============================================
	// MÉTODO PRINCIPAL DE BUSCA
	// =============================================

	public static List<String> buscarAtivosPorTipo(AssetType tipo, String query) {

		if (tipo == null)
			tipo = AssetType.UNKNOWN;

		try {
			return switch (tipo) {
			case CRYPTO -> buscarCripto(query);
			case ETF -> buscarEtfs(query);
			case FIIS, ACOES, UNKNOWN -> buscarAcoesOuFiis(tipo, query);

			// ❌ Tesouro NÃO entra aqui
			case TREASURY -> List.of();
			};
		} catch (Exception e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}


	// =============================================
	// API: Buscar Preço Rápido
	// =============================================
	public static Double buscarAssetPrice(String ticker) {
		try {
			AssetInfo info = buscarAssetInfo(ticker);
			return info != null ? info.price : null;
		} catch (Exception e) {
			return null;
		}
	}

	// =============================================
	// Buscar Cripto
	// =============================================


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

				if (name.toLowerCase().contains(q) || sym.toLowerCase().contains(q))
					out.add(name + " (" + sym + ") - R$ " + price);
			}

		} catch (Exception ignored) {
		}
		return out;
	}

	// =============================================
	// Buscar ETFs
	// =============================================
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

				if (stock.toLowerCase().contains(q) || name.toLowerCase().contains(q))
					out.add(stock + " - " + name);
			}

		} catch (Exception ignored) {
		}
		return out;
	}


	// =============================================
	// Buscar Tesouro Direto
	// =============================================
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



	// =============================================
	// Buscar Ações / FIIs
	// =============================================
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

	// ===============================
	// DIVIDENDOS / RENDA (12M)
	// ===============================
	public static double buscarDividendYieldMensal12mPercent(String tickerB3, double precoAtual) {

		try {
			if (tickerB3 == null || tickerB3.isBlank() || precoAtual <= 0)
				return 0.0;
			if (EODHD_TOKEN == null || EODHD_TOKEN.isBlank())
				return 0.0;

			String t = tickerB3.trim().toUpperCase();

			// 1) cache 24h
			Double cached = DividendCache.getIfFresh(t);
			if (cached != null)
				return cached;

			// 2) resolve símbolo EODHD (ex: PETR4.SA) — aqui vou usar heurística
			// Se você quiser "100% correto", abaixo eu te deixo uma versão com Search API.
			String symbol = toEodhdSymbol(t);

			// 3) busca dividendos (últimos 12 meses)
			LocalDate hoje = LocalDate.now();
			LocalDate from = hoje.minusMonths(12);

			String urlStr = BASE + "/div/" + URLEncoder.encode(symbol, StandardCharsets.UTF_8) + "?from=" + from + "&to=" + hoje + "&api_token="
					+ URLEncoder.encode(EODHD_TOKEN, StandardCharsets.UTF_8) + "&fmt=json";

			@SuppressWarnings("unused")
			JSONObject dummy = null;
			// sua fetchJson atual parece devolver JSONObject; aqui o endpoint retorna ARRAY.
			// Então você precisa de um fetchJsonArray (mostro abaixo).
			@SuppressWarnings("deprecation")
			JSONArray arr = fetchJsonArray(new URL(urlStr));
			if (arr == null || arr.isEmpty()) {
				DividendCache.put(t, 0.0, false);
				return 0.0;
			}


			double soma12mPorAcao = 0.0;

			for (int i = 0; i < arr.length(); i++) {
				JSONObject d = arr.optJSONObject(i);
				if (d == null)
					continue;

				// docs: "date" (ex-dividend date) e "value" :contentReference[oaicite:2]{index=2}
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

			if (soma12mPorAcao <= 0) {
				DividendCache.put(t, 0.0, false);
				return 0.0;
			}

			double yieldMensalPercent = ((soma12mPorAcao / precoAtual) / 12.0) * 100.0;
			yieldMensalPercent = Math.max(0.0, yieldMensalPercent);

			DividendCache.put(t, yieldMensalPercent, true);
			return yieldMensalPercent;

		} catch (Exception e) {
			return 0.0;
		}
	}

	// Heurística simples para B3.
	// Se o seu EODHD usar outro exchange id, você troca aqui.
	private static String toEodhdSymbol(String b3Ticker) {
		// Exemplo: "PETR4" -> "PETR4.SA"
		// O formato {SYMBOL}.{EXCHANGE_ID} é o padrão. :contentReference[oaicite:3]{index=3}
		return b3Ticker + ".SA";
	}

	// ========= você precisa disso pois /api/div retorna JSON ARRAY =========
	private static JSONArray fetchJsonArray(URL url) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Accept-Encoding", "gzip");

		int code = conn.getResponseCode();

		InputStream raw = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
		if (raw == null) {
			throw new Exception("HTTP " + code + " sem corpo de resposta");
		}

		InputStream in = "gzip".equalsIgnoreCase(conn.getContentEncoding()) ? new GZIPInputStream(raw) : raw;

		String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		conn.disconnect();


		// o endpoint /api/div retorna um JSON ARRAY
		if (body.startsWith("["))
			return new JSONArray(body);

		// às vezes pode vir algo inesperado
		if (body.startsWith("{")) {
			JSONObject obj = new JSONObject(body);
			// se por acaso vier {data:[...]} tenta extrair
			JSONArray data = obj.optJSONArray("data");
			if (data != null)
				return data;
		}

		throw new Exception("JSON inválido (esperado array). Body: " + body);
	}



	@SuppressWarnings("unused")
	private static double somarCashDividends12m(JSONObject first) {
		JSONObject dividendsData = first.optJSONObject("dividendsData");
		if (dividendsData == null)
			return 0.0;

		JSONArray cashDividends = dividendsData.optJSONArray("cashDividends");
		if (cashDividends == null || cashDividends.isEmpty())
			return 0.0;

		java.time.LocalDate hoje = java.time.LocalDate.now();
		java.time.LocalDate cutoff = hoje.minusMonths(12);

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

			java.time.LocalDate data = parseToLocalDate(dateStr);
			if (data == null)
				continue;

			if (data.isBefore(cutoff) || data.isAfter(hoje.plusDays(1)))
				continue;

			soma += rate;
		}
		return soma;
	}


	// Aceita "YYYY-MM-DD" e "YYYY-MM-DDTHH:mm:ss..." (ISO 8601)
	private static java.time.LocalDate parseToLocalDate(String s) {
		try {
			// tenta date puro
			return java.time.LocalDate.parse(s);
		} catch (Exception ignore) {
		}

		try {
			// tenta date-time com offset/Z
			return java.time.OffsetDateTime.parse(s).toLocalDate();
		} catch (Exception ignore) {
		}

		try {
			// fallback: instant
			return java.time.Instant.parse(s).atZone(java.time.ZoneId.systemDefault()).toLocalDate();
		} catch (Exception ignore) {
		}

		return null;
	}

	public static class DividendCache {
	    private static final Duration TTL_OK = Duration.ofHours(24);
	    private static final Duration TTL_EMPTY = Duration.ofHours(6); // “negative cache” pra dados ausentes

	    private static final ConcurrentHashMap<String, Entry> CACHE = new ConcurrentHashMap<>();

	    public static Double getIfFresh(String ticker) {
	        if (ticker == null) return null;
	        Entry e = CACHE.get(ticker);
	        if (e == null) return null;

	        Duration ttl = e.hadData ? TTL_OK : TTL_EMPTY;
	        if (Duration.between(e.fetchedAt, Instant.now()).compareTo(ttl) < 0) {
	            return e.value;
	        }
	        return null;
	    }

	    public static void put(String ticker, double value, boolean hadData) {
	        if (ticker == null) return;
	        CACHE.put(ticker, new Entry(value, Instant.now(), hadData));
	    }

	    private static class Entry {
	        final double value;       // dyMensalPercent
	        final Instant fetchedAt;
	        final boolean hadData;    // true se veio histórico/dividendos; false se veio vazio

	        Entry(double value, Instant fetchedAt, boolean hadData) {
	            this.value = value;
	            this.fetchedAt = fetchedAt;
	            this.hadData = hadData;
	        }
	    }
	}


	@SuppressWarnings("unused")
	private static java.time.OffsetDateTime parseOffsetDateTimeSafe(String iso) {
		try {
			// geralmente vem ISO 8601 completo
			return java.time.OffsetDateTime.parse(iso);
		} catch (Exception ignored) {
			try {
				// se vier sem offset, tenta como LocalDateTime e assume -03:00
				java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(iso);
				return ldt.atOffset(java.time.ZoneOffset.ofHours(-3));
			} catch (Exception ignored2) {
				return null;
			}
		}
	}

	// =============================================
	// Buscar informações completas (SEM LOGO)
	// =============================================
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
			String lower = name.toLowerCase();
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

	// =============================================
	// HTTP JSON
	// =============================================
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

	// =============================================
	// HTTP TEXT (para endpoints que retornam ARRAY direto)
	// =============================================
	@SuppressWarnings("unused")
	private static String fetchText(URL url) throws Exception {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setRequestProperty("Accept-Encoding", "gzip");
		conn.setRequestProperty("User-Agent", "InfinitInvest/1.0"); // ajuda alguns endpoints
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

		if (status < 200 || status >= 300) {

			throw new Exception("HTTP " + status);
		}

		return content;
	}
	
	

	// =============================================
	// CACHE: carregar / salvar
	// =============================================
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

	public static void shutdown() {
		DETAIL_EXECUTOR.shutdownNow();
	}
}