package org.example;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyThreadPool {

    private final int corePoolSize;
    private final int maxPoolSize;
    private final int keepAliveTime;
    private final TimeUnit unit;
    private final RejectHandle rejectHandle;
    private final BlockingQueue<Runnable> workQueue;
    private final List<Thread> coreThreads = new CopyOnWriteArrayList<>();
    private final   List<Thread> supportThreads = new CopyOnWriteArrayList<>();
    private final Lock lock = new ReentrantLock();
    private boolean isShutdown = false;

    MyThreadPool(int corePoolSize, int maxPoolSize, int keepAliveTime, TimeUnit unit, RejectHandle rejectHandle, BlockingQueue<Runnable> workQueue) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.rejectHandle = rejectHandle;
        this.workQueue = workQueue;
    }

    public void execute(Runnable command) {
        if (isShutdown) {
            throw new IllegalStateException("线程池已经关闭");
        }
        lock.lock();
        try {
            if (coreThreads.size() < corePoolSize) {
                Thread thread = new CoreThread();
                coreThreads.add(thread);
                thread.start();
            }
            if (workQueue.offer(command)) { return ;}
            if (coreThreads.size() + supportThreads.size() < maxPoolSize) {
                Thread thread = new SupportThread();
                supportThreads.add(thread);
                thread.start();
            }
            if (!workQueue.offer(command)) {
                rejectHandle.reject(command, this);
            }
        }  finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        lock.lock();
        System.out.println("线程池开始关闭");
        try {
            isShutdown = true;
            for (Thread coreThread : coreThreads) {
                try {
                    coreThread.interrupt();
                    coreThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            for (Thread supportThread : supportThreads) {
                try {
                    supportThread.interrupt();
                    supportThread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            lock.unlock();
        }

        System.out.println("线程池已经关闭");
    }

    class CoreThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Runnable command = workQueue.take();
                    command.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    class SupportThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Runnable command = workQueue.poll(keepAliveTime, unit);
                    if (command == null) {
                        break;
                    }
                    command.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println(Thread.currentThread().getName() + "结束了");
        }
    }

    public BlockingQueue<Runnable> getWorkQueue() {
        return workQueue;
    }
}
