package pcd.exercise1.actors;

import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.japi.pf.ReceiveBuilder;
import pcd.exercise1.actors.base.NodeActor;
import pcd.exercise1.actors.base.NodeName;
import pcd.exercise1.protocols.MessagesProtocol.*;
import pcd.exercise1.protocols.SystemState;
import java.time.Duration;

@NodeName("ControlUnit")
public class ControlUnitActor extends AbstractActor implements NodeActor {
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

    private SystemState state = SystemState.RECOVERY;
    private int exitDelay = 30;
    private int entryDelay = 30;

    private Cancellable entryTimeout;
    private Cancellable exitTimeout;

    public ControlUnitActor(ActorSelection zoneManagerSelection, ActorSelection uiActorSelection) {
        this.zoneManagerSelection = zoneManagerSelection;
        this.uiActorSelection = uiActorSelection;
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
                .match(DeleteZone.class, this::OnDeleteZone)
                .match(AssignSensorToZone.class, this::OnAssignSensorToZone)
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
                .match(GetZonesReply.class, this::OnZonesGetReply)
                .match(UpdatedZoneStates.class, this::OnUpdatedZoneStates)
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
        zoneManagerSelection.tell(req, getSelf());
    }

    private void onGetSystemStateRequest(GetSystemStateRequest request) {
        uiActorSelection.tell(new SystemStateUpdated(state), getSelf());
        uiActorSelection.tell(new ExitDelayUpdated(exitDelay), getSelf());
        uiActorSelection.tell(new EntryDelayUpdated(entryDelay), getSelf());
        zoneManagerSelection.tell(new GetZonesRequest(), getSelf());
    }

    public void OnCreateZone(CreateZone createZone) {
        var zoneId = createZone.zoneId() == null ? "" : createZone.zoneId().trim();
        if (!zoneId.isEmpty()) {
            zoneManagerSelection.tell(createZone, getSelf());
        }
    }

    public void OnZonesGetReply(GetZonesReply reply){
        uiActorSelection.tell(reply, getSelf());
    }

    public void OnDeleteZone(DeleteZone deleteZone) {
        zoneManagerSelection.tell(deleteZone, getSelf());
    }

    public void OnAssignSensorToZone(AssignSensorToZone assignSensorToZone) {
        zoneManagerSelection.tell(assignSensorToZone, getSelf());
    }

    public void OnUpdatedZoneStates(UpdatedZoneStates updatedZoneStates){
        uiActorSelection.tell(updatedZoneStates, getSelf());
    }

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
            if (pinEntered.zonesToActive() == null || pinEntered.zonesToActive().isEmpty()) {
                getSender().tell(new OperationRejected("Select at least one existing zone before arming."), getSelf());
                return;
            }
            zoneManagerSelection.tell(new ActivateZonesRange(pinEntered.zonesToActive(), true), getSelf());
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
        enterEntryDelay();
    }

    public void OnExitTimeout(ExitTimeout exitTimeout) {
        enterArmed();
    }

    public void OnEntryTimeout(EntryTimeout entryTimeout) {
        enterAlarm();
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
        getContext().become(armed());
        updateState(SystemState.ARMED);
    }

    private void enterAlarm() {
        getContext().become(alarm());
        updateState(SystemState.ALARM);
    }

    private void enterDisarmed() {
        postStop();

        zoneManagerSelection.tell(new DeactivateAllZones(), getSelf());
        getContext().become(disarmed());
        updateState(SystemState.DISARMED);
    }

    private void updateState(SystemState newState) {
        state = newState;
        if (uiActorSelection != null) {
            uiActorSelection.tell(new AlarmActiveUpdate(state == SystemState.ALARM), getSelf());
            uiActorSelection.tell(new SystemStateUpdated(state), getSelf());
        }
    }

    @Override
    public void postStop() {
        if (exitTimeout != null) exitTimeout.cancel();
        if (entryTimeout != null) entryTimeout.cancel();
    }
}
