package pcd.exercise3;

import pcd.exercise3.middleware.RabbitMQTransportService;
import pcd.exercise3.middleware.RicardAgrawalaMessage;

public class Main {

    public static void main(String[] args) throws Exception {

        String processId = args[0];

        try (RabbitMQTransportService transport =
                     new RabbitMQTransportService(processId)) {

            transport.onReceive(message -> {
                System.out.println(
                        "[" + processId + "] Received: " + message
                );
            });

            System.out.println(processId + " ready");

            if (processId.equals("P1")) {
                Thread.sleep(2000);

                RicardAgrawalaMessage request = new RicardAgrawalaMessage(
                        RicardAgrawalaMessage.Type.REQUEST,
                        "P1",
                        1
                );

                transport.send("P2", request);
                System.out.println("[P1] Sent direct REQUEST with timestamp=1 to P2");

                Thread.sleep(2000);

                RicardAgrawalaMessage broadcastRequest = new RicardAgrawalaMessage(
                        RicardAgrawalaMessage.Type.REQUEST,
                        "P1",
                        2
                );

                transport.sendBroadcast(broadcastRequest);
                System.out.println("[P1] Sent broadcast REQUEST with timestamp=2");
            }

            Thread.sleep(30000);
        }
    }
}
