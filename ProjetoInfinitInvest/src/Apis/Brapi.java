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

	private static final String TOKEN = "usX4AiGRdvqLmqXjMjD2Fx";
	private static final int CONNECT_TIMEOUT = 3000;
	private static final int READ_TIMEOUT = 3000;
	private static final int MAX_CANDIDATES = 40;

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