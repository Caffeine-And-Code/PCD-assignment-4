package pcd.exercise1.protocols;

import javafx.scene.control.Label;
import pcd.exercise1.gui.DelayControl;
import pcd.exercise1.gui.ZoneOverviewPanel;

import java.util.List;
import java.util.Map;

public abstract class MessagesProtocol {

    // Internally for actor communication
    public record ActiveZone() {
    }

    public record DeactiveZone() {
    }

    public record ZoneActivated(String zoneId) {
    }

    public record ZoneDeactivated(String zoneId) {
    }

    // From UI to actors
    public record GetZonesRequest() {
    }

    public record GetSystemStateRequest() {
    }

    public record CreateZone(String zoneId) {
    }

    // Only if no sensor in the zone is present
    public record DeleteZone(String zoneId) {
    }

    public record AssignSensorToZone(String zoneId, String sensorId) {
    }

    public record UpdateExitDelay(int seconds) {
    }

    public record UpdateEntryDelay(int seconds) {
    }

    public record PinEnteredWithZones(String pin, List<String> zonesToActive) {
    }

    public record PinEntered(String pin) {
    }

    public record SensorDetection() {
    }

    // To UI from actors
    public record GetZonesReply(Map<String, List<String>> zoneSensors) {
    }

    public record SystemStateUpdated(SystemState state) {
    }

    public record ExitDelayUpdated(int seconds) {
    }

    public record EntryDelayUpdated(int seconds) {
    }

    public record InvalidPinEntered() {
    }

    public record UpdatedZoneStates(Map<String, Boolean> zoneStates) {
    }

    public record UpdatedZoneSensors(Map<String, List<String>> zoneSensors) {
    }

    public record AlarmActiveUpdate(boolean alarmActive) {
    }

    public record OperationRejected(String reason) {
    }

    // From UI to UIActor
    public record SensorDetectionRequested(String sensorId) {
    }

    public record EnterPinDigit(int digit, List<String> zonesToActivate) {
    }

    public record BindUi(DelayControl entryDelayLabel, DelayControl exitDelayLabel, ZoneOverviewPanel zoneCanvas,
                         Label systemStateLabel, Label errorLabel) {
    }
}
