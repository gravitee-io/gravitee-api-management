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
package io.gravitee.apim.core.analytics_engine.model;

import java.util.List;

public record MetricSpec(
    Name name,
    String label,
    List<ApiSpec.Name> apis,
    Type type,
    Unit unit,
    List<Measure> measures,
    List<FilterSpec.Name> filters,
    List<FacetSpec.Name> facets
) {
    public enum Name {
        HTTP_REQUESTS,
        HTTP_ERRORS,
        HTTP_REQUEST_CONTENT_LENGTH,
        HTTP_RESPONSE_CONTENT_LENGTH,
        HTTP_ENDPOINT_RESPONSE_TIME,
        HTTP_GATEWAY_RESPONSE_TIME,
        HTTP_GATEWAY_LATENCY,
        LLM_PROMPT_TOKEN_SENT,
        LLM_PROMPT_TOKEN_RECEIVED,
        LLM_PROMPT_TOKEN_SENT_COST,
        LLM_PROMPT_TOKEN_RECEIVED_COST,
        LLM_PROMPT_TOTAL_TOKEN,
        LLM_PROMPT_TOKEN_COST,
        MESSAGE_PAYLOAD_SIZE,
        MESSAGES,
        MESSAGE_ERRORS,
        MESSAGE_GATEWAY_LATENCY,
        KAFKA_DOWNSTREAM_PUBLISH_MESSAGES,
        KAFKA_DOWNSTREAM_SUBSCRIBE_MESSAGES,
        KAFKA_DOWNSTREAM_PUBLISH_MESSAGES_BYTES,
        KAFKA_DOWNSTREAM_SUBSCRIBE_MESSAGE_BYTES,
        KAFKA_UPSTREAM_PUBLISH_MESSAGES,
        KAFKA_UPSTREAM_SUBSCRIBE_MESSAGES,
        KAFKA_UPSTREAM_PUBLISH_MESSAGE_BYTES,
        KAFKA_UPSTREAM_SUBSCRIBE_MESSAGE_BYTES,
        KAFKA_DOWNSTREAM_AUTHENTICATION_SUCCESSES,
        KAFKA_DOWNSTREAM_AUTHENTICATED_CONNECTIONS,
        KAFKA_UPSTREAM_AUTHENTICATION_SUCCESSES,
        KAFKA_UPSTREAM_AUTHENTICATED_CONNECTIONS,
        KAFKA_UPSTREAM_AUTHENTICATION_ERRORS,
        KAFKA_DOWNSTREAM_AUTHENTICATION_ERRORS,
        KAFKA_DOWNSTREAM_ACTIVE_CONNECTIONS,
        KAFKA_UPSTREAM_ACTIVE_CONNECTIONS,
        SUBSCRIPTIONS,
        APIS,
    }

    public enum Type {
        GAUGE,
        COUNTER,
    }

    public enum Unit {
        NUMBER,
        MILLISECONDS,
        BYTES,
    }

    public enum Measure {
        AVG,
        COUNT,
        MAX,
        MIN,
        RPS,
        P50,
        P99,
        P95,
        P90,
        PERCENTAGE,
    }
}
