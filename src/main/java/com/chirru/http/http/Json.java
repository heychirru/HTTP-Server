package com.chirru.http.http;

import java.util.Iterator;
import java.util.Map;

/**
 * Minimal JSON serializer for response payloads.
 * Supports strings, numbers, booleans, null and flat maps.
 */
public final class Json {
    private Json() {}

    public static String object(Map<String, ?> values) {
        StringBuilder json = new StringBuilder("{");
        Iterator<? extends Map.Entry<String, ?>> iterator = values.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, ?> entry = iterator.next();
            json.append('"').append(escape(entry.getKey())).append('"').append(':');
            appendValue(json, entry.getValue());
            if (iterator.hasNext()) json.append(',');
        }

        return json.append('}').toString();
    }

    private static void appendValue(StringBuilder json, Object value) {
        if (value == null) {
            json.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else {
            json.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace(""", "\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
