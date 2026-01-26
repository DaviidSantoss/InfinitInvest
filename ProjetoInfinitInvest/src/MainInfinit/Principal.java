package MainInfinit;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import BancoInfinit.Ativo;
import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO;
import ControllerInfinit.BarraController;
import ControllerInfinit.InfosController;
import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class Principal extends BorderPane {

	private String corFundo = "#212121";
	private BarraLateral barra;
	private BarraController barraController;
	private ControllerInfinit.AtualizadorCarteira atualizador;

	@SuppressWarnings("unused")
	public Principal() throws SQLException {


		// ============================
		// Cria os quadrados (Infos)
		// ============================
		Infos infos = new Infos();
		ListaDeAtivos lista = new ListaDeAtivos();

		new InfosController(infos, lista);

		// ============================
		// Obter usuário logado
		// ============================
		Integer usuarioId = null;

		try {
		    usuarioId = SessaoDAO.SessaoTemp.getUsuarioId();
		} catch (Exception ignored) {}

		if (usuarioId == null) {
		    try {
		        SessaoDAO sessaoDAO = new SessaoDAO();
		        usuarioId = SessaoDAO.buscarSessao();
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}

		// ✅ PRIMEIRO DE TUDO: seta a SessaoTemp assim que tiver o id
		if (usuarioId != null) {
			BancoInfinit.SessaoDAO.SessaoTemp.setUsuarioId(usuarioId);
		}

		// ============================
		// Carregar ativos do banco (UMA ÚNICA VEZ)
		// ============================
		if (usuarioId != null) {
			carregarAtivosDoUsuarioNaLista(lista, usuarioId);

			atualizador = new ControllerInfinit.AtualizadorCarteira(lista);
			atualizador.start();

		}

		if (usuarioId != null) {
			BancoInfinit.SessaoDAO.SessaoTemp.setUsuarioId(usuarioId);
		}

		// ============================
		// Background do app
		// ============================
		BackgroundFill fundo = new BackgroundFill(Color.web(corFundo), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(fundo));

		// ============================
		// Barra lateral
		// ============================
		barra = new BarraLateral();
		barraController = new BarraController(barra);
		barra.setController(barraController);

		// ============================
		// Lista de ativos (UI)
		// ============================
		lista.setPadding(new Insets(30, 0, 0, 0));
		lista.setMaxWidth(1200);

		// ============================
		// VBox principal
		// ============================
		VBox root = new VBox(30);
		root.setPadding(new Insets(120, 0, 50, 0));
		root.setAlignment(javafx.geometry.Pos.TOP_CENTER);

		VBox.setMargin(infos, new Insets(10, 0, 0, 0));
		root.getChildren().addAll(infos, lista);

		// ============================
		// ScrollPane
		// ============================
		ScrollPane scrollPane = new ScrollPane(root);

		scrollPane.setFitToWidth(true);
		scrollPane.setFitToHeight(false);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		scrollPane.setPannable(true);
		scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
		scrollPane.setOnScroll(event -> scrollPane.setVvalue(scrollPane.getVvalue() - event.getDeltaY() * 0.0009));

		// ============================
		// Finalização
		// ============================
		this.setLeft(barra);
		barraController.selecionarBotao(barra.getPrincipal(), "/LoginInfinit/imagens/userBlack.png");
		this.setPrefSize(1920, 1080);
		this.setCenter(scrollPane);
	}

	@SuppressWarnings({ "deprecation", "unused" })
	private String formatCurrency(double v) {
		return String.format(new Locale("pt", "BR"), "R$ %,.2f", v);
	}

	@SuppressWarnings({ "deprecation", "unused" })
	private String formatVariacao(double v) {
		return String.format(new Locale("pt", "BR"), "%.2f %%", v);
	}

	@SuppressWarnings({ "deprecation", "unused" })
	private String formatQuantidade(double q) {
		String s = String.format(new Locale("pt", "BR"), "%.8f", q);
		return s.replace('.', ',');
	}

	private void carregarAtivosDoUsuarioNaLista(ListaDeAtivos listaView, Integer usuarioId) {
		if (usuarioId == null)
			return;

		// Carrega do banco
		List<Ativo> ativos = Dao.listarAtivosPorUsuario(usuarioId);

		for (Ativo a : ativos) {

			String cat = (a.getCategoria() == null ? "" : a.getCategoria().trim());
			String nome = a.getAtivo();

			switch (cat.toLowerCase()) {

			case "tesouro direto" -> {
				listaView.adicionarTesouroDireto(nome, a.getQuantidade(), a.getVariacao(), a.getSaldo());
			}

			case "criptomoedas" -> {
				listaView.adicionarCriptomoeda(a.getIconUrl(), // pode ser null
						nome, a.getQuantidade(), a.getPrecoMedio(), a.getPrecoAtual(), a.getVariacao(), a.getSaldo());
			}

			case "renda fixa" -> {
				// seu modelo atual: quantidade = rentabilidade
				listaView.adicionarRendaFixa(nome, a.getQuantidade(), // rentabilidade
						a.getVariacao(), a.getSaldo());
			}

			default -> {
				// Ações / FIIs / ETFs
				listaView.adicionarAcoesEFiis(cat, nome, a.getIconUrl(), // pode ser null
						a.getQuantidade(), a.getPrecoMedio(), a.getPrecoAtual(), a.getVariacao(), a.getSaldo());
			}
			}
		}
	}
}
