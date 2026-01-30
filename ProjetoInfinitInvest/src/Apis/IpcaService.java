package Apis;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import org.json.JSONObject;

/**
 * Serviço para obter o valor atual do IPCA (Índice de Preços ao Consumidor Amplo) com cache em memória e atualização automática (TTL de 24 horas).
 *
 * Fonte: https://brasilapi.com.br/api/taxas/v1/ipca
 */
public class IpcaService {

	// ==========================================================
	// CONTEXTO DO SERVICE
	// ==========================================================
	// Busca o IPCA atual via BrasilAPI e mantém um cache em memória
	// para evitar chamadas repetidas (atualiza a cada 24h).

	private static final String IPCA_URL = "https://brasilapi.com.br/api/taxas/v1/ipca";
	private static final Duration TTL = Duration.ofHours(24);

	private static Double ipcaCache = null;
	private static Instant lastFetch = null;

	// ==========================================================
	// API PÚBLICA
	// ==========================================================
	// Retorna o IPCA atual (%). Usa cache se estiver dentro do TTL.
	// Se falhar, devolve cache antigo (se existir) ou -1.

	public static synchronized double getIpcaAtual() {
		try {
			Double cached = getIfFresh();
			if (cached != null)
				return cached;

			double valor = fetchIpcaFromApi();

			putCache(valor);
			return valor;

		} catch (Exception e) {
			return ipcaCache != null ? ipcaCache : -1;
		}
	}

	// ==========================================================
	// CACHE (MEMÓRIA)
	// ==========================================================
	// Controla validade do cache e atualização do valor.

	private static Double getIfFresh() {
		if (ipcaCache == null || lastFetch == null)
			return null;

		Duration age = Duration.between(lastFetch, Instant.now());
		if (age.compareTo(TTL) < 0)
			return ipcaCache;

		return null;
	}

	private static void putCache(double valor) {
		ipcaCache = valor;
		lastFetch = Instant.now();
	}

	// ==========================================================
	// HTTP / PARSE
	// ==========================================================
	// Faz a chamada na BrasilAPI e extrai o campo "valor".

	private static double fetchIpcaFromApi() throws Exception {
		HttpURLConnection conn = null;

		try {
			conn = (HttpURLConnection) new URL(IPCA_URL).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);
			conn.setRequestProperty("Accept", "application/json");

			int status = conn.getResponseCode();
			InputStream in = (status >= 200 && status < 300) ? conn.getInputStream() : conn.getErrorStream();
			if (in == null)
				throw new Exception("HTTP " + status + " sem corpo");

			String body = readAll(in);

			if (status < 200 || status >= 300) {
				throw new Exception("HTTP " + status + " Body: " + body);
			}

			JSONObject json = new JSONObject(body);
			return json.getDouble("valor");

		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	private static String readAll(InputStream in) throws Exception {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				sb.append(line);
			return sb.toString();
		}
	}
}
