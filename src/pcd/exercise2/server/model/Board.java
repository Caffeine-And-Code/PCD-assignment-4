package pcd.exercise2.server.model;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final ArrayList<String> board;
    private static final String VOID_SYMBOL = " ";
    private static final String PLAYER_1_SYMBOL = "X";
    private static final String PLAYER_2_SYMBOL = "O";
    private static final int SIZE = 9;
    private static final int[][] WIN_LINES = {
            {0, 1, 2},
            {3, 4, 5},
            {6, 7, 8},
            {0, 3, 6},
            {1, 4, 7},
            {2, 5, 8},
            {0, 4, 8},
            {2, 4, 6}
    };

    public Board() {
        this.board = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            this.board.add(VOID_SYMBOL);
        }
    }

    public boolean makeMove(int position, PlayerTurn player) {
        if (position < 0 || position >= SIZE || !board.get(position).equals(VOID_SYMBOL)) {
            return false;
        }
        String symbol = player == PlayerTurn.PLAYER1 ? PLAYER_1_SYMBOL : PLAYER_2_SYMBOL;
        this.board.set(position, symbol);
        return true;
    }

    public boolean hasWon(PlayerTurn player) {
        String symbol = player == PlayerTurn.PLAYER1 ? PLAYER_1_SYMBOL : PLAYER_2_SYMBOL;
        for (int[] line : WIN_LINES) {
            if (board.get(line[0]).equals(symbol)
                    && board.get(line[1]).equals(symbol)
                    && board.get(line[2]).equals(symbol)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFull() {
        return !board.contains(VOID_SYMBOL);
    }

    public List<String> getBoard() {
        return List.copyOf(board);
    }
}
