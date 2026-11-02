package pcd.exercise1.actors.base;

public interface NodeActor {
    static <T extends NodeActor> String getNodeName(Class<T> clazz) {
        NodeName annotation = clazz.getAnnotation(NodeName.class);

        if (annotation == null) {
            throw new IllegalStateException(
                    "Missing @NodeName on " + clazz.getName()
            );
        }

        return annotation.value();
    }
}
