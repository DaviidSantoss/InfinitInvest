package MainInfinit;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO.SessaoTemp;
import ControllerInfinit.PesquisaController;
import ControllerInfinit.ResumoCarteira;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;



public class Infos extends StackPane {

	private TextField searchField = new TextField();
	private Label PValor = new Label("R$ 279.027,76");
	private Label PPorcent = new Label("-1%");
	private Label VInvestT = new Label("R$ 280.187,55");
	private Label LValor = new Label("R$ -1.159,79");
	private Label VGCapital = new Label("R$ -1.309,79");
	private Label VariacaoP = new Label("-1,2%");
	private Label VariacaoValor = new Label("R$ -1.309,79");
	private Label RentabilidadeV = new Label("3,25%");
	private Button AddInit = new Button("+ Adicionar ativo");
	private ListaDeAtivos listaDeAtivos = new ListaDeAtivos();

	private final GraficoAtivos graficoAtivos = new GraficoAtivos();
	@SuppressWarnings("resource")
	private final ScheduledExecutorService graficoScheduler = Executors.newSingleThreadScheduledExecutor();



	public Infos() {
		getStylesheets().add(getClass().getResource("/LoginInfinit/Login.css").toExternalForm());

		// =================================
		// CONFIGURAÇÕES EDITÁVEIS BARRA DE PESQUISA.
		// =================================
		double barraLargura = 350;
		double barraAltura = 10;
		int margemTopo = -120;
		int paddingHorizontal = 20;

		// =====================
		// Barra de pesquisa.
		// =====================

		searchField.setPromptText("Pesquisar...Ex(BBAS3)");
		searchField.setPrefWidth(barraLargura);
		searchField.setPrefHeight(barraAltura);
		searchField.setMaxWidth(barraLargura);
		searchField.setStyle("-fx-font-size: 14px; -fx-background-radius: 20; -fx-padding: 10;");
		searchField.getStyleClass().add("search-bar");
		new PesquisaController().configurar(searchField);

		// ========================
		// Container para a barra de pesquisa.
		// ========================
		StackPane searchContainer = new StackPane();
		searchContainer.getChildren().add(searchField);
		searchContainer.setAlignment(Pos.CENTER);
		searchContainer.setPadding(new Insets(0, paddingHorizontal, 0, paddingHorizontal));

		// ==========================
		// Container para os quadrados principais.
		// ==========================
        HBox container = new HBox(10);
        container.setPadding(new Insets(10));
        container.setAlignment(Pos.CENTER);

		// ======================================
		// CONFIGURAÇÕES EDITÁVEIS DOS OUTROS QUADRADOS.
		// ======================================
		double quadradoLargura = 600;
		double quadradoAltura = 300;
		
		ImageView iconePT = new ImageView(new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/poupanca.png")));
		iconePT.setFitWidth(50); // largura do ícone
		iconePT.setFitHeight(50); // altura do ícone
		iconePT.setPreserveRatio(true);

		// =====================
		// Quadrado 1.
		// =====================
        StackPane quadrado1 = new StackPane();
        quadrado1.setPrefSize(quadradoLargura, quadradoAltura);
        quadrado1.setBackground(new Background(new BackgroundFill(Color.web("#424242"), new CornerRadii(15), Insets.EMPTY)));
        
		// LABEL PATRIMONIO TOTAL.
		Label PTotal = new Label("Patrimônio total");
		PTotal.setStyle("-fx-text-fill: white; -fx-font-size: 28px;");
		
		// LABEL VALOR DO PATRIMONIO (DEVE ALTERAR DE ACORDO COM AS VARIAÇÕES)

		PValor.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");


		PPorcent.setStyle("-fx-text-fill: red; -fx-font-size: 24px;");

		// LABEL VALOR INVESTIDO
		Label VInvest = new Label("Valor Investido");
		VInvest.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");


		VInvestT.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");

		// ICONE DA LABEL PATRIMONIO
		PTotal.setGraphic(iconePT);
		PTotal.setContentDisplay(ContentDisplay.LEFT);
		PTotal.setGraphicTextGap(5);

		// CONFIG POSIÇÃO LABEL PATRIMONIO
		StackPane.setAlignment(PTotal, Pos.TOP_LEFT);
		StackPane.setMargin(PTotal, new Insets(10, 0, 0, 20));// (cima, direita, baixo, esquerda

		// CONFIG POSIÇÃO LABEL VALOR
		StackPane.setAlignment(PValor, Pos.CENTER_LEFT);
		StackPane.setMargin(PValor, new Insets(-100, 0, 0, 100));

		// CONFIG POSIÇÃO LABEL PPorcent
		StackPane.setAlignment(PPorcent, Pos.CENTER_LEFT);
		StackPane.setMargin(PPorcent, new Insets(-100, 0, 0, 260));

		// CONFIG POSIÇÃO LABEL VALOR INVESTIDO
		StackPane.setAlignment(VInvest, Pos.TOP_LEFT);
		StackPane.setMargin(VInvest, new Insets(150, 0, 0, 75));

		// CONFIG POSIÇÃO LABEL VInvestT
		StackPane.setAlignment(VInvestT, Pos.TOP_LEFT);
		StackPane.setMargin(VInvestT, new Insets(200, 0, 0, 100));

		quadrado1.getChildren().addAll(PTotal, PValor, PPorcent, VInvest, VInvestT);

		// =====================
		// Quadrado 2.
		// =====================
        StackPane quadrado2 = new StackPane();
        quadrado2.setPrefSize(quadradoLargura, quadradoAltura);
		quadrado2.setBackground(new Background(new BackgroundFill(Color.web("#424242"), new CornerRadii(15), Insets.EMPTY)));

		ImageView iconePR = new ImageView(new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/proventos.png")));
		iconePR.setFitWidth(50); // largura do ícone
		iconePR.setFitHeight(50); // altura do ícone
		iconePR.setPreserveRatio(true);

		// LABEL LUCRO TOTAL
		Label LTotal = new Label("Lucro total");
		LTotal.setStyle("-fx-text-fill: white; -fx-font-size: 28px;");

		LValor.setStyle("-fx-text-fill: red; -fx-font-size: 24px;");

		Label GCapital = new Label("Ganho de Capital");
		GCapital.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");


		VGCapital.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");


//		VPRecebidos.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");

		// ICONE DA LABEL PROVENTOS RECEBIDOS.
		LTotal.setGraphic(iconePR);
		LTotal.setContentDisplay(ContentDisplay.LEFT);
		LTotal.setGraphicTextGap(5);

		// CONFIG POSIÇÃO LABEL LUCRO TOTAL
		StackPane.setAlignment(LTotal, Pos.TOP_LEFT);
		StackPane.setMargin(LTotal, new Insets(10, 0, 0, 20));

		// CONFIG POSIÇÃO LABEL VALOR DO LUCRO TOTAL
		StackPane.setAlignment(LValor, Pos.CENTER_LEFT);
		StackPane.setMargin(LValor, new Insets(-100, 0, 0, 100));

		// CONFIG POSIÇÃO LABEL GANHO DE CAPITAL
		StackPane.setAlignment(GCapital, Pos.TOP_LEFT);
		StackPane.setMargin(GCapital, new Insets(150, 0, 0, 75));

		// CONFIG POSIÇÃO LABEL VALOR GANHO DE CAPITAL
		StackPane.setAlignment(VGCapital, Pos.TOP_LEFT);
		StackPane.setMargin(VGCapital, new Insets(200, 0, 0, 100));


//		// CONFIG POSIÇÃO LABEL VALOR PROVENTOS RECEBIDOS
//		StackPane.setAlignment(VPRecebidos, Pos.TOP_LEFT);
//		StackPane.setMargin(VPRecebidos, new Insets(200, 0, 0, 380));

		quadrado2.getChildren().addAll(LTotal, LValor, GCapital, VGCapital);

		// =====================
		// Quadrado 3.
		// =====================
        StackPane quadrado3 = new StackPane();
        quadrado3.setPrefSize(quadradoLargura, quadradoAltura);
		quadrado3.setBackground(new Background(new BackgroundFill(Color.web("#424242"), new CornerRadii(15), Insets.EMPTY)));

		ImageView iconeVR = new ImageView(new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/lucro.png")));
		iconeVR.setFitWidth(50); // largura do ícone
		iconeVR.setFitHeight(50); // altura do ícone
		iconeVR.setPreserveRatio(true);


		Label Variacao = new Label("Variação");
		Variacao.setStyle("-fx-text-fill: white; -fx-font-size: 28px;");


		VariacaoP.setStyle("-fx-text-fill: red; -fx-font-size: 28px;");


		VariacaoValor.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");

		Label Rentabilidade = new Label("Rentabilidade");
		Rentabilidade.setStyle("-fx-text-fill: white; -fx-font-size: 28px;");


		RentabilidadeV.setStyle("-fx-text-fill: green; -fx-font-size: 24px;");

		Variacao.setGraphic(iconeVR);
		Variacao.setContentDisplay(ContentDisplay.LEFT);
		Variacao.setGraphicTextGap(5);

		// CONFIG POSIÇÃO LABEL VARIAÇÃO
		StackPane.setAlignment(Variacao, Pos.TOP_LEFT);
		StackPane.setMargin(Variacao, new Insets(20, 0, 0, 20));

		StackPane.setAlignment(VariacaoP, Pos.TOP_LEFT);
		StackPane.setMargin(VariacaoP, new Insets(70, 0, 0, 90));

		StackPane.setAlignment(VariacaoValor, Pos.TOP_LEFT);
		StackPane.setMargin(VariacaoValor, new Insets(110, 0, 0, 90));

		StackPane.setAlignment(Rentabilidade, Pos.TOP_LEFT);
		StackPane.setMargin(Rentabilidade, new Insets(160, 0, 0, 80));

		StackPane.setAlignment(RentabilidadeV, Pos.TOP_LEFT);
		StackPane.setMargin(RentabilidadeV, new Insets(200, 0, 0, 130));

		quadrado3.getChildren().addAll(Variacao, VariacaoP, VariacaoValor, Rentabilidade, RentabilidadeV);

		double espacamento = 10; // mesmo valor usado no HBox
		double larguraQuadrado4 = (quadradoLargura * 2) + (espacamento * 2);

		StackPane quadrado4 = new StackPane();
		quadrado4.setPrefSize(larguraQuadrado4, 450);
		quadrado4.setBackground(new Background(new BackgroundFill(Color.web("#424242"), new CornerRadii(15), Insets.EMPTY)));
		
		Label AtivosC = new Label("Ativos na Carteira");
		AtivosC.setStyle("-fx-text-fill: white; -fx-font-size: 28px;");

		StackPane.setAlignment(AtivosC, Pos.TOP_LEFT);
		StackPane.setMargin(AtivosC, new Insets(10, 0, 0, 20));// (cima, direita, baixo, esquerda

		VBox.setVgrow(quadrado4, javafx.scene.layout.Priority.NEVER);
		quadrado4.setMaxWidth(larguraQuadrado4);

		StackPane quadrado4Container = new StackPane(quadrado4);
		quadrado4Container.setAlignment(Pos.CENTER);

		Node graficoNode = graficoAtivos.getNode();
		quadrado4.getChildren().addAll(AtivosC, graficoNode);



		AddInit.setPrefWidth(200);
		AddInit.setPrefHeight(30);
		AddInit.getStyleClass().add("botao");

		StackPane botaoAtivos = new StackPane(AddInit);
		botaoAtivos.setAlignment(Pos.CENTER_RIGHT);
		botaoAtivos.setPadding(new Insets(0, 15, 0, 0));


		VBox quadradosContainer = new VBox(10); // espaçamento vertical
		quadradosContainer.setAlignment(Pos.CENTER);
		quadradosContainer.getChildren().addAll(container, quadrado4Container, botaoAtivos);

		container.getChildren().addAll(quadrado1, quadrado2, quadrado3);

        // BorderPane para organizar os elementos
        BorderPane mainContainer = new BorderPane();
		mainContainer.setTop(searchContainer);
		BorderPane.setAlignment(searchContainer, Pos.TOP_CENTER);
		BorderPane.setMargin(searchContainer, new Insets(margemTopo, 0, 0, 0));
		mainContainer.setCenter(quadradosContainer);

        // ✅ adiciona o container principal
		this.getChildren().addAll(mainContainer);

		iniciarAtualizacaoGrafico();

		this.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene == null) {
				graficoScheduler.shutdownNow();
			}
		});


    }

	private void iniciarAtualizacaoGrafico() {
		graficoScheduler.scheduleAtFixedRate(() -> {
			try {
				Integer usuarioId = SessaoTemp.getUsuarioId();
				if (usuarioId == null)
					return;

				Dao dao = new Dao();
				Map<String, Double> totais = dao.obterTotaisPorCategoria(usuarioId);
				ResumoCarteira resumo = dao.obterResumoCarteira(usuarioId);

				// gráfico pode ser direto (ele não é UI padrão? depende do seu GraficoAtivos)
				// mas por segurança, UI sempre no Platform.runLater
				Platform.runLater(() -> {
					graficoAtivos.atualizar(totais);
					atualizarLabelsPainel(resumo);
				});


			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 0, 10, TimeUnit.SECONDS); // 10s fica bem “vivo”
	}

	@SuppressWarnings("deprecation")
	private static final Locale LOCALE_BR = new Locale("pt", "BR");
	private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(LOCALE_BR);
	private static final NumberFormat PCT = NumberFormat.getPercentInstance(LOCALE_BR);

	private String fmtMoeda(double v) {
		return MOEDA.format(v);
	}

	private String fmtPct(double v) {
		PCT.setMinimumFractionDigits(2);
		PCT.setMaximumFractionDigits(2);
		return PCT.format(v);
	}

	private void aplicarCor(Label label, double valor) {
		if (valor > 0)
			label.setStyle(label.getStyle() + "-fx-text-fill: #2ecc71;"); // verde
		else if (valor < 0)
			label.setStyle(label.getStyle() + "-fx-text-fill: #e74c3c;"); // vermelho
		else
			label.setStyle(label.getStyle() + "-fx-text-fill: white;");
	}

	private void atualizarLabelsPainel(ResumoCarteira r) {
		// Valores
		PValor.setText(fmtMoeda(r.patrimonioTotal));
		VInvestT.setText(fmtMoeda(r.valorInvestido));
		LValor.setText(fmtMoeda(r.lucroTotal));
		VGCapital.setText(fmtMoeda(r.ganhoCapital));

		// Percentuais
		PPorcent.setText(fmtPct(r.variacaoPercentual));
		VariacaoP.setText(fmtPct(r.variacaoPercentual));
		VariacaoValor.setText(fmtMoeda(r.lucroTotal));
		RentabilidadeV.setText(fmtPct(r.rentabilidadePonderada));

		// Cores (baseadas nos sinais)
		aplicarCor(PPorcent, r.variacaoPercentual);
		aplicarCor(VariacaoP, r.variacaoPercentual);

		aplicarCor(LValor, r.lucroTotal);
		aplicarCor(VGCapital, r.ganhoCapital);

		aplicarCor(RentabilidadeV, r.rentabilidadePonderada);
	}



	// =====================
	// Getters and Setters
	// =====================

	public TextField getSearchField() {
		return searchField;
	}

	public void setSearchField(TextField searchField) {
		this.searchField = searchField;
	}

	public Label getPValor() {
		return PValor;
	}

	public void setPValor(Label pValor) {
		PValor = pValor;
	}

	public Label getPPorcent() {
		return PPorcent;
	}

	public void setPPorcent(Label pPorcent) {
		PPorcent = pPorcent;
	}

	public Label getVInvestT() {
		return VInvestT;
	}

	public void setVInvestT(Label vInvestT) {
		VInvestT = vInvestT;
	}

	public Label getLValor() {
		return LValor;
	}

	public void setLValor(Label lValor) {
		LValor = lValor;
	}

	public Label getVGCapital() {
		return VGCapital;
	}

	public void setVGCapital(Label vGCapital) {
		VGCapital = vGCapital;
	}

//	public Label getVPRecebidos() {
//		return VPRecebidos;
//	}
//
//	public void setVPRecebidos(Label vPRecebidos) {
//		VPRecebidos = vPRecebidos;
//	}

	public Label getVariacaoP() {
		return VariacaoP;
	}

	public void setVariacaoP(Label variacaoP) {
		VariacaoP = variacaoP;
	}

	public Label getVariacaoValor() {
		return VariacaoValor;
	}

	public void setVariacaoValor(Label variacaoValor) {
		VariacaoValor = variacaoValor;
	}

	public Label getRentabilidadeV() {
		return RentabilidadeV;
	}

	public void setRentabilidadeV(Label rentabilidadeV) {
		RentabilidadeV = rentabilidadeV;
	}

	public Button getAddInit() {
		return AddInit;
	}

	public void setAddInit(Button ativos) {
		AddInit = ativos;
	}

	public ListaDeAtivos getListaDeAtivos() {
		return listaDeAtivos;
	}

	public void setListaDeAtivos(ListaDeAtivos listaDeAtivos) {
		this.listaDeAtivos = listaDeAtivos;
	}

}