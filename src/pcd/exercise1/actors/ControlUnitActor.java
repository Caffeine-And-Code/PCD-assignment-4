package pcd.exercise1.actors;

import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import pcd.exercise1.protocols.MessagesProtocol.*;
import pcd.exercise1.protocols.SystemState;
import pcd.exercise1.support.SystemConfig;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class ControlUnitActor extends Actor {
    public String NODE_NAME = "";
    private static final int EXIT_LOWER_LIMIT = 20;
    private static final int EXIT_UPPER_LIMIT = 60;
    private static final int ENTRY_LOWER_LIMIT = 10;
    private static final int ENTRY_UPPER_LIMIT = 30;
    private static final String CORRECT_PIN = "12345";

    public record ExitTimeout() {
    }

    public record EntryTimeout() {
    }

    private final ActorSelection uiActorSelection;
    private final ActorSelection zoneManagerSelection;

    private final HashMap<String, List<String>> zoneSensors = new HashMap<>();
    private SystemState state = SystemState.RECOVERY;
    private int exitDelay = 30;
    private int entryDelay = 30;

    private Cancellable entryTimeout;
    private Cancellable exitTimeout;

    private final Set<String> activeZones = new HashSet<>();

    public ControlUnitActor(ActorSelection zoneManagerSelection, ActorSelection uiActorSelection) {
        this.zoneManagerSelection = zoneManagerSelection;
        this.uiActorSelection = uiActorSelection;
    }

    private ActorSelection getZone(String zoneId) {
        return getContext().actorSelection(SystemConfig.ZONES_NODE_PATH + zoneId);
    }

    @Override
    public Receive createReceive() {
        return recovery();
    }

    private Receive recovery() {
        //noinspection deprecation
        return defaultBuilder()
                .match(PinEntered.class, this::onPinEnteredFromRecovery)
                .match(SensorDetection.class, detection ->
                        System.out.println(
                                "Ignoring sensor detection during recovery: "
                                        + detection.sensorId()
                        )
                )
                .build();
    }

    private Receive disarmed() {
        //noinspection deprecation
        return defaultBuilder()
                .match(CreateZone.class, this::OnCreateZone)
                //.match(DeleteZone.class, this::OnDeleteZone)
                //.match(AssignSensorToZone.class, this::OnAssignSensorToZone)
                .match(UpdateExitDelay.class, this::OnUpdateExitDelay)
                .match(UpdateEntryDelay.class, this::OnUpdateEntryDelay)
                .match(PinEnteredWithZones.class, this::OnPinEnteredFromDisarmed)
                .build();
    }

    private Receive exitDelay() {
        //noinspection deprecation
        return defaultBuilder()
                .match(PinEntered.class, this::OnPinEnteredFromArmed)
                .match(ExitTimeout.class, this::OnExitTimeout)
                .build();
    }

    private Receive armed() {
        //noinspection deprecation
        return defaultBuilder()
                .match(SensorDetection.class, this::OnSensorDetection)
                .match(PinEntered.class, this::OnPinEnteredFromArmed)
                .build();
    }

    private Receive entryDelay() {
        //noinspection deprecation
        return defaultBuilder()
                .match(EntryTimeout.class, this::OnEntryTimeout)
                .match(PinEntered.class, this::OnPinEnteredFromArmed)
                .build();
    }

    private Receive alarm() {
        //noinspection deprecation
        return defaultBuilder()
                .match(PinEntered.class, this::OnPinEnteredFromArmed)
                .build();
    }

    private ReceiveBuilder defaultBuilder() {
        //noinspection deprecation
        return receiveBuilder()
                .match(GetZonesRequest.class, this::OnGetZonesRequest)
                .match(GetSystemStateRequest.class, this::onGetSystemStateRequest);
    }

    private void onPinEnteredFromRecovery(PinEntered message) {
        if (message.pin().equals(CORRECT_PIN)) {
            enterDisarmed();
            return;
        }

        getSender().tell(new InvalidPinEntered(), getSelf());
    }

    public void OnGetZonesRequest(GetZonesRequest req) {
        getSender().tell(new GetZonesReply(getZoneSensorCopy()), getSelf());
    }

    private void onGetSystemStateRequest(GetSystemStateRequest request) {
        uiActorSelection.tell(new SystemStateUpdated(state), getSelf());
        uiActorSelection.tell(new ExitDelayUpdated(exitDelay), getSelf());
        uiActorSelection.tell(new EntryDelayUpdated(entryDelay), getSelf());
        publishZones();
    }


    public void OnCreateZone(CreateZone createZone) {
        var zoneId = createZone.zoneId() == null ? "" : createZone.zoneId().trim();
        if (!zoneId.isEmpty()) {
            getContext().actorOf(Props.create(ZoneActor.class, getSelf(), zoneId), zoneId);
        }

        sendUpdatedZoneSensors(getSender());
        publishZones();
    }

    /**
    public void OnDeleteZone(DeleteZone deleteZone) {
        var zoneId = deleteZone.zoneId();
        if (zoneSensors.containsKey(zoneId) && zoneSensors.get(zoneId).isEmpty()) {
            zoneSensors.remove(zoneId);
            getContext().findChild(zoneId).ifPresent(zoneRef -> getContext().stop(zoneRef));
        }

        sendUpdatedZoneSensors(getSender());
        publishZones();
    }

    public void OnAssignSensorToZone(AssignSensorToZone assignSensorToZone) {
        var zoneId = assignSensorToZone.zoneId();
        var sensorId = assignSensorToZone.sensorId();
        if (zoneSensors.containsKey(zoneId) && zoneSensors.values().stream().anyMatch(s -> s.contains(sensorId))) {
            zoneSensors.values().forEach(sensors -> sensors.remove(sensorId));
            zoneSensors.get(zoneId).add(sensorId);
     //getContext().findChild(sensorId).ifPresent(sensor ->
     //        sensor.tell(new SensorActor.SetZone(getContext().findChild(zoneId).orElseThrow()), getSelf()));
        }

        sendUpdatedZoneSensors(getSender());
        publishZones();
    }
     **/

    public void OnUpdateExitDelay(UpdateExitDelay updateExitDelay) {
        var seconds = updateExitDelay.seconds();
        if (seconds >= EXIT_LOWER_LIMIT && seconds <= EXIT_UPPER_LIMIT) {
            exitDelay = seconds;
        }

        getSender().tell(new ExitDelayUpdated(exitDelay), getSelf());
    }

    public void OnUpdateEntryDelay(UpdateEntryDelay updateEntryDelay) {
        var seconds = updateEntryDelay.seconds();
        if (seconds >= ENTRY_LOWER_LIMIT && seconds <= ENTRY_UPPER_LIMIT) {
            entryDelay = seconds;
        }

        getSender().tell(new EntryDelayUpdated(entryDelay), getSelf());
    }

    public void OnPinEnteredFromDisarmed(PinEnteredWithZones pinEntered) {
        var pinAttempt = pinEntered.pin();
        if (pinAttempt.equals(CORRECT_PIN)) {
            if (pinEntered.zonesToActive() == null || pinEntered.zonesToActive().isEmpty()
                    || !zoneSensors.keySet().containsAll(pinEntered.zonesToActive())) {
                getSender().tell(new OperationRejected("Select at least one existing zone before arming."), getSelf());
                return;
            }
            activeZones.clear();
            activeZones.addAll(pinEntered.zonesToActive());
            enterExitDelay();
        } else {
            getSender().tell(new InvalidPinEntered(), getSelf());
        }
    }

    public void OnPinEnteredFromArmed(PinEntered pinEntered) {
        var pinAttempt = pinEntered.pin();
        if (pinAttempt.equals(CORRECT_PIN)) {
            if (entryTimeout != null) {
                entryTimeout.cancel();
            }
            enterDisarmed();
        } else {
            getSender().tell(new InvalidPinEntered(), getSelf());
        }
    }

    public void OnSensorDetection(SensorDetection sensorDetection) {
        if (activeZones.contains(sensorDetection.zoneId())) {
            enterEntryDelay();
        }
    }

    public void OnExitTimeout(ExitTimeout exitTimeout) {
        enterArmed();
    }

    public void OnEntryTimeout(EntryTimeout entryTimeout) {
        enterAlarm();
    }

    private void sendUpdatedZoneSensors(ActorRef to) {
        to.tell(new UpdatedZoneSensors(getZoneSensorCopy()), getSelf());
    }

    private void enterExitDelay() {
        if (exitTimeout != null) {
            exitTimeout.cancel();
        }

        getContext().become(exitDelay());
        updateState(SystemState.EXIT_DELAY);

        exitTimeout = getContext().getSystem().getScheduler().scheduleOnce(
                Duration.ofSeconds(exitDelay),
                getSelf(),
                new ExitTimeout(),
                getContext().getDispatcher(),
                getSelf()
        );
    }

    private void enterEntryDelay() {
        if (entryTimeout != null) {
            entryTimeout.cancel();
        }

        getContext().become(entryDelay());
        updateState(SystemState.ENTRY_DELAY);

        entryTimeout = getContext().getSystem().getScheduler().scheduleOnce(
                Duration.ofSeconds(entryDelay),
                getSelf(),
                new EntryTimeout(),
                getContext().getDispatcher(),
                getSelf()
        );
    }

    private void enterArmed() {
        activeZones.forEach(zone ->
                getZone(zone).tell(new ActiveZone(), getSelf())
        );

        getContext().become(armed());
        updateState(SystemState.ARMED);
    }

    private void enterAlarm() {
        getContext().become(alarm());
        updateState(SystemState.ALARM);
    }

    private void enterDisarmed() {
        if (exitTimeout != null) exitTimeout.cancel();
        if (entryTimeout != null) entryTimeout.cancel();

        activeZones.clear();

        zoneSensors.keySet().forEach(zone ->
                getZone(zone).tell(new DeactiveZone(), getSelf())
        );

        getContext().become(disarmed());
        updateState(SystemState.DISARMED);
    }

    private void updateState(SystemState newState) {
        state = newState;
        publishZones();
        if (uiActorSelection != null) {
            uiActorSelection.tell(new SystemStateUpdated(state), getSelf());
        }
    }

    private void publishZones() {
        if (uiActorSelection != null) {
            uiActorSelection.tell(new UpdatedZoneStates(zoneSensors.keySet().stream().collect(
                    Collectors.toUnmodifiableMap(zone -> zone, zone ->
                            state != SystemState.DISARMED && state != SystemState.EXIT_DELAY && activeZones.contains(zone)))), getSelf());
            uiActorSelection.tell(new AlarmActiveUpdate(state == SystemState.ALARM), getSelf());
        }
    }

    @Override
    public void postStop() {
        if (exitTimeout != null) exitTimeout.cancel();
        if (entryTimeout != null) entryTimeout.cancel();
    }

    private Map<String, List<String>> getZoneSensorCopy() {
        return zoneSensors.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> List.copyOf(e.getValue())
                ));
    }
}
