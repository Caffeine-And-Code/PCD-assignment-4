package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import pcd.exercise1.protocols.MessagesProtocol.SensorDetection;

public class SensorActor extends AbstractActor {
    public record SetZone(ActorRef zone) {
    }

    private ActorRef zoneRef;

    public SensorActor(ActorRef zoneRef) {
        this.zoneRef = zoneRef;
    }

    @Override
    public Receive createReceive() {
        //noinspection deprecation
        return receiveBuilder()
                .match(SetZone.class, message -> zoneRef = message.zone())
                .match(SensorDetection.class, this::onSensorDetection)
                .build();
    }

    public void onSensorDetection(SensorDetection sensorDetection) {
        zoneRef.tell(sensorDetection, getSelf());
    }

}
