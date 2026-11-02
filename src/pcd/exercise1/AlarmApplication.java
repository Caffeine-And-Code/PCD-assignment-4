package pcd.exercise1;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import pcd.exercise1.actors.ControlUnitActor;
import pcd.exercise1.actors.UIActor;
import pcd.exercise1.gui.Dashboard;

import java.util.List;
import java.util.Map;

public final class AlarmApplication extends Application {
    private ActorSystem system;

    @Override
    public void start(Stage stage) {
        system = ActorSystem.create("alarm");
        Map<String, List<String>> initialZoneSensors = Map.of(
                "Atrio", List.of("Porta1", "Movimento1", "Finestra1", "Finestra2"),
                "Soggiorno", List.of("Porta2", "Porta3", "Movimento2", "Finestra3")
        );
        ActorRef controlUnit = system.actorOf(Props.create(ControlUnitActor.class,
                () -> new ControlUnitActor(initialZoneSensors)), "controlUnit");
        ActorRef ui = system.actorOf(UIActor.props(controlUnit), "ui");

        stage.setTitle("Smart Home Alarm");
        stage.setScene(new Scene(new Dashboard(ui), 760, 560));
        stage.setMinWidth(650);
        stage.setMinHeight(470);
        stage.centerOnScreen();
        stage.show();
    }

    @Override
    public void stop() {
        if (system != null) {
            system.terminate();
        }
    }
}
