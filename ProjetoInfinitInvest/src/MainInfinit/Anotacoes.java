package MainInfinit;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

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
	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	public Anotacoes() {

		setBackground(new Background(new BackgroundFill(Color.web(corFundo), CornerRadii.EMPTY, Insets.EMPTY)));

		try {
			barra = new BarraLateral();
			barraController = new BarraController(barra);
			barra.setController(barraController);

			this.setLeft(barra);
			barraController.selecionarBotao(barra.getAnotacoes(), "/LoginInfinit/imagens/lanBlack.png");
		} catch (Exception e) {
			e.printStackTrace();
		}

		webView.setContextMenuEnabled(false);
		webView.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

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
				iniciarAutosavePoller();
			}
		});

		this.sceneProperty().addListener((obs, oldScene, newScene) -> {

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

			newScene.windowProperty().addListener((o, oldW, newW) -> {
				if (newW != null) {
					newW.setOnHiding(ev -> flushSalvarAgora());
				}
			});
		});
	}

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
				      white-space: normal;
				      overflow-wrap: anywhere;
				      word-break: break-word;
				      overflow-y: auto;
				      line-height: 1.35;
				    }

				    /* garante bloco com espaçamento bom */
				    #editor div {
				      min-height: 1.35em;
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

				    function placeCaretInsideStart(el) {
				      const range = document.createRange();
				      range.selectNodeContents(el);
				      range.collapse(true);
				      const sel = window.getSelection();
				      sel.removeAllRanges();
				      sel.addRange(range);
				    }

				    function tryExecCommand(cmd, value) {
				      try {
				        // algumas builds retornam false sem lançar exception
				        return document.execCommand(cmd, false, value);
				      } catch (e) {
				        return false;
				      }
				    }

				    function manualInsertBr(editor) {
				      const sel = window.getSelection();
				      if (!sel || sel.rangeCount === 0) {
				        // último recurso
				        editor.appendChild(document.createElement("br"));
				        return;
				      }

				      const range = sel.getRangeAt(0);
				      range.deleteContents();

				      const br = document.createElement("br");
				      range.insertNode(br);

				      // move caret depois do <br>
				      range.setStartAfter(br);
				      range.collapse(true);
				      sel.removeAllRanges();
				      sel.addRange(range);
				    }

				    function manualInsertParagraph(editor) {
				      const sel = window.getSelection();
				      if (!sel || sel.rangeCount === 0) {
				        const div = document.createElement("div");
				        div.appendChild(document.createElement("br"));
				        editor.appendChild(div);
				        placeCaretInsideStart(div);
				        return;
				      }

				      const range = sel.getRangeAt(0);
				      range.deleteContents();

				      const div = document.createElement("div");
				      div.appendChild(document.createElement("br"));

				      // insere um bloco e coloca o caret dentro
				      range.insertNode(div);
				      placeCaretInsideStart(div);
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
				          img.src = dataUrl || logoUrlForTicker(ticker);
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

				    // ✅ ENTER garantido (com fallback manual)
				    editor.addEventListener("keydown", (e) => {
				      if (e.key === "Enter") {
				        e.preventDefault();

				        if (e.shiftKey) {
				          // Shift+Enter = quebra simples
				          const ok = tryExecCommand("insertLineBreak");
				          if (!ok) manualInsertBr(editor);
				        } else {
				          // Enter = novo parágrafo / nova linha “de verdade”
				          const ok = tryExecCommand("insertParagraph");
				          if (!ok) manualInsertParagraph(editor);
				        }

				        scheduleNotify();
				      }
				    });

				    editor.addEventListener("input", scheduleNotify);
				    editor.addEventListener("keyup", scheduleNotify);
				    editor.addEventListener("paste", scheduleNotify);
				    editor.addEventListener("cut", scheduleNotify);
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

	@SuppressWarnings("removal")
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

	private String pegarHtmlDoEditor() {
		try {
			Object out = engine.executeScript("window.getEditorHtml()");
			return (out == null) ? "" : out.toString();
		} catch (Exception e) {
			return "";
		}
	}

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

	// =========================
	// LOGO: carregar + zoom/crop + remover halo branco
	// =========================

	private static Image loadImageSync(String url) {
		Image img = new Image(url, 0, 0, true, true, false);

		if (img.getProgress() >= 1.0 && !img.isError() && img.getWidth() > 0 && img.getHeight() > 0) {
			return img;
		}

		CountDownLatch latch = new CountDownLatch(1);

		img.progressProperty().addListener((o, a, b) -> {
			if (b != null && b.doubleValue() >= 1.0)
				latch.countDown();
		});
		img.errorProperty().addListener((o, a, b) -> {
			if (b != null && b.booleanValue())
				latch.countDown();
		});

		try {
			latch.await(4, TimeUnit.SECONDS);
		} catch (InterruptedException ignored) {
			Thread.currentThread().interrupt();
		}

		if (img.isError() || img.getWidth() <= 0 || img.getHeight() <= 0)
			return null;
		return img;
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

	private static Image zoomCropCentral(Image img, double zoom) {
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int w = (int) img.getWidth();
		int h = (int) img.getHeight();
		if (w <= 0 || h <= 0 || zoom <= 1.0)
			return img;

		int vw = (int) Math.round(w / zoom);
		int vh = (int) Math.round(h / zoom);

		vw = Math.max(8, Math.min(vw, w));
		vh = Math.max(8, Math.min(vh, h));

		int x = (w - vw) / 2;
		int y = (h - vh) / 2;

		return new WritableImage(pr, x, y, vw, vh);
	}

	private static Image removeWhiteMatteFringe(Image img) {
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int w = (int) img.getWidth();
		int h = (int) img.getHeight();
		if (w <= 0 || h <= 0)
			return img;

		WritableImage out = new WritableImage(w, h);
		var pw = out.getPixelWriter();

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				Color c = pr.getColor(x, y);
				double a = c.getOpacity();

				if (a <= 0.0001) {
					pw.setColor(x, y, Color.TRANSPARENT);
					continue;
				}

				if (a < 0.999) {
					double r = (c.getRed() - (1.0 - a)) / a;
					double g = (c.getGreen() - (1.0 - a)) / a;
					double b = (c.getBlue() - (1.0 - a)) / a;

					r = clamp01(r);
					g = clamp01(g);
					b = clamp01(b);

					pw.setColor(x, y, new Color(r, g, b, a));
				} else {
					pw.setColor(x, y, c);
				}
			}
		}

		return out;
	}

	private static double clamp01(double v) {
		if (v < 0)
			return 0;
		if (v > 1)
			return 1;
		return v;
	}

	private static Image trimTransparente(Image img) {
		PixelReader pr = img.getPixelReader();
		if (pr == null)
			return img;

		int w = (int) img.getWidth();
		int h = (int) img.getHeight();
		if (w <= 0 || h <= 0)
			return img;

		double alphaMin = 0.02;

		int top = 0, bottom = h - 1, left = 0, right = w - 1;

		topLoop: for (; top < h; top++) {
			for (int x = 0; x < w; x++) {
				if (pr.getColor(x, top).getOpacity() > alphaMin)
					break topLoop;
			}
		}

		bottomLoop: for (; bottom >= top; bottom--) {
			for (int x = 0; x < w; x++) {
				if (pr.getColor(x, bottom).getOpacity() > alphaMin)
					break bottomLoop;
			}
		}

		leftLoop: for (; left < w; left++) {
			for (int y = top; y <= bottom; y++) {
				if (pr.getColor(left, y).getOpacity() > alphaMin)
					break leftLoop;
			}
		}

		rightLoop: for (; right >= left; right--) {
			for (int y = top; y <= bottom; y++) {
				if (pr.getColor(right, y).getOpacity() > alphaMin)
					break rightLoop;
			}
		}

		int newW = right - left + 1;
		int newH = bottom - top + 1;

		if (newW <= 5 || newH <= 5)
			return img;

		return new WritableImage(pr, left, top, newW, newH);
	}

	private String gerarLogoDataUrl(String ticker) {
		try {
			String url = LogoKit.byStockTicker(ticker);

			Image img = loadImageSync(url);
			if (img == null)
				return "";

			final double zoom = 2.75;

			Image zoomed = zoomCropCentral(img, zoom);
			Image squared = cropQuadradoCentral(zoomed);
			Image defringed = removeWhiteMatteFringe(squared);
			Image trimmed = trimTransparente(defringed);

			var buffered = SwingFXUtils.fromFXImage(trimmed, null);

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
			if (htmlNow != null)
				htmlNow = htmlNow.trim();

			final String htmlFinal = (htmlNow != null && !htmlNow.isEmpty()) ? htmlNow : html;
			ultimoHtml.set(htmlFinal);

			Integer usuarioId = SessaoTemp.getUsuarioId();
			if (usuarioId == null)
				return;

			executor.submit(() -> {
				try {
					Dao dao = new Dao();
					dao.salvarAnotacoes(usuarioId, htmlFinal == null ? "" : htmlFinal);
				} catch (Exception e) {
					e.printStackTrace();
				}
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
