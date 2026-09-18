# Chirru HTTP Server

A lightweight HTTP/1.1 server built completely from scratch using Core Java.

> The goal is to understand how an HTTP server works internally instead of relying on Spring Boot or an existing server/framework.

## No Frameworks

This project intentionally does not use:
- Spring Boot
- Spring Framework
- Tomcat
- Jetty
- Netty
- Undertow
- Any external HTTP server library

Only the Java standard library is used.

## Tech Stack
- Java 21+
- Java Networking (ServerSocket, Socket)
- Java I/O
- Java Concurrency (ExecutorService)
- Java Collections
- Maven

## Current Progress

### Phase 1 — TCP + HTTP Foundation
- [x] TCP server using ServerSocket
- [x] Client connections using Socket
- [x] HTTP/1.1 request-line parsing
- [x] HTTP header parsing
- [x] HttpRequest model
- [x] HttpResponse model
- [x] HTTP status responses
- [x] UTF-8 response bodies
- [x] Concurrent client handling
- [x] Fixed worker thread pool

### Phase 2 — Routing
- [x] Router abstraction
- [x] Functional request handlers
- [x] GET routes
- [x] POST routes
- [x] PUT routes
- [x] DELETE routes
- [x] Route lookup using HashMap (Phase 2)
- [x] Trie-based route matching with static-route precedence
- [x] Dynamic path parameters such as /users/{id}
- [x] Duplicate route protection
- [x] 404 response for unknown routes

Example API:

    HttpServer server = new HttpServer(8080);
    server.get("/", request -> HttpResponse.ok("text/plain", "Hello"));
    server.get("/hello", request -> HttpResponse.ok("text/plain", "Hello, HTTP!"));
    server.post("/users", request -> HttpResponse.created("User endpoint reached."));
    server.get("/users/{id}", request ->
        HttpResponse.ok("text/plain", "User ID: " + request.pathParam("id")));
    server.start();

## Current Architecture

    Client
       |
       v
    ServerSocket
       |
       v
    ExecutorService
       |
       v
    HTTP Parser
       |
       v
    HttpRequest
       |
       v
    Router (Trie)
       |
       +-- static segments
       +-- parameter segments ({id})
       |
       v
    Handler
       |
       v
    HttpResponse
       |
       v
    Client

## Project Structure

    src/main/java/com/chirru/http/
    |
    +-- Main.java
    |
    +-- http/
    |   +-- HttpParser.java
    |   +-- HttpRequest.java
    |   +-- HttpResponse.java
    |
    +-- router/
    |   +-- Handler.java
    |   +-- Router.java
    |   +-- RouteTrie.java
    |
    +-- server/
        +-- ClientConnection.java
        +-- HttpServer.java

## Run

Requires JDK 21+ and Maven.

    mvn clean compile
    mvn exec:java

Server: http://localhost:8080

Run on another port:

    mvn exec:java -Dexec.args="9090"

## Test

Browser:
    http://localhost:8080/
    http://localhost:8080/hello

Using curl:
    curl http://localhost:8080/
    curl http://localhost:8080/hello
    curl -X POST http://localhost:8080/users
    curl -X PUT http://localhost:8080/users
    curl -X DELETE http://localhost:8080/users
    curl http://localhost:8080/users/123
    curl http://localhost:8080/users/me

## Roadmap
- [x] TCP server
- [x] HTTP request parsing
- [x] HTTP response generation
- [x] Concurrent client handling
- [x] Basic HTTP router
- [x] GET / POST / PUT / DELETE routing
- [x] Dynamic path parameters
- [x] Trie-based routing
- [ ] Static file server
- [ ] Request body parsing
- [ ] JSON responses
- [ ] Query parameter parsing
- [ ] HTTP keep-alive
- [ ] Custom bounded request queue
- [ ] Custom thread pool
- [ ] LRU cache from scratch
- [ ] Error handling system
- [ ] Structured logging
- [ ] HTTP integration tests
- [ ] Load testing
- [ ] Benchmarking and performance tuning
- [ ] Graceful server shutdown

## DSA & Systems Concepts
- Hash tables
- Trie
- Queues
- Doubly linked lists
- LRU cache
- Thread pools
- Producer-consumer pattern
- Concurrent programming
- TCP/IP fundamentals
- HTTP/1.1
- Request parsing
- Routing
- File I/O
- Caching
- Performance optimization

## Project Goal

Build a working HTTP server from the ground up and understand the complete flow:

    Browser -> TCP connection -> HTTP request -> Parser -> Router
    -> Application handler -> HTTP response -> Browser

**Core Java only. No Spring Boot.**