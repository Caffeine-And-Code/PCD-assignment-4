package pcd.exercise1.actors;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.apache.pekko.actor.AbstractActorWithStash;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.Props;
import pcd.exercise1.actors.base.NodeActor;
import pcd.exercise1.actors.base.NodeName;
import pcd.exercise1.gui.DelayControl;
import pcd.exercise1.gui.ZoneOverviewPanel;
import pcd.exercise1.protocols.MessagesProtocol.*;
import pcd.exercise1.protocols.SystemState;
import pcd.exercise1.support.SystemConfig;

@NodeName("UiActor")
public class UIActor extends AbstractActorWithStash implements NodeActor {
    private static final int PIN_LENGTH = 5;

    private final ActorSelection controlUnit;

    private DelayControl entryDelayRef;
    private DelayControl exitDelayRef;
    private ZoneOverviewPanel zoneCanvas;
    private Label errorLabel;
    private Label systemStateLabel;
    private String currentPin = "";
    private SystemState systemState = SystemState.RECOVERY;

    public static Props props(ActorSelection controlUnit) {
        return Props.create(UIActor.class, () -> new UIActor(controlUnit));
    }

    private UIActor(ActorSelection controlUnit) {
        this.controlUnit = controlUnit;
    }

    @Override
    public Receive createReceive() {
        //noinspection deprecation
        return receiveBuilder()
                .match(BindUi.class, this::handleBindUi)
                .matchAny(msg -> stash())
                .build();
    }

    private Receive ready() {
        //noinspection deprecation
        return receiveBuilder()
                .match(BindUi.class, this::handleBindUi) // enables the re-bind
                // "backend" messages
                .match(GetZonesReply.class, this::handleGetZoneReply)
                .match(SystemStateUpdated.class, this::handleSystemStateUpdated)
                .match(ExitDelayUpdated.class, this::handleExitDelayUpdated)
                .match(EntryDelayUpdated.class, this::handleEntryDelayUpdated)
                .match(InvalidPinEntered.class, this::handleInvalidPinEntered)
                .match(OperationRejected.class, msg -> showError(msg.reason()))
                .match(UpdatedZoneStates.class, this::handleUpdatedZoneStates)
                .match(UpdatedZoneSensors.class, this::handleUpdatedZoneSensors)
                .match(AlarmActiveUpdate.class, this::handleAlarmActiveUpdate)
                // "frontend" messages
                .match(AssignSensorToZone.class, this::onAssignSensorToZone)
                .match(EnterPinDigit.class, this::onEnterPinDigit)
                .match(UpdateEntryDelay.class, this::onUpdateEntryRequest)
                .match(UpdateExitDelay.class, this::onUpdateExitRequest)
                .match(GetZonesRequest.class, this::onGetZoneRequest)
                .match(GetSystemStateRequest.class, this::onGetSystemStateRequest)
                .match(DeleteZone.class, msg -> controlUnit.tell(msg, getSelf()))
                .match(SensorDetectionRequested.class, this::onSensorDetectionRequested)
                .match(CreateZone.class, this::onCreateZoneRequest)
                .build();
    }

    private void handleBindUi(BindUi ui) {
        this.entryDelayRef = ui.entryDelayLabel();
        this.exitDelayRef = ui.exitDelayLabel();
        this.errorLabel = ui.errorLabel();
        this.zoneCanvas = ui.zoneCanvas();
        this.systemStateLabel = ui.systemStateLabel();

        // now process the stashed messages
        getContext().become(ready());
        unstashAll();
    }

    /**
     * -- Responses from the Backend --
     **/

    private void handleGetZoneReply(GetZonesReply data) {
        Platform.runLater(() -> this.zoneCanvas.setSensorsByZone(data.zoneSensors()));
    }

    private void handleExitDelayUpdated(ExitDelayUpdated data) {
        Platform.runLater(() -> exitDelayRef.setSeconds(data.seconds()));
    }

    private void handleEntryDelayUpdated(EntryDelayUpdated data) {
        Platform.runLater(() -> entryDelayRef.setSeconds(data.seconds()));
    }

    private void handleInvalidPinEntered(InvalidPinEntered data) {
        showError("Invalid PIN provided");
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            this.errorLabel.setText(message);
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> errorLabel.setText(""));
            pause.play();
        });
    }

    private void handleSystemStateUpdated(SystemStateUpdated data) {
        systemState = data.state();
        String label = switch (data.state()) {
            case DISARMED -> "Disarmed";
            case EXIT_DELAY -> "Exit delay";
            case ARMED -> "Armed";
            case ENTRY_DELAY -> "Entry delay";
            case ALARM -> "Alarm";
            case RECOVERY -> "Recovering...";
        };

        Platform.runLater(() -> {
            systemStateLabel.setText(label);
            zoneCanvas.setEditingEnabled(data.state() == SystemState.DISARMED);
            zoneCanvas.setAlarmActive(data.state() == SystemState.ALARM);
        });
    }

    private void handleUpdatedZoneStates(UpdatedZoneStates data) {
        Platform.runLater(() -> zoneCanvas.setZoneStates(data.zoneStates()));
    }

    private void handleUpdatedZoneSensors(UpdatedZoneSensors data) {
        Platform.runLater(() -> this.zoneCanvas.setSensorsByZone(data.zoneSensors()));
    }

    private void handleAlarmActiveUpdate(AlarmActiveUpdate data) {
        Platform.runLater(() -> zoneCanvas.setAlarmActive(data.alarmActive()));
    }

    /**
     * -- Frontend Requests forwarded to ControlUnit --
     **/

    private void onSensorDetectionRequested(SensorDetectionRequested request) {
        getContext().actorSelection(
                SystemConfig.SENSORS_NODE_PATH + request.sensorId()
        ).tell(request, getSelf());
    }

    private void onCreateZoneRequest(CreateZone createZone) {
        controlUnit.tell(createZone, this.getSelf());
    }

    private void onUpdateExitRequest(UpdateExitDelay data) {
        controlUnit.tell(data, this.getSelf());
    }

    private void onUpdateEntryRequest(UpdateEntryDelay data) {
        controlUnit.tell(data, this.getSelf());
    }

    private void onGetZoneRequest(GetZonesRequest data) {
        controlUnit.tell(data, this.getSelf());
    }

    private void onGetSystemStateRequest(GetSystemStateRequest data) {
        controlUnit.tell(data, getSelf());
    }

    private void onAssignSensorToZone(AssignSensorToZone assignSensorToZone) {
        controlUnit.tell(assignSensorToZone, getSelf());
    }

    private void onEnterPinDigit(EnterPinDigit data) {
        if (data.digit() < 0 || data.digit() > 9) return;
        if (currentPin.length() >= PIN_LENGTH) {
            return;
        }

        currentPin += data.digit();

        if (currentPin.length() >= PIN_LENGTH) {
            if (systemState == SystemState.DISARMED) {
                controlUnit.tell(new PinEnteredWithZones(currentPin, data.zonesToActivate()), getSelf());
            } else {
                controlUnit.tell(new PinEntered(currentPin), getSelf());
            }

            currentPin = "";
        }
    }
}
