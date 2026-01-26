package MainInfinit;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import Apis.LogoFetcher;
import Apis.LogoKit;
import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO.SessaoTemp;
import ControllerInfinit.BarraController;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

public class Anotacoes extends BorderPane {

	private final String corFundo = "#212121";
	private BarraLateral barra;
	private BarraController barraController;

	private final WebView webView = new WebView();
	private WebEngine engine;
	private final AtomicReference<String> ultimoHtml = new AtomicReference<>("");
	private final Timeline autosavePoller = new Timeline();

	private volatile boolean aplicandoHtmlNoEditor = false;

	private final PauseTransition debounceSalvar = new PauseTransition(Duration.millis(800));
	@SuppressWarnings("resource")
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public Anotacoes() {

		// =========
		// Background
		// =========
		BackgroundFill fundo = new BackgroundFill(Color.web(corFundo), CornerRadii.EMPTY, Insets.EMPTY);
		setBackground(new Background(fundo));

		// ===================
		// Barra lateral
		// ===================
		try {
			barra = new BarraLateral();
			barraController = new BarraController(barra);
			barra.setController(barraController);

			this.setLeft(barra);
			barraController.selecionarBotao(barra.getAnotacoes(), "/LoginInfinit/imagens/lanBlack.png");
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// ===================
		// Editor rico (WebView)
		// ===================
		webView.setContextMenuEnabled(false);
		webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

		// ✅ Se perder foco (usuário clicou em outra tela/controle), força salvar
		webView.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
			if (wasFocused && !isFocused) {
				flushSalvarAgora();
			}
		});

		setCenter(webView);
		BorderPane.setMargin(webView, Insets.EMPTY);

		engine = webView.getEngine();
		engine.loadContent(templateHtmlBase(""));

		engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED) {
				conectarBridgeJs();
				carregarDoBanco();

				engine.getLoadWorker().stateProperty().addListener((obsWorker, stAnt, stNovo) -> {
					if (stNovo == Worker.State.SUCCEEDED) {
						conectarBridgeJs();
						carregarDoBanco();
						iniciarAutosavePoller(); // ✅
					}
				});
			}
		});

		// se a tela for removida, salva uma última vez e fecha thread
		this.sceneProperty().addListener((obs, oldScene, newScene) -> {

			// ✅ Quando esse componente sair do layout (troca de cena/tela), salva na hora
			this.parentProperty().addListener((pObs, oldParent, newParent) -> {
				if (newParent == null && oldParent != null) {
					autosavePoller.stop();
					flushSalvarAgora();
				}
			});

			if (newScene == null) {
				autosavePoller.stop();
				flushSalvarAgora();
				executor.shutdownNow();
				return;
			}

			// pega o Window e salva quando a janela fechar
			newScene.windowProperty().addListener((o, oldW, newW) -> {
				if (newW != null) {
					newW.setOnHiding(ev -> flushSalvarAgora());
				}
			});
		});

	}

	// =========================================================
	// HTML TEMPLATE
	// =========================================================
	private String templateHtmlBase(String conteudoHtml) {
		String body = (conteudoHtml == null) ? "" : conteudoHtml;

		return """
				<!DOCTYPE html>
				<html>
				<head>
				  <meta charset="UTF-8" />
				  <style>
				    html, body {
				      height: 100%;
				      margin: 0;
				      background: #212121;
				      color: #ffffff;
				      font-family: Arial, sans-serif;
				      font-size: 15px;
				    }

				    #editor {
				      height: 100%;
				      box-sizing: border-box;
				      padding: 16px;
				      outline: none;

				      /* ✅ importante no WebView */
				      white-space: normal;

				      overflow-wrap: anywhere;
				      word-break: break-word;

				      /* ✅ sem isso você digita “pra baixo” e parece que não criou linha */
				      overflow-y: auto;

				      /* opcional: melhora visual */
				      line-height: 1.35;
				    }

				    .chip {
				      display: inline-flex;
				      align-items: center;
				      padding: 0;
				      margin: 0 0px;
				      background: transparent;
				      border: none;
				      vertical-align: middle;
				    }

				    .chip img {
				      width: 50px;
				      height: 50px;
				      border-radius: 0px;
				      object-fit: cover;
				      background: transparent;
				    }
				  </style>
				</head>
				<body>
				  <div id="editor" contenteditable="true">""" + body + """
				  </div>

				  <script>
				    // ✅ força o “Enter” padrão virar <div> (quando o engine respeita)
				    try {
				      document.execCommand("defaultParagraphSeparator", false, "div");
				    } catch (e) {}

				    let isApplying = false;

				    function logoUrlForTicker(ticker) {
				      let t = (ticker || "").trim().toUpperCase();
				      if (/^[A-Z]{4}\\d{1,2}$/.test(t)) t = t + ".SA";
				      return "https://img.logokit.com/ticker/" + t
				        + "?token=pk_fre0d0771b214e45db3dbb"
				        + "&size=64"
				        + "&format=png"
				        + "&background=transparent"
				        + "&fallback=monogram";
				    }

				    function placeCaretAfter(node) {
				      const range = document.createRange();
				      range.setStartAfter(node);
				      range.collapse(true);
				      const sel = window.getSelection();
				      sel.removeAllRanges();
				      sel.addRange(range);
				    }

				    function getTextNodeAndCaret() {
				      const sel = window.getSelection();
				      if (!sel || sel.rangeCount === 0) return null;

				      const r = sel.getRangeAt(0);
				      let container = r.startContainer;
				      let offset = r.startOffset;

				      if (container && container.nodeType === Node.TEXT_NODE) {
				        return { node: container, caret: offset };
				      }

				      if (!container || container.nodeType !== Node.ELEMENT_NODE) return null;

				      let idx = offset - 1;
				      if (idx < 0) idx = container.childNodes.length - 1;
				      if (idx < 0) return null;

				      let node = container.childNodes[idx];

				      function lastText(n) {
				        if (!n) return null;
				        if (n.nodeType === Node.TEXT_NODE) return n;
				        if (n.childNodes && n.childNodes.length > 0) {
				          for (let i = n.childNodes.length - 1; i >= 0; i--) {
				            const t = lastText(n.childNodes[i]);
				            if (t) return t;
				          }
				        }
				        return null;
				      }

				      const textNode = lastText(node);
				      if (!textNode) return null;

				      return { node: textNode, caret: (textNode.nodeValue || "").length };
				    }

				    function transformAtTickerIfNeeded() {
				      const data = getTextNodeAndCaret();
				      if (!data) return;

				      const node = data.node;
				      const text = node.nodeValue || "";
				      const caret = data.caret;

				      const before = text.slice(0, caret);

				      const m = before.match(/@([A-Za-z]{4}\\d{1,2})\\s$/);
				      if (!m) return;

				      const raw = m[1];
				      const ticker = raw.toUpperCase();

				      const atToken = "@" + raw;
				      const startIndex = before.lastIndexOf(atToken);
				      if (startIndex < 0) return;

				      const prefix = text.slice(0, startIndex);
				      const suffix = text.slice(caret);

				      node.nodeValue = prefix;

				      const chip = document.createElement("span");
				      chip.className = "chip";
				      chip.setAttribute("data-ticker", ticker);
				      chip.title = ticker;

				      const img = document.createElement("img");

				      try {
				        if (window.bridge && window.bridge.getLogoDataUrl) {
				          const dataUrl = window.bridge.getLogoDataUrl(ticker);
				          img.src = (dataUrl && dataUrl.length > 0) ? dataUrl : logoUrlForTicker(ticker);
				        } else {
				          img.src = logoUrlForTicker(ticker);
				        }
				      } catch (e) {
				        img.src = logoUrlForTicker(ticker);
				      }

				      chip.appendChild(img);

				      const space = document.createTextNode(" ");
				      const after = document.createTextNode(suffix);

				      node.parentNode.insertBefore(chip, node.nextSibling);
				      node.parentNode.insertBefore(space, chip.nextSibling);
				      node.parentNode.insertBefore(after, space.nextSibling);

				      placeCaretAfter(space);
				    }

				    function notifyChange() {
				      if (isApplying) return;
				      if (window.bridge && window.bridge.onChange) {
				        window.bridge.onChange(document.getElementById("editor").innerHTML);
				      }
				    }

				    const editor = document.getElementById("editor");

				    let pending = false;

				    function scheduleNotify() {
				      if (pending) return;
				      pending = true;

				      setTimeout(() => {
				        pending = false;
				        transformAtTickerIfNeeded();
				        notifyChange();
				      }, 0);
				    }

				    // ✅ FIX DEFINITIVO: WebView às vezes não cria linha no Enter.
				    // Enter: nova linha (div)
				    // Shift+Enter: quebra simples (br)
				    editor.addEventListener("keydown", (e) => {
				      if (e.key === "Enter") {
				        e.preventDefault();

				        try {
				          if (e.shiftKey) {
				            document.execCommand("insertHTML", false, "<br>");
				          } else {
				            document.execCommand("insertHTML", false, "<div><br></div>");
				          }
				        } catch (err) {
				          // fallback bem simples
				          const br = document.createElement("br");
				          editor.appendChild(br);
				        }

				        scheduleNotify();
				      }
				    });

				    // input cobre quase tudo, mas não tudo
				    editor.addEventListener("input", scheduleNotify);

				    // esses eventos tapam buracos clássicos em contenteditable
				    editor.addEventListener("keyup", scheduleNotify);
				    editor.addEventListener("paste", scheduleNotify);
				    editor.addEventListener("cut", scheduleNotify);

				    // quando perde o foco, força salvar também
				    editor.addEventListener("blur", () => notifyChange());

				    window.setEditorHtml = (html) => {
				      isApplying = true;
				      editor.innerHTML = html || "";
				      isApplying = false;
				    };

				    window.getEditorHtml = () => editor.innerHTML;
				  </script>
				</body>
				</html>
				""";
	}

	// =========================================================
	// JS BRIDGE
	// =========================================================

	@SuppressWarnings({ "removal", "deprecation" })
	private void conectarBridgeJs() {
		JSObject window = (JSObject) engine.executeScript("window");
		window.setMember("bridge", new Bridge());
	}

	public class Bridge {
		public void onChange(String html) {
			if (aplicandoHtmlNoEditor)
				return;

			ultimoHtml.set(html == null ? "" : html);

			Platform.runLater(() -> {
				debounceSalvar.stop();
				debounceSalvar.setOnFinished(e -> salvarNoBanco(ultimoHtml.get()));
				debounceSalvar.playFromStart();
			});
		}

		public String getLogoDataUrl(String ticker) {
			return gerarLogoDataUrl(ticker);
		}
	}

	private void setHtmlNoEditor(String html) {
		aplicandoHtmlNoEditor = true;
		try {
			String safeJson = toJsonString(html == null ? "" : html);
			engine.executeScript("window.setEditorHtml(" + safeJson + ");");
		} finally {
			aplicandoHtmlNoEditor = false;
		}
	}

	@SuppressWarnings("unused")
	private String pegarHtmlDoEditor() {
		try {
			Object out = engine.executeScript("window.getEditorHtml()");
			return (out == null) ? "" : out.toString();
		} catch (Exception e) {
			return "";
		}
	}

	// =========================================================
	// BANCO
	// =========================================================
	private void carregarDoBanco() {
		Integer usuarioId = SessaoTemp.getUsuarioId();
		if (usuarioId == null)
			return;

		executor.submit(() -> {
			try {
				Dao dao = new Dao();
				String html = dao.carregarAnotacoes(usuarioId);
				boolean vazio = (html == null || html.trim().isEmpty());

				if (vazio) {
					String padrao = htmlMensagemPadrao();

					Platform.runLater(() -> setHtmlNoEditor(padrao));
					salvarNoBanco(padrao);
				} else {
					Platform.runLater(() -> setHtmlNoEditor(html));
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void salvarNoBanco(String html) {
		Integer usuarioId = SessaoTemp.getUsuarioId();
		if (usuarioId == null)
			return;

		final String snapshot = (html == null) ? "" : html;

		executor.submit(() -> {
			try {
				Dao dao = new Dao();
				dao.salvarAnotacoes(usuarioId, snapshot);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	// =========================================================
	// JSON escape para injetar HTML com segurança no JS
	// =========================================================
	private static String toJsonString(String s) {
		if (s == null)
			return "\"\"";

		StringBuilder sb = new StringBuilder(s.length() + 16);
		sb.append('\"');
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '\\' -> sb.append("\\\\");
			case '"' -> sb.append("\\\"");
			case '\n' -> sb.append("\\n");
			case '\r' -> sb.append("\\r");
			case '\t' -> sb.append("\\t");
			case '\b' -> sb.append("\\b");
			case '\f' -> sb.append("\\f");
			default -> {
				if (c < 32)
					sb.append(String.format("\\u%04x", (int) c));
				else
					sb.append(c);
			}
			}
		}
		sb.append('\"');
		return sb.toString();
	}

	// =========================================================
	// IMAGEM: trim + crop + remove branco
	// =========================================================
	private static Image trimBordasBrancas(Image img) {
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int w = (int) img.getWidth();
		int h = (int) img.getHeight();

		double white = 0.50;
		double alphaMin = 0.05;

		int top = 0, bottom = h - 1, left = 0, right = w - 1;

		topLoop: for (; top < h; top++) {
			for (int x = 0; x < w; x++) {
				Color c = pr.getColor(x, top);
				if (!ehQuaseBranco(c, white, alphaMin))
					break topLoop;
			}
		}

		bottomLoop: for (; bottom >= top; bottom--) {
			for (int x = 0; x < w; x++) {
				Color c = pr.getColor(x, bottom);
				if (!ehQuaseBranco(c, white, alphaMin))
					break bottomLoop;
			}
		}

		leftLoop: for (; left < w; left++) {
			for (int y = top; y <= bottom; y++) {
				Color c = pr.getColor(left, y);
				if (!ehQuaseBranco(c, white, alphaMin))
					break leftLoop;
			}
		}

		rightLoop: for (; right >= left; right--) {
			for (int y = top; y <= bottom; y++) {
				Color c = pr.getColor(right, y);
				if (!ehQuaseBranco(c, white, alphaMin))
					break rightLoop;
			}
		}

		int newW = right - left + 1;
		int newH = bottom - top + 1;

		if (newW <= 5 || newH <= 5)
			return img;

		return new WritableImage(pr, left, top, newW, newH);
	}

	private static Image cropQuadradoCentral(Image img) {
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int w = (int) img.getWidth();
		int h = (int) img.getHeight();
		if (w <= 0 || h <= 0)
			return img;

		int side = Math.min(w, h);

		int x = (w - side) / 2;
		int y = (h - side) / 2;

		return new WritableImage(pr, x, y, side, side);
	}

	private static Image tornarBrancoTransparente(Image img) {
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int w = (int) img.getWidth();
		int h = (int) img.getHeight();
		WritableImage out = new WritableImage(w, h);
		var pw = out.getPixelWriter();

		double white = 0.85;
		double alphaMin = 0.03;

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				Color c = pr.getColor(x, y);

				boolean quaseBranco = c.getOpacity() >= alphaMin && c.getRed() >= white && c.getGreen() >= white && c.getBlue() >= white;

				if (quaseBranco)
					pw.setColor(x, y, new Color(c.getRed(), c.getGreen(), c.getBlue(), 0.0));
				else
					pw.setColor(x, y, c);
			}
		}
		return out;
	}

	private static boolean ehQuaseBranco(Color c, double white, double alphaMin) {
		if (c.getOpacity() < alphaMin)
			return true;
		return c.getRed() >= white && c.getGreen() >= white && c.getBlue() >= white;
	}

	private static Image trimPorFundo(Image img) {
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int w = (int) img.getWidth();
		int h = (int) img.getHeight();
		if (w <= 0 || h <= 0)
			return img;

		Color c1 = pr.getColor(0, 0);
		Color c2 = pr.getColor(w - 1, 0);
		Color c3 = pr.getColor(0, h - 1);
		Color c4 = pr.getColor(w - 1, h - 1);

		Color bg = new Color((c1.getRed() + c2.getRed() + c3.getRed() + c4.getRed()) / 4.0, (c1.getGreen() + c2.getGreen() + c3.getGreen() + c4.getGreen()) / 4.0,
				(c1.getBlue() + c2.getBlue() + c3.getBlue() + c4.getBlue()) / 4.0, (c1.getOpacity() + c2.getOpacity() + c3.getOpacity() + c4.getOpacity()) / 4.0);

		double alphaMin = 0.10;
		double distMax = 0.08;

		int top = 0, bottom = h - 1, left = 0, right = w - 1;

		topLoop: for (; top < h; top++) {
			for (int x = 0; x < w; x++) {
				Color c = pr.getColor(x, top);
				if (!ehFundo(c, bg, alphaMin, distMax))
					break topLoop;
			}
		}

		bottomLoop: for (; bottom >= top; bottom--) {
			for (int x = 0; x < w; x++) {
				Color c = pr.getColor(x, bottom);
				if (!ehFundo(c, bg, alphaMin, distMax))
					break bottomLoop;
			}
		}

		leftLoop: for (; left < w; left++) {
			for (int y = top; y <= bottom; y++) {
				Color c = pr.getColor(left, y);
				if (!ehFundo(c, bg, alphaMin, distMax))
					break leftLoop;
			}
		}

		rightLoop: for (; right >= left; right--) {
			for (int y = top; y <= bottom; y++) {
				Color c = pr.getColor(right, y);
				if (!ehFundo(c, bg, alphaMin, distMax))
					break rightLoop;
			}
		}

		int newW = right - left + 1;
		int newH = bottom - top + 1;

		if (newW <= 5 || newH <= 5)
			return img;

		return new WritableImage(pr, left, top, newW, newH);
	}

	private static boolean ehFundo(Color c, Color bg, double alphaMin, double distMax) {
		if (c.getOpacity() < alphaMin)
			return true;

		double dr = c.getRed() - bg.getRed();
		double dg = c.getGreen() - bg.getGreen();
		double db = c.getBlue() - bg.getBlue();
		double dist = Math.sqrt(dr * dr + dg * dg + db * db);

		return dist < distMax;
	}

	private String gerarLogoDataUrl(String ticker) {
		try {
			String url = LogoKit.byStockTicker(ticker);
			Image img = LogoFetcher.loadImage(url);
			if (img == null)
				return "";

			Image trimmed = trimPorFundo(img);
			Image cropped = cropQuadradoCentral(trimmed);
			Image semBranco = tornarBrancoTransparente(cropped);
			Image finalImg = trimPorFundo(semBranco);

			var buffered = SwingFXUtils.fromFXImage(finalImg, null);

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(buffered, "png", baos);

			String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
			return "data:image/png;base64," + base64;
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	private String htmlMensagemPadrao() {
		String bbas3 = gerarLogoDataUrl("BBAS3");

		String src = (bbas3 != null && !bbas3.isBlank()) ? bbas3
				: "https://img.logokit.com/ticker/BBAS3.SA?token=pk_fre0d0771b214e45db3dbb&size=64&format=png&background=transparent&fallback=monogram";

		return """
				<div>
				  <span class="chip" data-ticker="BBAS3" title="BBAS3">
				    <img src="%s" />
				  </span>
				  Para você conseguir usar as logos nas anotações digite <b>@(A sigla do ativo)</b> — exemplo: <b>@bbas3</b>
				</div>
				<div><br/></div>
				""".formatted(src);
	}

	private void flushSalvarAgora() {
		try {
			debounceSalvar.stop();

			final String html = ultimoHtml.get();

			String htmlNow = pegarHtmlDoEditor();
			if (htmlNow != null) {
				htmlNow = htmlNow.trim();
			}
			final String htmlFinal = (htmlNow != null && !htmlNow.isEmpty()) ? htmlNow : html;

			ultimoHtml.set(htmlFinal);

			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			executor.submit(() -> {
				Dao dao = new Dao();
				dao.salvarAnotacoes(usuarioId, htmlFinal == null ? "" : htmlFinal);
			}).get();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void iniciarAutosavePoller() {
		autosavePoller.stop();
		autosavePoller.getKeyFrames().setAll(new KeyFrame(Duration.millis(500), e -> {
			if (aplicandoHtmlNoEditor)
				return;

			String atual = pegarHtmlDoEditor();
			if (atual == null)
				atual = "";

			String ultimo = ultimoHtml.get();
			if (ultimo == null)
				ultimo = "";

			if (!atual.equals(ultimo)) {
				ultimoHtml.set(atual);

				debounceSalvar.stop();
				debounceSalvar.setOnFinished(ev -> salvarNoBanco(ultimoHtml.get()));
				debounceSalvar.playFromStart();
			}
		}));
		autosavePoller.setCycleCount(Timeline.INDEFINITE);
		autosavePoller.play();
	}

}
