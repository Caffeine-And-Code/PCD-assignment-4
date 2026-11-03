package pcd.exercise1.support;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import pcd.exercise1.actors.ControlUnitActor;
import pcd.exercise1.actors.base.NodeActor;

public abstract class SystemConfig {
    private static final String HOST = "host.docker.internal";

    public static final String CONTROL_NODE_PATH = "pekko://alarm@" + HOST + ":2551/user/";
    public static final String ZONES_NODE_PATH = "pekko://alarm@" + HOST + ":2552/user/";
    public static final String SENSORS_NODE_PATH = "pekko://alarm@" + HOST + ":2553/user/";
    public static final String CONTROL_NODE_PATH_FOR_LOCAL_ENVIROMENT = CONTROL_NODE_PATH + NodeActor.getNodeName(ControlUnitActor.class);
    public static final String UIACTOR_NODE_PATH =  "pekko://alarm@" + HOST + ":2550/user/";

    public static Config LoadRemoteConfig() {
        return ConfigFactory.load("cluster.conf");
    }

    public static Config LoadLocalConfig() {
        return ConfigFactory.load("local-application.conf");
    }
}
