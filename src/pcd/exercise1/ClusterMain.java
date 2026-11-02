package pcd.exercise1;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.pekko.actor.ActorSystem;
import pcd.exercise1.support.ActorFactory;

public final class ClusterMain {
    private ClusterMain() {
    }

    public static void main(String[] args) {

        String role = System.getenv().getOrDefault("CLUSTER_ROLE", "control");

        Config config = ConfigFactory.load("cluster.conf");
        ActorSystem system = ActorSystem.create("alarm", config);

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
