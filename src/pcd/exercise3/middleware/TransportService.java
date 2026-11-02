package pcd.exercise3.middleware;

import java.io.IOException;
import java.util.function.Consumer;

public interface TransportService {
    void onReceive(Consumer<RicardAgrawalaMessage> callback) throws IOException;
    void send(String destination, RicardAgrawalaMessage message);
    void sendBroadcast(RicardAgrawalaMessage message);
}
