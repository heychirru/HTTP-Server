package com.chirru.http.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * LRU cache implemented with HashMap + doubly linked list.
 * get/put are O(1) average time.
 */
public final class LruCache<K, V> {
    private final int capacity;
    private final Map<K, Node<K, V>> entries = new HashMap<>();
    private Node<K, V> head;
    private Node<K, V> tail;

    public LruCache(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
    }

    public synchronized V get(K key) {
        Node<K, V> node = entries.get(key);
        if (node == null) return null;
        moveToFront(node);
        return node.value;
    }

    public synchronized void put(K key, V value) {
        Node<K, V> existing = entries.get(key);
        if (existing != null) {
            existing.value = value;
            moveToFront(existing);
            return;
        }

        Node<K, V> node = new Node<>(key, value);
        entries.put(key, node);
        addFirst(node);

        if (entries.size() > capacity) {
            Node<K, V> removed = tail;
            remove(removed);
            entries.remove(removed.key);
        }
    }

    public synchronized int size() {
        return entries.size();
    }

    private void moveToFront(Node<K, V> node) {
        if (node == head) return;
        remove(node);
        addFirst(node);
    }

    private void addFirst(Node<K, V> node) {
        node.prev = null;
        node.next = head;
        if (head != null) head.prev = node;
        else tail = node;
        head = node;
    }

    private void remove(Node<K, V> node) {
        if (node.prev != null) node.prev.next = node.next;
        else head = node.next;

        if (node.next != null) node.next.prev = node.prev;
        else tail = node.prev;

        node.prev = null;
        node.next = null;
    }

    private static final class Node<K, V> {
        private final K key;
        private V value;
        private Node<K, V> prev;
        private Node<K, V> next;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
