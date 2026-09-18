package com.chirru.http.server;

import com.chirru.http.http.HttpRequest;
import com.chirru.http.http.HttpResponse;
import com.chirru.http.router.Handler;
import com.chirru.http.router.Router;
import com.chirru.http.staticfile.StaticFileServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HttpServer {
    private final int port;
    private final Router router = new Router();
    private final ExecutorService workers;
    private StaticFileServer staticFiles;

    public HttpServer(int port) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid port");
        this.port = port;
        this.workers = Executors.newFixedThreadPool(
                Math.max(4, Runtime.getRuntime().availableProcessors()));
    }

    public HttpServer get(String path, Handler handler) { router.get(path, handler); return this; }
    public HttpServer post(String path, Handler handler) { router.post(path, handler); return this; }
    public HttpServer put(String path, Handler handler) { router.put(path, handler); return this; }
    public HttpServer delete(String path, Handler handler) { router.delete(path, handler); return this; }

    public HttpServer staticFiles(Path root) {
        this.staticFiles = new StaticFileServer(root);
        return this;
    }

    HttpResponse dispatch(HttpRequest request) {
        HttpResponse response = router.handle(request);

        if (response.statusCode() == 404
                && staticFiles != null
                && "GET".equals(request.method())) {
            return staticFiles.serve(request.path());
        }

        return response;
    }

    public void start() throws IOException {
        Runtime.getRuntime().addShutdownHook(new Thread(workers::shutdown));
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chirru HTTP Server listening on http://localhost:" + port);
            while (!serverSocket.isClosed()) {
                Socket client = serverSocket.accept();
                workers.submit(new ClientConnection(client, this));
            }
        }
    }
}
