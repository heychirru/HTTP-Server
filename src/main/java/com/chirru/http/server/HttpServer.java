package com.chirru.http.server;

import com.chirru.http.http.HttpRequest;
import com.chirru.http.http.HttpResponse;
import com.chirru.http.router.Handler;
import com.chirru.http.router.Router;
import com.chirru.http.staticfile.StaticFileServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class HttpServer {
    private final int port;
    private final Router router = new Router();
    private final WorkerPool workers;
    private final Set<Socket> activeConnections = ConcurrentHashMap.newKeySet();
    private volatile StaticFileServer staticFiles;
    private volatile boolean running;
    private volatile ServerSocket serverSocket;

    public HttpServer(int port) {
        if (port < 0 || port > 65535) throw new IllegalArgumentException("Invalid port");
        this.port = port;
        int workerCount = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.workers = new WorkerPool(workerCount, 256);
    }

    public HttpServer get(String path, Handler handler) { router.get(path, handler); return this; }
    public HttpServer post(String path, Handler handler) { router.post(path, handler); return this; }
    public HttpServer put(String path, Handler handler) { router.put(path, handler); return this; }
    public HttpServer delete(String path, Handler handler) { router.delete(path, handler); return this; }

    public HttpServer staticFiles(Path root) {
        this.staticFiles = new StaticFileServer(root);
        return this;
    }

    public int getPort() {
        ServerSocket socket = serverSocket;
        return socket == null ? -1 : socket.getLocalPort();
    }

    public boolean isRunning() {
        return running;
    }

    void registerConnection(Socket socket) {
        activeConnections.add(socket);
    }

    void unregisterConnection(Socket socket) {
        activeConnections.remove(socket);
    }

    HttpResponse dispatch(HttpRequest request) {
        try {
            HttpResponse response = router.handle(request);
            if (response.statusCode() == 404 && staticFiles != null && "GET".equals(request.method())) {
                return staticFiles.serve(request.path());
            }
            return response;
        } catch (RuntimeException e) {
            StructuredLogger.error("request_handler_failed",
                    Map.of("method", request.method(), "path", request.path(),
                            "error", String.valueOf(e.getMessage())));
            return ErrorHandler.handle(e);
        }
    }

    public void start() throws IOException {
        ServerSocket socket = new ServerSocket(port);
        serverSocket = socket;
        running = true;

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "http-shutdown"));
        StructuredLogger.info("server_started",
                Map.of("port", socket.getLocalPort()));

        try (socket) {
            while (running) {
                try {
                    Socket client = socket.accept();
                    if (!workers.submit(new ClientConnection(client, this))) {
                        client.close();
                    }
                } catch (SocketException e) {
                    if (running) throw e;
                    break;
                }
            }
        } finally {
            serverSocket = null;
            running = false;
            closeActiveConnections();
            workers.shutdown();
            StructuredLogger.info("server_stopped", Map.of("port", socket.getLocalPort()));
        }
    }

    public void stop() {
        if (!running) return;
        running = false;

        ServerSocket socket = serverSocket;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                StructuredLogger.warn("server_socket_close_failed",
                        Map.of("error", String.valueOf(e.getMessage())));
            }
        }

        closeActiveConnections();
        workers.shutdown();
    }

    private void closeActiveConnections() {
        for (Socket connection : activeConnections) {
            try {
                connection.close();
            } catch (IOException e) {
                StructuredLogger.warn("connection_close_failed",
                        Map.of("remote", String.valueOf(connection.getRemoteSocketAddress()),
                                "error", String.valueOf(e.getMessage())));
            }
        }
        activeConnections.clear();
    }
}
