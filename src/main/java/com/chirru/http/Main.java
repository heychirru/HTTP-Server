package com.chirru.http;

import com.chirru.http.server.HttpServer;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        HttpServer server = new HttpServer(port);
        server.start();
    }
}
