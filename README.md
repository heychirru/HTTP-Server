# Chirru HTTP Server

A lightweight HTTP/1.1 web server built from scratch using Core Java.

## Why this project?

This project is a hands-on systems and DSA exercise. Instead of using a ready-made web server, we build the important pieces ourselves: TCP connections, HTTP parsing, routing, static files, request bodies, JSON, concurrency, caching, and shutdown.

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
| 6 | Custom concurrency and systems layer | In progress |

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
- HTTP/1.1 keep-alive
- Maximum 100 requests per connection

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

## Architecture

    Browser
       |
       v
    ServerSocket
       |
       v
    Worker Executor
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
       +---------+----------+
                 v
            HttpResponse
                 |
                 v
              Client

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
    │   └── server/
    │       ├── ClientConnection.java
    │       └── HttpServer.java
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

## Test

Open the web server:

    http://localhost:8080/

Dynamic route:

    http://localhost:8080/users/123

Query parameters:

    http://localhost:8080/search?q=java&page=2

Request body:

    curl -X POST http://localhost:8080/users -H "Content-Type: application/json" -d "{\"name\":\"Chirru\"}"

## Phase 6 — Custom Concurrency & Server Lifecycle

Phase 6 replaces the high-level `ExecutorService` approach with a small producer-consumer system built from Java threads, `wait()` / `notifyAll()`, and a bounded queue.

Implemented:

- [x] Bounded blocking request queue
- [x] Producer-consumer worker architecture
- [x] Custom worker pool
- [x] Graceful worker shutdown
- [x] Connection request limit

## Phase 7 — LRU Caching

Static files are cached with an LRU cache built from scratch.

Implemented:

- [x] HashMap + doubly linked list
- [x] O(1) average `get()`
- [x] O(1) average `put()`
- [x] Least-recently-used eviction
- [x] Thread-safe cache operations
- [x] Static-file integration

## Phase 6 roadmap

Phase 6 focuses on building the server's internal infrastructure instead of relying on high-level concurrency utilities.

- [x] Custom bounded request queue
- [x] Producer-consumer architecture
- [x] Custom thread pool
- [x] LRU cache from scratch
- [ ] Central error handling
- [ ] Structured logging
- [ ] HTTP integration tests
- [ ] Load testing
- [ ] Benchmarking and performance tuning
- [x] Graceful worker shutdown

## DSA and systems concepts

- Trie and tree traversal
- Doubly linked list + HashMap for LRU cache
- Producer-consumer concurrency
- HashMap
- Queue
- Producer-consumer pattern
- Thread pools
- Concurrency
- TCP/IP
- HTTP/1.1
- Parsing
- File I/O
- LRU cache
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