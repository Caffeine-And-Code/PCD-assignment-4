package pcd.exercise3.middleware;

public record RicardAgrawalaMessage(
        Type type,
        String senderId,
        long timestamp,
        long requestTimestamp
) {

    public RicardAgrawalaMessage(Type type, String senderId, long timestamp) {
        this(type, senderId, timestamp, timestamp);
    }

    public enum Type{
        HELLO,
        HELLO_REPLY,
        REQUEST,
        REPLY
    }
}
