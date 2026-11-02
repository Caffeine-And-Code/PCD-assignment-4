package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.Props;
import pcd.exercise1.protocols.MessagesProtocol.CreateZone;
import pcd.exercise1.protocols.MessagesProtocol.OperationRejected;

import java.util.HashMap;
import java.util.List;

public class ZoneManagerActor extends AbstractActor {
    private final HashMap<String, List<String>> zoneSensors = new HashMap<>();

    public ZoneManagerActor(
            HashMap<String, List<String>> zoneSensors
    ) {
        for (String zoneId : zoneSensors.keySet()) {
            OnCreateZone(new CreateZone(zoneId));
        }
    }

    @Override
    public Receive createReceive() {
        //noinspection deprecation
        return receiveBuilder()
                .match(CreateZone.class, this::OnCreateZone)
                .build();
    }

    public void OnCreateZone(CreateZone createZone) {
        if (zoneSensors.containsKey(createZone.zoneId())) {
            getSender().tell(new OperationRejected("Use a unique zone name without spaces or slashes."), getSelf());
            return;
        }

        getContext().actorOf(
                Props.create(ZoneActor.class),
                createZone.zoneId()
        );
        zoneSensors.put(createZone.zoneId(), List.of());
    }
}
