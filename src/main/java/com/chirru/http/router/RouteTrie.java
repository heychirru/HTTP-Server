package com.chirru.http.router;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Trie for HTTP route matching.
 *
 * Static segments take precedence over parameter segments.
 * Example: /users/me matches before /users/{id}.
 */
public final class RouteTrie {
    private final Node root = new Node();

    public void insert(String method, String path, Handler handler) {
        String[] segments = segments(path);
        Node current = root;

        for (String segment : segments) {
            if (isParameter(segment)) {
                if (current.parameterChild == null) {
                    current.parameterChild = new Node();
                    current.parameterName = segment.substring(1, segment.length() - 1);
                }
                current = current.parameterChild;
            } else {
                current = current.staticChildren.computeIfAbsent(segment, ignored -> new Node());
            }
        }

        if (current.handlers.containsKey(method)) {
            throw new IllegalArgumentException("Route already registered: " + method + " " + path);
        }
        current.handlers.put(method, handler);
    }

    public Match match(String method, String path) {
        Node node = findNode(path);
        if (node == null) return null;

        Handler handler = node.handlers.get(method);
        if (handler == null) return null;

        Map<String, String> params = new LinkedHashMap<>();
        findNode(root, segments(path), 0, params);
        return new Match(handler, params);
    }

    public Set<String> allowedMethods(String path) {
        Node node = findNode(path);
        return node == null ? Set.of() : Set.copyOf(node.handlers.keySet());
    }

    private Node findNode(String path) {
        Map<String, String> ignoredParams = new LinkedHashMap<>();
        return findNode(root, segments(path), 0, ignoredParams);
    }

    private Node findNode(Node node, String[] segments, int index, Map<String, String> params) {
        if (index == segments.length) return node;

        Node staticNode = node.staticChildren.get(segments[index]);
        if (staticNode != null) {
            Node result = findNode(staticNode, segments, index + 1, params);
            if (result != null) return result;
        }

        if (node.parameterChild != null) {
            params.put(node.parameterName, segments[index]);
            Node result = findNode(node.parameterChild, segments, index + 1, params);
            if (result != null) return result;
            params.remove(node.parameterName);
        }

        return null;
    }

    private static boolean isParameter(String segment) {
        return segment.length() >= 3
                && segment.startsWith("{")
                && segment.endsWith("}");
    }

    private static String[] segments(String path) {
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Route path must start with /");
        }
        if (path.equals("/")) return new String[0];
        return path.substring(1).split("/");
    }

    public record Match(Handler handler, Map<String, String> pathParams) {}

    private static final class Node {
        private final Map<String, Node> staticChildren = new LinkedHashMap<>();
        private Node parameterChild;
        private String parameterName;
        private final Map<String, Handler> handlers = new LinkedHashMap<>();
    }
}
