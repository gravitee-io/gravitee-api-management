/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.management.v2.rest.resource.kafka_console;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.PoolOptions;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;
import org.springframework.core.env.Environment;

/**
 * The HTTP clients the Kafka console proxy talks to Kafbat with, one per Kafbat address, reused for the life of the node.
 * <p>
 * Vert.x holds on to every client it creates until its owner is closed, so a client per proxied request would
 * accumulate for as long as the node runs. Reusing one client means its pool is shared by every user of the node: each
 * request is therefore bounded in time, the wait queue is bounded, and only end-to-end headers are relayed.
 * <p>
 * Registered as a Spring bean by {@code RestManagementConfiguration}; its clients are closed when the context stops.
 */
@CustomLog
public class KafkaConsoleClient {

    // The console page loads its assets in parallel: the default pool of 5 would queue them.
    static final int MAX_CONNECTIONS = 20;

    // Beyond this, a request fails at once instead of waiting for a connection behind stalled ones.
    static final int MAX_WAIT_QUEUE_SIZE = 200;

    private static final long DEFAULT_TIMEOUT_MS = 30_000;

    /**
     * Headers not relayed to Kafbat, compared without case: the credentials, cookies and host belong to APIM and are
     * replaced or dropped, and the hop-by-hop and framing headers describe the browser's connection, not the pooled one
     * shared with other users. The body length is set again by Vert.x from the body it sends.
     */
    private static final Set<String> HEADER_NAMES_TO_REMOVE = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    static {
        HEADER_NAMES_TO_REMOVE.addAll(
            List.of(
                "Authorization",
                "Cookie",
                "Host",
                "Connection",
                "Keep-Alive",
                "Proxy-Authenticate",
                "Proxy-Authorization",
                "Proxy-Connection",
                "TE",
                "Trailer",
                "Transfer-Encoding",
                "Upgrade",
                "Content-Length"
            )
        );
    }

    private final Vertx vertx;
    private final Environment environment;
    private final Map<ClientKey, HttpClient> clients = new ConcurrentHashMap<>();

    public KafkaConsoleClient(Vertx vertx, Environment environment) {
        this.vertx = vertx;
        this.environment = environment;
    }

    public HttpClient client(String host, int port, boolean ssl) {
        return clients.computeIfAbsent(new ClientKey(host, port, ssl), this::buildHttpClient);
    }

    /** How long a proxied request may stay idle before Vert.x gives up on it, in milliseconds. */
    public long idleTimeout() {
        return environment.getProperty("kafka.console.server.timeout", Long.class, DEFAULT_TIMEOUT_MS);
    }

    public MultivaluedMap<String, String> forwardedHeaders(HttpServletRequest httpRequest) {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        httpRequest
            .getHeaderNames()
            .asIterator()
            .forEachRemaining(headerName -> {
                if (!HEADER_NAMES_TO_REMOVE.contains(headerName)) {
                    headers.putSingle(headerName, httpRequest.getHeader(headerName));
                }
            });
        return headers;
    }

    @PreDestroy
    public void close() {
        clients.values().forEach(HttpClient::close);
        clients.clear();
    }

    private HttpClient buildHttpClient(ClientKey key) {
        HttpClientOptions options = new HttpClientOptions();
        options.setDefaultHost(key.host());
        options.setDefaultPort(key.port());
        options.setDecompressionSupported(true);
        if (key.ssl()) {
            options.setSsl(true);
            options.setVerifyHost(false);
        }

        log.debug("HttpClient options: {}", options);

        return vertx.createHttpClient(options, new PoolOptions().setHttp1MaxSize(MAX_CONNECTIONS).setMaxWaitQueueSize(MAX_WAIT_QUEUE_SIZE));
    }

    private record ClientKey(String host, int port, boolean ssl) {}
}
