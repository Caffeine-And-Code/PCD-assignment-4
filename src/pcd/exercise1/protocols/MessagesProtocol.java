package pcd.exercise1.protocols;

import javafx.scene.control.Label;
import pcd.exercise1.gui.DelayControl;
import pcd.exercise1.gui.ZoneOverviewPanel;

import java.util.List;
import java.util.Map;

public abstract class MessagesProtocol {

    // Internally for actor communication
    public record ActiveZone() implements Serializable {
    }

    public record DeactiveZone() implements Serializable {
    }

    public record ZoneActivated(String zoneId) implements Serializable {
    }

    public record ZoneDeactivated(String zoneId) implements Serializable {
    }

    // From UI to actors
    public record GetZonesRequest() implements Serializable {
    }

    public record GetSystemStateRequest() implements Serializable {
    }

    public record CreateZone(String zoneId) implements Serializable {
    }

    // Only if no sensor in the zone is present
    public record DeleteZone(String zoneId) implements Serializable {
    }

    public record AssignSensorToZone(String zoneId, String sensorId) implements Serializable {
    }

    public record UpdateExitDelay(int seconds) implements Serializable {
    }

    public record UpdateEntryDelay(int seconds) implements Serializable {
    }

    public record PinEnteredWithZones(String pin, List<String> zonesToActive) implements Serializable {
    }

    public record PinEntered(String pin) implements Serializable {
    }

    public record SensorDetection(
            String sensorId,
            String zoneId
    ) implements Serializable {
    }

    // To UI from actors
    public record GetZonesReply(Map<String, List<String>> zoneSensors) implements Serializable {
    }

    public record SystemStateUpdated(SystemState state) implements Serializable {
    }

    public record ExitDelayUpdated(int seconds) implements Serializable {
    }

    public record EntryDelayUpdated(int seconds) implements Serializable {
    }

    public record InvalidPinEntered() implements Serializable {
    }

    public record UpdatedZoneStates(Map<String, Boolean> zoneStates) implements Serializable {
    }

    public record UpdatedZoneSensors(Map<String, List<String>> zoneSensors) implements Serializable {
    }

    public record AlarmActiveUpdate(boolean alarmActive) implements Serializable {
    }

    public record OperationRejected(String reason) implements Serializable {
    }

    // From UI to UIActor
    public record SensorDetectionRequested(String sensorId) implements Serializable {
    }

    public record EnterPinDigit(int digit, List<String> zonesToActivate) implements Serializable {
    }

    public record BindUi(DelayControl entryDelayLabel, DelayControl exitDelayLabel, ZoneOverviewPanel zoneCanvas,
                         Label systemStateLabel, Label errorLabel) {
    }
}
