package org.example;

public interface RejectHandle {
    void reject(Runnable command, MyThreadPool myThreadPool);
}
