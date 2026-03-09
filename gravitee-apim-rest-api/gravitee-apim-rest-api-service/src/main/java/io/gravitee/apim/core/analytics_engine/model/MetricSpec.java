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
    Unit unit,
    List<Measure> measures,
    List<FilterSpec.Name> filters,
    List<FacetSpec.Name> facets
) {
    public enum Name {
        HTTP_REQUESTS,
        HTTP_ERRORS,
        HTTP_ERROR_RATE,
        HTTP_RPS,
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
        LLM_PROMPT_TOKEN_TOTAL_COST,
        MESSAGE_PAYLOAD_SIZE,
        MESSAGES,
        MESSAGE_ERRORS,
        MESSAGE_GATEWAY_LATENCY,
        MESSAGE_RPS,
    }

    public enum Unit {
        NUMBER,
        MILLISECONDS,
        BYTES,
        PERCENTAGE,
        RPS,
    }

    public enum Measure {
        AVG,
        COUNT,
        MAX,
        MIN,
        VALUE,
        P50,
        P99,
        P95,
        P90,
    }
}
