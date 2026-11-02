package pcd.exercise2.server.controller;

import pcd.exercise2.server.remote.RemoteLobby;
import pcd.exercise2.server.remote.RemoteGame;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class RemoteLobbyImpl implements RemoteLobby {
    private record GameEntry(RemoteGameImpl controller, RemoteGame stub) { }
    private final Map<String, GameEntry> games = new HashMap<>();

    @Override
    public synchronized RemoteGame createGame(String gameName, String playerName) throws RemoteException {
        if (gameName == null || gameName.isBlank()
                || playerName == null || playerName.isBlank()) {
            throw new IllegalArgumentException("Game and player names cannot be empty");
        }
        if (games.containsKey(gameName)) {
            throw new IllegalArgumentException("Game already exists");
        }
        RemoteGameImpl controller = new RemoteGameImpl(gameName, playerName);
        RemoteGame stub = (RemoteGame) UnicastRemoteObject.exportObject(controller, 0);
        games.put(gameName, new GameEntry(controller, stub));
        return stub;
    }

    @Override
    public synchronized RemoteGame joinGame(String gameName, String playerName) {
        GameEntry entry = games.get(gameName);
        if (entry == null) {
            throw new IllegalArgumentException("Game does not exist");
        }
        entry.controller().join(playerName);
        return entry.stub();
    }
}
