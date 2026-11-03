package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorSelection;
import pcd.exercise1.actors.base.NodeActor;
import pcd.exercise1.protocols.MessagesProtocol.ActiveZone;
import pcd.exercise1.protocols.MessagesProtocol.DeactiveZone;
import pcd.exercise1.protocols.MessagesProtocol.SensorDetection;
import pcd.exercise1.support.SystemConfig;

public class ZoneActor extends AbstractActor implements NodeActor {
    private final ActorSelection controlUnitSelector;

    public ZoneActor(String zoneId) {
        this.controlUnitSelector = getContext().actorSelection(
                SystemConfig.CONTROL_NODE_PATH + NodeActor.getNodeName(ControlUnitActor.class)
        );
    }

    @Override
    public Receive createReceive() {
        return inactive();
    }

    public Receive inactive() {
        return receiveBuilder()
                .match(ActiveZone.class, msg -> getContext().become(active()))
                .match(DeactiveZone.class, msg -> { /* already inactive */ })
                .match(SensorDetection.class, msg -> { /* ignored when inactive */ })
                .build();
    }

    public Receive active() {
        //noinspection deprecation
        return receiveBuilder()
                .match(ActiveZone.class, msg -> { /* already active */ })
                .match(DeactiveZone.class, msg -> getContext().become(inactive()))
                .match(SensorDetection.class, this::forwardSensorDetection)
                .build();
    }

    private void forwardSensorDetection(SensorDetection detection) {
        controlUnitSelector.tell(detection, getSelf());
    }
}
