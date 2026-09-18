package com.chirru.http.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HttpServer {
    private final int port;
    private final ExecutorService workers;

    public HttpServer(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        this.port = port;
        this.workers = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()));
    }

    public void start() throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(workers::shutdown));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("======================================");
            System.out.println("      Chirru Core Java HTTP Server");
            System.out.println("======================================");
            System.out.println("Listening on http://localhost:" + port);

            while (!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                workers.submit(new ClientConnection(client));
            }
        }
    }
}
