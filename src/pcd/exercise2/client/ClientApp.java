package pcd.exercise2.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pcd.exercise2.client.gui.Frame;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) {
        String host = getParameters().getRaw().isEmpty() ? "localhost" : getParameters().getRaw().getFirst();
        Frame frame = new Frame(host);
        stage.setTitle("Tic-Tac-Toe");
        stage.setScene(new Scene(frame, 340, 410));
        stage.setOnCloseRequest(event -> frame.close());
        stage.show();
    }
}
