package com.chirru.http.http;

import java.nio.charset.StandardCharsets;

public record HttpResponse(int statusCode, String reason, String contentType, byte[] body) {

    public static HttpResponse ok(String contentType, String body) {
        return new HttpResponse(200, "OK", contentType, body.getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse json(String json) {
        return ok("application/json; charset=UTF-8", json);
    }

    public static HttpResponse created(String body) {
        return new HttpResponse(201, "Created", "text/plain; charset=UTF-8",
                body.getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse createdJson(String json) {
        return new HttpResponse(201, "Created", "application/json; charset=UTF-8",
                json.getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse badRequest(String body) {
        return new HttpResponse(400, "Bad Request", "text/plain; charset=UTF-8",
                body.getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse forbidden(String body) {
        return new HttpResponse(403, "Forbidden", "text/plain; charset=UTF-8",
                body.getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse notFound(String body) {
        return new HttpResponse(404, "Not Found", "text/plain; charset=UTF-8",
                body.getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse internalServerError(String body) {
        return new HttpResponse(500, "Internal Server Error", "text/plain; charset=UTF-8",
                body.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] toBytes() {
        String headers = "HTTP/1.1 " + statusCode + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";
        byte[] headerBytes = headers.getBytes(StandardCharsets.ISO_8859_1);
        byte[] result = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(body, 0, result, headerBytes.length, body.length);
        return result;
    }
}
