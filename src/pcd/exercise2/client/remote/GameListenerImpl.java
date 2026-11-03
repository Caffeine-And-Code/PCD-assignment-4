package pcd.exercise2.client.remote;

import pcd.exercise2.common.RemoteGameListener;
import pcd.exercise2.common.Match;

import java.util.function.Consumer;

public class GameListenerImpl implements RemoteGameListener {
    private final Consumer<Match> onUpdate;

    public GameListenerImpl(Consumer<Match> onUpdate) {
        this.onUpdate = onUpdate;
    }

    @Override
    public void gameUpdated(Match update) {
        onUpdate.accept(update);
    }
}
