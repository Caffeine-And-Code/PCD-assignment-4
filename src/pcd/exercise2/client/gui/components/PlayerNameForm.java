package pcd.exercise2.client.gui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class PlayerNameForm extends HBox {
    private final TextField name = new TextField();

    public PlayerNameForm() {
        super(8);
        setAlignment(Pos.CENTER_LEFT);
        name.setPromptText("Player name");
        getChildren().addAll(new Label("Player:"), name);
    }

    public String getPlayerName() {
        return name.getText().trim();
    }
}
