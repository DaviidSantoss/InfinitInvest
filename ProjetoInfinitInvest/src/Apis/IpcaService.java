package Apis;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.json.JSONObject;

/**
 * Serviço para obter o valor atual do IPCA (Índice de Preços ao Consumidor Amplo) de forma automática, com cache local e atualização automática a cada 24 horas.
 *
 * Fonte de dados: https://brasilapi.com.br/api/taxas/v1/ipca
 */
public class IpcaService {

	// Cache interno para evitar consultas repetidas
	private static Double ipcaCache = null;
	private static LocalDateTime ultimaAtualizacao = null;

	// URL da API oficial
	private static final String IPCA_URL = "https://brasilapi.com.br/api/taxas/v1/ipca";

	/**
	 * Obtém o IPCA atual em percentual (ex: 4.28). Se o cache estiver recente (menos de 24h), usa o valor em memória. Caso contrário, busca novamente da API.
	 *
	 * @return IPCA atual (% a.a.) ou -1 se falhar
	 */
	public static double getIpcaAtual() {
		try {
			// Verifica se temos cache válido
			if (ipcaCache != null && ultimaAtualizacao != null) {
				long horas = ChronoUnit.HOURS.between(ultimaAtualizacao, LocalDateTime.now());
				if (horas < 24) {
					return ipcaCache;
				}
			}


			@SuppressWarnings("deprecation")
			HttpURLConnection conn = (HttpURLConnection) new URL(IPCA_URL).openConnection();
			conn.setRequestMethod("GET");
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(5000);

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null)
				response.append(line);
			reader.close();

			JSONObject json = new JSONObject(response.toString());
			double valor = json.getDouble("valor");

			// Atualiza cache
			ipcaCache = valor;
			ultimaAtualizacao = LocalDateTime.now();

			return valor;

		} catch (Exception e) {

			return ipcaCache != null ? ipcaCache : -1; // fallback: usa cache se disponível
		}
	}
}
