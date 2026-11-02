package pcd.exercise1.support;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public abstract class SystemConfig {
    private static final String HOST = "host.docker.internal";

    public static final String CONTROL_NODE_PATH = "pekko://alarm@" + HOST + ":2551/user/";
    public static final String ZONES_NODE_PATH = "pekko://alarm@" + HOST + ":2552/user/";
    public static final String SENSORS_NODE_PATH = "pekko://alarm@" + HOST + ":2553/user/";
    public static final String CONTROL_NODE_PATH_FOR_LOCAL_ENVIROMENT = CONTROL_NODE_PATH + "controlUnit";
    public static final String UIACTOR_NODE_PATH = "/user/uiActor";

    public static Config LoadRemoteConfig() {
        return ConfigFactory.load("cluster.conf");
    }

    public static Config LoadLocalConfig() {
        return ConfigFactory.load("local-application.conf");
    }
}
