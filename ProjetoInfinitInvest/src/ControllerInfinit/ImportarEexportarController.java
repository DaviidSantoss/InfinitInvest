package ControllerInfinit;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import BancoInfinit.Ativo;
import BancoInfinit.Conexao;
import BancoInfinit.Dao;
import BancoInfinit.SessaoDAO.SessaoTemp;
import BancoInfinit.Usuario;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

public class ImportarEexportarController {

	// Formato simples e robusto (TSV dentro de ZIP)
	private static final String EXT = "*.infinit";
	private static final String DEFAULT_NAME_PREFIX = "backup_infinit_";
	private static final String ENTRY_META = "meta.tsv";
	private static final String ENTRY_ATIVOS = "ativos.tsv";
	private static final String ENTRY_LANC = "lancamentos.tsv";
	private static final String ENTRY_ANOT = "anotacoes.txt";
	private static final String ENTRY_FOTO = "foto.png";

	// =========================
	// API pública (chamar de qualquer tela)
	// =========================
	public void abrirPopup(Window owner) {
		Stage stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		if (owner != null)
			stage.initOwner(owner);
		stage.setTitle("Import / Export");

		Label title = new Label("Import / Export");
		title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

		Label desc = new Label("Export: gera um arquivo com seus dados (ativos, lançamentos, anotações e foto).\n" + "Import: aplica um arquivo desses no usuário logado (sem mexer em email/senha).");
		desc.setStyle("-fx-text-fill: #d0d0d0; -fx-font-size: 12px;");

		Button btnExport = new Button("Export");
		Button btnImport = new Button("Import");

		btnExport.setPrefWidth(140);
		btnImport.setPrefWidth(140);

		btnExport.setOnAction(e -> exportar(owner));
		btnImport.setOnAction(e -> importar(owner));

		HBox buttons = new HBox(12, btnImport, btnExport);
		buttons.setAlignment(Pos.CENTER);

		VBox root = new VBox(14, title, desc, buttons);
		root.setAlignment(Pos.CENTER);
		root.setPadding(new Insets(18));
		root.setStyle("-fx-background-color: #212121; -fx-border-color: #333; -fx-border-radius: 10; -fx-background-radius: 10;");

		Scene scene = new Scene(root, 420, 170);
		stage.setScene(scene);
		stage.showAndWait();
	}

	// =========================
	// EXPORT
	// =========================
	private void exportar(Window owner) {
		Integer usuarioId = SessaoTemp.getUsuarioId();
		if (usuarioId == null) {
			alert(AlertType.ERROR, "Nenhum usuário logado", "Faça login antes de exportar.");
			return;
		}

		Usuario u;
		try {
			u = Dao.buscarPorId(usuarioId);
		} catch (Exception ex) {
			ex.printStackTrace();
			alert(AlertType.ERROR, "Falha ao obter usuário", "Não consegui carregar o usuário logado.");
			return;
		}

		if (u == null) {
			alert(AlertType.ERROR, "Usuário não encontrado", "Não consegui carregar o usuário logado.");
			return;
		}

		FileChooser fc = new FileChooser();
		fc.setTitle("Salvar backup");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup Infinit (" + EXT + ")", EXT));

		String safeName = DEFAULT_NAME_PREFIX + usuarioId + "_" + LocalDateTime.now().toString().replace(":", "-") + ".infinit";
		fc.setInitialFileName(safeName);

		File out = (owner != null) ? fc.showSaveDialog(owner) : fc.showSaveDialog(null);
		if (out == null)
			return;

		try {
			Dao dao = new Dao();

			List<Ativo> ativos = Dao.listarAtivosPorUsuario(usuarioId);
			List<Dao.Lancamento> lancs = dao.listarLancamentos(usuarioId);
			String anotacoes = dao.carregarAnotacoes(usuarioId);
			byte[] foto = dao.getImagem(usuarioId); // pode ser null

			try (OutputStream fos = java.nio.file.Files.newOutputStream(out.toPath()); ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {

				// META
				writeEntry(zos, ENTRY_META, w -> {
					w.write("version\t1\n");
					w.write("exported_at\t" + LocalDateTime.now() + "\n");
					w.write("usuario_id_origem\t" + usuarioId + "\n"); // só informativo
					w.write("nome\t" + safe(u.getNome()) + "\n");
				});

				// ATIVOS (TSV)
				writeEntry(zos, ENTRY_ATIVOS, w -> {
					w.write(String.join("\t", "categoria", "ativo", "iconUrl", "quantidade", "preco_medio", "preco_atual", "variacao", "saldo", "td_indexador", "td_taxa_anual", "td_principal",
							"td_ultimo_dia", "rf_indexador", "rf_forma", "rf_taxa_anual", "rf_principal", "rf_ultimo_dia", "rf_percent_indexador", "rf_spread_anual"));
					w.write("\n");

					for (Ativo a : ativos) {
						w.write(String.join("\t", safe(a.getCategoria()), safe(a.getAtivo()), safe(a.getIconUrl()),

								d(a.getQuantidade()), d(a.getPrecoMedio()), d(a.getPrecoAtual()), d(a.getVariacao()), d(a.getSaldo()),

								safe(a.getTdIndexador()), d(a.getTdTaxaAnual()), d(a.getTdPrincipal()), safe(a.getTdUltimoDia()),

								safe(a.getRfIndexador()), safe(a.getRfForma()), d(a.getRfTaxaAnual()), d(a.getRfPrincipal()), safe(a.getRfUltimoDia()),

								d(a.getRfPercentIndexador()), d(a.getRfSpreadAnual())));
						w.write("\n");
					}
				});

				// LANCAMENTOS (TSV)
				writeEntry(zos, ENTRY_LANC, w -> {
					w.write(String.join("\t", "categoria", "ativo", "quantidade", "preco_unit", "data_lancamento_iso"));
					w.write("\n");
					for (Dao.Lancamento l : lancs) {
						w.write(String.join("\t", safe(l.categoria), safe(l.ativo), d(l.quantidade), d(l.precoUnitario), safe(l.dataLancamentoIso)));
						w.write("\n");
					}
				});

				// ANOTAÇÕES (TXT)
				writeEntryText(zos, ENTRY_ANOT, anotacoes);

				// FOTO (PNG) opcional
				if (foto != null && foto.length > 0) {
					writeEntry(zos, ENTRY_FOTO, foto);
				}

				zos.finish(); // opcional (legal)
			}

			alert(AlertType.INFORMATION, "Export concluído ✅", "Backup salvo em:\n" + out.getAbsolutePath());

		} catch (Exception ex) {
			ex.printStackTrace();
			alert(AlertType.ERROR, "Falha ao exportar", "Ocorreu um erro ao gerar o backup.");
		}
	}

	// =========================
	// IMPORT
	// =========================
	private void importar(Window owner) {
		Integer usuarioId = SessaoTemp.getUsuarioId();
		if (usuarioId == null) {
			alert(AlertType.ERROR, "Nenhum usuário logado", "Faça login antes de importar.");
			return;
		}

		FileChooser fc = new FileChooser();
		fc.setTitle("Selecionar backup");
		fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Backup Infinit (" + EXT + ")", EXT));
		File in = (owner != null) ? fc.showOpenDialog(owner) : fc.showOpenDialog(null);
		if (in == null)
			return;

		// Confirma sobrescrita
		ButtonType ok = new ButtonType("Aplicar (sobrescrever)", ButtonData.OK_DONE);
		ButtonType cancel = new ButtonType("Cancelar", ButtonData.CANCEL_CLOSE);
		Alert confirm = new Alert(AlertType.WARNING, "Isso vai substituir os dados do usuário logado (ativos, lançamentos e anotações).\n" + "Email/senha NÃO serão alterados.\n\nContinuar?", ok,
				cancel);
		confirm.setTitle("Confirmar Import");
		var res = confirm.showAndWait();
		if (res.isEmpty() || res.get() != ok)
			return;

		try (ZipFile zip = new ZipFile(in, StandardCharsets.UTF_8)) {

			Map<String, String> meta = readMeta(zip);

			List<String[]> ativosRows = readTsv(zip, ENTRY_ATIVOS);
			List<String[]> lancRows = readTsv(zip, ENTRY_LANC);
			String anotacoes = readText(zip, ENTRY_ANOT);
			byte[] foto = readBytes(zip, ENTRY_FOTO); // pode ser null

			Dao dao = new Dao();

			// Atualiza nome (se tiver)
			String nomeBackup = meta.getOrDefault("nome", "");
			if (!nomeBackup.isBlank()) {
				dao.updateNome(usuarioId, nomeBackup);
			}

			// Atualiza foto (se tiver)
			if (foto != null && foto.length > 0) {
				dao.atualizarImagem(usuarioId, foto);
			}

			// 1) limpa dados do usuário atual
			limparDadosUsuario(usuarioId);

			// 2) insere tudo do backup
			inserirAtivosDoBackup(usuarioId, ativosRows);
			inserirLancamentosDoBackup(usuarioId, lancRows);

			// 3) anotações
			dao.salvarAnotacoes(usuarioId, anotacoes == null ? "" : anotacoes);

			alert(AlertType.INFORMATION, "Import concluído ✅", "Dados aplicados no usuário logado com sucesso.");

		} catch (Exception ex) {
			ex.printStackTrace();
			alert(AlertType.ERROR, "Falha ao importar", "Backup inválido ou erro ao aplicar os dados.");
		}
	}

	// =========================
	// DB helpers
	// =========================
	private void limparDadosUsuario(int usuarioId) throws Exception {
		try (Connection conn = Conexao.getInstance().getConnection()) {

			try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lancamentos WHERE usuario_id = ?")) {
				ps.setInt(1, usuarioId);
				ps.executeUpdate();
			}

			try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ativos WHERE usuario_id = ?")) {
				ps.setInt(1, usuarioId);
				ps.executeUpdate();
			}

			try (PreparedStatement ps = conn.prepareStatement("DELETE FROM anotacoes WHERE usuario_id = ?")) {
				ps.setInt(1, usuarioId);
				ps.executeUpdate();
			}
		}
	}

	private void inserirAtivosDoBackup(int usuarioId, List<String[]> rows) throws Exception {
		if (rows == null || rows.size() <= 1)
			return;

		for (int i = 1; i < rows.size(); i++) {
			String[] r = rows.get(i);
			if (r.length < 19)
				continue;

			String categoria = r[0];
			String ativo = r[1];
			String iconUrl = r[2];

			double quantidade = pd(r[3]);
			double precoMedio = pd(r[4]);
			double precoAtual = pd(r[5]);
			double variacao = pd(r[6]);
			double saldo = pd(r[7]);

			String tdIndexador = r[8];
			double tdTaxaAnual = pd(r[9]);
			double tdPrincipal = pd(r[10]);
			String tdUltimoDia = r[11];

			String rfIndexador = r[12];
			String rfForma = r[13];
			double rfTaxaAnual = pd(r[14]);
			double rfPrincipal = pd(r[15]);
			String rfUltimoDia = r[16];

			double rfPercentIndexador = pd(r[17]);
			double rfSpreadAnual = pd(r[18]);

			inserirAtivoCompletoViaSQL(usuarioId, categoria, ativo, iconUrl, quantidade, precoMedio, precoAtual, variacao, saldo, tdIndexador, tdTaxaAnual, tdPrincipal, tdUltimoDia, rfIndexador,
					rfForma, rfTaxaAnual, rfPrincipal, rfUltimoDia, rfPercentIndexador, rfSpreadAnual);
		}
	}

	private void inserirAtivoCompletoViaSQL(int usuarioId, String categoria, String ativo, String iconUrl, double quantidade, double precoMedio, double precoAtual, double variacao, double saldo,
			String tdIndexador, double tdTaxaAnual, double tdPrincipal, String tdUltimoDia, String rfIndexador, String rfForma, double rfTaxaAnual, double rfPrincipal, String rfUltimoDia,
			double rfPercentIndexador, double rfSpreadAnual) throws Exception {

		String sql = """
				    INSERT INTO ativos (
				        usuario_id, categoria, ativo, iconUrl,
				        quantidade, preco_medio, preco_atual, variacao, saldo,
				        td_indexador, td_taxa_anual, td_principal, td_ultimo_dia,
				        rf_indexador, rf_forma, rf_taxa_anual, rf_principal, rf_ultimo_dia,
				        rf_percent_indexador, rf_spread_anual
				    ) VALUES (
				        ?, ?, ?, ?,
				        ?, ?, ?, ?, ?,
				        ?, ?, ?, ?,
				        ?, ?, ?, ?, ?,
				        ?, ?
				    )
				""";

		try (Connection conn = Conexao.getInstance().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

			int k = 1;
			ps.setInt(k++, usuarioId);
			ps.setString(k++, nullToEmpty(categoria));
			ps.setString(k++, nullToEmpty(ativo));
			ps.setString(k++, emptyToNull(iconUrl));

			ps.setDouble(k++, quantidade);
			ps.setDouble(k++, precoMedio);
			ps.setDouble(k++, precoAtual);
			ps.setDouble(k++, variacao);
			ps.setDouble(k++, saldo);

			ps.setString(k++, emptyToNull(tdIndexador));
			ps.setDouble(k++, tdTaxaAnual);
			ps.setDouble(k++, tdPrincipal);
			ps.setString(k++, emptyToNull(tdUltimoDia));

			ps.setString(k++, emptyToNull(rfIndexador));
			ps.setString(k++, emptyToNull(rfForma));
			ps.setDouble(k++, rfTaxaAnual);
			ps.setDouble(k++, rfPrincipal);
			ps.setString(k++, emptyToNull(rfUltimoDia));

			ps.setDouble(k++, rfPercentIndexador);
			ps.setDouble(k++, rfSpreadAnual);

			ps.executeUpdate();
		}
	}

	private void inserirLancamentosDoBackup(int usuarioId, List<String[]> rows) throws Exception {
		if (rows == null || rows.size() <= 1)
			return;

		Dao dao = new Dao();

		for (int i = 1; i < rows.size(); i++) {
			String[] r = rows.get(i);
			if (r.length < 5)
				continue;

			String categoria = r[0];
			String ativo = r[1];
			double qtd = pd(r[2]);
			double pu = pd(r[3]);
			String dataIso = r[4];

			dao.inserirLancamento(usuarioId, categoria, ativo, qtd, pu, dataIso);
		}
	}

	// =========================
	// ZIP helpers
	// =========================
	private interface EntryWriter {
		void write(Writer w) throws Exception;
	}

	/**
	 * ✅ ESCRITA SEGURA: Escreve primeiro em memória e só depois grava como entry no ZIP. Assim você pode usar Writer sem risco de fechar o ZipOutputStream.
	 */
	private static void writeEntry(ZipOutputStream zos, String entryName, EntryWriter fn) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (Writer w = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
			fn.write(w);
		}
		writeEntry(zos, entryName, baos.toByteArray());
	}

	/**
	 * ✅ ESCRITA BAIXO NÍVEL: Nunca fecha o ZipOutputStream. Só abre entry, escreve bytes, fecha entry.
	 */
	private static void writeEntry(ZipOutputStream zos, String entryName, byte[] data) throws IOException {
		ZipEntry entry = new ZipEntry(entryName);
		zos.putNextEntry(entry);

		if (data != null && data.length > 0) {
			zos.write(data);
		}

		zos.closeEntry();
	}

	private static void writeEntryText(ZipOutputStream zos, String entryName, String text) throws IOException {
		byte[] data = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
		writeEntry(zos, entryName, data);
	}

	private Map<String, String> readMeta(ZipFile zip) throws Exception {
		Map<String, String> out = new HashMap<>();
		var entry = zip.getEntry(ENTRY_META);
		if (entry == null)
			return out;

		try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isBlank())
					continue;
				String[] parts = line.split("\t", 2);
				if (parts.length == 2)
					out.put(parts[0].trim(), parts[1]);
			}
		}
		return out;
	}

	private List<String[]> readTsv(ZipFile zip, String entryName) throws Exception {
		var entry = zip.getEntry(entryName);
		if (entry == null)
			return new ArrayList<>();

		List<String[]> rows = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				rows.add(line.split("\t", -1));
			}
		}
		return rows;
	}

	private String readText(ZipFile zip, String entryName) throws Exception {
		var entry = zip.getEntry(entryName);
		if (entry == null)
			return "";

		StringBuilder sb = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line).append("\n");
			}
		}
		// remove o último \n extra (opcional)
		if (sb.length() > 0)
			sb.setLength(sb.length() - 1);
		return sb.toString();
	}

	private byte[] readBytes(ZipFile zip, String entryName) throws Exception {
		var entry = zip.getEntry(entryName);
		if (entry == null)
			return null;

		try (var in = zip.getInputStream(entry); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

			in.transferTo(baos);
			return baos.toByteArray();
		}
	}

	// =========================
	// small helpers
	// =========================
	private void alert(AlertType type, String title, String msg) {
		Alert a = new Alert(type);
		a.setTitle(title);
		a.setHeaderText(null);
		a.setContentText(msg);
		a.showAndWait();
	}

	private static String safe(String s) {
		if (s == null)
			return "";
		return s.replace("\t", " ").replace("\r", "").replace("\n", "\\n");
	}

	private static String nullToEmpty(String s) {
		return s == null ? "" : s;
	}

	private static String emptyToNull(String s) {
		if (s == null)
			return null;
		s = s.trim();
		return s.isEmpty() ? null : s;
	}

	private static String d(double v) {
		return Double.toString(v);
	}

	private static double pd(String s) {
		if (s == null)
			return 0.0;
		s = s.trim();
		if (s.isBlank())
			return 0.0;
		return Double.parseDouble(s);
	}
}
