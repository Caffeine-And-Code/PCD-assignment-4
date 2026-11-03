package pcd.exercise2.client.controller;

import javafx.application.Platform;
import pcd.exercise2.client.gui.Frame;
import pcd.exercise2.client.remote.GameClient;
import pcd.exercise2.common.Match;
import pcd.exercise2.common.MatchState;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;

public class ClientController {
    private final Frame frame;
    private final GameClient remote;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "rmi-client");
        thread.setDaemon(true);
        return thread;
    });
    private String playerName;
    private boolean busy;
    private volatile boolean closed;

    public ClientController(Frame frame, String host, int port) {
        this.frame = frame;
        this.remote = new GameClient(host, port, this::onGameUpdated);
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
        });
    }

    public void makeMove(int position) {
        if (playerName != null) command(() -> remote.makeMove(playerName, position));
    }

    private void showState(Match state) {
        frame.showMatch(state, playerName);
    }

    private void onGameUpdated(Match update) {
        if (closed) return;
        Platform.runLater(() -> {
            if (closed) return;
            if (update.state() == MatchState.CLOSED) {
                playerName = null;
                busy = false;
                frame.showGameClosed();
                try {
                    worker.execute(remote::close);
                } catch (RejectedExecutionException ignored) {
                }
            } else {
                showState(update);
            }
        });
    }

    private void request(Callable<Match> call, Consumer<Match> onSuccess) {
        if (busy || closed) return;
        busy = true;
        frame.setBusy(true);
        worker.execute(() -> {
            try {
                Match state = call.call();
                Platform.runLater(() -> {
                    if (closed) return;
                    busy = false;
                    frame.setBusy(false);
                    onSuccess.accept(state);
                });
            } catch (Exception error) {
                Platform.runLater(() -> {
                    if (closed) return;
                    busy = false;
                    frame.setBusy(false);
                    frame.showError(errorMessage(error));
                });
            }
        });
    }

    private void command(RemoteCommand command) {
        if (busy || closed) return;
        busy = true;
        frame.setBusy(true);
        worker.execute(() -> {
            try {
                command.run();
                Platform.runLater(() -> {
                    if (closed) return;
                    busy = false;
                    frame.setBusy(false);
                });
            } catch (Exception error) {
                Platform.runLater(() -> {
                    if (closed) return;
                    busy = false;
                    frame.setBusy(false);
                    frame.showError(errorMessage(error));
                });
            }
        });
    }

    private String errorMessage(Exception error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    public void close() {
        if (closed) return;
        closed = true;
        worker.execute(remote::close);
        worker.shutdown();
    }

    @FunctionalInterface
    private interface RemoteCommand {
        void run() throws Exception;
    }
}
