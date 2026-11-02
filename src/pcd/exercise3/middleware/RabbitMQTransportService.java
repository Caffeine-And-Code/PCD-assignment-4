package pcd.exercise3.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class RabbitMQTransportService implements TransportService, AutoCloseable{

    private final String exchange;
    private final String broadcast;

    private final Connection connection;
    private final Channel channel;
    private final String queueName;
    private final ObjectMapper jsonMapper;

    public RabbitMQTransportService(String processId) throws IOException, TimeoutException {
        this(processId, "shared-resource");
    }

    public RabbitMQTransportService(String processId, String resourceId) throws IOException, TimeoutException {
        if (resourceId == null || resourceId.isBlank()) {
            throw new IllegalArgumentException("Resource ID must not be blank");
        }
        exchange = "mutex." + resourceId + ".direct";
        broadcast = "mutex." + resourceId + ".broadcast";
        jsonMapper = new ObjectMapper();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(
                exchange,
                BuiltinExchangeType.DIRECT
        );

        channel.exchangeDeclare(
                broadcast,
                BuiltinExchangeType.FANOUT
        );

        queueName = channel.queueDeclare().getQueue();

        channel.queueBind(queueName, exchange, processId);
        channel.queueBind(queueName, broadcast, "");
    }

    @Override
    public void onReceive(Consumer<RicardAgrawalaMessage> handler) throws IOException {
        DeliverCallback callback = (tag, delivery) -> {
            String body = new String(
                    delivery.getBody(),
                    StandardCharsets.UTF_8
            );

            RicardAgrawalaMessage message = jsonMapper.readValue(body, RicardAgrawalaMessage.class);
            handler.accept(message);
        };

        channel.basicConsume(
                queueName,
                true,
                callback,
                tag -> {}
        );
    }

    @Override
    public synchronized void send(String destination, RicardAgrawalaMessage message) {
        try {
            byte[] messageBytes = jsonMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);

            channel.basicPublish(exchange, destination, null, messageBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void sendBroadcast(RicardAgrawalaMessage message) {
        try {
            byte[] messageBytes = jsonMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);

            channel.basicPublish(broadcast, "", null, messageBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close()
            throws IOException, TimeoutException {
        channel.close();
        connection.close();
    }
}
