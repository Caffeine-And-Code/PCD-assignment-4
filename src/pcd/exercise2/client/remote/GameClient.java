package pcd.exercise2.client.remote;

import pcd.exercise2.common.Match;
import pcd.exercise2.common.RemoteGame;
import pcd.exercise2.common.RemoteGameListener;
import pcd.exercise2.common.RemoteLobby;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;

public class GameClient {
    private final String host;
    private final int port;
    private final Consumer<Match> onUpdate;
    private RemoteGame game;
    private GameListenerImpl listener;
    private RemoteGameListener listenerStub;

    public GameClient(String host, int port, Consumer<Match> onUpdate) {
        this.host = host;
        this.port = port;
        this.onUpdate = onUpdate;
    }

    public Match createGame(String gameName, String playerName) throws Exception {
        RemoteLobby lobby = (RemoteLobby) LocateRegistry.getRegistry(host, port).lookup("TTTLobby");
        return register(lobby.createGame(gameName, playerName));
    }

    public Match joinGame(String gameName, String playerName) throws Exception {
        RemoteLobby lobby = (RemoteLobby) LocateRegistry.getRegistry(host, port).lookup("TTTLobby");
        return register(lobby.joinGame(gameName, playerName));
    }

    private Match register(RemoteGame selectedGame) throws RemoteException {
        listener = new GameListenerImpl(onUpdate);
        listenerStub = (RemoteGameListener) UnicastRemoteObject.exportObject(listener, 0);
        game = selectedGame;
        try {
            return game.addListener(listenerStub);
        } catch (RemoteException | RuntimeException e) {
            close();
            throw e;
        }
    }

    public void makeMove(String playerName, int position) throws Exception {
        game.makeMove(playerName, position);
    }

    public void close() {
        if (game != null && listenerStub != null) {
            try {
                game.removeListener(listenerStub);
            } catch (RemoteException ignored) {
            }
        }
        if (listener != null) {
            try {
                UnicastRemoteObject.unexportObject(listener, true);
            } catch (NoSuchObjectException ignored) {
            }
        }
        game = null;
        listener = null;
        listenerStub = null;
    }
}
