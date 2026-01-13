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

	private static final Path ARQUIVO_CSV = Paths.get("C:/Users/David/Documents/InfinitInvest/ProjetoInfinitInvest/src/Apis/precotaxatesourodireto.csv");

	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy");

	public static List<TituloTesouro> buscarTitulos(int anoMinimo) {

		List<TituloTesouro> titulos = new ArrayList<>();

		if (!Files.exists(ARQUIVO_CSV)) {
			System.out.println("❌ CSV do Tesouro NÃO encontrado: " + ARQUIVO_CSV.toAbsolutePath());
			return titulos;
		}

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

				// ✅ PU COMPRA MANHÃ (preço unitário)
				double puCompra;
				try {
					puCompra = Double.parseDouble(c[5].replace("R$", "").replace(".", "").replace(",", ".").trim());
				} catch (Exception e) {
					continue; // ignora linha inválida
				}

				Date data;
				try {
					data = SDF.parse(dataVenc);
				} catch (Exception e) {
					continue;
				}

				Calendar cal = Calendar.getInstance();
				cal.setTime(data);

				if (cal.get(Calendar.YEAR) < anoMinimo)
					continue;

				titulos.add(new TituloTesouro(nome, dataVenc, taxaCompra, puCompra));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		// 🔹 Remove duplicados (nome + ano)
		Map<String, TituloTesouro> mapa = new HashMap<>();

		for (TituloTesouro t : titulos) {
			String chave = t.getNome().toLowerCase() + "_" + t.getAnoVencimento();
			mapa.putIfAbsent(chave, t);
        }

		List<TituloTesouro> resultado = new ArrayList<>(mapa.values());
		resultado.sort(Comparator.comparing(TituloTesouro::getDataAsDate));

		return resultado;
    }
}
