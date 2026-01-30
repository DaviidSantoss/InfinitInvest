package Apis;

public final class LogoKit {

	// ==========================================================
	// CONTEXTO DO UTILITÁRIO
	// ==========================================================
	// Gera URLs do LogoKit para domínio, ticker (ações/FIIs) e cripto,
	// aplicando pequenas heurísticas para compatibilidade com B3.

	private static final String TOKEN = "Seu_Token";
	private static final int DEFAULT_SIZE = 64;

	private LogoKit() {
	}

	// ==========================================================
	// LOGO POR DOMÍNIO
	// ==========================================================
	// Retorna URL de logo baseado no domínio (ex: apple.com).

	public static String byDomain(String domain) {
		if (domain == null || domain.isBlank())
			return null;
		String d = domain.trim().toLowerCase();
		return "https://img.logokit.com/" + d + "?token=" + TOKEN + "&size=" + DEFAULT_SIZE;
	}

	public static String byDomain404(String domain) {
		if (domain == null || domain.isBlank())
			return null;
		String d = domain.trim().toLowerCase();
		return "https://img.logokit.com/" + d + "?token=" + TOKEN + "&size=" + DEFAULT_SIZE + "&fallback=404";
	}

	// ==========================================================
	// LOGO POR TICKER (AÇÕES/FIIs)
	// ==========================================================
	// Retorna URL de logo baseado em ticker. Aplica heurística B3 (.SA).

	public static String byStockTicker(String ticker) {
		if (ticker == null || ticker.isBlank())
			return null;

		String t = normalizeB3Ticker(ticker);
		return "https://img.logokit.com/ticker/" + t + "?token=" + TOKEN + "&size=" + DEFAULT_SIZE + "&fallback=monogram";
	}

	private static String normalizeB3Ticker(String ticker) {
		String t = ticker.trim().toUpperCase();
		if (t.matches("^[A-Z]{4}\\d{1,2}$")) {
			t = t + ".SA";
		}
		return t;
	}

	// ==========================================================
	// LOGO POR CRIPTO
	// ==========================================================
	// Retorna URL de logo baseado em símbolo de cripto (ex: BTC).

	public static String byCryptoSymbol(String symbol) {
		if (symbol == null || symbol.isBlank())
			return null;
		String s = symbol.trim().toUpperCase();
		return "https://img.logokit.com/crypto/" + s + "?token=" + TOKEN + "&size=" + DEFAULT_SIZE;
	}
}
