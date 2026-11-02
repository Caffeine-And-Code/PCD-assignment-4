package pcd.exercise1.gui;

import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import org.apache.pekko.actor.ActorRef;
import pcd.exercise1.protocols.MessagesProtocol;

final class DelaySettingsPanel extends VBox {
    private static final int EXIT_UPPER_LIMIT = 60;
    private static final int EXIT_LOWER_LIMIT = 20;
    private static final int ENTRY_UPPER_LIMIT = 30;
    private static final int ENTRY_LOWER_LIMIT = 10;

    private final DelayControl exitDelay;
    private final DelayControl entryDelay;

    DelaySettingsPanel(ActorRef uiActor) {
        super(12);
        setPadding(new Insets(10, 8, 10, 8));
        setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-border-radius: 8;");

        exitDelay = new DelayControl(
                "ExitDelay (" + EXIT_LOWER_LIMIT + "-" + EXIT_UPPER_LIMIT + ")",
                30,
                newValue -> uiActor.tell(new MessagesProtocol.UpdateExitDelay(newValue), ActorRef.noSender())
        );

        entryDelay = new DelayControl(
                "EntryDelay (" + ENTRY_LOWER_LIMIT + "-" + ENTRY_UPPER_LIMIT + ")",
                30,
                newValue -> uiActor.tell(new MessagesProtocol.UpdateEntryDelay(newValue), ActorRef.noSender())
        );

        getChildren().addAll(exitDelay, entryDelay);
    }

    public DelayControl getExitDelay() {
        return exitDelay;
    }

    public DelayControl getEntryDelay() {
        return entryDelay;
    }
}
