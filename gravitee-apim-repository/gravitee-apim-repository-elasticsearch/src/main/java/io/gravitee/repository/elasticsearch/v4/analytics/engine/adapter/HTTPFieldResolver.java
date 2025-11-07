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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HTTPFieldResolver implements FieldResolver {

    public String fromMetric(Metric metric) {
        return switch (metric) {
            case HTTP_REQUESTS, HTTP_ERRORS -> "@timestamp";
            case HTTP_REQUEST_CONTENT_LENGTH -> "request-content-length";
            case HTTP_RESPONSE_CONTENT_LENGTH -> "response-content-length";
            case HTTP_ENDPOINT_RESPONSE_TIME -> "endpoint-response-time-ms";
            case HTTP_GATEWAY_RESPONSE_TIME -> "gateway-response-time-ms";
            case HTTP_GATEWAY_LATENCY -> "gateway-latency-ms";
            case LLM_PROMPT_TOKEN_SENT -> "additional-metrics.long_ai-prompt-token-sent";
            case LLM_PROMPT_TOKEN_RECEIVED -> "additional-metrics.long_ai-prompt-token-receive";
            case LLM_PROMPT_TOKEN_SENT_COST -> "additional-metrics.double_ai-prompt-token-sent-cost";
            case LLM_PROMPT_TOKEN_RECEIVED_COST -> "additional-metrics.double_ai-prompt-token-receive-cost";
            default -> throw new UnsupportedOperationException("not an HTTP metric");
        };
    }

    @Override
    public String fromFilter(Filter filter) {
        return switch (filter.name()) {
            case API -> "api-id";
            case APPLICATION -> "application-id";
            case PLAN -> "plan-id";
            case GATEWAY -> "gateway";
            case HOST -> "host";
            case HTTP_METHOD -> "http-method";
            case HTTP_STATUS -> "status";
            default -> throw new UnsupportedOperationException("not an HTTP filter");
        };
    }
}
