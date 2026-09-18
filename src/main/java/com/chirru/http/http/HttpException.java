package com.chirru.http.http;

/**
 * Exception representing an HTTP-level client error.
 */
public final class HttpException extends RuntimeException {
    private final int statusCode;
    private final String reason;

    public HttpException(int statusCode, String reason, String message) {
        super(message);
        this.statusCode = statusCode;
        this.reason = reason;
    }

    public int statusCode() {
        return statusCode;
    }

    public String reason() {
        return reason;
    }

    public static HttpException badRequest(String message) {
        return new HttpException(400, "Bad Request", message);
    }

    public static HttpException requestHeaderFieldsTooLarge(String message) {
        return new HttpException(431, "Request Header Fields Too Large", message);
    }

    public static HttpException payloadTooLarge(String message) {
        return new HttpException(413, "Payload Too Large", message);
    }
}
