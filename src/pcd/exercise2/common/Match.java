package pcd.exercise2.common;

import java.io.Serializable;
import java.util.List;

public record Match(
        String gameName,
        MatchState state,
        List<String> board,
        PlayerTurn playerTurn,
        String player1,
        String player2
) implements Serializable {
    public Match {
        board = List.copyOf(board);
    }
}
