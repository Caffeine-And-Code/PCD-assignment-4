package pcd.exercise1.gui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

public final class DelayControl extends VBox {
    private int seconds;
    private final Label value;

    public DelayControl(
            String name,
            int seconds,
            Consumer<Integer> onNewValue
    ) {
        super(4);
        this.seconds = seconds;
        setAlignment(Pos.CENTER);

        Label title = new Label(name);
        value = new Label(seconds + " s");
        value.setAlignment(Pos.CENTER);
        value.setMinSize(50, 30);
        value.setPrefSize(50, 30);

        Button decrease = new Button("-");
        Button increase = new Button("+");
        for (Button button : new Button[]{decrease, increase}) {
            button.setMinSize(30, 30);
            button.setPrefSize(30, 30);
            button.setMaxSize(30, 30);
            button.setFocusTraversable(false);
            button.setStyle("-fx-padding: 0; -fx-font-size: 14px;");
        }

        decrease.setOnAction(event -> onNewValue.accept(this.seconds - 1));
        increase.setOnAction(event -> onNewValue.accept(this.seconds + 1));

        HBox stepper = new HBox(2, decrease, value, increase);
        stepper.setAlignment(Pos.CENTER);
        getChildren().addAll(title, stepper);
    }

    public void setSeconds(int seconds) {
        this.seconds = seconds;
        value.setText(seconds + " s");
    }
}
