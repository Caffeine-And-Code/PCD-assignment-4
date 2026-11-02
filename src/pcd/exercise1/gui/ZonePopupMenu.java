package pcd.exercise1.gui;

import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.apache.pekko.actor.ActorRef;
import pcd.exercise1.protocols.MessagesProtocol.DeleteZone;

import java.util.List;

final class ZonePopupMenu extends ContextMenu {
    private final ObservableMap<String, List<String>> sensorsByZone;
    private final ActorRef uiActor;

    ZonePopupMenu(ObservableMap<String, List<String>> sensorsByZone, ActorRef uiActor) {
        this.sensorsByZone = sensorsByZone;
        this.uiActor = uiActor;
    }

    void showFor(Node owner, double screenX, double screenY, String zone) {
        rebuild(zone);
        show(owner, screenX, screenY);
    }

    private void rebuild(String zone) {
        getItems().clear();

        MenuItem title = new MenuItem(zone);
        title.setDisable(true);
        getItems().addAll(title, new SeparatorMenuItem());

        List<String> sensors = sensorsByZone.getOrDefault(zone, List.of());
        if (sensors.isEmpty()) {
            MenuItem empty = new MenuItem("Nessun sensore");
            empty.setDisable(true);
            getItems().add(empty);
            MenuItem delete = new MenuItem("Delete zone");
            delete.setOnAction(event -> uiActor.tell(new DeleteZone(zone), ActorRef.noSender()));
            getItems().add(delete);
        }

    }
}
