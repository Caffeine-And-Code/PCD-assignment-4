package pcd.exercise3.middleware;

public interface DistributedLock {

    enum State{
        RELEASED,
        WANTED,
        HELD
    }

    void acquire() throws InterruptedException;
    void release();
}
