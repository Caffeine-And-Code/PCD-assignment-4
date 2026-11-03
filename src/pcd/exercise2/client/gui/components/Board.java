package pcd.exercise2.client.gui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import pcd.exercise2.common.Match;

import java.util.function.IntConsumer;

public class Board extends GridPane {
    private final Button[] cells = new Button[9];

    public Board(IntConsumer onMove) {
        setHgap(5);
        setVgap(5);
        setAlignment(Pos.CENTER);
        for (int i = 0; i < cells.length; i++) {
            int position = i;
            Button cell = new Button(" ");
            cell.setFont(Font.font(28));
            cell.setPrefSize(80, 80);
            cell.setOnAction(event -> onMove.accept(position));
            cells[i] = cell;
            add(cell, i % 3, i / 3);
        }
        showMatch(null, false);
    }

    public void showMatch(Match match, boolean canMove) {
        for (int i = 0; i < cells.length; i++) {
            String value = match == null ? " " : match.board().get(i);
            cells[i].setText(value);
            cells[i].setDisable(!canMove || !value.isBlank());
        }
    }
}
