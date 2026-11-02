package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.Props;
import pcd.exercise1.actors.base.NodeActor;
import pcd.exercise1.actors.base.NodeName;
import pcd.exercise1.protocols.MessagesProtocol.CreateZone;
import pcd.exercise1.protocols.MessagesProtocol.OperationRejected;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NodeName("ZoneManager")
public class ZoneManagerActor extends AbstractActor implements NodeActor {
    private final Map<String, List<String>> zoneSensors = new HashMap<>();

    public ZoneManagerActor(
            Map<String, List<String>> zoneSensors
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
                Props.create(ZoneActor.class, () -> new ZoneActor(createZone.zoneId())),
                createZone.zoneId()
        );
        zoneSensors.put(createZone.zoneId(), List.of());
    }
}
