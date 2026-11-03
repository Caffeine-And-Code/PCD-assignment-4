package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorSelection;

import pcd.exercise1.actors.base.NodeActor;
import pcd.exercise1.protocols.MessagesProtocol.SensorDetection;
import pcd.exercise1.protocols.MessagesProtocol.SensorDetectionRequested;
import pcd.exercise1.protocols.MessagesProtocol.SensorZoneChanged;
import pcd.exercise1.protocols.SensorType;
import pcd.exercise1.support.SystemConfig;

public class SensorActor extends AbstractActor implements NodeActor {
    private final String sensorId;
    private final SensorType type;

    private String zoneId;
    private ActorSelection zone;

    public SensorActor(
            String sensorId,
            SensorType type,
            String zoneId
    ) {
        this.sensorId = sensorId;
        this.type = type;
        bindToZone(zoneId);
    }

    private void bindToZone(String zoneId) {
        this.zoneId = zoneId;
        this.zone = getContext().actorSelection(
                SystemConfig.ZONES_NODE_PATH
                        + NodeActor.getNodeName(ZoneManagerActor.class) + "/" + zoneId
        );
    }

    @Override
    public Receive createReceive() {
        //noinspection deprecation
        return receiveBuilder()
                .match(SensorDetectionRequested.class, this::onDetectionRequested)
                .match(SensorZoneChanged.class, msg -> bindToZone(msg.zoneId()))
                .build();
    }

    private void onDetectionRequested(SensorDetectionRequested request) {
        if (!sensorId.equals(request.sensorId())) {
            return;
        }

        System.out.println(
                "Sensor " + sensorId + " (" + type + ") detected an event in zone " + zoneId
        );

        zone.tell(
                new SensorDetection(sensorId, type, zoneId),
                getSelf()
        );
    }
}
