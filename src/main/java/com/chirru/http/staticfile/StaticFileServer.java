package com.chirru.http.staticfile;

import com.chirru.http.cache.LruCache;
import com.chirru.http.http.HttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class StaticFileServer {
    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry(".html", "text/html; charset=UTF-8"),
            Map.entry(".htm", "text/html; charset=UTF-8"),
            Map.entry(".css", "text/css; charset=UTF-8"),
            Map.entry(".js", "application/javascript; charset=UTF-8"),
            Map.entry(".json", "application/json; charset=UTF-8"),
            Map.entry(".txt", "text/plain; charset=UTF-8"),
            Map.entry(".xml", "application/xml; charset=UTF-8"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".ico", "image/x-icon")
    );

    private final Path root;
    private final LruCache<Path, byte[]> cache;

    public StaticFileServer(Path root) {
        this(root, 128);
    }

    public StaticFileServer(Path root, int cacheCapacity) {
        this.root = root.toAbsolutePath().normalize();
        this.cache = new LruCache<>(cacheCapacity);
    }

    public HttpResponse serve(String requestPath) {
        if (requestPath == null || !requestPath.startsWith("/")) {
            return HttpResponse.badRequest("Invalid file path");
        }

        int queryIndex = requestPath.indexOf('?');
        String cleanPath = queryIndex >= 0 ? requestPath.substring(0, queryIndex) : requestPath;
        Path requested = root.resolve(cleanPath.substring(1)).normalize();

        if (!requested.startsWith(root)) return HttpResponse.forbidden("Forbidden");

        if (Files.isDirectory(requested)) {
            requested = requested.resolve("index.html").normalize();
        }

        if (!requested.startsWith(root) || !Files.isRegularFile(requested)) {
            return HttpResponse.notFound("File not found");
        }

        try {
            byte[] content = cache.get(requested);
            if (content == null) {
                content = Files.readAllBytes(requested);
                cache.put(requested, content);
            }
            return new HttpResponse(200, "OK", contentType(requested), content);
        } catch (IOException e) {
            return HttpResponse.internalServerError("Could not read file");
        }
    }

    public int cacheSize() {
        return cache.size();
    }

    private String contentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0
                ? MIME_TYPES.getOrDefault(name.substring(dot), "application/octet-stream")
                : "application/octet-stream";
    }
}
