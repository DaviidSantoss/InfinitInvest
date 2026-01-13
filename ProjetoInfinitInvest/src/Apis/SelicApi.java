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

	// Pegando só o último valor para evitar retorno grande / regra pós-26/03/2025
	private static final String URL_ULTIMO = "https://api.bcb.gov.br/dados/serie/bcdata.sgs.11/dados/ultimos/1?formato=json";

	private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

	private final Gson gson = new Gson();

	// DTO do Bacen
	public static class PontoSelic {
		String data; // "dd/MM/yyyy"
		String valor; // "0.0456" ou "0,0456" (por via das dúvidas)
	}

	public double getUltimaTaxaSelic() throws Exception {
		HttpRequest req = HttpRequest.newBuilder().uri(URI.create(URL_ULTIMO)).timeout(Duration.ofSeconds(20)).header("Accept", "application/json").header("User-Agent", "Java-HttpClient").GET()
				.build();

		HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

		if (resp.statusCode() != 200) {
			throw new RuntimeException("Erro API Selic: HTTP " + resp.statusCode() + " | body: " + resp.body());
		}

		Type listType = new TypeToken<List<PontoSelic>>() {
		}.getType();
		List<PontoSelic> lista = gson.fromJson(resp.body(), listType);

		if (lista == null || lista.isEmpty()) {
			throw new RuntimeException("API Selic retornou vazio.");
		}

		String valorStr = lista.get(0).valor.replace(",", ".");
		return Double.parseDouble(valorStr);
	}

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
