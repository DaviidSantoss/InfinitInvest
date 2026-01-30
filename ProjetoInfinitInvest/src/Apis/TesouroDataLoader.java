package Apis;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TesouroDataLoader {

	// ==========================================================
	// CONTEXTO DO LOADER
	// ==========================================================
	// Lê um CSV local do Tesouro Direto, extrai títulos válidos,
	// remove duplicados (nome + ano) e retorna ordenado por vencimento.

	// COLOQUE O CAMINHO RESPECTIVO DO SEU COMPUTADOR
	private static final Path ARQUIVO_CSV = Paths.get("C:/Users/David/Documents/InfinitInvest/ProjetoInfinitInvest/src/Apis/precotaxatesourodireto.csv");

	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

	// ==========================================================
	// API PÚBLICA
	// ==========================================================
	// Retorna títulos com ano de vencimento >= anoMinimo, sem duplicatas e ordenados.

	public static List<TituloTesouro> buscarTitulos(int anoMinimo) {
		List<TituloTesouro> titulos = new ArrayList<>();

		if (!Files.exists(ARQUIVO_CSV)) {
			System.out.println("❌ CSV do Tesouro NÃO encontrado: " + ARQUIVO_CSV.toAbsolutePath());
			return titulos;
		}

		lerCsvETrazerTitulos(anoMinimo, titulos);

		List<TituloTesouro> resultado = removerDuplicados(titulos);
		resultado.sort(Comparator.comparing(TituloTesouro::getDataAsDate));
		return resultado;
	}

	// ==========================================================
	// LEITURA DO CSV
	// ==========================================================
	// Lê o arquivo, valida colunas mínimas e converte para TituloTesouro.

	private static void lerCsvETrazerTitulos(int anoMinimo, List<TituloTesouro> out) {
		try (BufferedReader br = Files.newBufferedReader(ARQUIVO_CSV)) {

			String linha;
			boolean primeiraLinha = true;

			while ((linha = br.readLine()) != null) {

				if (primeiraLinha) {
					primeiraLinha = false;
					continue;
				}

				linha = linha.replace("\"", "").trim();
				String[] c = linha.split(";");

				if (c.length < 8)
					continue;

				String nome = c[0].trim();
				String dataVenc = c[1].trim();
				String taxaCompra = c[3].trim();

				double puCompra = parsePuCompra(c[5]);
				if (puCompra <= 0)
					continue;

				Date data = parseData(dataVenc);
				if (data == null)
					continue;

				int ano = getAno(data);
				if (ano < anoMinimo)
					continue;

				out.add(new TituloTesouro(nome, dataVenc, taxaCompra, puCompra));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ==========================================================
	// DEDUPLICAÇÃO
	// ==========================================================
	// Remove títulos duplicados com base em (nome + ano de vencimento).

	private static List<TituloTesouro> removerDuplicados(List<TituloTesouro> titulos) {
		Map<String, TituloTesouro> mapa = new HashMap<>();

		for (TituloTesouro t : titulos) {
			String chave = t.getNome().toLowerCase() + "_" + t.getAnoVencimento();
			mapa.putIfAbsent(chave, t);
		}

		return new ArrayList<>(mapa.values());
	}

	// ==========================================================
	// HELPERS: PARSE
	// ==========================================================
	// Conversões e validações de campos do CSV.

	private static double parsePuCompra(String raw) {
		try {
			if (raw == null)
				return -1;
			String s = raw.replace("R$", "").replace(".", "").replace(",", ".").trim();
			return Double.parseDouble(s);
		} catch (Exception e) {
			return -1;
		}
	}

	private static Date parseData(String raw) {
		try {
			return SDF.parse(raw);
		} catch (Exception e) {
			return null;
		}
	}

	private static int getAno(Date data) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(data);
		return cal.get(Calendar.YEAR);
	}
}
