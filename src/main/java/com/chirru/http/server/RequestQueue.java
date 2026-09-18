package com.chirru.http.server;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bounded blocking queue implemented with wait/notify.
 */
public final class RequestQueue<T> {
    private final int capacity;
    private final Deque<T> queue = new ArrayDeque<>();
    private boolean shutdown;

    public RequestQueue(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
    }

    public synchronized boolean offer(T item) throws InterruptedException {
        if (item == null) throw new NullPointerException("item");

        while (queue.size() >= capacity && !shutdown) {
            wait();
        }

        if (shutdown) return false;
        queue.addLast(item);
        notifyAll();
        return true;
    }

    public synchronized T take() throws InterruptedException {
        while (queue.isEmpty() && !shutdown) {
            wait();
        }

        if (queue.isEmpty()) return null;
        T item = queue.removeFirst();
        notifyAll();
        return item;
    }

    public synchronized void shutdown() {
        shutdown = true;
        notifyAll();
    }

    public synchronized int size() {
        return queue.size();
    }
}
