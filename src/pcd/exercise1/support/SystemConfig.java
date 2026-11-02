package pcd.exercise1.support;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public abstract class SystemConfig {
    public static final String CONTROL_NODE_PATH = "pekko://alarm@node1:2551/user/";
    public static final String ZONES_NODE_PATH = "pekko://alarm@node2:2551/user/";
    public static final String SENSORS_NODE_PATH = "pekko://alarm@node3:2551/user/";
    public static final String CONTROL_NODE_PATH_FOR_LOCAL_ENVIROMENT =
            "pekko://alarm@localhost:2551/user/controlUnit";

    public static Config LoadRemoteConfig() {
        return ConfigFactory.load("cluster.conf");
    }

    public static Config LoadLocalConfig() {
        return ConfigFactory.load("local-application.conf");
    }
}
