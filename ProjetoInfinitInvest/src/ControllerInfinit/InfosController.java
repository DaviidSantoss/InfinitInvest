package ControllerInfinit;

import MainInfinit.AddAtivos;
import MainInfinit.Infos;
import MainInfinit.ListaDeAtivos;
import javafx.application.Platform;
import javafx.stage.Stage;

public class InfosController {

	@SuppressWarnings("unused")
	private Infos infos;
	@SuppressWarnings("unused")
	private ListaDeAtivos lista;

	public InfosController(Infos infos, ListaDeAtivos lista) {
		this.infos = infos;
		this.lista = lista;

		Platform.runLater(() -> {
			infos.getAddInit().setOnAction(e -> {
				AddAtivos dialog = new AddAtivos();

				// usa a mesma instância da lista da tela principal
				@SuppressWarnings("unused")
				AddAtivosController controller = new AddAtivosController(dialog, lista);

				Stage owner = (Stage) infos.getAddInit().getScene().getWindow();
				dialog.show(owner);
			});
		});
	}
}
