package pcd.exercise2.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteGameListener extends Remote {
    void gameUpdated(Match update) throws RemoteException;
}
