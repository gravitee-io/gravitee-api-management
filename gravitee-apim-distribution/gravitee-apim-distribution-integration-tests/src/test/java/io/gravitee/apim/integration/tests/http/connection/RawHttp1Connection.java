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
package io.gravitee.apim.integration.tests.http.connection;

import static org.awaitility.Awaitility.await;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.rxjava3.core.Vertx;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.awaitility.core.ConditionTimeoutException;

/**
 * A deliberately naive HTTP/1.1 client over a raw socket.
 *
 * <p>It always writes the next request on the same socket, whatever the previous response said — a
 * {@code Connection: close} included. Real clients honour that header and silently open a new connection,
 * which would hide a server that neither answers nor closes. Writes are asynchronous: a request body the
 * server does not read never blocks the test.
 */
class RawHttp1Connection {

    enum SecondOutcome {
        /** The server answered the second request on the same connection. */
        RESPONDED,
        /** The server closed the connection after having fully sent the first response. */
        CLOSED_AFTER_FIRST,
        /** The server neither answered nor closed: the connection is stalled. */
        STALLED,
    }

    record Request(String method, String path, String version, int bodySize, boolean chunkedEncoding, Map<String, String> headers) {
        static Request get(String path) {
            return new Request("GET", path, "HTTP/1.1", 0, false, Map.of());
        }

        static Request post(String path, int bodySize) {
            return new Request("POST", path, "HTTP/1.1", bodySize, false, Map.of());
        }

        Request chunked() {
            return new Request(method, path, version, bodySize, true, headers);
        }

        Request http10KeepAlive() {
            return new Request(method, path, "HTTP/1.0", bodySize, chunkedEncoding, Map.of("Connection", "keep-alive"));
        }

        Request withHeader(String name, String value) {
            Map<String, String> newHeaders = new TreeMap<>(headers);
            newHeaders.put(name, value);
            return new Request(method, path, version, bodySize, chunkedEncoding, newHeaders);
        }

        Request withoutBody() {
            return new Request(method, path, version, 0, false, headers);
        }
    }

    record Response(int status, Map<String, String> headers) {
        String header(String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }
    }

    record Outcome(Response first, SecondOutcome second, Response secondResponse) {}

    private static final Duration FIRST_RESPONSE_TIMEOUT = Duration.ofSeconds(10);
    // Twice the time the gateway spends at most discarding an unread body, so that a close driven by that delay is
    // never mistaken for a stall.
    private static final Duration SECOND_OUTCOME_TIMEOUT = Duration.ofSeconds(10);
    private static final int CHUNK_SIZE = 64 * 1024;

    private final Buffer received = Buffer.buffer();
    private final NetSocket socket;
    private volatile boolean closed;

    private RawHttp1Connection(NetSocket socket) {
        this.socket = socket;
        socket.handler(buffer -> {
            synchronized (received) {
                received.appendBuffer(buffer);
            }
        });
        socket.exceptionHandler(throwable -> closed = true);
        socket.closeHandler(v -> closed = true);
    }

    static RawHttp1Connection open(Vertx vertx, int port) throws Exception {
        NetSocket socket = vertx.getDelegate().createNetClient().connect(port, "localhost").await(10, TimeUnit.SECONDS);
        return new RawHttp1Connection(socket);
    }

    /**
     * Sends {@code first}, waits for its response, then sends the same request without a body on the very same socket
     * and reports what the server did with it.
     */
    static Outcome sendTwoRequestsOnSameConnection(Vertx vertx, int port, Request first) throws Exception {
        RawHttp1Connection connection = open(vertx, port);
        try {
            connection.write(first);
            Response firstResponse = connection.awaitResponses(1, FIRST_RESPONSE_TIMEOUT).get(0);

            connection.write(first.withoutBody());
            try {
                await()
                    .atMost(SECOND_OUTCOME_TIMEOUT)
                    .pollInterval(Duration.ofMillis(20))
                    .until(() -> connection.closed || connection.parseResponses().size() >= 2);
            } catch (ConditionTimeoutException e) {
                return new Outcome(firstResponse, SecondOutcome.STALLED, null);
            }

            List<Response> responses = connection.parseResponses();
            if (responses.size() >= 2) {
                return new Outcome(firstResponse, SecondOutcome.RESPONDED, responses.get(1));
            }
            return new Outcome(firstResponse, SecondOutcome.CLOSED_AFTER_FIRST, null);
        } finally {
            connection.close();
        }
    }

    void write(Request request) {
        StringBuilder head = new StringBuilder()
            .append(request.method())
            .append(' ')
            .append(request.path())
            .append(' ')
            .append(request.version())
            .append("\r\n")
            .append("Host: localhost\r\n");
        if (request.chunkedEncoding()) {
            head.append("Transfer-Encoding: chunked\r\n");
        } else if (request.bodySize() > 0 || !"GET".equals(request.method())) {
            head.append("Content-Length: ").append(request.bodySize()).append("\r\n");
        }
        request.headers().forEach((name, value) -> head.append(name).append(": ").append(value).append("\r\n"));
        head.append("\r\n");

        Buffer bytes = Buffer.buffer(head.toString());
        if (request.chunkedEncoding()) {
            for (int remaining = request.bodySize(); remaining > 0; remaining -= CHUNK_SIZE) {
                int size = Math.min(CHUNK_SIZE, remaining);
                bytes.appendString(Integer.toHexString(size) + "\r\n").appendBytes(new byte[size]).appendString("\r\n");
            }
            bytes.appendString("0\r\n\r\n");
        } else if (request.bodySize() > 0) {
            bytes.appendBytes(new byte[request.bodySize()]);
        }
        // Ignore write failures: the server is allowed to close the connection before reading everything.
        socket.write(bytes).onFailure(throwable -> closed = true);
    }

    List<Response> awaitResponses(int count, Duration timeout) {
        await()
            .atMost(timeout)
            .pollInterval(Duration.ofMillis(20))
            .until(() -> parseResponses().size() >= count);
        return parseResponses();
    }

    void close() {
        if (!closed) {
            socket.close();
        }
    }

    /**
     * Parses every complete response received so far. A response whose body is not fully received yet is ignored.
     */
    List<Response> parseResponses() {
        byte[] bytes;
        synchronized (received) {
            bytes = received.getBytes();
        }
        List<Response> responses = new ArrayList<>();
        int offset = 0;
        while (offset < bytes.length) {
            int headEnd = indexOf(bytes, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), offset);
            if (headEnd < 0) {
                break;
            }
            String[] lines = new String(bytes, offset, headEnd - offset, StandardCharsets.ISO_8859_1).split("\r\n");
            int status = Integer.parseInt(lines[0].split(" ")[1]);
            Map<String, String> responseHeaders = new TreeMap<>();
            for (int i = 1; i < lines.length; i++) {
                int colon = lines[i].indexOf(':');
                if (colon > 0) {
                    responseHeaders.put(lines[i].substring(0, colon).trim().toLowerCase(Locale.ROOT), lines[i].substring(colon + 1).trim());
                }
            }

            int bodyStart = headEnd + 4;
            int bodyEnd;
            if (responseHeaders.containsKey("content-length")) {
                bodyEnd = bodyStart + Integer.parseInt(responseHeaders.get("content-length"));
            } else if ("chunked".equalsIgnoreCase(responseHeaders.get("transfer-encoding"))) {
                bodyEnd = chunkedBodyEnd(bytes, bodyStart);
            } else {
                bodyEnd = bodyStart;
            }
            if (bodyEnd < 0 || bodyEnd > bytes.length) {
                break;
            }
            responses.add(new Response(status, responseHeaders));
            offset = bodyEnd;
        }
        return responses;
    }

    private static int chunkedBodyEnd(byte[] bytes, int start) {
        byte[] crlf = "\r\n".getBytes(StandardCharsets.US_ASCII);
        int offset = start;
        while (true) {
            int sizeEnd = indexOf(bytes, crlf, offset);
            if (sizeEnd < 0) {
                return -1;
            }
            String sizeLine = new String(bytes, offset, sizeEnd - offset, StandardCharsets.US_ASCII);
            int size = Integer.parseInt(sizeLine.split(";")[0].trim(), 16);
            if (size == 0) {
                // Last chunk: the optional trailer section ends with an empty line.
                int trailerEnd = indexOf(bytes, "\r\n\r\n".getBytes(StandardCharsets.US_ASCII), sizeEnd);
                return trailerEnd < 0 ? -1 : trailerEnd + 4;
            }
            offset = sizeEnd + 2 + size + 2;
            if (offset > bytes.length) {
                return -1;
            }
        }
    }

    private static int indexOf(byte[] bytes, byte[] pattern, int from) {
        outer: for (int i = from; i <= bytes.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (bytes[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
