/*
 * Copyright 2015 Adobe Global Services.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.ags.curly.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;

/**
 *
 * @author brobert
 */
public class TestWebServer {

    private static int IP_PORT = (int) ((Math.random() * 5000.0) + 49152);
    HttpServer server;
    String responseMessage = "This is a sample response";
    HttpRequest lastRequest;
    long processingDelay = 0;
    boolean requireLogin = true;
    int port;

    static public TestWebServer getServer() throws IOException, InterruptedException {
        return new TestWebServer(IP_PORT++);
    }

    private TestWebServer(int port) throws IOException, InterruptedException {
        this.port=port;
        ServerBootstrap bootstrap = ServerBootstrap.bootstrap();
        bootstrap.setListenerPort(port);
        bootstrap.setServerInfo("Test/1.1");
        bootstrap.setSocketConfig(SocketConfig.DEFAULT);
        bootstrap.registerHandler("*", this::handleHttpRequest);
        server = bootstrap.create();
        server.start();
    }

    public void setResponseDelay(long delayInMillis) {
        processingDelay = delayInMillis;
    }

    public void setResponseMessage(String message) {
        responseMessage = message;
    }

    void handleHttpRequest(HttpRequest request, HttpResponse response, HttpContext context) throws UnsupportedEncodingException {
        lastRequest = request;
        try {
            if (requireLogin && request.getFirstHeader("Authorization") == null) {
                response.setStatusCode(401);
                response.setHeader("WWW-Authenticate", "Basic realm=\"test\"");
            } else {
                Thread.sleep(processingDelay);
                if (request.getRequestLine().getUri().contains("failure")) {
                    response.setStatusCode(403);
                } else {
                    response.setEntity(new StringEntity(responseMessage));
                }
            }
        } catch (InterruptedException ex) {
            Logger.getLogger(TestWebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void shutdown() {
        if (server != null) {
            server.shutdown(1, TimeUnit.SECONDS);
        }
    }
}
