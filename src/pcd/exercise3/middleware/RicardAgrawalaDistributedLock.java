package pcd.exercise3.middleware;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RicardAgrawalaDistributedLock implements DistributedLock {

    private final String id;
    private State state = State.RELEASED;
    private final LampardClock clock = new LampardClockImpl();
    private final Set<String> peers = new HashSet<>();
    private final Set<String> pendingReplies = new HashSet<>();
    private final Map<String, Long> pendingQueue = new HashMap<>();
    private final TransportService transportService;
    private final Object monitor = new Object();
    private long requestTimestamp;

    public RicardAgrawalaDistributedLock(String id, TransportService transportService) throws IOException {
        this.id = id;
        this.transportService = transportService;
        transportService.onReceive(this::onMessage);
        synchronized (monitor) {
            transportService.sendBroadcast(new RicardAgrawalaMessage(
                    RicardAgrawalaMessage.Type.HELLO, id, clock.getTime()));
        }
    }

    /**
     * Pauses the calling thread to allow HELLO_REPLY messages to arrive.
     * Does not hold the monitor and does not guarantee discovery is complete.
     *
     * @param seconds number of seconds to wait; zero returns immediately
     * @throws IllegalArgumentException if seconds is negative
     * @throws InterruptedException if the waiting thread is interrupted
     */
    public void waitForHelloReplies(long seconds) throws InterruptedException {
        if (seconds < 0) {
            throw new IllegalArgumentException("Seconds must not be negative");
        }
        TimeUnit.SECONDS.sleep(seconds);
    }

    @Override
    public void acquire() throws InterruptedException {
        synchronized (monitor) {
            while (state != State.RELEASED) {
                monitor.wait();
            }
            state = State.WANTED;
            requestTimestamp = clock.tick();
            pendingReplies.clear();
            pendingReplies.addAll(peers);

            try {
                var request = new RicardAgrawalaMessage(
                        RicardAgrawalaMessage.Type.REQUEST, id, requestTimestamp);
                for (String peer : peers) {
                    transportService.send(peer, request);
                }
                while (!pendingReplies.isEmpty()) {
                    monitor.wait();
                }
                state = State.HELD;
            } catch (InterruptedException | RuntimeException e) {
                state = State.RELEASED;
                sendDeferredReplies();
                monitor.notifyAll();
                throw e;
            }
        }
    }

    @Override
    public void release() {
        synchronized (monitor) {
            if (state != State.HELD) {
                throw new IllegalStateException("The lock is not held");
            }
            state = State.RELEASED;
            sendDeferredReplies();
            monitor.notifyAll();
        }
    }

    private void onMessage(RicardAgrawalaMessage message) {
        synchronized (monitor) {
            if (id.equals(message.senderId())) {
                return;
            }

            switch (message.type()) {
                case HELLO -> {
                    if(peers.add(message.senderId())){
                        sendHelloReply(message.senderId());
                    }
                }
                case HELLO_REPLY -> peers.add(message.senderId());
                case REPLY -> {
                    if (state == State.WANTED && message.requestTimestamp() == requestTimestamp) {
                        pendingReplies.remove(message.senderId());
                        monitor.notifyAll();
                    }
                }
                case REQUEST -> {
                    clock.update(message.timestamp());
                    boolean myRequestFirst = state == State.WANTED
                            && (requestTimestamp < message.timestamp()
                            || (requestTimestamp == message.timestamp() && id.compareTo(message.senderId()) < 0));
                    if (state == State.HELD || myRequestFirst) {
                        pendingQueue.put(message.senderId(), message.timestamp());
                    } else {
                        sendReply(message.senderId(), message.timestamp());
                    }
                }
            }
        }
    }

    private void sendHelloReply(String destination) {
        transportService.send(destination, new RicardAgrawalaMessage(
                RicardAgrawalaMessage.Type.HELLO_REPLY, id, clock.tick()));
    }

    private void sendReply(String destination, long requestedAt) {
        transportService.send(destination, new RicardAgrawalaMessage(
                RicardAgrawalaMessage.Type.REPLY, id, clock.tick(), requestedAt));
    }

    private void sendDeferredReplies() {
        pendingQueue.forEach(this::sendReply);
        pendingQueue.clear();
    }
}
