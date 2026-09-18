package com.chirru.http.http;

import java.util.Map;

public record HttpRequest(
        String method,
        String path,
        String version,
        Map<String, String> headers,
        String body,
        Map<String, String> pathParams) {

    public HttpRequest {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        pathParams = pathParams == null ? Map.of() : Map.copyOf(pathParams);
    }

    public HttpRequest(
            String method,
            String path,
            String version,
            Map<String, String> headers,
            String body) {
        this(method, path, version, headers, body, Map.of());
    }

    public String pathParam(String name) {
        return pathParams.get(name);
    }
}
