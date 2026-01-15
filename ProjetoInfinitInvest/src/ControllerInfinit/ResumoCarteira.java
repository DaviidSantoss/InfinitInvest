package ControllerInfinit;

public class ResumoCarteira {
	public final double patrimonioTotal; // soma dos saldos
	public final double valorInvestido; // custo bruto (sem variação)
	public final double lucroTotal; // patrimonio - investido
	public final double ganhoCapital; // (mesma ideia do lucro, mas aqui deixei igual ao lucro total)
	public final double variacaoPercentual; // lucro / investido
	public final double rentabilidadePonderada; // ponderada pelo valor investido

	public ResumoCarteira(double patrimonioTotal, double valorInvestido, double lucroTotal, double ganhoCapital, double variacaoPercentual, double rentabilidadePonderada) {
		this.patrimonioTotal = patrimonioTotal;
		this.valorInvestido = valorInvestido;
		this.lucroTotal = lucroTotal;
		this.ganhoCapital = ganhoCapital;
		this.variacaoPercentual = variacaoPercentual;
		this.rentabilidadePonderada = rentabilidadePonderada;
	}
}
