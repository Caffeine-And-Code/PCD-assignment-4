package pcd.exercise2.server.remote;

import pcd.exercise2.server.model.Match;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteGame extends Remote {
    Match getState() throws RemoteException;

    Match makeMove(String playerName, int position) throws RemoteException;
}
