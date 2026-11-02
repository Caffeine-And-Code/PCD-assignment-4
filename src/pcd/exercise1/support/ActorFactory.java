package pcd.exercise1.support;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import pcd.exercise1.actors.ControlUnitActor;
import pcd.exercise1.actors.SensorActor;
import pcd.exercise1.actors.ZoneManagerActor;
import pcd.exercise1.actors.base.NodeActor;

import java.util.List;
import java.util.Map;

public abstract class ActorFactory {
    private static final Map<String, List<String>> INITIAL_ZONES = Map.of(
            "Atrio", List.of("Porta1", "Movimento1", "Finestra1", "Finestra2"),
            "Soggiorno", List.of("Porta2", "Porta3", "Movimento2", "Finestra3")
    );

    public static void CreateControlUnit(ActorSystem system) {
        var uiActor = system.actorSelection(
                SystemConfig.UIACTOR_NODE_PATH + NodeActor.getNodeName(ControlUnitActor.class)
        );
        var zoneManager = system.actorSelection(
                SystemConfig.ZONES_NODE_PATH + NodeActor.getNodeName(ZoneManagerActor.class)
        );

        system.actorOf(
                Props.create(ControlUnitActor.class,
                        () -> new ControlUnitActor(zoneManager, uiActor)),
                NodeActor.getNodeName(ControlUnitActor.class)
        );

        System.out.println("Control Unit started");
    }

    public static void CreateZones(ActorSystem system) {
        system.actorOf(
                Props.create(ZoneManagerActor.class,
                        () -> new ZoneManagerActor(INITIAL_ZONES)),
                NodeActor.getNodeName(ZoneManagerActor.class)
        );

        System.out.println("Zones and ZoneManager actors started");
    }

    public static void CreateSensors(ActorSystem system) {
        for (String zoneId : INITIAL_ZONES.keySet()) {
            for (String sensorId : INITIAL_ZONES.getOrDefault(zoneId, List.of())) {
                system.actorOf(
                        Props.create(
                                SensorActor.class,
                                () -> new SensorActor(sensorId, zoneId)
                        ),
                        sensorId
                );
            }
        }

        System.out.println("Sensors actors started");
    }
}
