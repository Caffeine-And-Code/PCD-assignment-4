package pcd.exercise3.middleware;

public class LampardClockImpl implements LampardClock{

    private long clock;

    @Override
    public synchronized long tick() {
        return ++clock;
    }

    @Override
    public synchronized void update(long receivedTimestamp) {
        clock = Math.max(clock, receivedTimestamp) + 1;
    }

    @Override
    public synchronized long getTime() {
        return clock;
    }
}
