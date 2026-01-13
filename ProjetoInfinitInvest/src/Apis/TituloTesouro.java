package Apis;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class TituloTesouro {

	private final String nome;
	private final String dataVencimento; // dd/MM/yyyy
	private final String taxaCompraManha; // ex: "7,88%"
	private final double puCompraManha; // PREÇO UNITÁRIO REAL

	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));

	public TituloTesouro(String nome, String dataVencimento, String taxaCompraManha, double puCompraManha) {
		this.nome = nome;
		this.dataVencimento = dataVencimento;
		this.taxaCompraManha = taxaCompraManha;
		this.puCompraManha = puCompraManha;
	}

	// =====================
	// GETTERS BÁSICOS
	// =====================

	public String getNome() {
		return nome;
	}

	public String getDataVencimento() {
		return dataVencimento;
	}

	public String getTaxaCompraManha() {
		return taxaCompraManha;
	}

	// =====================
	// PREÇO (PONTO-CHAVE)
	// =====================

	/** Preço unitário do Tesouro (PU Compra Manhã) */
	public double getPreco() {
		return puCompraManha;
	}

	// =====================
	// TAXA
	// =====================

	public double getTaxaAnual() {
		try {
			return Double.parseDouble(taxaCompraManha.replace("%", "").replace(",", ".")) / 100.0;
		} catch (Exception e) {
			return 0.0;
		}
	}

	// =====================
	// DERIVAÇÕES
	// =====================

	public int getAnoVencimento() {
		try {
			return Integer.parseInt(dataVencimento.split("/")[2]);
		} catch (Exception e) {
			return 0;
		}
	}

	public Date getDataAsDate() {
		try {
			return SDF.parse(dataVencimento);
		} catch (ParseException e) {
			return new Date(0);
		}
	}

	public String getIndexador() {
		String n = nome.toUpperCase();
		if (n.contains("IPCA"))
			return "IPCA+";
		if (n.contains("SELIC"))
			return "SELIC";
		if (n.contains("PREFIX"))
			return "PREFIXADO";
		return "OUTRO";
	}

	public String getNomeCanonico() {
		return String.format("%s %s a.a. %d", nome, taxaCompraManha, getAnoVencimento());
	}

	// =====================
	// OVERRIDES
	// =====================

	@Override
	public String toString() {
		return getNomeCanonico();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof TituloTesouro))
			return false;
		TituloTesouro t = (TituloTesouro) o;
		return nome.equalsIgnoreCase(t.nome) && getAnoVencimento() == t.getAnoVencimento();
	}

	@Override
	public int hashCode() {
		return Objects.hash(nome.toLowerCase(), getAnoVencimento());
	}
}
