package com.chirru.http.server;

import com.chirru.http.http.HttpParser;
import com.chirru.http.http.HttpRequest;
import com.chirru.http.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

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
        try (socket;
             BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {

            for (int requestCount = 0; requestCount < MAX_REQUESTS_PER_CONNECTION; requestCount++) {
                HttpRequest request = HttpParser.parse(input);
                if (request == null) return;

                boolean keepAlive = shouldKeepAlive(request)
                        && requestCount + 1 < MAX_REQUESTS_PER_CONNECTION;

                HttpResponse response = server.dispatch(request);
                output.write(response.toBytes(keepAlive));
                output.flush();

                if (!keepAlive) return;
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Connection error: " + e.getMessage());
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
