# Chirru HTTP Server

A lightweight HTTP/1.1 server built from scratch with Core Java.

## Rules

- No Spring Boot
- No Spring Framework
- No Tomcat / Jetty / Netty
- No external HTTP server library
- Java standard library for networking and HTTP handling

## Current milestone

**Phase 1 — TCP + HTTP parsing + concurrent client handling**

Implemented:
- `ServerSocket` TCP listener
- `Socket` client connections
- Java `ExecutorService` worker pool
- Basic HTTP/1.1 request parser
- HTTP response builder
- `GET /` route
- 404 response
- UTF-8 HTML responses

## Run

Requires JDK 21+.

```bash
mvn clean compile
mvn exec:java
```

Open http://localhost:8080

Choose another port:

```bash
mvn exec:java -Dexec.args="9090"
```

## Roadmap

- [x] TCP server
- [x] Basic HTTP request parsing
- [x] HTTP response generation
- [x] Concurrent client handling
- [ ] Router
- [ ] Dynamic path parameters
- [ ] Static file server
- [ ] POST body parsing
- [ ] JSON responses
- [ ] Keep-alive connections
- [ ] Custom bounded request queue
- [ ] LRU cache implemented from scratch
- [ ] Error handling and logging
- [ ] HTTP integration tests
- [ ] Benchmarking and performance tuning
