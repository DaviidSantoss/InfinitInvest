package ControllerInfinit;

public class ResumoCarteira {

	// ==========================================================
	// CONTEXTO DO MODELO
	// ==========================================================
	// Representa um resumo consolidado da carteira do usuário.
	// Usado para exibição de métricas globais como patrimônio,
	// lucro, variação percentual e rentabilidade ponderada.

	public final double patrimonioTotal;
	public final double valorInvestido;
	public final double lucroTotal;
	public final double ganhoCapital;
	public final double variacaoPercentual;
	public final double rentabilidadePonderada;

	// ==========================================================
	// CONSTRUTOR
	// ==========================================================

	public ResumoCarteira(double patrimonioTotal, double valorInvestido, double lucroTotal, double ganhoCapital, double variacaoPercentual, double rentabilidadePonderada) {
		this.patrimonioTotal = patrimonioTotal;
		this.valorInvestido = valorInvestido;
		this.lucroTotal = lucroTotal;
		this.ganhoCapital = ganhoCapital;
		this.variacaoPercentual = variacaoPercentual;
		this.rentabilidadePonderada = rentabilidadePonderada;
	}
}
