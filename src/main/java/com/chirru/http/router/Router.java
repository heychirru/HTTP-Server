package com.chirru.http.router;

import com.chirru.http.http.HttpRequest;
import com.chirru.http.http.HttpResponse;

import java.util.Objects;
import java.util.Set;

public final class Router {
    private final RouteTrie routes = new RouteTrie();

    public Router get(String path, Handler handler) { return register("GET", path, handler); }
    public Router post(String path, Handler handler) { return register("POST", path, handler); }
    public Router put(String path, Handler handler) { return register("PUT", path, handler); }
    public Router delete(String path, Handler handler) { return register("DELETE", path, handler); }

    public HttpResponse handle(HttpRequest request) {
        RouteTrie.Match match = routes.match(request.method(), request.path());
        if (match == null) {
            Set<String> allowed = routes.allowedMethods(request.path());
            if (!allowed.isEmpty()) {
                return HttpResponse.methodNotAllowed("Method not allowed");
            }
            return HttpResponse.notFound("Route not found");
        }

        HttpRequest routedRequest = new HttpRequest(
                request.method(), request.path(), request.version(),
                request.headers(), request.body(),
                match.pathParams(), request.queryParams());

        return match.handler().handle(routedRequest);
    }

    private Router register(String method, String path, Handler handler) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(handler, "handler");
        routes.insert(method, path, handler);
        return this;
    }
}
