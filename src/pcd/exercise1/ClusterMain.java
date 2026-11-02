package pcd.exercise1;

import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.actor.Props;
import pcd.exercise1.actors.ControlUnitActor;

import java.util.List;
import java.util.Map;

public final class ClusterMain {

    private static final Map<String, List<String>> INITIAL_ZONES = Map.of(
            "Atrio", List.of("Porta1", "Movimento1", "Finestra1", "Finestra2"),
            "Soggiorno", List.of("Porta2", "Porta3", "Movimento2", "Finestra3")
    );

    private ClusterMain() {
    }

    public static void main(String[] args) {

        String role = System.getenv().getOrDefault("CLUSTER_ROLE", "control");

        ActorSystem system = ActorSystem.create("alarm");

        System.out.println("Starting cluster node with role: " + role);

        switch (role) {
            case "control" -> {
                system.actorOf(
                        Props.create(ControlUnitActor.class,
                                () -> new ControlUnitActor(INITIAL_ZONES)),
                        "controlUnit"
                );

                System.out.println("Control Unit started");
            }

            case "zones" -> {
                System.out.println("Zones node started");
            }

            case "sensors" -> {
                System.out.println("Sensors node started");
            }

            case "keypad" -> {
                System.out.println("Keypad node started");
            }

            default -> {
                System.err.println("Unknown cluster role: " + role);
                system.terminate();
            }
        }
    }
}
