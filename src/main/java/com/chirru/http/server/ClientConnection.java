package com.chirru.http.server;

import com.chirru.http.http.HttpException;
import com.chirru.http.http.HttpParser;
import com.chirru.http.http.HttpRequest;
import com.chirru.http.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

final class ClientConnection implements Runnable {
    private static final int MAX_REQUESTS_PER_CONNECTION = 100;

    private final Socket socket;
    private final HttpServer server;

    ClientConnection(Socket socket, HttpServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        String remote = String.valueOf(socket.getRemoteSocketAddress());
        server.registerConnection(socket);
        StructuredLogger.info("connection_opened", Map.of("remote", remote));

        try (socket;
             BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {

            for (int requestCount = 0; requestCount < MAX_REQUESTS_PER_CONNECTION; requestCount++) {
                HttpRequest request;

                try {
                    request = HttpParser.parse(input);
                } catch (HttpException e) {
                    StructuredLogger.warn("request_rejected",
                            Map.of("remote", remote, "status", e.statusCode(),
                                    "error", String.valueOf(e.getMessage())));
                    output.write(ErrorHandler.handle(e).toBytes(false));
                    output.flush();
                    return;
                } catch (IOException e) {
                    if (server.isRunning()) {
                        StructuredLogger.warn("connection_read_failed",
                                Map.of("remote", remote, "error", String.valueOf(e.getMessage())));
                    }
                    return;
                }

                if (request == null) return;

                StructuredLogger.info("request_received",
                        Map.of("remote", remote, "method", request.method(), "path", request.path()));

                boolean keepAlive = shouldKeepAlive(request)
                        && requestCount + 1 < MAX_REQUESTS_PER_CONNECTION;

                HttpResponse response = server.dispatch(request);
                boolean includeBody = !"HEAD".equals(request.method());

                output.write(response.toBytes(keepAlive, includeBody));
                output.flush();

                StructuredLogger.info("response_sent",
                        Map.of("remote", remote, "method", request.method(),
                                "path", request.path(), "status", response.statusCode()));

                if (!keepAlive) return;
            }
        } catch (IOException e) {
            if (server.isRunning()) {
                StructuredLogger.warn("connection_io_failed",
                        Map.of("remote", remote, "error", String.valueOf(e.getMessage())));
            }
        } finally {
            server.unregisterConnection(socket);
            StructuredLogger.info("connection_closed", Map.of("remote", remote));
        }
    }

    private boolean shouldKeepAlive(HttpRequest request) {
        String connection = request.headers().get("connection");

        if ("HTTP/1.1".equalsIgnoreCase(request.version())) {
            return !"close".equalsIgnoreCase(connection);
        }

        return "keep-alive".equalsIgnoreCase(connection);
    }
}
