package BancoInfinit;

public class Usuario {

	// =============
	// Atributos do usuario.
	// =============
	private int id;
	private String nome;
	private String email;
	private String senhaHash;
	private boolean emailVerificado;
	private String codigoVerificacao;
	private String dataEnvioCodigo;

	// =============
	// Construtor padrão.
	// =============
	/**/
	public Usuario() {

	}

	// =====================
	// Construtores para criar usuários.
	// =====================
	public Usuario(String nome, String email, String senhaHash) {
		this.nome = nome;
		this.email = email;
		this.senhaHash = senhaHash;
		this.emailVerificado = false;
		this.dataEnvioCodigo = null; // Pode setar depois
	}

	public Usuario(int id, String nome, String email, String senhaHash) {
		this.id = id;
		this.nome = nome;
		this.email = email;
		this.senhaHash = senhaHash;
		this.emailVerificado = false;
		this.dataEnvioCodigo = null; // Pode setar depois
	}

	public Usuario(int id, String nome, String email) {
		this.id = id;
		this.nome = nome;
		this.email = email;
	}

	// =============
	// Getters and Setters.
	// =============
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenhaHash() {
		return senhaHash;
	}

	public void setSenhaHash(String senhaHash) {
		this.senhaHash = senhaHash;
	}

	public boolean isEmailVerificado() {
		return emailVerificado;
	}

	public void setEmailVerificado(boolean emailVerificado) {
		this.emailVerificado = emailVerificado;
	}

	public String getCodigoVerificacao() {
		return codigoVerificacao;
	}

	public void setCodigoVerificacao(String codigoVerificacao) {
		this.codigoVerificacao = codigoVerificacao;
	}

	public String getDataEnvioCodigo() {
		return dataEnvioCodigo;
	}

	public void setDataEnvioCodigo(String dataEnvioCodigo) {
		this.dataEnvioCodigo = dataEnvioCodigo;
	}

	// ======================
	// Método toString para formatação.
	// ======================
	@Override
	public String toString() {

		return "Id: " + id + ", Nome: " + nome + ", Email: " + email;
	}

}
