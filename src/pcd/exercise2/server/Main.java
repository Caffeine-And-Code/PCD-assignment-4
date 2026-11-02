package pcd.exercise2.server;

import pcd.exercise2.server.remote.RemoteLobby;
import pcd.exercise2.server.controller.RemoteLobbyImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Main {
    public static void main(String[] args) throws Exception {
        RemoteLobby lobby = new RemoteLobbyImpl();
        RemoteLobby stub = (RemoteLobby) UnicastRemoteObject.exportObject(lobby, 0);
        Registry registry = LocateRegistry.createRegistry(1099);
        registry.rebind("TTTLobby", stub);
        System.out.println("TTT server ready");
    }
}
