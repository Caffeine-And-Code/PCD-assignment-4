package pcd.exercise2.server.controller;

import pcd.exercise2.server.model.Board;
import pcd.exercise2.common.Match;
import pcd.exercise2.common.MatchState;
import pcd.exercise2.common.PlayerTurn;
import pcd.exercise2.common.RemoteGame;
import pcd.exercise2.common.RemoteGameListener;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RemoteGameImpl implements RemoteGame {
    private static final int TURN_TIMEOUT_SECONDS = 20;
    private static final int CLOSE_DELAY_SECONDS = 5;

    private final String gameName;
    private final Board board = new Board();
    private final String player1;
    private final Runnable onClose;
    private String player2;
    private MatchState state = MatchState.WAITING;
    private PlayerTurn playerTurn = PlayerTurn.PLAYER1;
    private final List<RemoteGameListener> listeners = new ArrayList<>();
    private final ExecutorService notifications;
    private final ScheduledExecutorService timer;
    private ScheduledFuture<?> turnTimeout;
    private long turnNumber;

    public RemoteGameImpl(String gameName, String player1, Runnable onClose) {
        this.gameName = gameName;
        this.player1 = player1;
        this.onClose = onClose;
        notifications = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "game-" + gameName + "-notifications");
            thread.setDaemon(true);
            return thread;
        });
        timer = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "game-" + gameName + "-timer");
            thread.setDaemon(true);
            return thread;
        });
    }

    public synchronized void join(String playerName) {
        if (playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be empty");
        }
        if (player1.equals(playerName)) {
            throw new IllegalArgumentException("Players need different names");
        }
        if (player2 != null) {
            throw new IllegalStateException("Game is full");
        }
        player2 = playerName;
        state = MatchState.ACTIVE;
        scheduleTurnTimeout();
        notifyChanged();
    }

    @Override
    public synchronized void makeMove(String playerName, int position) {
        if (state != MatchState.ACTIVE) {
            throw new IllegalStateException("Game is not active");
        }
        PlayerTurn player = player1.equals(playerName) ? PlayerTurn.PLAYER1
                : player2.equals(playerName) ? PlayerTurn.PLAYER2 : null;
        if (player == null) {
            throw new IllegalArgumentException("Unknown player");
        }
        if (player != playerTurn) {
            throw new IllegalStateException("Not your turn");
        }
        if (!board.makeMove(position, player)) {
            throw new IllegalArgumentException("Invalid position or occupied cell");
        }

        if (board.hasWon(player)) {
            finish(player == PlayerTurn.PLAYER1 ? MatchState.WIN_1 : MatchState.WIN_2);
        } else if (board.isFull()) {
            finish(MatchState.DRAW);
        } else {
            playerTurn = player == PlayerTurn.PLAYER1 ? PlayerTurn.PLAYER2 : PlayerTurn.PLAYER1;
            scheduleTurnTimeout();
            notifyChanged();
        }
    }

    @Override
    public synchronized Match addListener(RemoteGameListener listener) {
        if (listener == null) throw new IllegalArgumentException("Listener cannot be null");
        if (!listeners.contains(listener)) listeners.add(listener);
        return snapshot();
    }

    @Override
    public synchronized void removeListener(RemoteGameListener listener) {
        listeners.remove(listener);
    }

    private void scheduleTurnTimeout() {
        turnNumber++;
        long expectedTurn = turnNumber;
        if (turnTimeout != null) turnTimeout.cancel(false);
        turnTimeout = timer.schedule(() -> turnExpired(expectedTurn),
                TURN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private synchronized void turnExpired(long expectedTurn) {
        if (state != MatchState.ACTIVE || expectedTurn != turnNumber) return;
        finish(playerTurn == PlayerTurn.PLAYER1
                ? MatchState.WIN_2_TIMEOUT
                : MatchState.WIN_1_TIMEOUT);
    }

    private void finish(MatchState result) {
        state = result;
        turnNumber++;
        if (turnTimeout != null) turnTimeout.cancel(false);
        notifyChanged();
        timer.schedule(onClose, CLOSE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void notifyChanged() {
        Match update = snapshot();
        for (RemoteGameListener listener : List.copyOf(listeners)) {
            notifications.execute(() -> {
                try {
                    listener.gameUpdated(update);
                } catch (RemoteException e) {
                    removeListener(listener);
                }
            });
        }
    }

    public synchronized void close() {
        state = MatchState.CLOSED;
        notifyChanged();
        listeners.clear();
        notifications.shutdown();
        timer.shutdown();
    }

    private Match snapshot() {
        return new Match(gameName, state, board.getBoard(), playerTurn, player1, player2);
    }
}
