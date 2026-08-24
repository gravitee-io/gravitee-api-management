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
package io.gravitee.gamma.rest.core.observability.logs.model;

import java.util.List;
import java.util.Map;
import lombok.Builder;

/**
 * One message that crossed a Message API during a connection, seen from both sides: the gateway
 * records an entrypoint document (the client leg) and an endpoint document (the broker leg), joined
 * here on the message id.
 *
 * <p>A connection log answers "who connected and did it work"; this answers "what actually flowed",
 * which for an event-driven API is the question worth asking. It is scoped to a single
 * {@code requestId} — there is no cross-connection message search.
 *
 * @param timestamp ISO-8601, unlike the epoch millis a {@link LogEntry} carries: this mirrors what
 *                  the underlying message log stores, and what the console renders.
 */
@Builder(toBuilder = true)
public record MessageLog(
    String requestId,
    String apiId,
    String timestamp,
    String clientIdentifier,
    String correlationId,
    String parentCorrelationId,
    String operation,
    Message entrypoint,
    Message endpoint
) {
    /**
     * One leg of a message. {@code connectorId} names the plugin that handled it — an entrypoint id
     * (http-post, sse, webhook…) on the client side, a connector type (kafka, mqtt5…) on the broker
     * side — so the two legs are told apart by their position, not by the value's shape.
     */
    @Builder(toBuilder = true)
    public record Message(
        String id,
        String timestamp,
        String connectorId,
        String payload,
        boolean error,
        Map<String, List<String>> headers,
        Map<String, String> metadata
    ) {}
}
