package com.chirru.http;

import com.chirru.http.http.HttpResponse;
import com.chirru.http.http.Json;
import com.chirru.http.server.HttpServer;

import java.nio.file.Path;
import java.util.Map;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;

        HttpServer server = new HttpServer(port)
                .staticFiles(Path.of("public"));

        server.get("/hello", request ->
                HttpResponse.ok("text/plain; charset=UTF-8", "Hello, HTTP!"));

        server.post("/users", request ->
                HttpResponse.createdJson(Json.object(Map.of(
                        "message", "User endpoint reached",
                        "body", request.body()))));

        server.get("/users/me", request ->
                HttpResponse.ok("text/plain; charset=UTF-8", "Static user route"));

        server.get("/users/{id}", request ->
                HttpResponse.json(Json.object(Map.of(
                        "id", request.pathParam("id"),
                        "message", "User found"))));

        server.start();
    }
}
