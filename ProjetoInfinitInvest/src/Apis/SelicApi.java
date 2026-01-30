package Apis;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class SelicApi {

	// ==========================================================
	// CONTEXTO DO SERVICE
	// ==========================================================
	// Consulta a taxa SELIC diária (SGS 11) no Bacen, pegando apenas
	// o último valor para reduzir payload e evitar retornos grandes.

	private static final String URL_ULTIMO = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.11/dados/ultimos/1?formato=json";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

	@SuppressWarnings("resource")
	private final HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();

	private final Gson gson = new Gson();

	// ==========================================================
	// DTO
	// ==========================================================
	// Modelo do retorno do Bacen (lista com 1 item).

	public static class PontoSelic {
		String data; // "dd/MM/yyyy"
		String valor; // "0.0456" ou "0,0456"
	}

	// ==========================================================
	// API PÚBLICA
	// ==========================================================
	// Retorna a última SELIC diária (SGS 11) como double.

	public double getUltimaTaxaSelic() throws Exception {
		HttpRequest req = HttpRequest.newBuilder().uri(URI.create(URL_ULTIMO)).timeout(REQUEST_TIMEOUT).header("Accept", "application/json").header("User-Agent", "Java-HttpClient").GET()
				.build();

		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

		if (resp.statusCode() != 200) {
			throw new RuntimeException("Erro API Selic: HTTP " + resp.statusCode() + " | body: " + resp.body());
		}

		Type listType = new TypeToken<List<PontoSelic>>() {
		}.getType();
		List<PontoSelic> lista = gson.fromJson(resp.body(), listType);

		if (lista == null || lista.isEmpty() || lista.get(0) == null || lista.get(0).valor == null) {
			throw new RuntimeException("API Selic retornou vazio.");
		}

		String valorStr = lista.get(0).valor.replace(",", ".").trim();
		return Double.parseDouble(valorStr);
	}

	// ==========================================================
	// TESTE LOCAL
	// ==========================================================
	// Executa uma consulta simples para validar rapidamente o serviço.

	public static void main(String[] args) {
		try {
			SelicApi api = new SelicApi();
			double selicDia = api.getUltimaTaxaSelic();
			System.out.println("Selic diária (SGS 11) última: " + selicDia);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
