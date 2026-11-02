package pcd.exercise1;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSystem;
import pcd.exercise1.actors.UIActor;
import pcd.exercise1.gui.Dashboard;

public final class AlarmApplication extends Application {
    private ActorSystem system;

    @Override
    public void start(Stage stage) {
        Config config = ConfigFactory.load("local-applicaion.conf");
        system = ActorSystem.create("alarm", config);

        ActorRef ui = system.actorOf(
                UIActor.props(
                        system.actorSelection("pekko://alarm@localhost:2551/user/controlUnit")
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
