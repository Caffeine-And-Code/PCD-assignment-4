package pcd.exercise1.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.apache.pekko.actor.ActorRef;
import pcd.exercise1.protocols.MessagesProtocol;

public final class Dashboard extends BorderPane {
    public Dashboard(ActorRef uiActor) {
        VBox controls = new VBox(10);

        final var errorLabel = new Label("");
        errorLabel.setAlignment(Pos.CENTER);
        errorLabel.setMaxWidth(Double.MAX_VALUE);
        errorLabel.setWrapText(true);
        errorLabel.setStyle("-fx-text-fill: #b42318; -fx-font-size: 13px; -fx-font-weight: bold;");
        controls.setPrefWidth(200);
        final var zoneOverview = new ZoneOverviewPanel(uiActor);
        controls.getChildren().add(new KeypadPanel(uiActor, zoneOverview::getSelectedZones));

        DelaySettingsPanel delaySettings = new DelaySettingsPanel(uiActor);
        final var addZoneBtn = createAddZoneBtn(uiActor);

        controls.getChildren().add(delaySettings);
        controls.getChildren().add(addZoneBtn);
        controls.getChildren().add(errorLabel);

        setLeft(controls);

        uiActor.tell(new MessagesProtocol.GetZonesRequest(), ActorRef.noSender());
        uiActor.tell(new MessagesProtocol.GetSystemStateRequest(), ActorRef.noSender());
        setCenter(zoneOverview);

        BorderPane.setMargin(getCenter(), new Insets(0, 0, 0, 10));
        setPadding(new Insets(10));
        setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-border-radius: 8;");

        uiActor.tell(
                new MessagesProtocol.BindUi(
                        delaySettings.getEntryDelay(),
                        delaySettings.getExitDelay(),
                        zoneOverview,
                        zoneOverview.getSystemStateLabel(),
                        errorLabel
                ),
                ActorRef.noSender()
        );
    }

    private static Button createAddZoneBtn(ActorRef uiActor) {
        final var addZoneBtn = new Button("Add Zone...");
        addZoneBtn.setPrefSize(200, 25);

        addZoneBtn.setOnAction(event -> {
            final var dialog = new TextInputDialog();
            dialog.setTitle("Add Zone");
            dialog.setHeaderText("Insert the name of the zone");
            dialog.setContentText("Name:");

            dialog.showAndWait().ifPresent(value -> {
                final var trimmed = value.trim();
                if (trimmed.isEmpty()) {
                    new Alert(Alert.AlertType.WARNING, "The value cannot be empty.").showAndWait();
                    return;
                }

                uiActor.tell(new MessagesProtocol.CreateZone(trimmed), ActorRef.noSender());
            });
        });

        return addZoneBtn;
    }
}
