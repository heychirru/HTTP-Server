package com.chirru.http.router;

import java.util.LinkedHashMap;
import java.util.Map;

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
        String[] segments = segments(path);
        Map<String, String> params = new LinkedHashMap<>();
        Node node = match(root, segments, 0, params);

        if (node == null) {
            return null;
        }

        Handler handler = node.handlers.get(method);
        return handler == null ? null : new Match(handler, params);
    }

    private Node match(Node node, String[] segments, int index, Map<String, String> params) {
        if (index == segments.length) {
            return node;
        }

        Node staticNode = node.staticChildren.get(segments[index]);
        if (staticNode != null) {
            Node result = match(staticNode, segments, index + 1, params);
            if (result != null) {
                return result;
            }
        }

        if (node.parameterChild != null) {
            params.put(node.parameterName, segments[index]);
            Node result = match(node.parameterChild, segments, index + 1, params);
            if (result != null) {
                return result;
            }
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
        if (path.equals("/")) {
            return new String[0];
        }
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
