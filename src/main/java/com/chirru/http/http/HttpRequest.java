package com.chirru.http.http;

import java.util.Map;

public record HttpRequest(
        String method,
        String path,
        String version,
        Map<String, String> headers,
        String body) {
}
