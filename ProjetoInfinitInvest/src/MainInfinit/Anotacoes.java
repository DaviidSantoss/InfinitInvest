package MainInfinit;

import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO.SessaoTemp;
import ControllerInfinit.BarraController;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.util.Duration;


public class Anotacoes extends BorderPane {

	private String corFundo = "#212121";
	private BarraLateral barra;
	private BarraController barraController;
	private final TextArea area = new TextArea();
	private final PauseTransition debounceSalvar = new PauseTransition(Duration.millis(800));
	private final ExecutorService executor = Executors.newSingleThreadExecutor();


	// =============================
	// Clase que irá conter as anotações do usuário.
	// =============================
	public Anotacoes() {


		// =========
		// BackGround.
		// =========
		BackgroundFill fundo = new BackgroundFill(Color.web(corFundo), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(fundo));

		// ===================
		// Cria a barra dentro da classe.
		// ===================
		try {
			barra = new BarraLateral();
			barraController = new BarraController(barra);
			barra.setController(barraController);

			this.setLeft(barra);
			barraController.selecionarBotao(barra.getAnotacoes(), "/LoginInfinit/imagens/lanBlack.png");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// ===================
		// Bloco de notas
		// ===================

		// aparência
		area.setWrapText(true);
		area.setStyle("""
				    -fx-control-inner-background: #1f1f1f;
				    -fx-text-fill: white;
				    -fx-font-size: 14px;
				""");
		area.setPromptText("Escreva suas anotações aqui... (salva automaticamente)");
		area.setPadding(new Insets(12));

		// ===================
		// Bloco de notas ocupa toda a tela
		// ===================

		area.setWrapText(true);
		area.setStyle("""
				    -fx-control-inner-background: #1f1f1f;
				    -fx-text-fill: white;
				    -fx-font-size: 15px;
				    -fx-background-insets: 0;
				    -fx-padding: 16;
				""");

		area.setPromptText("Escreva suas anotações aqui... (salva automaticamente)");

		// 🔥 ESSENCIAL: permite crescimento total
		area.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		// 🔥 coloca direto no centro do BorderPane
		setCenter(area);

		// garante que o BorderPane use todo o espaço
		BorderPane.setMargin(area, Insets.EMPTY);


		// carrega do banco assim que a tela nasce
		carregarDoBanco();

		// auto-save com debounce
		area.textProperty().addListener((obs, oldVal, newVal) -> {
			debounceSalvar.stop();
			debounceSalvar.setOnFinished(e -> salvarNoBanco(newVal));
			debounceSalvar.playFromStart();
		});

		// se a tela for removida, salva e fecha thread
		this.sceneProperty().addListener((obs, oldScene, newScene) -> {
			if (newScene == null) {
				try {
					// salva uma última vez
					salvarNoBanco(area.getText());
				} catch (Exception ignored) {
				}

				executor.shutdownNow();
			}
		});

	}

	private void carregarDoBanco() {
		Integer usuarioId = SessaoTemp.getUsuarioId();
		if (usuarioId == null)
			return;

		executor.submit(() -> {
			try {
				Dao dao = new Dao();
				String txt = dao.carregarAnotacoes(usuarioId);

				Platform.runLater(() -> area.setText(txt));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void salvarNoBanco(String texto) {
		Integer usuarioId = SessaoTemp.getUsuarioId();
		if (usuarioId == null)
			return;

		final String snapshot = (texto == null) ? "" : texto;

		executor.submit(() -> {
			try {
				Dao dao = new Dao();
				dao.salvarAnotacoes(usuarioId, snapshot);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}


}
