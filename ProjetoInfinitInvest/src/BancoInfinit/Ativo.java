package BancoInfinit;

public class Ativo {

	// ==========================================================
	// CONTEXTO DO MODELO
	// ==========================================================
	// Representa um ativo da carteira (ações/FIIs/ETFs/cripto) e também
	// guarda metadados para Tesouro Direto e Renda Fixa (para cálculo de rendimento).
	// Mantém construtores retrocompatíveis para não quebrar o DAO atual.

	// ==========================================================
	// CAMPOS PRINCIPAIS (CARTEIRA)
	// ==========================================================
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

	// ==========================================================
	// CAMPOS LEGADOS (RENDA FIXA - UI ANTIGA)
	// ==========================================================
	@SuppressWarnings("unused")
	private double rentabilidade;

	@SuppressWarnings("unused")
	private boolean isRendaFixa;

	// ==========================================================
	// TESOURO DIRETO (META / ATUALIZAÇÃO)
	// ==========================================================
	private String tdIndexador;
	private double tdTaxaAnual;
	private double tdPrincipal;
	private String tdUltimoDia;

	// ==========================================================
	// RENDA FIXA (META / ATUALIZAÇÃO)
	// ==========================================================
	private String rfIndexador;
	private String rfForma;
	private double rfTaxaAnual;
	private double rfPrincipal;
	private String rfUltimoDia;

	// Opcionais (para CDI+ “bem feito”)
	private double rfPercentIndexador; // ex: 1.00 (100% CDI)
	private double rfSpreadAnual; // ex: 0.02 (2% a.a.)

	// ==========================================================
	// CONSTRUTORES
	// ==========================================================

	// Construtor base (legado) — mantém compatibilidade com DAO antigo.
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

		// comportamento legado: para “Renda Fixa”, quantidade vinha como rentabilidade (%)
		if ("Renda Fixa".equalsIgnoreCase(categoria)) {
			this.isRendaFixa = true;
			this.rentabilidade = quantidade;
		}
	}

	// Construtor com Tesouro Direto.
	public Ativo(int id, int usuarioId, String categoria, String ativo, String iconUrl, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo, String tdIndexador,
			double tdTaxaAnual, double tdPrincipal, String tdUltimoDia) {
		this(id, usuarioId, categoria, ativo, iconUrl, quantidade, precoMedio, precoAtual, variacao, saldo);
		this.tdIndexador = tdIndexador;
		this.tdTaxaAnual = tdTaxaAnual;
		this.tdPrincipal = tdPrincipal;
		this.tdUltimoDia = tdUltimoDia;
	}

	// Construtor com Tesouro Direto + Renda Fixa (meta).
	public Ativo(int id, int usuarioId, String categoria, String ativo, String iconUrl, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo, String tdIndexador,
			double tdTaxaAnual, double tdPrincipal, String tdUltimoDia, String rfIndexador, String rfForma, double rfTaxaAnual, double rfPrincipal, String rfUltimoDia, double rfPercentIndexador,
			double rfSpreadAnual) {
		this(id, usuarioId, categoria, ativo, iconUrl, quantidade, precoMedio, precoAtual, variacao, saldo);

		this.tdIndexador = tdIndexador;
		this.tdTaxaAnual = tdTaxaAnual;
		this.tdPrincipal = tdPrincipal;
		this.tdUltimoDia = tdUltimoDia;

		this.rfIndexador = rfIndexador;
		this.rfForma = rfForma;
		this.rfTaxaAnual = rfTaxaAnual;
		this.rfPrincipal = rfPrincipal;
		this.rfUltimoDia = rfUltimoDia;

		this.rfPercentIndexador = rfPercentIndexador;
		this.rfSpreadAnual = rfSpreadAnual;
	}

	// ==========================================================
	// GETTERS (CARTEIRA)
	// ==========================================================

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

	// ==========================================================
	// TESOURO DIRETO (GETTERS/SETTERS)
	// ==========================================================

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

	// ==========================================================
	// RENDA FIXA (GETTERS)
	// ==========================================================

	public String getRfIndexador() {
		return rfIndexador;
	}

	public String getRfForma() {
		return rfForma;
	}

	public double getRfTaxaAnual() {
		return rfTaxaAnual;
	}

	public double getRfPrincipal() {
		return rfPrincipal;
	}

	public String getRfUltimoDia() {
		return rfUltimoDia;
	}

	public double getRfPercentIndexador() {
		return rfPercentIndexador;
	}

	public double getRfSpreadAnual() {
		return rfSpreadAnual;
	}
}
