package com.chirru.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

/**
 * Simple repeatable latency/throughput benchmark.
 *
 * Arguments: [url] [warmup] [iterations]
 */
public final class Benchmark {
    private Benchmark() {}

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://localhost:8080/hello";
        int warmup = args.length > 1 ? Integer.parseInt(args[1]) : 100;
        int iterations = args.length > 2 ? Integer.parseInt(args[2]) : 1000;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();

        for (int i = 0; i < warmup; i++) {
            client.send(request, BodyHandlers.discarding());
        }

        long totalNanos = 0;
        long minNanos = Long.MAX_VALUE;
        long maxNanos = Long.MIN_VALUE;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            var response = client.send(request, BodyHandlers.discarding());
            long elapsed = System.nanoTime() - start;

            if (response.statusCode() < 200 || response.statusCode() >= 400) {
                throw new IllegalStateException("Unexpected HTTP status: " + response.statusCode());
            }

            totalNanos += elapsed;
            minNanos = Math.min(minNanos, elapsed);
            maxNanos = Math.max(maxNanos, elapsed);
        }

        double averageMicros = totalNanos / (double) iterations / 1_000.0;
        double minMicros = minNanos / 1_000.0;
        double maxMicros = maxNanos / 1_000.0;
        double seconds = totalNanos / 1_000_000_000.0;

        System.out.printf(
                "Benchmark: iterations=%d average=%.2f us min=%.2f us max=%.2f us throughput=%.2f req/s%n",
                iterations, averageMicros, minMicros, maxMicros,
                iterations / seconds);
    }
}
