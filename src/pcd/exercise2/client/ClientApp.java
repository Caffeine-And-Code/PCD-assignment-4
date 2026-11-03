package pcd.exercise2.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pcd.exercise2.client.gui.Frame;

public class ClientApp extends Application {
    @Override
    public void start(Stage stage) {
        var arguments = getParameters().getRaw();
        String host = arguments.isEmpty() ? "localhost" : arguments.getFirst();
        int port = arguments.size() < 2 ? 1099 : Integer.parseInt(arguments.get(1));
        Frame frame = new Frame(host, port);
        stage.setTitle("Tic-Tac-Toe");
        stage.setScene(new Scene(frame, 340, 410));
        stage.setOnCloseRequest(event -> frame.close());
        stage.show();
    }
}
