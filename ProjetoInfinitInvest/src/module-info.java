module ProjetoInfinitInvest {

	requires javafx.controls;
	requires javafx.fxml;
	requires org.controlsfx.controls;
	requires javafx.media;
	requires javafx.graphics;
	requires java.xml;
	requires java.net.http;
	requires org.json;
	requires jdk.jdi;
	requires java.desktop;
	requires java.sql;
	requires java.logging;
	requires jakarta.activation;
	requires jakarta.mail;
	requires org.eclipse.angus.mail;

	opens LoginInfinit;
	opens BancoInfinit;

	
}