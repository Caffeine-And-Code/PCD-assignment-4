package pcd.exercise3.middleware;

public interface LampardClock {

    long tick();
    void update(long receivedTimestamp);
    long getTime();
}
