package com.chirru.http.server;

import com.chirru.http.http.HttpParser;
import com.chirru.http.http.HttpRequest;
import com.chirru.http.http.HttpResponse;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

final class ClientConnection implements Runnable {
    private final Socket socket;

    ClientConnection(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (socket;
             BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
             BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream())) {

            HttpRequest request = HttpParser.parse(input);
            HttpResponse response;

            if (request == null) {
                return;
            }

            if ("GET".equals(request.method()) && "/".equals(request.path())) {
                String body = """
                        <html>
                          <head><title>Chirru HTTP Server</title></head>
                          <body>
                            <h1>Hello from Core Java 🚀</h1>
                            <p>No Spring Boot. No Tomcat. Just Java.</p>
                          </body>
                        </html>
                        """;
                response = HttpResponse.ok("text/html; charset=UTF-8", body);
            } else {
                response = HttpResponse.notFound("Route not found");
            }

            output.write(response.toBytes());
            output.flush();
        } catch (IOException | RuntimeException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
}
