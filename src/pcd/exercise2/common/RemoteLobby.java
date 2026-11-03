package pcd.exercise2.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteLobby extends Remote {
    RemoteGame createGame(String gameName, String playerName) throws RemoteException;

    RemoteGame joinGame(String gameName, String playerName) throws RemoteException;
}
