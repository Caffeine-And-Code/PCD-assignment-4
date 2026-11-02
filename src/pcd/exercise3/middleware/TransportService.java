package pcd.exercise3.middleware;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public interface TransportService extends AutoCloseable {
    void onReceive(Consumer<RicardAgrawalaMessage> callback) throws IOException;
    void send(String destination, RicardAgrawalaMessage message);
    void sendBroadcast(RicardAgrawalaMessage message);
    @Override
    void close() throws IOException, TimeoutException;
}
