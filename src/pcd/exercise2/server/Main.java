package pcd.exercise2.server;

import pcd.exercise2.common.RemoteLobby;
import pcd.exercise2.server.controller.RemoteLobbyImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = args.length == 0 ? 1099 : Integer.parseInt(args[0]);
        RemoteLobby lobby = new RemoteLobbyImpl();
        RemoteLobby stub = (RemoteLobby) UnicastRemoteObject.exportObject(lobby, 0);
        Registry registry = LocateRegistry.createRegistry(port);
        registry.rebind("TTTLobby", stub);
        System.out.println("TTT server ready on port " + port);
    }
}
