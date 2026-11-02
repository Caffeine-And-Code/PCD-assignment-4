package pcd.exercise1.gui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.apache.pekko.actor.ActorRef;
import pcd.exercise1.protocols.MessagesProtocol;

import java.util.List;
import java.util.function.Supplier;

final class KeypadPanel extends VBox {
    private static final int MAX_DIGITS_NUMBER = 5;

    private final ActorRef ui;
    private String enteredDigits = "";
    private final Label display = new Label();

    private final Supplier<List<String>> selectedZones;

    KeypadPanel(ActorRef ui, Supplier<List<String>> selectedZones) {
        super(8);
        this.ui = ui;
        this.selectedZones = selectedZones;
        setPadding(new Insets(8));
        setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-border-radius: 8;");

        display.setAlignment(Pos.CENTER);
        display.setMaxWidth(Double.MAX_VALUE);
        display.setMinHeight(34);
        display.setStyle("-fx-border-color: gray; -fx-border-radius: 6;");

        GridPane keys = createKeys();

        getChildren().addAll(display, keys);
    }

    private GridPane createKeys() {
        GridPane keys = new GridPane();
        keys.setHgap(6);
        keys.setVgap(6);
        keys.setAlignment(Pos.CENTER);

        for (int value = 1; value <= 9; value++) {
            int digit = value;
            Button key = new Button(Integer.toString(digit));
            key.setPrefSize(48, 36);
            key.setMaxWidth(Double.MAX_VALUE);
            key.setFocusTraversable(false);
            key.setOnAction(event -> pinButtonPressed(digit));
            keys.add(key, (value - 1) % 3, (value - 1) / 3);
        }
        Button zero = new Button("0");
        zero.setPrefSize(48, 36);
        zero.setOnAction(event -> pinButtonPressed(0));
        keys.add(zero, 1, 3);
        return keys;
    }

    private void pinButtonPressed(int digit) {
        enteredDigits += digit;
        this.ui.tell(new MessagesProtocol.EnterPinDigit(digit, selectedZones.get()), ActorRef.noSender());

        if (enteredDigits.length() >= MAX_DIGITS_NUMBER) {
            enteredDigits = "";
        }

        display.setText("*".repeat(enteredDigits.length()));
    }
}
