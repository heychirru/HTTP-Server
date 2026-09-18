package com.chirru.http.staticfile;

import com.chirru.http.http.HttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Serves files from a configured directory without using a web framework.
 * Path traversal is prevented by resolving and normalizing the requested path.
 */
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

    public StaticFileServer(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    public HttpResponse serve(String requestPath) {
        if (requestPath == null || !requestPath.startsWith("/")) {
            return HttpResponse.badRequest("Invalid file path");
        }

        String cleanPath = requestPath.split("\\?", 2)[0];
        Path requested = root.resolve(cleanPath.substring(1)).normalize();

        if (!requested.startsWith(root)) {
            return HttpResponse.forbidden("Forbidden");
        }

        if (Files.isDirectory(requested)) {
            requested = requested.resolve("index.html").normalize();
        }

        if (!requested.startsWith(root) || !Files.isRegularFile(requested)) {
            return HttpResponse.notFound("File not found");
        }

        try {
            byte[] content = Files.readAllBytes(requested);
            return new HttpResponse(200, "OK", contentType(requested), content);
        } catch (IOException e) {
            return HttpResponse.internalServerError("Could not read file");
        }
    }

    private String contentType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        int dot = name.lastIndexOf('.');
        if (dot >= 0) {
            return MIME_TYPES.getOrDefault(
                    name.substring(dot),
                    "application/octet-stream");
        }
        return "application/octet-stream";
    }
}
