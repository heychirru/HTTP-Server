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

    public static HttpResponse error(int statusCode, String reason, String body) {
        return new HttpResponse(statusCode, reason, "text/plain; charset=UTF-8",
                body.getBytes(StandardCharsets.UTF_8));
    }

    public static HttpResponse badRequest(String body) {
        return error(400, "Bad Request", body);
    }

    public static HttpResponse methodNotAllowed(String body) {
        return error(405, "Method Not Allowed", body);
    }

    public static HttpResponse forbidden(String body) {
        return error(403, "Forbidden", body);
    }

    public static HttpResponse notFound(String body) {
        return error(404, "Not Found", body);
    }

    public static HttpResponse requestHeaderFieldsTooLarge(String body) {
        return error(431, "Request Header Fields Too Large", body);
    }

    public static HttpResponse payloadTooLarge(String body) {
        return error(413, "Payload Too Large", body);
    }

    public static HttpResponse internalServerError(String body) {
        return error(500, "Internal Server Error", body);
    }

    public byte[] toBytes(boolean keepAlive) {
        String connection = keepAlive ? "keep-alive" : "close";
        String headers = "HTTP/1.1 " + statusCode + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: " + connection + "\r\n"
                + "\r\n";
        byte[] headerBytes = headers.getBytes(StandardCharsets.ISO_8859_1);
        byte[] result = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(body, 0, result, headerBytes.length, body.length);
        return result;
    }

    public byte[] toBytes() {
        return toBytes(false);
    }
}
