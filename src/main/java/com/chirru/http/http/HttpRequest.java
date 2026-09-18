package com.chirru.http.http;

import java.util.Map;

public record HttpRequest(
        String method,
        String path,
        String version,
        Map<String, String> headers,
        String body,
        Map<String, String> pathParams,
        Map<String, String> queryParams) {

    public HttpRequest {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
        pathParams = pathParams == null ? Map.of() : Map.copyOf(pathParams);
        queryParams = queryParams == null ? Map.of() : Map.copyOf(queryParams);
    }

    public HttpRequest(String method, String path, String version,
                       Map<String, String> headers, String body) {
        this(method, path, version, headers, body, Map.of(), Map.of());
    }

    public HttpRequest(String method, String path, String version,
                       Map<String, String> headers, String body,
                       Map<String, String> pathParams) {
        this(method, path, version, headers, body, pathParams, Map.of());
    }

    public String pathParam(String name) {
        return pathParams.get(name);
    }

    public String queryParam(String name) {
        return queryParams.get(name);
    }
}
