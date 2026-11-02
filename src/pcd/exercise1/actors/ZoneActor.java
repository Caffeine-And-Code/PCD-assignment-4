package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorSelection;
import pcd.exercise1.protocols.MessagesProtocol.*;

public class ZoneActor extends AbstractActor {
    private final ActorSelection controlUnitSelector;
    private final String zoneId;

    public ZoneActor(ActorSelection controlUnitSelector, String zoneId) {
        this.controlUnitSelector = controlUnitSelector;
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
        controlUnitSelector.tell(new ZoneActivated(zoneId), getSelf());
    }

    private void deactivateZone(DeactiveZone deactivateZone) {
        getContext().become(inactive());
        controlUnitSelector.tell(new ZoneDeactivated(zoneId), getSelf());
    }

    private void forwardSensorDetection(SensorDetection detection) {
        controlUnitSelector.tell(detection, getSelf());
    }
}
