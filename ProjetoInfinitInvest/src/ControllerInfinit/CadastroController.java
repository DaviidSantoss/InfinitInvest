package ControllerInfinit;

import java.io.IOException;
import java.sql.SQLException;

import BancoInfinit.CodigoVerific;
import BancoInfinit.Dao;
import BancoInfinit.Usuario;
import LoginInfinit.CadastroForm;
import LoginInfinit.LoginView;
import LoginInfinit.Verificacao;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CadastroController {

	// ====================
	// Componentes da view.
	// ====================
	private CadastroForm view;
	private Verificacao verificacao;
	private Stage stageAtual;

	// ====================
	// Dependencias.
	// ====================
	private Scene novaCena;
	private final CodigoVerific codigoVerific;


	public CadastroController(CadastroForm view) {
		this.view = view;
		this.verificacao = new Verificacao();
		this.codigoVerific = new CodigoVerific();
	}

	@SuppressWarnings("static-access")
	public void configurarAcoes() {


		// ====================
		// Ações após o botão cadastro ser clicado.
		// ====================
		view.getButtonCadastro().setOnAction(e -> {

			String nome = view.getNome();
			String email = view.getEmail();
			String senha = view.getSenha();

			view.nomeerro().setVisible(false);
			view.emailErro().setVisible(false);
			view.senhaerro().setVisible(false);

			boolean valido = true;

			if (nome == null || nome.trim().isEmpty()) {
				view.nomeerro().setText("Nome é obrigatório");
				view.nomeerro().setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
				view.nomeerro().setVisible(true);
				valido = false;
			}

			if (email == null || !view.validaremail(email)) {
				view.emailErro().setText("Email inválido");
				view.emailErro().setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
				view.emailErro().setVisible(true);
				valido = false;
			}

			if (senha == null || senha.trim().length() < 8) {
				view.senhaerro().setText("Senha deve ter pelo menos 8 caracteres");
				view.senhaerro().setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
				view.senhaerro().setVisible(true);
				valido = false;
			}

			if (valido) {

				stageAtual = (Stage) view.getButtonCadastro().getScene().getWindow();
				verificacao = new Verificacao();

				// ====================
				// Seta para voltar à tela anterior.
				// ====================
				verificacao.getVerFoto().setOnMouseClicked(f -> {
					CadastroForm novaView = new CadastroForm().build();
					CadastroController novoController = new CadastroController(novaView);
					novoController.configurarAcoes();

					Stage stageAtual = (Stage) verificacao.getVerFoto().getScene().getWindow();
					Scene cenaCadastro = new Scene(novaView);
					stageAtual.setScene(cenaCadastro);
					stageAtual.setMaximized(true);
				});

				// ====================
				// Verificação primária.
				// ====================
				new Thread(() -> {
					String codString = codigoVerific.CodAle();
					codigoVerific.envioCod(email, codString);

					verificacao.getConfirmar().setOnMouseClicked(g -> {
						if (codString.equals(verificacao.getCodField().getText())) {
							try {
								inserir(nome, email, senha);
								verificacao.mostrarTelaSucesso(() -> {
									System.out.println("Redirecionando para login...");

									LoginView loginView = new LoginView(stageAtual);
									Scene cenaCadastro = new Scene(loginView);
									stageAtual.setScene(cenaCadastro);
									stageAtual.setMaximized(true);


								});
							} catch (IOException | SQLException e1) {
								System.out.println("Erro ao inserir usuário.");
							}
						} else {
							verificacao.getErroCodigo().setVisible(true);
						}


					});
				}).start();

				// ====================
				// Verificação secundária.
				// ====================
				verificacao.getReenvio().setOnMouseClicked(ev -> {
					System.out.println("Código reenviado.");

					new Thread(() -> {
						String codString = codigoVerific.CodAle();
						codigoVerific.envioCod(email, codString);

						verificacao.getConfirmar().setOnMouseClicked(f -> {
							if (codString.equals(verificacao.getCodField().getText())) {
								try {
									
									inserir(nome, email, senha);

									verificacao.mostrarTelaSucesso(() -> {

										LoginView loginView = new LoginView(stageAtual);
										Scene cenaCadastro = new Scene(loginView);
										stageAtual.setScene(cenaCadastro);
										stageAtual.setMaximized(true);
									});
									
								} catch (IOException | SQLException e1) {
									System.out.println("Erro ao inserir usuário.");
								}
							} else {
								verificacao.getErroCodigo().setVisible(true);
							}
						});
					}).start();
				});

				// ====================
				// Cria uma nova cena passando a
				// classe instanciada como parâmetro.
				// ====================
				novaCena = new Scene(verificacao, 1920, 1080);
				stageAtual.setScene(novaCena);
				stageAtual.setMaximized(true);


			}


		});


		

	}
	


	// ====================
	// Método para inserir usuário.
	// ====================
	public static void inserir(String nome, String email, String senha) throws IOException, SQLException {
		Usuario user = new Usuario(nome, email, senha);
		Dao dao = new Dao();
		dao.insert(user);
	}
}
