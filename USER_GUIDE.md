# Chirru HTTP Server — Complete User Guide

> A Core-Java HTTP/1.1 server built from scratch — without Spring Boot, Tomcat, Jetty, Netty, Undertow, or any external HTTP server library.

This guide explains how to install, run, configure, use, test, and extend the project.

---

## 1. What is this project?

Chirru HTTP Server is a small HTTP server implemented directly with Java's standard library.

The project intentionally builds the server internals instead of hiding them behind a framework.

Main request flow:

~~~text
Client
  |
  v
ServerSocket
  |
  v
Bounded Request Queue
  |
  v
Custom Worker Pool
  |
  v
ClientConnection
  |
  v
HTTP Parser
  |
  v
HttpRequest
  |
  v
Trie Router
  |
  +----> Application Handler
  |
  +----> Static File Server ---> LRU Cache
  |
  v
HttpResponse
  |
  v
Client
~~~

## 2. Requirements

- JDK 21 or newer
- Maven
- Git

Verify Java:

~~~bash
java -version
~~~

Verify Maven:

~~~bash
mvn -version
~~~

## 3. Clone the project

~~~bash
git clone https://github.com/heychirru/HTTP-Server.git
cd HTTP-Server
git checkout core-java-http-server
~~~

## 4. Project structure

~~~text
HTTP-Server/
├── public/
│   ├── index.html
│   ├── style.css
│   └── script.js
├── src/
│   ├── main/java/com/chirru/http/
│   │   ├── Main.java
│   │   ├── http/
│   │   │   ├── HttpException.java
│   │   │   ├── HttpParser.java
│   │   │   ├── HttpRequest.java
│   │   │   ├── HttpResponse.java
│   │   │   └── Json.java
│   │   ├── router/
│   │   │   ├── Handler.java
│   │   │   ├── Router.java
│   │   │   └── RouteTrie.java
│   │   ├── staticfile/
│   │   │   └── StaticFileServer.java
│   │   ├── cache/
│   │   │   └── LruCache.java
│   │   └── server/
│   │       ├── ClientConnection.java
│   │       ├── ErrorHandler.java
│   │       ├── HttpServer.java
│   │       ├── RequestQueue.java
│   │       ├── StructuredLogger.java
│   │       └── WorkerPool.java
│   └── test/java/com/chirru/http/
│       ├── Benchmark.java
│       ├── HttpIntegrationTest.java
│       └── LoadTest.java
└── pom.xml
~~~

## 5. Build the project

From the project root:

~~~bash
mvn clean compile
~~~

If compilation succeeds, the project is ready to run.

## 6. Start the server

The default port is 8080.

~~~bash
mvn exec:java
~~~

Open:

http://localhost:8080

The server writes structured JSON logs such as:

~~~json
{"timestamp":"...","level":"INFO","event":"server_started","port":8080}
~~~

## 7. Start on another port

Pass the port as the first argument:

~~~bash
mvn exec:java -Dexec.args="9090"
~~~

Then open http://localhost:9090.

Port 0 can also be used programmatically to request an automatically assigned free port.

## 8. Current application routes

The current Main.java demonstrates static files, GET routes, POST routes, dynamic path parameters, and JSON responses.

| Method | Path | Purpose |
|---|---|---|
| GET | / | Static public/index.html |
| GET | /hello | Plain-text response |
| POST | /users | Reads request body |
| GET | /users/me | Static route |
| GET | /users/{id} | Dynamic route |

## 9. Static website hosting

The public/ directory is the static web root.

~~~java
HttpServer server = new HttpServer(port)
        .staticFiles(Path.of("public"));
~~~

URL mapping:

~~~text
/                  -> public/index.html
/style.css         -> public/style.css
/script.js         -> public/script.js
/images/logo.png   -> public/images/logo.png
~~~

Add public/about.html and open http://localhost:8080/about.html.

## 10. Static-file security

The server normalizes static paths and verifies that the resulting path remains inside the configured root.

Path traversal attempts such as /../../secret.txt are rejected with 403 Forbidden.

## 11. Creating GET routes

~~~java
server.get("/hello", request ->
        HttpResponse.ok(
                "text/plain; charset=UTF-8",
                "Hello, HTTP!"
        ));
~~~

## 12. Creating POST routes

~~~java
server.post("/users", request ->
        HttpResponse.createdJson(
                Json.object(Map.of(
                        "message", "User endpoint reached",
                        "body", request.body()
                ))
        ));
~~~

The request body is available through request.body().

## 13. PUT and DELETE

~~~java
server.put("/users/{id}", request ->
        HttpResponse.ok(
                "text/plain; charset=UTF-8",
                "Updated user " + request.pathParam("id")
        ));

server.delete("/users/{id}", request ->
        HttpResponse.ok(
                "text/plain; charset=UTF-8",
                "Deleted user " + request.pathParam("id")
        ));
~~~

## 14. Dynamic path parameters

Register:

~~~java
server.get("/users/{id}", request ->
        HttpResponse.json(
                Json.object(Map.of(
                        "id", request.pathParam("id"),
                        "message", "User found"
                ))
        ));
~~~

Request /users/42 gives request.pathParam("id") = "42".

The router uses a trie and gives static routes priority over parameter routes. Therefore /users/me takes precedence over /users/{id}.

## 15. Query parameters

Example URL:

~~~text
/search?q=java&page=2
~~~

Read values with:

~~~java
request.queryParam("q")
request.queryParam("page")
~~~

Example:

~~~java
server.get("/search", request ->
        HttpResponse.json(Json.object(Map.of(
                "q", request.queryParam("q"),
                "page", request.queryParam("page")
        ))));
~~~

Query parameter names and values are URL-decoded.

## 16. Reading headers

Headers are available through request.headers(). Header names are normalized to lowercase.

~~~java
String userAgent = request.headers().get("user-agent");
String contentType = request.headers().get("content-type");
~~~

## 17. Reading request bodies

The current parser supports request bodies using Content-Length.

~~~http
POST /users HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 16

{"name":"Chirru"}
~~~

Read the body with request.body().

The current maximum request body size is 10 MB. Larger bodies return 413 Payload Too Large.

## 18. JSON responses

The project includes a minimal dependency-free JSON serializer.

~~~java
String json = Json.object(Map.of(
        "name", "Chirru",
        "age", 22,
        "active", true
));

return HttpResponse.json(json);
~~~

It supports strings, numbers, booleans, null, and flat maps. It is intentionally not a full JSON library.

## 19. HTTP response helpers

Available helpers include:

~~~text
HttpResponse.ok(contentType, body)
HttpResponse.json(json)
HttpResponse.created(body)
HttpResponse.createdJson(json)
HttpResponse.error(status, reason, body)
HttpResponse.badRequest(body)
HttpResponse.methodNotAllowed(body)
HttpResponse.forbidden(body)
HttpResponse.notFound(body)
HttpResponse.payloadTooLarge(body)
HttpResponse.requestHeaderFieldsTooLarge(body)
HttpResponse.notImplemented(body)
HttpResponse.httpVersionNotSupported(body)
HttpResponse.internalServerError(body)
~~~

## 20. HTTP status codes

| Status | Meaning | Typical cause |
|---:|---|---|
| 200 | OK | Successful request |
| 201 | Created | Successful creation |
| 400 | Bad Request | Invalid HTTP request |
| 403 | Forbidden | Static path escapes web root |
| 404 | Not Found | Unknown route/file |
| 405 | Method Not Allowed | Path exists but method is not registered |
| 413 | Payload Too Large | Body exceeds 10 MB |
| 431 | Request Header Fields Too Large | Header limits exceeded |
| 500 | Internal Server Error | Application/server failure |
| 501 | Not Implemented | Unsupported HTTP method/feature |
| 505 | HTTP Version Not Supported | Unsupported HTTP version |

## 21. HTTP/1.1 Host header

HTTP/1.1 requests require Host.

~~~http
GET /hello HTTP/1.1
Host: localhost:8080
~~~

A missing Host header returns 400 Bad Request.

## 22. Supported HTTP methods

The parser currently accepts GET, POST, PUT, DELETE, and HEAD.

Unsupported methods return 501 Not Implemented.

## 23. HEAD requests

HEAD returns the headers that describe the corresponding resource without sending a response body.

~~~powershell
curl.exe -i -X HEAD http://localhost:8080/hello
~~~

## 24. Keep-alive

HTTP/1.1 connections use keep-alive by default unless Connection: close is requested.

The server allows multiple requests on one TCP connection and limits one connection to 100 requests.

## 25. Custom concurrency

The server does not use ExecutorService.

It uses a custom bounded request queue and custom worker pool.

Current queue capacity: 256 tasks.

The queue uses ArrayDeque with wait() and notifyAll(). Workers are platform threads named http-worker-N.

## 26. LRU static-file cache

Static files are cached with a custom LRU cache built from:

~~~text
HashMap + Doubly Linked List
~~~

Average operations:

~~~text
get()  -> O(1)
put()  -> O(1)
~~~

Default capacity: 128 files.

The cached entry stores file metadata and is refreshed when the file's last-modified time or size changes.

## 27. Structured logging

The server uses a dependency-free JSON-lines logger.

Example:

~~~json
{"timestamp":"...","level":"INFO","event":"request_received","method":"GET","path":"/hello"}
~~~

Common events include server_started, server_stopped, connection_opened, connection_closed, request_received, response_sent, request_rejected, and request_handler_failed.

## 28. Central error handling

Parser failures are represented by HttpException and converted into responses by ErrorHandler.

Application failures are converted into 500 Internal Server Error. Internal exception details are logged server-side rather than returned to the client.

## 29. Graceful shutdown

When stop() is called:

1. New connections stop being accepted.
2. The listening ServerSocket is closed.
3. Active client connections are closed.
4. New worker-pool work is rejected.
5. Queued worker tasks are handled according to the worker-pool shutdown behavior.
6. Worker threads are joined.

Ctrl+C can be used to stop the foreground process.

## 30. Testing with curl

Windows PowerShell examples:

~~~powershell
curl.exe -i http://localhost:8080/hello
curl.exe -i http://localhost:8080/users/42
curl.exe -i http://localhost:8080/
curl.exe -i -X POST "http://localhost:8080/users" -H "Content-Type: application/json" --data-raw '{"name":"Chirru"}'
curl.exe -i -X PUT http://localhost:8080/users/42
curl.exe -i -X DELETE http://localhost:8080/users/42
curl.exe -i -X HEAD http://localhost:8080/hello
curl.exe -i http://localhost:8080/missing
~~~

## 31. Integration tests

Compile test sources:

~~~bash
mvn test-compile
~~~

Run:

~~~bash
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.HttpIntegrationTest
~~~

Expected final line:

~~~text
ALL INTEGRATION TESTS PASSED
~~~

The integration suite covers GET, dynamic routes, static files, POST bodies, missing routes, 405, query parameters, and HEAD.

## 32. Load testing

Start the server first:

~~~bash
mvn exec:java
~~~

Then run:

~~~bash
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.LoadTest -Dexec.args="http://localhost:8080/hello 1000 16"
~~~

Arguments are URL, request count, and concurrency.

The output reports successful requests, failures, total time, and throughput. Results depend on your machine and environment.

## 33. Benchmarking

Run:

~~~bash
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.Benchmark -Dexec.args="http://localhost:8080/hello 100 1000"
~~~

Arguments are URL, warmup iterations, and measured iterations.

The benchmark reports average, minimum, and maximum latency plus sequential throughput. These are local development measurements, not production performance guarantees.

## 34. Build your own application

Modify Main.java and register your own routes.

~~~java
package com.chirru.http;

import com.chirru.http.http.HttpResponse;
import com.chirru.http.http.Json;
import com.chirru.http.server.HttpServer;
import java.nio.file.Path;
import java.util.Map;

public final class Main {
    public static void main(String[] args) throws Exception {
        HttpServer server = new HttpServer(8080)
                .staticFiles(Path.of("public"));

        server.get("/", request ->
                HttpResponse.ok("text/plain; charset=UTF-8", "My Java HTTP server"));

        server.get("/api/health", request ->
                HttpResponse.json(Json.object(Map.of("status", "UP"))));

        server.get("/api/users/{id}", request ->
                HttpResponse.json(Json.object(Map.of(
                        "id", request.pathParam("id")
                ))));

        server.post("/api/users", request ->
                HttpResponse.createdJson(Json.object(Map.of(
                        "body", request.body()
                ))));

        server.start();
    }
}
~~~

## 35. API-only server

Static files are optional.

~~~java
HttpServer server = new HttpServer(8080);

server.get("/api/health", request ->
        HttpResponse.json(Json.object(Map.of("status", "UP"))));

server.start();
~~~

## 36. Website + API server

~~~java
HttpServer server = new HttpServer(8080)
        .staticFiles(Path.of("public"));

server.get("/api/health", request ->
        HttpResponse.json(Json.object(Map.of("status", "UP"))));

server.start();
~~~

Application routes are checked before the static-file fallback, so an explicit route takes priority over a static file with the same URL.

## 37. Recommended development workflow

~~~text
Understand
   ↓
Design
   ↓
Implement
   ↓
Compile
   ↓
Write integration test
   ↓
Run load/benchmark test when relevant
   ↓
Update documentation
   ↓
Commit
~~~

Useful commands:

~~~bash
mvn clean compile
mvn test-compile
git status
git diff
git log --oneline
~~~

## 38. Common problems

### Port already in use

Use another port:

~~~bash
mvn exec:java -Dexec.args="9090"
~~~

On Windows:

~~~powershell
netstat -ano | findstr :8080
~~~

### Java version too old

Run java -version and install JDK 21 or newer.

### Stale build

Run mvn clean compile.

### 404

Check the method, path, route registration, and whether the application was restarted after changing Main.java.

### 405

The path exists, but the requested HTTP method is not registered for that path.

### 400

Common causes include malformed request lines, invalid headers, missing HTTP/1.1 Host, invalid request targets, invalid Content-Length, duplicate Content-Length, invalid query encoding, or incomplete bodies.

### 413

The request body is larger than 10 MB.

### 431

Current header limits include 64 KB total header data, 8 KB per header line, and 100 headers.

## 39. Current limitations

This is a learning-oriented HTTP server, not a production replacement for mature web servers.

Current limitations include:

- HTTP/1.0 and HTTP/1.1 only
- No HTTP/2 or HTTP/3
- No built-in TLS/HTTPS
- No chunked request-body decoding
- No multipart/form-data parser
- Minimal JSON support
- No compression
- No WebSocket support
- No advanced connection timeout management
- Static files are loaded into memory before being sent
- No conditional requests / 304 yet
- No Range / 206 yet
- Not a complete implementation of every HTTP feature

## 40. Security considerations

Current protections include:

- Static path traversal protection
- 10 MB request-body limit
- Header size and count limits
- Duplicate Content-Length rejection
- Unsupported Transfer-Encoding rejection
- Internal exception details are not returned to clients

For public production deployment, additional security controls are required.

## 41. How to study the source

Recommended order:

1. Main.java — server configuration
2. HttpServer.java — ServerSocket, accept loop, lifecycle, dispatch
3. ClientConnection.java — socket I/O and keep-alive
4. HttpParser.java — HTTP parsing and validation
5. HttpRequest.java — request model
6. Router.java — route registration and dispatch
7. RouteTrie.java — trie matching
8. StaticFileServer.java — filesystem and caching
9. LruCache.java — HashMap + doubly linked list
10. RequestQueue.java — producer-consumer queue
11. WorkerPool.java — worker threads
12. ErrorHandler.java — centralized errors
13. StructuredLogger.java — JSON logging

## 42. One complete request, step by step

Suppose the client sends:

~~~http
GET /users/42 HTTP/1.1
Host: localhost:8080
~~~

1. ServerSocket accepts the TCP connection.
2. The connection is submitted to the bounded queue.
3. A worker consumes it.
4. HttpParser reads the method, target, version, and headers.
5. HttpRequest represents the parsed request.
6. RouteTrie finds /users/{id}.
7. The handler reads pathParam("id"), which is 42.
8. The handler creates HttpResponse.
9. ClientConnection serializes the response and writes it to the socket.

## 43. Current phases

~~~text
Phase 1  ✅ TCP + HTTP foundation
Phase 2  ✅ Basic routing
Phase 3  ✅ Trie + dynamic routes
Phase 4  ✅ Static files + request bodies + JSON
Phase 5  ✅ Query parameters + HTTP keep-alive
Phase 6  ✅ Concurrency + errors + logging + tests
Phase 7  ✅ LRU static-file caching
Phase 8  🚧 HTTP protocol hardening
~~~

## 44. Phase 8 direction

Current protocol-hardening work includes method and version validation, Host validation, Transfer-Encoding validation, request-target validation, header validation, HEAD support, response metadata, cache invalidation, and protocol tests.

Planned extensions include OPTIONS, more malformed-request tests, conditional requests and 304, Range requests and 206, and additional HTTP validation.

## 45. FAQ

### Can I use Spring Boot?

No. This project is specifically intended to demonstrate HTTP server internals using Core Java.

### Can I use Tomcat, Jetty, Netty, or Undertow?

No. Those are intentionally excluded.

### Can I use Java standard-library classes?

Yes. Examples include ServerSocket, Socket, InputStream, OutputStream, Thread, HashMap, ArrayDeque, ConcurrentHashMap, Java file APIs, and java.net.http.HttpClient for tests.

### Can I host a frontend?

Yes. Put the frontend files in public/ and configure staticFiles(Path.of("public")).

### Can I build REST APIs?

Yes. Register GET, POST, PUT, and DELETE routes and use HttpRequest plus HttpResponse.

### Can I use a database?

The current project has no database layer. You can add JDBC or another database-access implementation later.

### Can I deploy it?

It can run anywhere that supports the required Java version and exposes a TCP port, but it should currently be treated as a learning/systems project rather than a hardened production server.

## 46. Quick-start cheat sheet

Clone:

~~~bash
git clone https://github.com/heychirru/HTTP-Server.git
cd HTTP-Server
git checkout core-java-http-server
~~~

Build:

~~~bash
mvn clean compile
~~~

Run:

~~~bash
mvn exec:java
~~~

Open:

http://localhost:8080

Test:

~~~powershell
curl.exe -i http://localhost:8080/hello
curl.exe -i http://localhost:8080/users/42
~~~

Integration test:

~~~bash
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.HttpIntegrationTest
~~~

Load test:

~~~bash
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.LoadTest -Dexec.args="http://localhost:8080/hello 1000 16"
~~~

Benchmark:

~~~bash
mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.Benchmark -Dexec.args="http://localhost:8080/hello 100 1000"
~~~

---

Built with Core Java only.