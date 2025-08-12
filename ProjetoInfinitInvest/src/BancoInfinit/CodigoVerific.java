package BancoInfinit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.Random;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/* Método que gera um código aleatorio. */
public class CodigoVerific {

	public String CodAle() {
		
		/* criando objeto random */
		Random rand = new Random();
		
		/* O nosso código aleatorio agora vai de de "100000" a "999999". */
		int codigo = 100000 + rand.nextInt(999999);

		/* Transformando nosso "codigo" do tipo int em String. */
		return String.valueOf(codigo);
	}

	/* Método de envio de Datas. */
	public static String DatasEnvio() {

		/*
		 * Metodos de "pegar" a data e o horário atual e formatar para uma saida
		 * interessante.
		 */
		LocalDateTime agora = LocalDateTime.now();
		DateTimeFormatter formatar = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

		/* retornando a data e hora já formatados. */
		return agora.format(formatar);
	}

	String emailteste = "DaviidSantosss@outlook.com";

	/*
	 * Método responsável por enviar um e-mail com um código de verificação para o
	 * destinatário informado.
	 */
	public static void envioCod(String destinatario, String codigo) {
		
		/* Endereço de e-mail do remetente (quem envia o e-mail). */
		final String remetente = "sodavidmesmo@zohomail.com";
		final String senhaApp = "100pra30";

		/* Objeto Properties para configurar os parâmetros do servidor SMTP. */
		Properties props = new Properties();

		/* Define que será usada autenticação (login/senha). */
		props.put("mail.smtp.auth", "true");

		/* Ativa o protocolo TLS para segurança na comunicação. */
		props.put("mail.smtp.starttls.enable", "true");

		/* Endereço do servidor SMTP da Zoho. */
		props.put("mail.smtp.host", "smtp.zoho.com");

		/* Porta usada para o envio de e-mails com TLS. */
		props.put("mail.smtp.port", "587");

		/*
		 * Cria uma sessão de envio de e-mails usando as configurações definidas acima.
		 */
		Session session = Session.getInstance(props, new Authenticator() {

			/*
			 * Define como o Java vai se autenticar no servidor SMTP (com e-mail e senha do
			 * app).
			 */
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(remetente, senhaApp);
			}
		});

		try {
			/* Cria a estrutura da mensagem de e-mail. */
			Message message = new MimeMessage(session);

			/* Define o remetente (quem está enviando o e-mail). */
			message.setFrom(new InternetAddress(remetente));

			/* Define o(s) destinatário(s) do e-mail. */
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));

			/* Define o assunto do e-mail. */
			message.setSubject("Seu código de verificação");

			/* Define o corpo do e-mail (mensagem que o destinatário vai ler). */
			message.setText("Olá!\n\nSeu código de verificação é: " + codigo + "\n\n Data: " + DatasEnvio());

			/* Envia o e-mail */
			Transport.send(message);
			System.out.println("Código enviado com sucesso!");

		} catch (MessagingException e) {
			e.printStackTrace();
			System.out.println("Erro ao enviar o e-mail.");
		}
	}




}
