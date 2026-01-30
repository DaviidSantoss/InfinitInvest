package Apis;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class TituloTesouro {

	// ==========================================================
	// CONTEXTO DO MODELO
	// ==========================================================
	// Representa um título do Tesouro Direto carregado do CSV,
	// com dados principais (nome, vencimento, taxa e PU) e derivadas
	// úteis para busca/ordenação/exibição.

	private final String nome;
	private final String dataVencimento; // dd/MM/yyyy
	private final String taxaCompraManha; // ex: "7,88%"
	private final double puCompraManha; // preço unitário (PU compra manhã)

	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));

	public TituloTesouro(String nome, String dataVencimento, String taxaCompraManha, double puCompraManha) {
		this.nome = nome;
		this.dataVencimento = dataVencimento;
		this.taxaCompraManha = taxaCompraManha;
		this.puCompraManha = puCompraManha;
	}

	// ==========================================================
	// GETTERS BÁSICOS
	// ==========================================================
	// Acesso direto aos campos principais do título.

	public String getNome() {
		return nome;
	}

	public String getDataVencimento() {
		return dataVencimento;
	}

	public String getTaxaCompraManha() {
		return taxaCompraManha;
	}

	// ==========================================================
	// PREÇO
	// ==========================================================
	// PU compra manhã (preço unitário do título).

	public double getPreco() {
		return puCompraManha;
	}

	// ==========================================================
	// TAXA / CONVERSÕES
	// ==========================================================
	// Converte taxa textual para decimal anual (ex: "7,88%" -> 0.0788).

	public double getTaxaAnual() {
		try {
			if (taxaCompraManha == null || taxaCompraManha.isBlank())
				return 0.0;
			return Double.parseDouble(taxaCompraManha.replace("%", "").replace(",", ".").trim()) / 100.0;
		} catch (Exception e) {
			return 0.0;
		}
	}

	// ==========================================================
	// DERIVAÇÕES
	// ==========================================================
	// Campos calculados para filtro, ordenação e exibição.

	public int getAnoVencimento() {
		try {
			if (dataVencimento == null)
				return 0;
			String[] parts = dataVencimento.split("/");
			if (parts.length < 3)
				return 0;
			return Integer.parseInt(parts[2]);
		} catch (Exception e) {
			return 0;
		}
	}

	public Date getDataAsDate() {
		try {
			if (dataVencimento == null || dataVencimento.isBlank())
				return new Date(0);
			return SDF.parse(dataVencimento);
		} catch (ParseException e) {
			return new Date(0);
		}
	}

	public String getIndexador() {
		String n = (nome == null) ? "" : nome.toUpperCase();
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

	// ==========================================================
	// OVERRIDES
	// ==========================================================
	// toString para UI, e equals/hashCode para deduplicação.

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

		String n1 = (this.nome == null) ? "" : this.nome;
		String n2 = (t.nome == null) ? "" : t.nome;

		return n1.equalsIgnoreCase(n2) && getAnoVencimento() == t.getAnoVencimento();
	}

	@Override
	public int hashCode() {
		String n = (nome == null) ? "" : nome.toLowerCase();
		return Objects.hash(n, getAnoVencimento());
	}
}
