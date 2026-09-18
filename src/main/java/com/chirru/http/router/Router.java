package com.chirru.http.router;

import com.chirru.http.http.HttpRequest;
import com.chirru.http.http.HttpResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class Router {
    private final Map<RouteKey, Handler> routes = new HashMap<>();

    public Router get(String path, Handler handler) { return register("GET", path, handler); }
    public Router post(String path, Handler handler) { return register("POST", path, handler); }
    public Router put(String path, Handler handler) { return register("PUT", path, handler); }
    public Router delete(String path, Handler handler) { return register("DELETE", path, handler); }

    public HttpResponse handle(HttpRequest request) {
        Handler handler = routes.get(new RouteKey(request.method(), request.path()));
        return handler == null
                ? HttpResponse.notFound("Route not found")
                : handler.handle(request);
    }

    private Router register(String method, String path, Handler handler) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(handler, "handler");

        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("Route path must start with /");
        }

        RouteKey key = new RouteKey(method, path);
        if (routes.putIfAbsent(key, handler) != null) {
            throw new IllegalArgumentException("Route already registered: " + method + " " + path);
        }
        return this;
    }

    private record RouteKey(String method, String path) {}
}
