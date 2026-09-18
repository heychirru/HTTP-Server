package com.chirru.http.server;

import com.chirru.http.http.HttpParser;
import com.chirru.http.http.HttpRequest;
import com.chirru.http.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;

final class ClientConnection implements Runnable {
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

            HttpRequest request = HttpParser.parse(input);
            if (request == null) return;

            HttpResponse response = server.dispatch(request);
            output.write(response.toBytes());
            output.flush();
        } catch (IOException | RuntimeException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }
}
