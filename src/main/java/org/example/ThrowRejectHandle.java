package org.example;

public class ThrowRejectHandle implements RejectHandle {
    @Override
    public void reject(Runnable command, MyThreadPool myThreadPool) {
        throw new RuntimeException("阻塞队列满了");
    }
}
