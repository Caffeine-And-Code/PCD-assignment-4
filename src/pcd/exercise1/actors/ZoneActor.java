package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import pcd.exercise1.protocols.MessagesProtocol.*;

public class ZoneActor extends AbstractActor {
    private final ActorRef controlUnitRef;
    private final String zoneId;

    public ZoneActor(ActorRef controlUnitRef, String zoneId) {
        this.controlUnitRef = controlUnitRef;
        this.zoneId = zoneId;
    }

    @Override
    public Receive createReceive() {
        return inactive();
    }

    public Receive inactive() {
        //noinspection deprecation
        return receiveBuilder()
                .match(ActiveZone.class, this::activeZone)
                .match(DeactiveZone.class, this::deactivateZone)
                .build();
    }

    public Receive active() {
        //noinspection deprecation
        return receiveBuilder()
                .match(ActiveZone.class, this::activeZone)
                .match(DeactiveZone.class, this::deactivateZone)
                .match(SensorDetection.class, this::forwardSensorDetection)
                .build();
    }

    private void activeZone(ActiveZone activateZone) {
        getContext().become(active());
        controlUnitRef.tell(new ZoneActivated(zoneId), getSelf());
    }

    private void deactivateZone(DeactiveZone deactivateZone) {
        getContext().become(inactive());
        controlUnitRef.tell(new ZoneDeactivated(zoneId), getSelf());
    }

    private void forwardSensorDetection(SensorDetection detection) {
        getContext().getParent().tell(detection, getSelf());
    }
}
