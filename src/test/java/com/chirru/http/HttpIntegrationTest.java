package com.chirru.http;

import com.chirru.http.http.HttpResponse;
import com.chirru.http.http.Json;
import com.chirru.http.server.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Dependency-free HTTP integration tests using Java's built-in HttpClient.
 *
 * Run:
 * mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.HttpIntegrationTest
 */
public final class HttpIntegrationTest {
    private HttpIntegrationTest() {}

    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer(0).staticFiles(Path.of("public"));

        server.get("/hello", request ->
                HttpResponse.ok("text/plain; charset=UTF-8", "Hello, HTTP!"));

        server.post("/users", request ->
                HttpResponse.createdJson(Json.object(Map.of(
                        "body", request.body()))));

        server.get("/users/{id}", request ->
                HttpResponse.json(Json.object(Map.of(
                        "id", request.pathParam("id")))));

        Thread serverThread = Thread.ofPlatform()
                .name("http-integration-test-server")
                .start(() -> {
                    try {
                        server.start();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        try {
            waitForServer(server);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();

            URI base = URI.create("http://localhost:" + server.getPort());

            assertResponse(client, HttpRequest.newBuilder(base.resolve("/hello"))
                    .GET().build(), 200, "Hello, HTTP!");

            assertResponse(client, HttpRequest.newBuilder(base.resolve("/users/42"))
                    .GET().build(), 200, ""id":"42"");

            assertResponse(client, HttpRequest.newBuilder(base.resolve("/"))
                    .GET().build(), 200, "<!DOCTYPE html>");

            HttpRequest post = HttpRequest.newBuilder(base.resolve("/users"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{"name":"Chirru"}"))
                    .build();

            assertResponse(client, post, 201, "\"name\":\"Chirru\"");

            assertResponse(client, HttpRequest.newBuilder(base.resolve("/missing"))
                    .GET().build(), 404, "Route not found");

            System.out.println("ALL INTEGRATION TESTS PASSED");
        } finally {
            server.stop();
            serverThread.join(3000);
        }
    }

    private static void assertResponse(
            HttpClient client,
            HttpRequest request,
            int expectedStatus,
            String expectedBodyPart) throws Exception {

        java.net.http.HttpResponse<String> response =
                client.send(request, BodyHandlers.ofString());

        if (response.statusCode() != expectedStatus) {
            throw new AssertionError("Expected " + expectedStatus
                    + " but got " + response.statusCode());
        }

        if (!response.body().contains(expectedBodyPart)) {
            throw new AssertionError("Response did not contain: " + expectedBodyPart
                    + "\nActual: " + response.body());
        }
    }

    private static void waitForServer(HttpServer server) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (server.getPort() > 0) return;
            Thread.sleep(20);
        }
        throw new IllegalStateException("Server did not start");
    }
}
