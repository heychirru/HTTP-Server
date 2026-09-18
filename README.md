# Chirru HTTP Server

A lightweight HTTP/1.1 web server built from scratch using Core Java.

## Why this project?

This project is a hands-on systems and DSA exercise. Instead of using a ready-made web server, we build the important pieces ourselves: TCP connections, HTTP parsing, routing, static files, request bodies, JSON, concurrency, caching, error handling, logging, testing, and shutdown.

## Rules

Core Java only.

No Spring Boot, Spring Framework, Tomcat, Jetty, Netty, Undertow, or external HTTP server libraries.

Java 21+ and Maven are used for the project build.

## Current status

| Phase | Area | Status |
|---|---|---|
| 1 | TCP + HTTP foundation | Done |
| 2 | Basic routing | Done |
| 3 | Trie + dynamic routes | Done |
| 4 | Static files + request bodies + JSON | Done |
| 5 | Query parameters + HTTP keep-alive | Done |
| 6 | Concurrency, errors, logging and test infrastructure | Done |
| 7 | LRU static-file caching | Done |
| 8 | HTTP protocol hardening | Next |

## Features implemented

- TCP server using ServerSocket
- Client connections using Socket
- HTTP/1.1 request line and header parsing
- Request body parsing with Content-Length
- 10 MB request body limit
- Query parameter parsing
- Trie-based routing
- Dynamic path parameters such as /users/{id}
- Static route priority over parameter routes
- HTML, CSS, JavaScript and image serving
- public/ as the web root
- MIME type detection
- Path traversal protection
- JSON response helper
- Concurrent client handling
- Custom bounded blocking queue
- Custom producer-consumer worker pool
- HTTP/1.1 keep-alive
- Maximum 100 requests per connection
- Central HTTP error handling
- 400 Bad Request
- 403 Forbidden
- 404 Not Found
- 405 Method Not Allowed
- 413 Payload Too Large
- 431 Request Header Fields Too Large
- 500 Internal Server Error
- Structured JSON-lines logging
- Dependency-free HTTP integration tests
- Dependency-free load testing
- Dependency-free latency/throughput benchmarking
- Graceful server shutdown with active connection cleanup
- LRU cache for static files

## Example routes

    server.get("/users/{id}", request ->
        HttpResponse.json(
            Json.object(Map.of(
                "id", request.pathParam("id"),
                "message", "User found"
            ))
        ));

Request:

    GET /users/42

Path parameter:

    request.pathParam("id")

Query parameters:

    GET /search?q=java&page=2

Access:

    request.queryParam("q")
    request.queryParam("page")

Request body:

    request.body()

## Static web files

The public directory is the web root.

    public/
    ├── index.html
    ├── style.css
    ├── script.js
    └── images/

URLs map directly to files:

    /              -> public/index.html
    /style.css     -> public/style.css
    /script.js     -> public/script.js
    /images/a.png  -> public/images/a.png

Static file bytes are cached using the custom LRU cache.

## Architecture

    Browser
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
       +-- Request line
       +-- Headers
       +-- Body
       +-- Query parameters
       |
       v
    HttpRequest
       |
       v
    Trie Router
       |
       +-- Static routes
       +-- Dynamic routes
       |
       +--------------------+
       |                    |
       v                    v
    Handler            StaticFileServer
       |                    |
       |                LRU Cache
       |                    |
       +---------+----------+
                 v
            HttpResponse
                 |
                 v
              Client

Errors flow through the central error handler and operational events are emitted as structured JSON logs.

## Project structure

    HTTP-Server/
    ├── public/
    │   ├── index.html
    │   ├── style.css
    │   └── script.js
    │
    ├── src/main/java/com/chirru/http/
    │   ├── Main.java
    │   ├── http/
    │   │   ├── HttpException.java
    │   │   ├── HttpParser.java
    │   │   ├── HttpRequest.java
    │   │   ├── HttpResponse.java
    │   │   └── Json.java
    │   ├── router/
    │   │   ├── Handler.java
    │   │   ├── Router.java
    │   │   └── RouteTrie.java
    │   ├── staticfile/
    │   │   └── StaticFileServer.java
    │   ├── cache/
    │   │   └── LruCache.java
    │   └── server/
    │       ├── ClientConnection.java
    │       ├── ErrorHandler.java
    │       ├── HttpServer.java
    │       ├── RequestQueue.java
    │       ├── StructuredLogger.java
    │       └── WorkerPool.java
    │
    ├── src/test/java/com/chirru/http/
    │   ├── Benchmark.java
    │   ├── HttpIntegrationTest.java
    │   └── LoadTest.java
    │
    └── pom.xml

## Run

Requirements: JDK 21+ and Maven.

    mvn clean compile
    mvn exec:java

Default address:

    http://localhost:8080

Use another port:

    mvn exec:java -Dexec.args="9090"

## Test manually

Open the web server:

    http://localhost:8080/

Dynamic route:

    http://localhost:8080/users/123

Request body from PowerShell:

    curl.exe -i -X POST "http://localhost:8080/users" -H "Content-Type: application/json" --data-raw '{"name":"Chirru"}'

Method error:

    curl.exe -i -X POST http://localhost:8080/hello

Missing route:

    curl.exe -i http://localhost:8080/missing

## Phase 6 — Custom Concurrency, Errors, Logging & Test Infrastructure

Phase 6 builds the server's internal infrastructure without ExecutorService or a third-party HTTP stack.

Implemented:

- [x] Bounded blocking request queue
- [x] Producer-consumer architecture
- [x] Custom thread pool
- [x] Central error handling
- [x] Structured JSON-lines logging
- [x] HTTP integration tests
- [x] Load testing
- [x] Benchmarking and performance measurement
- [x] Graceful worker shutdown
- [x] Graceful server socket shutdown
- [x] Active connection cleanup
- [x] HTTP method-aware 404 / 405 handling

### Central error handling

Parser failures are represented by HttpException and converted into HTTP responses by ErrorHandler.

Examples:

    400 Bad Request
    405 Method Not Allowed
    413 Payload Too Large
    431 Request Header Fields Too Large
    500 Internal Server Error

Application handler failures are converted into 500 Internal Server Error without exposing internal exception details to the client.

### Structured logging

The server writes JSON lines containing fields such as:

    {
      "timestamp": "...",
      "level": "INFO",
      "event": "request_received",
      "remote": "...",
      "method": "GET",
      "path": "/hello"
    }

No logging framework is required.

### Graceful shutdown

Shutdown now:

1. Stops accepting new connections.
2. Closes the listening ServerSocket.
3. Closes active client connections.
4. Stops accepting new queue work.
5. Drains queued worker tasks.
6. Waits for worker threads to finish.

## Phase 7 — LRU Caching

Static files are cached with an LRU cache built from scratch.

Implemented:

- [x] HashMap + doubly linked list
- [x] O(1) average get()
- [x] O(1) average put()
- [x] Least-recently-used eviction
- [x] Thread-safe cache operations
- [x] Static-file integration

## Integration tests

The project intentionally avoids adding a test framework dependency. Tests use Java 21's built-in HttpClient.

Compile the test sources:

    mvn test-compile

Run the integration tests:

    mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.HttpIntegrationTest

Expected final line:

    ALL INTEGRATION TESTS PASSED

## Load testing

Start the server first:

    mvn exec:java

Then run:

    mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.LoadTest -Dexec.args="http://localhost:8080/hello 1000 16"

Arguments:

    [url] [requests] [concurrency]

Example:

    1000 requests
    16 concurrent load workers

## Benchmarking

Start the server first, then run:

    mvn test-compile exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.chirru.http.Benchmark -Dexec.args="http://localhost:8080/hello 100 1000"

Arguments:

    [url] [warmup] [iterations]

The benchmark reports:

- Average latency
- Minimum latency
- Maximum latency
- Sequential throughput

These measurements are local development measurements, not production performance claims.

## Phase 8 — HTTP Protocol Hardening

Next phase focuses on making the protocol implementation more correct and robust.

Planned:

- [ ] Validate HTTP method and version
- [ ] Require Host for HTTP/1.1
- [ ] Handle unsupported transfer encodings explicitly
- [ ] Improve request-target validation
- [ ] Improve header parsing rules
- [ ] Add HEAD support
- [ ] Add response headers such as Date and Server
- [ ] Improve static-file cache invalidation
- [ ] Add more protocol-level integration tests
- [ ] Add malformed-request test cases

## DSA and systems concepts

- Trie and tree traversal
- Doubly linked list + HashMap for LRU cache
- Producer-consumer concurrency
- Bounded queue
- Custom thread pool
- Synchronization with wait() / notifyAll()
- Concurrent collections
- TCP/IP
- HTTP/1.1
- Parsing
- File I/O
- Error handling
- Structured logging
- Load testing
- Latency measurement
- Performance optimization

## Goal

Build a small but real HTTP server while understanding every important layer that a framework normally hides.

    HTTP Request
        ↓
      TCP
        ↓
     Parser
        ↓
   HttpRequest
        ↓
    Trie Router
        ↓
     Handler
        ↓
   HttpResponse
        ↓
      TCP
        ↓
     Client

Built with Core Java only.
