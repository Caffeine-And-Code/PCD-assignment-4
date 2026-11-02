package pcd.exercise2.client.gui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class GameNameForm extends HBox {
    private final TextField name = new TextField();

    public GameNameForm() {
        super(8);
        setAlignment(Pos.CENTER_LEFT);
        name.setPromptText("Game name");
        getChildren().addAll(new Label("Game:"), name);
    }

    public String getGameName() {
        return name.getText().trim();
    }
}
