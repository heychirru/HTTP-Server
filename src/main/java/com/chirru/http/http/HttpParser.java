package com.chirru.http.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpParser {
    private static final int MAX_HEADER_LINE = 8192;
    private static final int MAX_HEADER_SIZE = 64 * 1024;
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

            if (headerBytes.size() > MAX_HEADER_SIZE) {
                throw HttpException.requestHeaderFieldsTooLarge("HTTP headers too large");
            }
        }

        if (headerBytes.size() == 0) return null;
        if (matched != 4) throw HttpException.badRequest("Malformed HTTP headers");

        String[] lines = headerBytes.toString(StandardCharsets.ISO_8859_1).split("\\r\\n");
        if (lines.length == 0) throw HttpException.badRequest("Malformed HTTP request");

        String[] parts = lines[0].split(" ", 3);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw HttpException.badRequest("Malformed HTTP request line");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;
            if (lines[i].length() > MAX_HEADER_LINE) {
                throw HttpException.requestHeaderFieldsTooLarge("HTTP header too large");
            }

            int separator = lines[i].indexOf(':');
            if (separator <= 0) throw HttpException.badRequest("Malformed HTTP header");

            headers.put(lines[i].substring(0, separator).trim().toLowerCase(),
                    lines[i].substring(separator + 1).trim());
        }

        int contentLength = parseContentLength(headers.get("content-length"));
        byte[] bodyBytes = input.readNBytes(contentLength);
        if (bodyBytes.length != contentLength) {
            throw HttpException.badRequest("Incomplete request body");
        }

        String target = parts[1];
        int queryIndex = target.indexOf('?');
        String path = queryIndex >= 0 ? target.substring(0, queryIndex) : target;
        String query = queryIndex >= 0 ? target.substring(queryIndex + 1) : "";

        return new HttpRequest(parts[0], path, parts[2], Map.copyOf(headers),
                new String(bodyBytes, StandardCharsets.UTF_8), Map.of(), parseQuery(query));
    }

    private static Map<String, String> parseQuery(String query) throws IOException {
        if (query.isEmpty()) return Map.of();

        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            if (pair.isEmpty()) continue;
            int equals = pair.indexOf('=');
            String rawName = equals >= 0 ? pair.substring(0, equals) : pair;
            String rawValue = equals >= 0 ? pair.substring(equals + 1) : "";
            try {
                params.put(URLDecoder.decode(rawName, StandardCharsets.UTF_8),
                        URLDecoder.decode(rawValue, StandardCharsets.UTF_8));
            } catch (IllegalArgumentException e) {
                throw HttpException.badRequest("Invalid query parameter encoding");
            }
        }
        return Map.copyOf(params);
    }

    private static int parseContentLength(String value) throws IOException {
        if (value == null || value.isBlank()) return 0;
        try {
            int length = Integer.parseInt(value);
            if (length < 0) throw HttpException.badRequest("Invalid Content-Length");
            if (length > MAX_BODY_SIZE) {
                throw HttpException.payloadTooLarge("Request body too large");
            }
            return length;
        } catch (NumberFormatException e) {
            throw HttpException.badRequest("Invalid Content-Length");
        }
    }
}
