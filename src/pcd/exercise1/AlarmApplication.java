package pcd.exercise1;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import pcd.exercise1.actors.UIActor;
import pcd.exercise1.gui.Dashboard;
import pcd.exercise1.support.SystemConfig;

public final class AlarmApplication extends Application {
    private ActorSystem system;

    public static void main(String[] args) {
        Application.launch(AlarmApplication.class, args);
    }

    @Override
    public void start(Stage stage) {
        system = ActorSystem.create("alarm", SystemConfig.LoadLocalConfig());

        ActorRef ui = system.actorOf(
                UIActor.props(
                        system.actorSelection(SystemConfig.CONTROL_NODE_PATH_FOR_LOCAL_ENVIROMENT)
                ),
                "ui"
        );

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
