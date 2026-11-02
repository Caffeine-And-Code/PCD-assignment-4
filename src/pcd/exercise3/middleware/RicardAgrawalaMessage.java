package pcd.exercise3.middleware;

public record RicardAgrawalaMessage(
        Type type,
        String senderId,
        long timestamp
) {

    public enum Type{
        REQUEST,
        REPLY
    }
}
