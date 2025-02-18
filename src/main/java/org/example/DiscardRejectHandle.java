package org.example;

import java.util.concurrent.BlockingQueue;

public class DiscardRejectHandle implements RejectHandle {
    @Override
    public void reject(Runnable command, MyThreadPool myThreadPool) {
        BlockingQueue<Runnable> workQueue = myThreadPool.getWorkQueue();
        workQueue.poll();
        myThreadPool.execute(command);
    }
}
