package com.chirru.http.server;

import com.chirru.http.http.HttpException;
import com.chirru.http.http.HttpResponse;

/**
 * Converts application/parser failures into safe HTTP responses.
 */
public final class ErrorHandler {
    private ErrorHandler() {}

    public static HttpResponse handle(Throwable error) {
        if (error instanceof HttpException httpError) {
            return HttpResponse.error(
                    httpError.statusCode(),
                    httpError.reason(),
                    httpError.getMessage());
        }

        return HttpResponse.internalServerError("Internal server error");
    }
}
