package com.chirru.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Small dependency-free load test for the local server.
 *
 * Arguments: [url] [requests] [concurrency]
 *
 * Example:
 * mvn test-compile exec:java -Dexec.classpathScope=test
 *   -Dexec.mainClass=com.chirru.http.LoadTest
 *   -Dexec.args="http://localhost:8080/hello 1000 16"
 */
public final class LoadTest {
    private LoadTest() {}

    public static void main(String[] args) throws Exception {
        String url = args.length > 0 ? args[0] : "http://localhost:8080/hello";
        int requests = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
        int concurrency = args.length > 2 ? Integer.parseInt(args[2]) : 16;

        if (requests < 1 || concurrency < 1) {
            throw new IllegalArgumentException("requests and concurrency must be positive");
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();

        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        List<Thread> workers = new ArrayList<>();
        CountDownLatch start = new CountDownLatch(1);

        int perWorker = (requests + concurrency - 1) / concurrency;
        for (int i = 0; i < concurrency; i++) {
            int count = Math.min(perWorker, requests - i * perWorker);
            if (count <= 0) break;

            Thread worker = Thread.ofPlatform().name("load-worker-" + i).start(() -> {
                try {
                    start.await();
                    for (int j = 0; j < count; j++) {
                        try {
                            var response = client.send(request, BodyHandlers.discarding());
                            if (response.statusCode() >= 200 && response.statusCode() < 500) {
                                success.incrementAndGet();
                            } else {
                                failure.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failure.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            workers.add(worker);
        }

        long startNanos = System.nanoTime();
        start.countDown();

        for (Thread worker : workers) worker.join();

        double seconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
        double throughput = success.get() / seconds;

        System.out.printf(
                "Load test: requests=%d concurrency=%d success=%d failure=%d time=%.3fs throughput=%.2f req/s%n",
                requests, concurrency, success.get(), failure.get(), seconds, throughput);
    }
}
