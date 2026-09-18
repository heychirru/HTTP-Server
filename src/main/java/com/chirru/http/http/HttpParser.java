package com.chirru.http.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpParser {
    private static final int MAX_HEADER_LINE = 8192;
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;

    private HttpParser() {}

    public static HttpRequest parse(InputStream input) throws IOException {
        ByteArrayOutputStream headerBytes = new ByteArrayOutputStream();
        int matched = 0;
        int current;

        while ((current = input.read()) != -1) {
            headerBytes.write(current);

            if ((matched == 0 && current == '\r') || (matched == 2 && current == '\r')) {
                matched++;
            } else if ((matched == 1 && current == '\n') || (matched == 3 && current == '\n')) {
                matched++;
                if (matched == 4) break;
            } else {
                matched = current == '\r' ? 1 : 0;
            }

            if (headerBytes.size() > MAX_HEADER_LINE * 100) {
                throw new IOException("HTTP headers too large");
            }
        }

        if (headerBytes.size() == 0) return null;

        String headerText = headerBytes.toString(StandardCharsets.ISO_8859_1);
        String[] lines = headerText.split("\\r\\n");
        if (lines.length == 0) throw new IOException("Malformed HTTP request");

        String[] parts = lines[0].split(" ", 3);
        if (parts.length != 3) throw new IOException("Malformed HTTP request line");

        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;
            if (lines[i].length() > MAX_HEADER_LINE) throw new IOException("HTTP header too large");

            int separator = lines[i].indexOf(':');
            if (separator <= 0) throw new IOException("Malformed HTTP header");

            headers.put(lines[i].substring(0, separator).trim().toLowerCase(),
                    lines[i].substring(separator + 1).trim());
        }

        int contentLength = parseContentLength(headers.get("content-length"));
        byte[] bodyBytes = input.readNBytes(contentLength);
        if (bodyBytes.length != contentLength) throw new IOException("Incomplete request body");

        String target = parts[1];
        int queryIndex = target.indexOf('?');
        String path = queryIndex >= 0 ? target.substring(0, queryIndex) : target;

        return new HttpRequest(parts[0], path, parts[2], Map.copyOf(headers),
                new String(bodyBytes, StandardCharsets.UTF_8));
    }

    private static int parseContentLength(String value) throws IOException {
        if (value == null || value.isBlank()) return 0;

        try {
            int length = Integer.parseInt(value);
            if (length < 0 || length > MAX_BODY_SIZE) throw new IOException("Request body too large");
            return length;
        } catch (NumberFormatException e) {
            throw new IOException("Invalid Content-Length");
        }
    }
}
