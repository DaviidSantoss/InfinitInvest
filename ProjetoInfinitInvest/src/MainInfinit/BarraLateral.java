package MainInfinit;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.imageio.ImageIO;

import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO;
import BancoInfinit.Usuario;
import ControllerInfinit.BarraController;
import LoginInfinit.LoginForm;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

public class BarraLateral extends StackPane {

	private Button Principal = new Button("Principal");
	private Button Anotacoes = new Button("Anotações");
	private Button Lancamentos = new Button("Lançamentos");
	private Button Importexport = new Button("Import/Export");
	private Button Sair = new Button("Sair");
	@SuppressWarnings("unused")
	private LoginForm login;
	String corBarra = "333333";

	public BarraLateral() throws SQLException {

		BackgroundFill fundo = new BackgroundFill(Color.web(corBarra), new CornerRadii(10), Insets.EMPTY);
		setBackground(new Background(fundo));

		setMaxHeight(Double.MAX_VALUE);
		getChildren().addAll(criarFotoPerfil());
	}

	public VBox Botoes() {

		getStylesheets().add(getClass().getResource("/LoginInfinit/Login.css").toExternalForm());

		VBox vboxx = new VBox();
		vboxx.setAlignment(Pos.TOP_CENTER);
		vboxx.setSpacing(25);
		vboxx.setPadding(new Insets(70, 0, 0, 0));

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

		Principal.getStyleClass().add("botao-lateral");
		Anotacoes.getStyleClass().add("botao-lateral");
		Lancamentos.getStyleClass().add("botao-lateral");
		Importexport.getStyleClass().add("botao-lateral");
		Sair.getStyleClass().add("botao-lateral");

		imagemPrincipal.setFitWidth(54);
		imagemPrincipal.setFitHeight(54);

		imagemAnotacoes.setFitWidth(54);
		imagemAnotacoes.setFitHeight(54);

		imagemLan.setFitWidth(54);
		imagemLan.setFitHeight(54);
		imagemLan.setTranslateX(-4);

		imagemImport.setFitWidth(54);
		imagemImport.setFitHeight(54);
		imagemImport.setTranslateX(-5);

		imagemExit.setFitWidth(54);
		imagemExit.setFitHeight(54);

		Principal.setGraphic(imagemPrincipal);
		Principal.setGraphicTextGap(25);
		Principal.setContentDisplay(ContentDisplay.LEFT);

		Anotacoes.setGraphic(imagemAnotacoes);
		Anotacoes.setGraphicTextGap(25);
		Anotacoes.setContentDisplay(ContentDisplay.LEFT);

		Lancamentos.setGraphic(imagemLan);
		Lancamentos.setGraphicTextGap(25);
		Lancamentos.setContentDisplay(ContentDisplay.LEFT);

		Importexport.setGraphic(imagemImport);
		Importexport.setGraphicTextGap(25);
		Importexport.setContentDisplay(ContentDisplay.LEFT);

		Sair.setGraphic(imagemExit);
		Sair.setGraphicTextGap(25);
		Sair.setContentDisplay(ContentDisplay.LEFT);

		vboxx.getChildren().addAll(Principal, Anotacoes, Lancamentos, Importexport, Sair);

		return vboxx;

	}

	@SuppressWarnings("static-access")
	public VBox NomeUsuario() {

		VBox vbox = new VBox();
		vbox.setAlignment(Pos.TOP_CENTER);

		Usuario usuario = SessaoDAO.SessaoTemp.getUsuarioLogado();

		try {

			if (usuario == null) {
				SessaoDAO sessaoDAO = new SessaoDAO();
				Integer usuarioId = sessaoDAO.buscarSessao();

				if (usuarioId != null) {
					usuario = Dao.buscarPorId(usuarioId);
				}
			}

		} catch (Exception e) {
		}

		if (usuario == null) {
			Label erro = new Label("Usuário não encontrado");
			erro.setStyle("-fx-text-fill: red;");
			return new VBox(erro);
		}

		Label nome = new Label(usuario.getNome());
		nome.setStyle("-fx-font-size: 22px; -fx-text-fill: white;");
		vbox.getChildren().add(nome);
		return vbox;
	}

	// ====================
	// FOTO ULTRA PREMIUM
	// ====================
	@SuppressWarnings({ "static-access", "unused" })
	public VBox criarFotoPerfil() throws SQLException {

		VBox vbox = new VBox();
		vbox.setPadding(new Insets(20, 0, 0, 0));
		vbox.setSpacing(20);
		vbox.setAlignment(Pos.TOP_CENTER);

		// Foto "real" renderizada
		Circle fotoPerfil = new Circle(100);

		// Stack final: primeiro a sombra, depois a foto
		StackPane fotoStack = new StackPane(fotoPerfil);
		fotoStack.setAlignment(Pos.CENTER);


		// Fade suave
		FadeTransition ft = new FadeTransition(Duration.millis(300), fotoStack);
		ft.setFromValue(0);
		ft.setToValue(1);
		ft.play();

		// Busca usuário
		try {
			Dao daoLocal = new Dao();
			Usuario usuario = SessaoDAO.SessaoTemp.getUsuarioLogado();

			if (usuario == null) {
				SessaoDAO sessaoDAO = new SessaoDAO();
				Integer usuarioId = sessaoDAO.buscarSessao();
				if (usuarioId != null)
					usuario = Dao.buscarPorId(usuarioId);
			}

			if (usuario != null && usuario.getFotoPerfil() != null) {

				Image image = new Image(new ByteArrayInputStream(usuario.getFotoPerfil()));
				fotoPerfil.setFill(new ImagePattern(image));

			} else {
				Image imagemPadrao = new Image(getClass().getResource("/LoginInfinit/imagens/fotopadrao.jpg").toExternalForm());
				fotoPerfil.setFill(new ImagePattern(imagemPadrao));
			}

			Usuario finalUsuario = usuario;

			fotoStack.setOnMouseClicked(e -> {

				Window owner = getScene() != null ? getScene().getWindow() : null;

				FileChooser fileChooser = new FileChooser();
				fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg"));
				File file = fileChooser.showOpenDialog(owner);

				if (file != null && finalUsuario != null) {
					try {
						Image image = new Image(file.toURI().toString());
						fotoPerfil.setFill(new ImagePattern(image));

						byte[] imagemBytes = processarFoto(file, 260);

						new Dao().atualizarImagem(finalUsuario.getId(), imagemBytes);
						finalUsuario.setFotoPerfil(imagemBytes);

					} catch (Exception ex) {
						System.err.println("Falha ao atualizar imagem de perfil");
						ex.printStackTrace();
					}
				}
			});

			// Hover Animation
			fotoStack.setOnMouseEntered(ev -> {
				ScaleTransition st = new ScaleTransition(Duration.millis(200), fotoStack);
				st.setToX(1.07);
				st.setToY(1.07);
				st.play();
			});

			fotoStack.setOnMouseExited(ev -> {
				ScaleTransition st = new ScaleTransition(Duration.millis(200), fotoStack);
				st.setToX(1.0);
				st.setToY(1.0);
				st.play();
			});

		} catch (IOException e) {
			e.printStackTrace();
		}

		vbox.getChildren().addAll(fotoStack, NomeUsuario(), Botoes());
		return vbox;
	}

	// ============================
	// PROCESSAMENTO DA FOTO (Alta Qualidade)
	// ============================
	public static byte[] processarFoto(File file, int finalSize) throws Exception {

		BufferedImage original = ImageIO.read(file);

		int min = Math.min(original.getWidth(), original.getHeight());
		int x = (original.getWidth() - min) / 2;
		int y = (original.getHeight() - min) / 2;

		BufferedImage square = original.getSubimage(x, y, min, min);

		BufferedImage resized = new BufferedImage(finalSize, finalSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = resized.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

		g2.drawImage(square, 0, 0, finalSize, finalSize, null);
		g2.dispose();

		BufferedImage circle = new BufferedImage(finalSize, finalSize, BufferedImage.TYPE_INT_ARGB);
		g2 = circle.createGraphics();
		g2.setClip(new java.awt.geom.Ellipse2D.Double(0, 0, finalSize, finalSize));
		g2.drawImage(resized, 0, 0, null);
		g2.dispose();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(circle, "png", baos);

		return baos.toByteArray();
	}

	public Button getPrincipal() {
		return Principal;
	}

	public Button getAnotacoes() {
		return Anotacoes;
	}

	public Button getLancamentos() {
		return Lancamentos;
	}

	public Button getImportexport() {
		return Importexport;
	}

	public Button getSair() {
		return Sair;
	}

	public void setController(BarraController controller) {
		controller.configurarAcoes();
	}

}
