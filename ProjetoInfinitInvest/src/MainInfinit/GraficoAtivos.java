package MainInfinit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GraficoAtivos {

	private final PieChart grafico;
	private final VBox listaAtivos;
	private final ObservableList<PieChart.Data> dados;

	// mantém sempre as mesmas fatias (só muda o valor)
	private final Map<String, PieChart.Data> fatias = new LinkedHashMap<>();

	private final HBox root;

	// ✅ cores fixas por categoria (do jeito que você pediu)
	// Ações = vermelho, FIIs = verde, Renda Fixa = azul, Criptos = amarelo, ETFs = sobra, Tesouro Direto = neutro
	private static final Map<String, String> COR_POR_CATEGORIA = Map.of("Ações", "#ff7981", // vermelho
			"FIIs", "#88FFC3", // verde
			"Renda Fixa", "#50A0FF", // azul
			"Criptomoedas", "#fff369", // amarelo
			"ETFs", "#02757B", // sobrou
			"Tesouro Direto", "#999999" // neutro
	);

	public GraficoAtivos() {

		// ordem fixa das fatias
		fatias.put("Ações", new PieChart.Data("Ações", 0));
		fatias.put("FIIs", new PieChart.Data("FIIs", 0));
		fatias.put("ETFs", new PieChart.Data("ETFs", 0));
		fatias.put("Criptomoedas", new PieChart.Data("Criptomoedas", 0));
		fatias.put("Tesouro Direto", new PieChart.Data("Tesouro Direto", 0));
		fatias.put("Renda Fixa", new PieChart.Data("Renda Fixa", 0));

		dados = FXCollections.observableArrayList(fatias.values());

		grafico = new PieChart(dados);
		grafico.setLegendVisible(false);
		grafico.setLabelsVisible(true);
		grafico.setStartAngle(90);
		grafico.setStyle("-fx-background-color: transparent;");

		// donut
		Circle centro = new Circle(60, Color.web("#2b2b2b"));
		StackPane donut = new StackPane(grafico, centro);

		// lista lateral
		listaAtivos = new VBox(8);
		listaAtivos.setAlignment(Pos.CENTER_LEFT);

		// labels brancas dentro do gráfico
		Platform.runLater(() -> grafico.lookupAll(".chart-pie-label").forEach(n -> n.setStyle("-fx-fill: white; -fx-font-weight: bold;")));

		root = new HBox(30, donut, listaAtivos);
		root.setAlignment(Pos.CENTER);

		// ✅ aplica as cores quando o gráfico realmente renderizar
		aplicarCoresDepoisDoRender();
	}

	public Node getNode() {
		return root;
	}

	/** Atualiza o gráfico com saldos reais (R$) por categoria */
	public void atualizar(Map<String, Double> totalPorCategoria) {
		Platform.runLater(() -> {

			// 1) atualiza valores do pie (em R$)
			for (Map.Entry<String, PieChart.Data> e : fatias.entrySet()) {
				String nome = e.getKey();
				PieChart.Data data = e.getValue();

				double valor = totalPorCategoria.getOrDefault(nome, 0.0);

				// normaliza NaN/negativo
				if (!Double.isFinite(valor) || valor < 0)
					valor = 0;

				data.setPieValue(valor);
			}

			// 2) recalcula lista lateral com percentuais reais
			double total = dados.stream().mapToDouble(PieChart.Data::getPieValue).sum();
			listaAtivos.getChildren().clear();

			// ordena por maior valor
			List<PieChart.Data> ordenado = new ArrayList<>(dados);
			ordenado.sort(Comparator.comparingDouble(PieChart.Data::getPieValue).reversed());

			for (PieChart.Data d : ordenado) {
				double v = d.getPieValue();
				if (v <= 0)
					continue;

				double pct = (total > 0) ? (v / total) * 100.0 : 0;

				String cor = corCategoria(d.getName());

				Label titulo = new Label(d.getName());
				titulo.setStyle("-fx-text-fill: " + cor + "; -fx-font-size: 14px; -fx-font-weight: bold;");

				Label percentual = new Label(String.format(" (%.2f%%)", pct));
				percentual.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

				Label valor = new Label(String.format("  R$ %.2f", v));
				valor.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 13px;");

				HBox linha = new HBox(6, titulo, percentual, valor);
				linha.setAlignment(Pos.CENTER_LEFT);

				listaAtivos.getChildren().add(linha);
			}

			// ✅ garante que as cores continuem certas depois do update
			aplicarCoresDepoisDoRender();
			aplicarCorLabelsGrafico(Color.web("#424242"));

		});
	}

	// =========================
	// CORES (ROBUSTO)
	// =========================

	private void aplicarCoresFatias() {
		for (PieChart.Data d : dados) {
			if (d.getNode() != null) {
				String cor = corCategoria(d.getName());
				d.getNode().setStyle("-fx-pie-color: " + cor + ";" + "-fx-border-color: transparent;");
			}
		}
	}

	private void aplicarCoresDepoisDoRender() {
		// 1º runLater: depois do CSS/layout inicial
		Platform.runLater(() -> {
			aplicarCoresFatias();

			// 2º runLater: garante se o JavaFX recriar os nodes
			Platform.runLater(this::aplicarCoresFatias);
		});
	}

	private static String corCategoria(String categoria) {
		if (categoria == null)
			return "#999999";
		return COR_POR_CATEGORIA.getOrDefault(categoria.trim(), "#999999");
	}

	private void aplicarCorLabelsGrafico(Color cor) {
		String css = String.format("-fx-fill: rgb(%d,%d,%d); -fx-font-weight: bold;", (int) (cor.getRed() * 255), (int) (cor.getGreen() * 255), (int) (cor.getBlue() * 255));

		grafico.lookupAll(".chart-pie-label").forEach(n -> n.setStyle(css));
	}



}