package Apis;

public final class LogoKit {

	private static final String TOKEN = "pk_fre0d0771b214e45db3dbb";

	private LogoKit() {
	}

	// Para logo por DOMÍNIO (apple.com, microsoft.com etc.)
	public static String byDomain(String domain) {
		return "https://img.logokit.com/" + domain + "?token=" + TOKEN + "&size=64";
	}

	// fallback 404 (útil pra você detectar e esconder imagem)
	public static String byDomain404(String domain) {
		return "https://img.logokit.com/" + domain + "?token=" + TOKEN + "&size=64&fallback=404";
	}

	// Se você usar logo por TICKER (seu caso de ações) — depende da Stock Logo API
	public static String byStockTicker(String ticker) {
		if (ticker == null || ticker.isBlank())
			return null;

		String t = ticker.trim().toUpperCase();

		// Heurística B3: 4 letras + 1 ou 2 dígitos
		if (t.matches("^[A-Z]{4}\\d{1,2}$")) {
			t = t + ".SA";
		}

		// fallback monogram evita imagem quebrada
		return "https://img.logokit.com/ticker/" + t + "?token=" + TOKEN + "&size=64&fallback=monogram";
	}


	public static String byCryptoSymbol(String symbol) {
		// Exemplo típico:
		return "https://img.logokit.com/crypto/" + symbol + "?token=" + TOKEN + "&size=64";
	}
}
