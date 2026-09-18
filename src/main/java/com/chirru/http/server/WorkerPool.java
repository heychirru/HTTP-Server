package com.chirru.http.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Small producer-consumer worker pool built without ExecutorService.
 */
public final class WorkerPool {
    private final RequestQueue<Runnable> queue;
    private final List<Thread> workers = new ArrayList<>();
    private volatile boolean running = true;

    public WorkerPool(int workerCount, int queueCapacity) {
        if (workerCount < 1) throw new IllegalArgumentException("Worker count must be positive");
        queue = new RequestQueue<>(queueCapacity);

        for (int i = 0; i < workerCount; i++) {
            Thread worker = new Thread(this::work, "http-worker-" + i);
            worker.start();
            workers.add(worker);
        }
    }

    public boolean submit(Runnable task) {
        if (!running) return false;
        try {
            return queue.offer(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public void shutdown() {
        if (!running) return;
        running = false;
        queue.shutdown();

        for (Thread worker : workers) {
            worker.interrupt();
        }
    }

    private void work() {
        while (running || queue.size() > 0) {
            try {
                Runnable task = queue.take();
                if (task == null) break;
                try {
                    task.run();
                } catch (RuntimeException e) {
                    System.err.println("Worker task error: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                if (!running) break;
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
