package pcd.exercise3.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class RabbitMQTransportService implements TransportService, AutoCloseable{

    private static final String EXCHANGE = "mutex.exchange";
    private static final String BROADCAST = "mutex.broadcast";

    private final Connection connection;
    private final Channel channel;
    private final String queueName;
    private final ObjectMapper jsonMapper;

    public RabbitMQTransportService(String processId) throws IOException, TimeoutException {
        jsonMapper = new ObjectMapper();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(
                EXCHANGE,
                BuiltinExchangeType.DIRECT
        );

        channel.exchangeDeclare(
                BROADCAST,
                BuiltinExchangeType.FANOUT
        );

        queueName = channel.queueDeclare().getQueue();

        channel.queueBind(queueName, EXCHANGE, processId);
        channel.queueBind(queueName, BROADCAST, "");
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
    public void send(String destination, RicardAgrawalaMessage message) {
        try {
            byte[] messageBytes = jsonMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);

            channel.basicPublish(EXCHANGE, destination, null, messageBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendBroadcast(RicardAgrawalaMessage message) {
        try {
            byte[] messageBytes = jsonMapper.writeValueAsString(message).getBytes(StandardCharsets.UTF_8);

            channel.basicPublish(BROADCAST, "", null, messageBytes);
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
