package pcd.exercise2.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteGame extends Remote {
    void makeMove(String playerName, int position) throws RemoteException;

    Match addListener(RemoteGameListener listener) throws RemoteException;

    void removeListener(RemoteGameListener listener) throws RemoteException;
}
