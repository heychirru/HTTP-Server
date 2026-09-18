package com.chirru.http;

import com.chirru.http.http.HttpResponse;
import com.chirru.http.server.HttpServer;
import com.chirru.http.staticfile.StaticFileServer;

import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        HttpServer server = new HttpServer(port);
        StaticFileServer files = new StaticFileServer(Path.of("public"));

        server.get("/", request -> HttpResponse.ok(
                "text/html; charset=UTF-8",
                """
                <html>
                  <head><title>Chirru HTTP Server</title></head>
                  <body>
                    <h1>Hello from Core Java 🚀</h1>
                    <p>HTTP server built from scratch.</p>
                  </body>
                </html>
                """));

        server.get("/files/{path}", request -> files.serve("/" + request.pathParam("path")));

        server.get("/hello", request ->
                HttpResponse.ok("text/plain; charset=UTF-8", "Hello, HTTP!"));

        server.post("/users", request ->
                HttpResponse.created("User endpoint reached."));

        server.get("/users/me", request ->
                HttpResponse.ok("text/plain; charset=UTF-8", "Static user route"));

        server.get("/users/{id}", request ->
                HttpResponse.ok(
                        "text/plain; charset=UTF-8",
                        "User ID: " + request.pathParam("id")));

        server.start();
    }
}
