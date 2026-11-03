package pcd.exercise2.client.gui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import pcd.exercise2.client.controller.ClientController;
import pcd.exercise2.client.gui.components.Board;
import pcd.exercise2.client.gui.components.GameNameForm;
import pcd.exercise2.client.gui.components.PlayerNameForm;
import pcd.exercise2.common.Match;
import pcd.exercise2.common.MatchState;
import pcd.exercise2.common.PlayerTurn;

public class Frame extends BorderPane {
    private final ClientController controller;
    private final GameNameForm gameNameForm = new GameNameForm();
    private final PlayerNameForm playerNameForm = new PlayerNameForm();
    private final Button createButton = new Button("Create");
    private final Button joinButton = new Button("Join");
    private final Board board;
    private final Label players = new Label();
    private final Label status = new Label("Enter names, then create or join a game");
    private Match match;
    private String playerName;
    private boolean busy;

    public Frame(String host, int port) {
        controller = new ClientController(this, host, port);
        board = new Board(controller::makeMove);

        createButton.setOnAction(event -> controller.createGame(gameNameForm.getGameName(), playerNameForm.getPlayerName()));
        joinButton.setOnAction(event -> controller.joinGame(gameNameForm.getGameName(), playerNameForm.getPlayerName()));

        VBox top = new VBox(8, gameNameForm, playerNameForm, new HBox(8, createButton, joinButton), players);
        setPadding(new Insets(12));
        setTop(top);
        setCenter(status);
        setBottom(board);
        BorderPane.setMargin(board, new Insets(12, 0, 0, 0));
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
        updateControls();
    }

    public void showMatch(Match match, String playerName) {
        this.match = match;
        this.playerName = playerName;
        players.setText("X: " + match.player1() + "    O: " + (match.player2() == null ? "?" : match.player2()));
        status.setText(switch (match.state()) {
            case WAITING -> "Waiting for another player";
            case ACTIVE -> isMyTurn() ? "Your turn" : "Waiting for the other player";
            case WIN_1 -> match.player1() + " won";
            case WIN_2 -> match.player2() + " won";
            case WIN_1_TIMEOUT -> match.player1() + " won: " + match.player2() + " timed out";
            case WIN_2_TIMEOUT -> match.player2() + " won: " + match.player1() + " timed out";
            case DRAW -> "Draw";
            case CLOSED -> "Game closed";
        });
        updateControls();
    }

    public void showGameClosed() {
        match = null;
        playerName = null;
        busy = false;
        players.setText("");
        status.setText("Game closed. You can create or join another game");
        updateControls();
    }

    public void showError(String message) {
        status.setText(message == null ? "Request failed" : message);
    }

    private boolean isMyTurn() {
        return match != null && match.state() == MatchState.ACTIVE
                && (match.playerTurn() == PlayerTurn.PLAYER1 && playerName.equals(match.player1())
                || match.playerTurn() == PlayerTurn.PLAYER2 && playerName.equals(match.player2()));
    }

    private void updateControls() {
        createButton.setDisable(match != null || busy);
        joinButton.setDisable(match != null || busy);
        board.showMatch(match, !busy && isMyTurn());
    }

    public void close() {
        controller.close();
    }
}
