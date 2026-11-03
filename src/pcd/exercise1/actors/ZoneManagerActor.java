package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.Props;
import pcd.exercise1.actors.base.NodeActor;
import pcd.exercise1.actors.base.NodeName;
import pcd.exercise1.protocols.MessagesProtocol.*;
import pcd.exercise1.support.SystemConfig;

import java.util.*;
import java.util.stream.Collectors;

@NodeName("ZoneManager")
public class ZoneManagerActor extends AbstractActor implements NodeActor {
    private final Map<String, List<String>> zoneSensors = new HashMap<>();
    private final Set<String> activeZones = new HashSet<>();


    public ZoneManagerActor(
            Map<String, List<String>> initialZones
    ) {
        initialZones.forEach((zoneId, sensors) -> {
            zoneSensors.put(zoneId, new ArrayList<>(sensors));
            spawnZoneActor(zoneId);
        });
    }

    @Override
    public Receive createReceive() {
        //noinspection deprecation
        return receiveBuilder()
                // Zones Crud
                .match(GetZonesRequest.class, this::OnGetZonesRequest)
                .match(CreateZone.class, this::OnCreateZone)
                .match(DeleteZone.class, this::OnDeleteZone)
                .match(AssignSensorToZone.class, this::OnAssignSensorToZone)
                // Activate/Deactivate Zones
                .match(DeactivateAllZones.class, this::OnDeactivateAllZones)
                .match(ActivateZonesRange.class, this::OnActivateZonesRange)
                .build();
    }

    /**
     * -- Zones Crud --
     **/

    private void OnCreateZone(CreateZone createZone) {
        if (zoneSensors.containsKey(createZone.zoneId())) {
            getSender().tell(new OperationRejected("Use a unique zone name without spaces or slashes."), getSelf());
            return;
        }

        zoneSensors.put(createZone.zoneId(), new ArrayList<>());
        spawnZoneActor(createZone.zoneId());
        getSender().tell(new GetZonesReply(getZoneSensorCopy()), getSelf());
    }

    private void OnDeleteZone(DeleteZone deleteZone) {
        var zoneId = deleteZone.zoneId();
        if (!zoneSensors.containsKey(zoneId)) {
            getSender().tell(new OperationRejected("Zone does not exist."), getSelf());
            return;
        }

        if (!zoneSensors.get(zoneId).isEmpty()) {
            getSender().tell(new OperationRejected("Move all the sensors before deleting the zone."), getSelf());
            return;
        }

        zoneSensors.remove(zoneId);
        getContext().findChild(zoneId).ifPresent(child -> getContext().stop(child));
        getSender().tell(new GetZonesReply(getZoneSensorCopy()), getSelf());
    }

    private void OnAssignSensorToZone(AssignSensorToZone assignSensorToZone) {
        var zoneId = assignSensorToZone.zoneId();
        var sensorId = assignSensorToZone.sensorId();
        var oldZoneId = zoneSensors.entrySet().stream().filter(entry -> entry.getValue().contains(sensorId)).findFirst();

        if (zoneSensors.containsKey(zoneId) && oldZoneId.isPresent()
                && !oldZoneId.get().getKey().equals(zoneId)) {
            zoneSensors.get(oldZoneId.get().getKey()).remove(sensorId);
            zoneSensors.get(zoneId).add(sensorId);

            getContext().actorSelection(SystemConfig.SENSORS_NODE_PATH + sensorId)
                    .tell(new SensorZoneChanged(zoneId), getSelf());
        }

        getSender().tell(new GetZonesReply(getZoneSensorCopy()), getSelf());
    }

    private void OnGetZonesRequest(GetZonesRequest req) {
        getSender().tell(new GetZonesReply(getZoneSensorCopy()), getSelf());
    }

    /**
     * -- Activate/Deactivate Zones --
     **/

    private void OnDeactivateAllZones(DeactivateAllZones req) {
        activeZones.clear();
        getContext().getChildren()
                .forEach(child -> child.tell(new DeactiveZone(), getSelf()));

        notifyControlUnitOfZoneStateUpdate();
    }

    private void OnActivateZonesRange(ActivateZonesRange req) {
        if (!zoneSensors.keySet().containsAll(req.toActivate())) {
            getSender().tell(new OperationRejected("Some of the selected zones do not exist."), getSelf());
            notifyControlUnitOfZoneStateUpdate();
            return;
        }

        if (req.clearAlreadyActiveZones()) {
            activeZones.clear();
        }
        activeZones.addAll(req.toActivate());

        getContext().getChildren().forEach(child -> {
            if (activeZones.contains(child.path().name())) {
                child.tell(new ActiveZone(), getSelf());
            } else {
                child.tell(new DeactiveZone(), getSelf());
            }
        });

        notifyControlUnitOfZoneStateUpdate();
    }

    /**
     * -- Utils --
     **/

    private Map<String, List<String>> getZoneSensorCopy() {
        return zoneSensors.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> List.copyOf(e.getValue())
                ));
    }

    private void notifyControlUnitOfZoneStateUpdate() {
        var immutableActiveSensorMap = zoneSensors.keySet().stream().collect(
                Collectors.toUnmodifiableMap(zone -> zone, activeZones::contains)
        );

        getSender().tell(new UpdatedZoneStates(immutableActiveSensorMap), getSelf());
    }

    private void spawnZoneActor(String zoneId) {
        getContext().actorOf(
                Props.create(ZoneActor.class, () -> new ZoneActor(zoneId)),
                zoneId
        );
    }
}
