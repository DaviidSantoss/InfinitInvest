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

public class CodigoVerific {

	// =========================
	// Método que gera um código aleatorio.
	// =========================
	public String CodAle() {
		
		Random rand = new Random();
		
		int codigo = 100000 + rand.nextInt(999999);

		return String.valueOf(codigo);
	}

	// ====================
	// Método de envio de Datas.
	// ====================
	public static String DatasEnvio() {

		LocalDateTime agora = LocalDateTime.now();
		DateTimeFormatter formatar = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

		return agora.format(formatar);
	}

	// ========================================
	// Método responsável por enviar e-mail e o código de verificação.
	// ========================================
	public static void envioCod(String destinatario, String codigo) {
		
		// =====================================
		// Endereço de e-mail do remetente (quem envia o e-mail).
		// =====================================
		final String remetente = "";
		final String senhaApp = "";

		Properties props = new Properties();

		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");

		// ========================
		// Endereço do servidor SMTP da Zoho
		// ========================
		props.put("mail.smtp.host", "smtp.zoho.com");
		props.put("mail.smtp.port", "587");

		// ========================
		// Cria uma sessão de envio de e-mails
		// ========================
		Session session = Session.getInstance(props, new Authenticator() {

			// ====================================
			// Define como o Java vai se autenticar no servidor SMTP
			// ====================================
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(remetente, senhaApp);
			}
		});

		try {
			Message message = new MimeMessage(session);

			message.setFrom(new InternetAddress(remetente));

			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));

			message.setSubject("Seu código de verificação");

			message.setText("Olá!\n\nSeu código de verificação é: " + codigo + "\n\n Data: " + DatasEnvio());

			Transport.send(message);
			System.out.println("Código enviado com sucesso!");

		} catch (MessagingException e) {
			e.printStackTrace();
			System.out.println("Erro ao enviar o e-mail.");
		}
	}




}
