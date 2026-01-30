package ControllerInfinit;

import java.sql.SQLException;

import BancoInfinit.SessaoDAO;
import MainInfinit.Anotacoes;
import MainInfinit.BarraLateral;
import MainInfinit.Principal;
import MainInfinit.lancamentos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class BarraController {

	// ==========================================================
	// CONTEXTO DO CONTROLLER
	// ==========================================================
	// Controla a barra lateral de navegação.
	// Alterna telas (Principal/Anotações/Lançamentos), abre popup de import/export
	// e executa logout mantendo o estado visual dos botões (selecionado/ícones).

	private final BarraLateral barraLateral;

	// ==========================================================
	// ÍCONES (WHITE = padrão / BLACK = selecionado)
	// ==========================================================

	private final Image imagemWhitePrincipal = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/userWhite.png"));
	private final Image imagemWhiteAnota = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/bookWhite.png"));
	private final Image imagemWhiteLan = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/lanWhite.png"));
	private final Image imagemWhiteImport = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/importWhite.png"));
	private final Image imagemWhiteExit = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/exitWhite.png"));

	@SuppressWarnings("unused")
	private final ImageView imagemPrincipal = new ImageView(imagemWhitePrincipal);
	@SuppressWarnings("unused")
	private final ImageView imagemAnotacoes = new ImageView(imagemWhiteAnota);
	@SuppressWarnings("unused")
	private final ImageView imagemLan = new ImageView(imagemWhiteLan);
	@SuppressWarnings("unused")
	private final ImageView imagemImport = new ImageView(imagemWhiteImport);
	@SuppressWarnings("unused")
	private final ImageView imagemExit = new ImageView(imagemWhiteExit);

	// ==========================================================
	// CONSTRUTOR
	// ==========================================================

	public BarraController(BarraLateral barraLateral) {
		this.barraLateral = barraLateral;
	}

	// ==========================================================
	// AÇÕES / NAVEGAÇÃO
	// ==========================================================

	public void configurarAcoes() {

		barraLateral.getPrincipal().setOnAction(e -> {
			selecionarBotao(barraLateral.getPrincipal(), "/LoginInfinit/imagens/userBlack.png");

			try {
				Parent principal = new Principal();
				Scene novaCena = new Scene(principal, 1920, 1080);

				Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
				stage.setScene(novaCena);
				stage.setMaximized(true);
				stage.show();

			} catch (SQLException ex) {
				ex.printStackTrace();
			}
		});

		barraLateral.getAnotacoes().setOnAction(e -> {
			selecionarBotao(barraLateral.getAnotacoes(), "/LoginInfinit/imagens/bookBlack.png");

			Anotacoes anotacoesPane = new Anotacoes();
			Scene novaCena = new Scene(anotacoesPane, 1920, 1080);

			Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
			stage.setScene(novaCena);
			stage.setMaximized(true);
			stage.show();
		});

		barraLateral.getLancamentos().setOnAction(e -> {
			selecionarBotao(barraLateral.getLancamentos(), "/LoginInfinit/imagens/lanBlack.png");

			lancamentos lancamentosPane = new lancamentos();
			Scene novaCena = new Scene(lancamentosPane, 1920, 1080);

			Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
			stage.setScene(novaCena);
			stage.setMaximized(true);
			stage.show();
		});

		barraLateral.getImportexport().setOnAction(e -> {
			selecionarBotao(barraLateral.getImportexport(), "/LoginInfinit/imagens/importBlack.png");
			new ImportarEexportarController().abrirPopup(barraLateral.getScene() != null ? barraLateral.getScene().getWindow() : null);
		});

		barraLateral.getSair().setOnAction(e -> {
			try {
				SessaoDAO.SessaoTemp.limpar();
				SessaoDAO sessaoDAO = new SessaoDAO();
				sessaoDAO.limparSessao();

				Stage currentStage = (Stage) barraLateral.getScene().getWindow();
				ScreenManager.mostrarLogin(currentStage);

				System.out.println("✅ Logout realizado com sucesso!");
			} catch (Exception ex) {
				ex.printStackTrace();
				System.out.println("❌ Erro ao realizar logout!");
			}
		});
	}

	// ==========================================================
	// ESTADO VISUAL DOS BOTÕES
	// ==========================================================

	private void desselecionarTodosBotoes() {
		// Principal
		barraLateral.getPrincipal().getStyleClass().add("botao-lateral");
		barraLateral.getPrincipal().getStyleClass().remove("botao-lateral-selecionado");
		((ImageView) barraLateral.getPrincipal().getGraphic()).setImage(imagemWhitePrincipal);

		// Anotações
		barraLateral.getAnotacoes().getStyleClass().add("botao-lateral");
		barraLateral.getAnotacoes().getStyleClass().remove("botao-lateral-selecionado");
		((ImageView) barraLateral.getAnotacoes().getGraphic()).setImage(imagemWhiteAnota);

		// Lançamentos
		barraLateral.getLancamentos().getStyleClass().add("botao-lateral");
		barraLateral.getLancamentos().getStyleClass().remove("botao-lateral-selecionado");
		((ImageView) barraLateral.getLancamentos().getGraphic()).setImage(imagemWhiteLan);

		// Import/Export
		barraLateral.getImportexport().getStyleClass().add("botao-lateral");
		barraLateral.getImportexport().getStyleClass().remove("botao-lateral-selecionado");
		((ImageView) barraLateral.getImportexport().getGraphic()).setImage(imagemWhiteImport);

		// Sair
		barraLateral.getSair().getStyleClass().add("botao-lateral");
		barraLateral.getSair().getStyleClass().remove("botao-lateral-selecionado");
		((ImageView) barraLateral.getSair().getGraphic()).setImage(imagemWhiteExit);
	}

	public void selecionarBotao(Button botao, String caminhoImagem) {
		desselecionarTodosBotoes();

		botao.getStyleClass().remove("botao-lateral");
		botao.getStyleClass().add("botao-lateral-selecionado");

		Image imagemBlack = new Image(getClass().getResourceAsStream(caminhoImagem));
		((ImageView) botao.getGraphic()).setImage(imagemBlack);
	}
}
