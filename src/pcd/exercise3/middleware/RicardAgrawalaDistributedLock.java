package pcd.exercise3.middleware;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RicardAgrawalaDistributedLock implements DistributedLock {

    private String id;
    private State state;
    private final LampardClock clock;

    private Set<String> repliesReceived;
    private final Set<String> deferredReplies;

    private final TransportService transportService;
    private final Object monitor = new Object();

    public RicardAgrawalaDistributedLock(String id, TransportService transportService) throws IOException {
        state = State.RELEASED;
        clock = new LampardClockImpl();
        repliesReceived = new HashSet<>();
        deferredReplies = new HashSet<>();
        this.transportService = transportService;
        this.id = id;
        this.transportService.onReceive(this::onMessage);
    }

    @Override
    public void acquire() {
        state = State.WANTED;
        clock.tick();
        repliesReceived = new HashSet<>();

        var timestamp = clock.getTime();
        var message = new RicardAgrawalaMessage(
                RicardAgrawalaMessage.Type.REQUEST,
                id,
                timestamp
        );

        transportService.sendBroadcast(message);

        //finish here
    }

    @Override
    public void release() {

    }

    private void onMessage(RicardAgrawalaMessage message){
        if(message.type() == RicardAgrawalaMessage.Type.REPLY){

        }
    }
}
