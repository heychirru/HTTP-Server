package com.chirru.http.http;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpParser {
    private static final int MAX_HEADER_LINE = 8192;

    private HttpParser() {}

    public static HttpRequest parse(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.ISO_8859_1));

        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isBlank()) {
            return null;
        }

        String[] parts = requestLine.split(" ", 3);
        if (parts.length != 3) {
            throw new IOException("Malformed HTTP request line");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            if (line.length() > MAX_HEADER_LINE) {
                throw new IOException("HTTP header too large");
            }

            int separator = line.indexOf(':');
            if (separator <= 0) {
                throw new IOException("Malformed HTTP header");
            }

            String name = line.substring(0, separator).trim().toLowerCase();
            String value = line.substring(separator + 1).trim();
            headers.put(name, value);
        }

        return new HttpRequest(parts[0], parts[1], parts[2], Map.copyOf(headers), "");
    }
}
