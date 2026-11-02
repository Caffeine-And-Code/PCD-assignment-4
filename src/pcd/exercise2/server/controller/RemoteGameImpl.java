package pcd.exercise2.server.controller;

import pcd.exercise2.server.model.Board;
import pcd.exercise2.server.model.Match;
import pcd.exercise2.server.model.MatchState;
import pcd.exercise2.server.model.PlayerTurn;
import pcd.exercise2.server.remote.RemoteGame;

public class RemoteGameImpl implements RemoteGame {
    private final String gameName;
    private final Board board = new Board();
    private final String player1;
    private String player2;
    private MatchState state = MatchState.WAITING;
    private PlayerTurn playerTurn = PlayerTurn.PLAYER1;

    public RemoteGameImpl(String gameName, String player1) {
        this.gameName = gameName;
        this.player1 = player1;
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
    }

    @Override
    public synchronized Match getState() {
        return snapshot();
    }

    @Override
    public synchronized Match makeMove(String playerName, int position) {
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
            state = player == PlayerTurn.PLAYER1 ? MatchState.WIN_1 : MatchState.WIN_2;
        } else if (board.isFull()) {
            state = MatchState.DRAW;
        } else {
            playerTurn = player == PlayerTurn.PLAYER1 ? PlayerTurn.PLAYER2 : PlayerTurn.PLAYER1;
        }
        return snapshot();
    }

    private Match snapshot() {
        return new Match(gameName, state, board.getBoard(), playerTurn, player1, player2);
    }
}
