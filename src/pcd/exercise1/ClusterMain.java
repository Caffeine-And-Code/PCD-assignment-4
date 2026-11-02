package pcd.exercise1;

import org.apache.pekko.actor.ActorSystem;
import pcd.exercise1.support.ActorFactory;
import pcd.exercise1.support.SystemConfig;

public final class ClusterMain {
    private ClusterMain() {
    }

    public static void main(String[] args) {

        String role = System.getenv().getOrDefault("CLUSTER_ROLE", "control");

        ActorSystem system = ActorSystem.create("alarm", SystemConfig.LoadRemoteConfig());

        switch (role) {
            case "control" -> ActorFactory.CreateControlUnit(system);

            case "zones" -> ActorFactory.CreateZones(system);

            case "sensors" -> ActorFactory.CreateSensors(system);

            default -> {
                System.err.println("Unknown cluster role: " + role + ", if UiActor was provided something is wrong, as if it should be local and not present in the cluster");
                system.terminate();
            }
        }
    }
}
