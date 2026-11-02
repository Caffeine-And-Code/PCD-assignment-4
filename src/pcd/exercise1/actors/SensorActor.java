package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorSelection;

import pcd.exercise1.protocols.MessagesProtocol.SensorDetection;
import pcd.exercise1.protocols.MessagesProtocol.SensorDetectionRequested;
import pcd.exercise1.support.SystemConfig;

public class SensorActor extends AbstractActor {
    private final String sensorId;
    private final String zoneId;
    private final ActorSelection zone;

    public SensorActor(
            String sensorId,
            String zoneId
    ) {
        this.sensorId = sensorId;
        this.zoneId = zoneId;

        this.zone = getContext().actorSelection(
                SystemConfig.ZONES_NODE_PATH + zoneId
        );
    }

    @Override
    public Receive createReceive() {
        //noinspection deprecation
        return receiveBuilder()
                .match(
                        SensorDetectionRequested.class,
                        this::onDetectionRequested
                )
                .build();
    }

    private void onDetectionRequested(SensorDetectionRequested request) {
        if (!sensorId.equals(request.sensorId())) {
            return;
        }

        System.out.println(
                "Sensor " + sensorId + " detected an event"
        );

        zone.tell(
                new SensorDetection(sensorId, zoneId),
                getSelf()
        );
    }
}