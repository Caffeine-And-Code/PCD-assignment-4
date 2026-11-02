package pcd.exercise2.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;
import pcd.exercise2.client.gui.Frame;
import pcd.exercise2.client.remote.GameClient;
import pcd.exercise2.server.model.Match;
import pcd.exercise2.server.model.MatchState;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ClientController {
    private final Frame frame;
    private final GameClient remote;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "rmi-client");
        thread.setDaemon(true);
        return thread;
    });
    private final Timeline refreshTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> refresh()));
    private String playerName;
    private boolean busy;

    public ClientController(Frame frame, String host) {
        this.frame = frame;
        this.remote = new GameClient(host);
        refreshTimer.setCycleCount(Timeline.INDEFINITE);
    }

    public void createGame(String gameName, String name) {
        startGame(gameName, name, true);
    }

    public void joinGame(String gameName, String name) {
        startGame(gameName, name, false);
    }

    private void startGame(String gameName, String name, boolean create) {
        if (playerName != null || busy) return;
        if (gameName.isBlank() || name.isBlank()) {
            frame.showError("Insert a game name and a player name");
            return;
        }
        request(() -> create ? remote.createGame(gameName, name) : remote.joinGame(gameName, name), state -> {
            playerName = name;
            showState(state);
            if (state.state() == MatchState.WAITING || state.state() == MatchState.ACTIVE) refreshTimer.play();
        });
    }

    public void makeMove(int position) {
        if (playerName != null) request(() -> remote.makeMove(playerName, position), this::showState);
    }

    private void refresh() {
        if (playerName != null) request(remote::getState, this::showState);
    }

    private void showState(Match state) {
        frame.showMatch(state, playerName);
        if (state.state() != MatchState.WAITING && state.state() != MatchState.ACTIVE) refreshTimer.stop();
    }

    private void request(Callable<Match> call, Consumer<Match> onSuccess) {
        if (busy) return;
        busy = true;
        frame.setBusy(true);
        worker.execute(() -> {
            try {
                Match state = call.call();
                Platform.runLater(() -> {
                    busy = false;
                    frame.setBusy(false);
                    onSuccess.accept(state);
                });
            } catch (Exception error) {
                Throwable cause = error;
                while (cause.getCause() != null) cause = cause.getCause();
                String message = cause.getMessage();
                Platform.runLater(() -> {
                    busy = false;
                    frame.setBusy(false);
                    frame.showError(message);
                });
            }
        });
    }

    public void close() {
        refreshTimer.stop();
        worker.shutdownNow();
    }
}
