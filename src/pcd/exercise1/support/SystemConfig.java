package pcd.exercise1.support;

public abstract class SystemConfig {
    public static final String CONTROL_NODE_PATH = "pekko://alarm@node1:2551/user/";
    public static final String ZONES_NODE_PATH = "pekko://alarm@node2:2551/user/";
    public static final String SENSORS_NODE_PATH = "pekko://alarm@node3:2551/user/";
}
