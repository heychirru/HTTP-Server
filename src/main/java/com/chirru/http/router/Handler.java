package com.chirru.http.router;

import com.chirru.http.http.HttpRequest;
import com.chirru.http.http.HttpResponse;

@FunctionalInterface
public interface Handler {
    HttpResponse handle(HttpRequest request);
}
