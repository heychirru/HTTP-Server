package com.chirru.http.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class HttpParser {
    private static final int MAX_HEADER_LINE = 8192;
    private static final int MAX_HEADER_SIZE = 64 * 1024;
    private static final int MAX_HEADER_COUNT = 100;
    private static final int MAX_BODY_SIZE = 10 * 1024 * 1024;

    private static final Set<String> SUPPORTED_METHODS =
            Set.of("GET", "POST", "PUT", "DELETE", "HEAD");

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

        String method = parts[0].toUpperCase(Locale.ROOT);
        String version = parts[2];

        if (!SUPPORTED_METHODS.contains(method)) {
            throw HttpException.notImplemented("HTTP method not implemented");
        }

        if (!"HTTP/1.0".equals(version) && !"HTTP/1.1".equals(version)) {
            throw HttpException.httpVersionNotSupported("HTTP version not supported");
        }

        String target = parts[1];
        if (!target.startsWith("/") || target.startsWith("//")) {
            throw HttpException.badRequest("Invalid request target");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) continue;
            if (lines[i].length() > MAX_HEADER_LINE) {
                throw HttpException.requestHeaderFieldsTooLarge("HTTP header too large");
            }
            if (headers.size() >= MAX_HEADER_COUNT) {
                throw HttpException.requestHeaderFieldsTooLarge("Too many HTTP headers");
            }

            int separator = lines[i].indexOf(':');
            if (separator <= 0) throw HttpException.badRequest("Malformed HTTP header");

            String name = lines[i].substring(0, separator);
            if (!isToken(name)) throw HttpException.badRequest("Invalid HTTP header name");

            String normalizedName = name.toLowerCase(Locale.ROOT);
            String value = lines[i].substring(separator + 1).trim();

            if (headers.containsKey(normalizedName)) {
                if ("content-length".equals(normalizedName)) {
                    throw HttpException.badRequest("Duplicate Content-Length");
                }
                headers.put(normalizedName, headers.get(normalizedName) + ", " + value);
            } else {
                headers.put(normalizedName, value);
            }
        }

        String transferEncoding = headers.get("transfer-encoding");
        if (transferEncoding != null && !transferEncoding.isBlank()) {
            throw HttpException.notImplemented("Transfer-Encoding is not supported");
        }

        if ("HTTP/1.1".equals(version) && !headers.containsKey("host")) {
            throw HttpException.badRequest("Host header is required for HTTP/1.1");
        }

        int contentLength = parseContentLength(headers.get("content-length"));
        byte[] bodyBytes = input.readNBytes(contentLength);
        if (bodyBytes.length != contentLength) {
            throw HttpException.badRequest("Incomplete request body");
        }

        int queryIndex = target.indexOf('?');
        String path = queryIndex >= 0 ? target.substring(0, queryIndex) : target;
        String query = queryIndex >= 0 ? target.substring(queryIndex + 1) : "";

        return new HttpRequest(method, path, version, Map.copyOf(headers),
                new String(bodyBytes, StandardCharsets.UTF_8), Map.of(), parseQuery(query));
    }

    private static boolean isToken(String value) {
        if (value.isEmpty()) return false;

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isLetterOrDigit(c)) continue;

            if ("!#$%&'*+-.^_|~".indexOf(c) >= 0 || c == 96) continue;
            return false;
        }
        return true;
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
