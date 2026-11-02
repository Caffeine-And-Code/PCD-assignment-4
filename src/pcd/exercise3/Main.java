package pcd.exercise3;

import pcd.exercise3.middleware.RicardAgrawalaDistributedLock;
import pcd.exercise3.middleware.RabbitMQTransportService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static void log(String message) {
        System.out.println("[" + LocalDateTime.now().format(LOG_TIME) + "] " + message);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) {
            log("Usage: Main <processId>");
            return;
        }
        String processId = args[0];

        try (var transport = new RabbitMQTransportService(processId)) {
            var lock = new RicardAgrawalaDistributedLock(processId, transport);
            log("[" + processId + "] attendo gli HELLO_REPLY per 5 secondi");
            lock.waitForHelloReplies(5);

            lock.acquire();
            try {
                log("ho il lock[" + processId + "]");
                TimeUnit.SECONDS.sleep(3);
            } finally {
                log("rilascio il lock[" + processId + "]");
                lock.release();
            }

            // Restare disponibili: gli altri processi possono ancora richiedere il lock.
            log("[" + processId + "] Premi Invio solo quando tutti hanno rilasciato il lock.");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
    }
}
