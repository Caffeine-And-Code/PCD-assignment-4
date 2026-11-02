package pcd.exercise1.gui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.pekko.actor.ActorRef;
import pcd.exercise1.protocols.MessagesProtocol.AssignSensorToZone;
import pcd.exercise1.protocols.MessagesProtocol.SensorDetectionRequested;

import java.util.List;
import java.util.Map;
import java.util.Random;

final class ZoneCanvas extends VBox {
    private final ActorRef uiActor;
    private final ZonePopupMenu menu;
    private Map<String, List<String>> sensorsByZone = Map.of();
    private Map<String, Boolean> zoneStates = Map.of();
    private boolean editingEnabled = true;

    ZoneCanvas(ActorRef uiActor, ZonePopupMenu menu) {
        super(10);
        this.uiActor = uiActor;
        this.menu = menu;
        setPadding(new Insets(3));
    }

    void setSensorsByZone(Map<String, List<String>> sensors) {
        sensorsByZone = Map.copyOf(sensors);
        rebuildZones();
    }

    void setZoneStates(Map<String, Boolean> states) {
        zoneStates = Map.copyOf(states);
        rebuildZones();
    }

    void setEditingEnabled(boolean enabled) {
        editingEnabled = enabled;
        rebuildZones();
    }

    private void rebuildZones() {
        menu.hide();
        getChildren().clear();
        sensorsByZone.keySet().stream().sorted().forEach(zone -> getChildren().add(createZone(zone)));
    }

    private VBox createZone(String zone) {
        Label title = new Label(zone);
        title.setStyle("-fx-font-weight: bold;");
        HBox.setHgrow(title, Priority.ALWAYS);
        title.setMaxWidth(Double.MAX_VALUE);
        Label status = new Label(zoneStates.getOrDefault(zone, false) ? "Active" : "Inactive");
        HBox heading = new HBox(8, title, status);
        heading.setAlignment(Pos.CENTER_LEFT);

        VBox panel = new VBox(8, heading);
        panel.setPadding(new Insets(10));
        Color color = Color.hsb(new Random(zone.hashCode()).nextDouble() * 360, 0.25, 0.95);
        panel.setStyle("-fx-background-color: " + toHex(color) + "; -fx-border-color: #454545;");
        panel.setOnContextMenuRequested(event -> {
            menu.showFor(panel, event.getScreenX(), event.getScreenY(), zone);
            event.consume();
        });

        List<String> sensors = sensorsByZone.get(zone);
        if (sensors.isEmpty()) {
            panel.getChildren().add(new Label("Nessun sensore"));
        } else {
            sensors.stream().sorted().forEach(sensor -> panel.getChildren().add(createSensor(sensor, zone)));
        }
        return panel;
    }

    private HBox createSensor(String sensor, String zone) {
        Label name = new Label(sensor);
        name.setMinWidth(70);
        name.setMaxWidth(Double.MAX_VALUE);
        name.setTooltip(new Tooltip(sensor));
        HBox.setHgrow(name, Priority.ALWAYS);

        Button detect = new Button("Attiva sensore");
        detect.setOnAction(event -> uiActor.tell(new SensorDetectionRequested(sensor), ActorRef.noSender()));

        List<String> destinations = sensorsByZone.keySet().stream()
                .filter(destination -> !destination.equals(zone)).sorted().toList();
        ComboBox<String> move = new ComboBox<>(FXCollections.observableArrayList(destinations));
        move.setPromptText("Sposta in...");
        move.setPrefWidth(145);
        move.setDisable(!editingEnabled || destinations.isEmpty());
        move.setAccessibleText("Sposta " + sensor + " in un'altra zona");
        move.setOnAction(event -> {
            String destination = move.getValue();
            if (destination != null) {
                uiActor.tell(new AssignSensorToZone(destination, sensor), ActorRef.noSender());
            }
        });

        HBox row = new HBox(8, name, detect, move);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255));
    }
}
