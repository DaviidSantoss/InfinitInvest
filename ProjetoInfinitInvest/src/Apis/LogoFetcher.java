package Apis;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javafx.scene.image.Image;

public final class LogoFetcher {

	// ==========================================================
	// CONTEXTO DO FETCHER
	// ==========================================================
	// Faz download de logos por URL e mantém cache em memória
	// para evitar rebaixar a mesma imagem repetidamente.

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(8);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	@SuppressWarnings("resource")
	private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(CONNECT_TIMEOUT).build();

	// ==========================================================
	// CACHE (MEMÓRIA)
	// ==========================================================
	// Cache simples com limite pra não crescer infinito.
	// Se estourar o limite, limpa tudo (simples e eficaz pro MVP).

	private static final int MAX_CACHE_SIZE = 500;
	private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

	private LogoFetcher() {
	}

	// ==========================================================
	// API PÚBLICA
	// ==========================================================
	// Carrega uma imagem por URL. Primeiro tenta cache. Se não tiver,
	// baixa, valida e armazena.

	public static Image loadImage(String url) {
		if (url == null || url.isBlank())
			return null;

		// cache hit
		Image cached = CACHE.get(url);
		if (cached != null)
			return cached;

		try {
			Image img = downloadImage(url);
			if (img == null)
				return null;

			putCache(url, img);
			return img;

		} catch (Exception e) {
			System.out.println("LogoFetcher erro -> " + url + " | " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		}
	}

	// ==========================================================
	// DOWNLOAD / VALIDAÇÃO
	// ==========================================================
	// Faz o request e converte bytes em Image. Se der erro, retorna null.

	private static Image downloadImage(String url) throws Exception {
		HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(REQUEST_TIMEOUT)
				.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36")
				.header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8").GET().build();

		HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());

		int code = resp.statusCode();
		if (code < 200 || code >= 300) {
			System.out.println("LogoFetcher HTTP " + code + " -> " + url);
			return null;
		}

		byte[] bytes = resp.body();
		if (bytes == null || bytes.length == 0)
			return null;

		Image img = new Image(new ByteArrayInputStream(bytes));
		if (img.isError())
			return null;

		return img;
	}

	// ==========================================================
	// HELPERS: CACHE PUT
	// ==========================================================
	// Controla limite simples de cache pra evitar crescimento infinito.

	private static void putCache(String url, Image img) {
		if (CACHE.size() >= MAX_CACHE_SIZE) {
			CACHE.clear();
		}
		CACHE.put(url, img);
	}
}
