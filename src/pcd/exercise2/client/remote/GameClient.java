package pcd.exercise2.client.remote;

import pcd.exercise2.server.model.Match;
import pcd.exercise2.server.remote.RemoteGame;
import pcd.exercise2.server.remote.RemoteLobby;

import java.rmi.registry.LocateRegistry;

public class GameClient {
    private final String host;
    private RemoteGame game;

    public GameClient(String host) {
        this.host = host;
    }

    public Match createGame(String gameName, String playerName) throws Exception {
        RemoteLobby lobby = (RemoteLobby) LocateRegistry.getRegistry(host).lookup("TTTLobby");
        game = lobby.createGame(gameName, playerName);
        return game.getState();
    }

    public Match joinGame(String gameName, String playerName) throws Exception {
        RemoteLobby lobby = (RemoteLobby) LocateRegistry.getRegistry(host).lookup("TTTLobby");
        game = lobby.joinGame(gameName, playerName);
        return game.getState();
    }

    public Match getState() throws Exception {
        return game.getState();
    }

    public Match makeMove(String playerName, int position) throws Exception {
        return game.makeMove(playerName, position);
    }
}
