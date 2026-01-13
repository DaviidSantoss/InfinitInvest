package BancoInfinit;

public class Ativo {

	private int id;
	private int usuarioId;
	private String categoria;
	private String ativo;
	private String iconUrl;

	private double quantidade;
	private double precoMedio;
	private double precoAtual;
	private double variacao;
	private double saldo;

	// Renda fixa (como você já fez)
	@SuppressWarnings("unused")
	private double rentabilidade;
	@SuppressWarnings("unused")
	private boolean isRendaFixa;

	// ===== Tesouro Direto (NOVO) =====
	private String tdIndexador; // "SELIC", "PREFIXADO", "IPCA+", "RENDA+", "EDUCA+"
	private double tdTaxaAnual; // ex: 0.1188
	private double tdPrincipal; // base
	private String tdUltimoDia; // "2026-01-08"

	// Construtor antigo continua existindo (pra não quebrar)
	public Ativo(int id, int usuarioId, String categoria, String ativo, String iconUrl, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo) {

		this.id = id;
		this.usuarioId = usuarioId;
		this.categoria = categoria;
		this.ativo = ativo;
		this.iconUrl = iconUrl;

		this.quantidade = quantidade;
		this.precoMedio = precoMedio;
		this.precoAtual = precoAtual;
		this.variacao = variacao;
		this.saldo = saldo;

		if ("Renda Fixa".equalsIgnoreCase(categoria)) {
			this.isRendaFixa = true;
			this.rentabilidade = quantidade;
		}
	}

	// Construtor NOVO (com campos do Tesouro)
	public Ativo(int id, int usuarioId, String categoria, String ativo, String iconUrl, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo, String tdIndexador,
			double tdTaxaAnual, double tdPrincipal, String tdUltimoDia) {

		this(id, usuarioId, categoria, ativo, iconUrl, quantidade, precoMedio, precoAtual, variacao, saldo);

		this.tdIndexador = tdIndexador;
		this.tdTaxaAnual = tdTaxaAnual;
		this.tdPrincipal = tdPrincipal;
		this.tdUltimoDia = tdUltimoDia;
	}

	// ===== getters existentes =====
	public int getId() {
		return id;
	}

	public int getUsuarioId() {
		return usuarioId;
	}

	public String getCategoria() {
		return categoria;
	}

	public String getAtivo() {
		return ativo;
	}

	public String getIconUrl() {
		return iconUrl;
	}

	public double getQuantidade() {
		return quantidade;
	}

	public double getPrecoMedio() {
		return precoMedio;
	}

	public double getPrecoAtual() {
		return precoAtual;
	}

	public double getVariacao() {
		return variacao;
	}

	public double getSaldo() {
		return saldo;
	}


	public String getTdIndexador() {
		return tdIndexador;
	}

	public double getTdTaxaAnual() {
		return tdTaxaAnual;
	}

	public double getTdPrincipal() {
		return tdPrincipal;
	}

	public String getTdUltimoDia() {
		return tdUltimoDia;
	}

	// Se quiser setar depois (útil)
	public void setTdIndexador(String v) {
		this.tdIndexador = v;
	}

	public void setTdTaxaAnual(double v) {
		this.tdTaxaAnual = v;
	}

	public void setTdPrincipal(double v) {
		this.tdPrincipal = v;
	}

	public void setTdUltimoDia(String v) {
		this.tdUltimoDia = v;
	}
}
