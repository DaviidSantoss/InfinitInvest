package ControllerInfinit;

import java.sql.SQLException;

import BancoInfinit.SessaoDAO;
import MainInfinit.Anotacoes;
import MainInfinit.BarraLateral;
import MainInfinit.Import;
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

	private BarraLateral barraLateral;
	Stage stage;

	public BarraController(BarraLateral barraLateral) {
		this.barraLateral = barraLateral;
	}

	public void configurarAcoes() {
		

	      barraLateral.getPrincipal().setOnAction(e -> {

				selecionarBotao(barraLateral.getPrincipal(), "/LoginInfinit/imagens/userBlack.png");

				Parent principal;
				try {
					principal = new Principal();
					Scene novaCena = new Scene(principal, 1920, 1080); // tamanho da cena

					Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
					stage.setScene(novaCena);
					stage.setMaximized(true);
					stage.show();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}

	        });

			barraLateral.getAnotacoes().setOnAction(e -> {
				selecionarBotao(barraLateral.getAnotacoes(), "/LoginInfinit/imagens/bookBlack.png");

				Anotacoes anotacoesPane = new Anotacoes(); // instanciando o layout
				Scene novaCena = new Scene(anotacoesPane, 1920, 1080); // tamanho da cena

				Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
				stage.setScene(novaCena);
				stage.setMaximized(true);
				stage.show();
			});
			
			
			barraLateral.getLancamentos().setOnAction(e -> {
				selecionarBotao(barraLateral.getLancamentos(), "/LoginInfinit/imagens/lanBlack.png");

				lancamentos lancamentos = new lancamentos(); // instanciando o layout
				Scene novaCena = new Scene(lancamentos, 1920, 1080); // tamanho da cena

				Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
				stage.setScene(novaCena);
				stage.setMaximized(true);
				stage.show();
			});

			barraLateral.getImportexport().setOnAction(e -> {
				selecionarBotao(barraLateral.getImportexport(), "/LoginInfinit/imagens/importBlack.png");
				
				Import import1 = new Import(); // instanciando o layout
				Scene novaCena = new Scene(import1, 1920, 1080); // tamanho da cena

				Stage stage = (Stage) ((Node) e.getSource()).getScene().getWindow();
				stage.setScene(novaCena);
				stage.setMaximized(true);
				stage.show();
				
				
			});

			barraLateral.getSair().setOnAction(e -> {
				try {
					SessaoDAO.SessaoTemp.limpar();
					SessaoDAO sessaoDAO = new SessaoDAO();
					sessaoDAO.limparSessao();

					// 🔥 Pega o Stage diretamente da cena atual (garante que nunca é nulo)
					Stage currentStage = (Stage) barraLateral.getScene().getWindow();

					ScreenManager.mostrarLogin(currentStage);

					System.out.println("✅ Logout realizado com sucesso!");
				} catch (Exception ex) {
					ex.printStackTrace();
					System.out.println("❌ Erro ao realizar logout!");
				}
			});





		}

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
			desselecionarTodosBotoes(); // Primeiro desseleciona todos

			// Agora seleciona o botão clicado
			botao.getStyleClass().remove("botao-lateral");
			botao.getStyleClass().add("botao-lateral-selecionado");

			Image imagemBlack = new Image(getClass().getResourceAsStream(caminhoImagem));
			((ImageView) botao.getGraphic()).setImage(imagemBlack);
		}

		Image imagemWhitePrincipal = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/userWhite.png"));
		ImageView imagemPrincipal = new ImageView(imagemWhitePrincipal);

		Image imagemWhiteAnota = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/bookWhite.png"));
		ImageView imagemAnotacoes = new ImageView(imagemWhiteAnota);

		Image imagemWhiteLan = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/lanWhite.png"));
		ImageView imagemLan = new ImageView(imagemWhiteLan);

		Image imagemWhiteImport = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/importWhite.png"));
		ImageView imagemImport = new ImageView(imagemWhiteImport);

		Image imagemWhiteExit = new Image(getClass().getResourceAsStream("/LoginInfinit/imagens/exitWhite.png"));
		ImageView imagemExit = new ImageView(imagemWhiteExit);
}
