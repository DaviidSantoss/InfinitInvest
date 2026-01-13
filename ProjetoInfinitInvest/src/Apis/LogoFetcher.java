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

	private static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(8)).build();

	// cache simples pra não ficar baixando o mesmo logo toda hora
	private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

	private LogoFetcher() {
	}

	public static Image loadImage(String url) {
		if (url == null || url.isBlank())
			return null;

		// cache hit
		Image cached = CACHE.get(url);
		if (cached != null)
			return cached;

		try {
			HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(10))
					.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36")
					.header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8").GET().build();

			HttpResponse<byte[]> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofByteArray());

			int code = resp.statusCode();
			if (code >= 200 && code < 300) {
				byte[] bytes = resp.body();
				Image img = new Image(new ByteArrayInputStream(bytes));
				CACHE.put(url, img);
				return img;
			}

			System.out.println("LogoFetcher HTTP " + code + " -> " + url);
			return null;

		} catch (Exception e) {
			System.out.println("LogoFetcher erro -> " + url + " | " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		}
	}
}
