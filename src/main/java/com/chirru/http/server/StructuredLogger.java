package com.chirru.http.server;

import com.chirru.http.http.Json;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small dependency-free JSON-lines logger.
 */
public final class StructuredLogger {
    private StructuredLogger() {}

    public static void info(String event, Map<String, ?> fields) {
        log("INFO", event, fields);
    }

    public static void warn(String event, Map<String, ?> fields) {
        log("WARN", event, fields);
    }

    public static void error(String event, Map<String, ?> fields) {
        log("ERROR", event, fields);
    }

    private static synchronized void log(String level, String event, Map<String, ?> fields) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("timestamp", Instant.now().toString());
        entry.put("level", level);
        entry.put("event", event);
        if (fields != null) entry.putAll(fields);
        System.out.println(Json.object(entry));
    }
}
