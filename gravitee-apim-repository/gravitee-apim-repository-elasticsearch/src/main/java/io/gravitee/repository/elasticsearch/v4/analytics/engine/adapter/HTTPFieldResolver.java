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
import io.gravitee.repository.analytics.engine.api.query.Facet;
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
            case LLM_PROMPT_TOKEN_SENT -> "additional-metrics.long_llm-proxy_tokens-sent";
            case LLM_PROMPT_TOKEN_RECEIVED -> "additional-metrics.long_llm-proxy_tokens-received";
            case LLM_PROMPT_TOKEN_SENT_COST -> "additional-metrics.double_llm-proxy_sent-cost";
            case LLM_PROMPT_TOKEN_RECEIVED_COST -> "additional-metrics.double_llm-proxy_received-cost";
            default -> throw new UnsupportedOperationException("not an HTTP metric");
        };
    }

    @Override
    public String fromFilter(Filter filter) {
        return switch (filter.name()) {
            case Filter.Name.API -> "api-id";
            case Filter.Name.APPLICATION -> "application-id";
            case Filter.Name.PLAN -> "plan-id";
            case Filter.Name.GATEWAY -> "gateway";
            case Filter.Name.HOST -> "host";
            case Filter.Name.TENANT -> "tenant";
            case Filter.Name.ZONE -> "zone";
            case Filter.Name.HTTP_METHOD -> "http-method";
            case Filter.Name.HTTP_STATUS_CODE_GROUP, HTTP_STATUS -> "status";
            case Filter.Name.HTTP_PATH -> "path";
            case Filter.Name.HTTP_PATH_MAPPING -> "mapped-path";
            case Filter.Name.GEO_IP_COUNTRY -> "geoip.country_iso_code";
            case Filter.Name.GEO_IP_REGION -> "geoip.region_name";
            case Filter.Name.GEO_IP_CITY -> "geoip.city_name";
            case Filter.Name.GEO_IP_CONTINENT -> "geoip.continent_name";
            case Filter.Name.CONSUMER_IP -> "remote-address";
            case Filter.Name.HTTP_USER_AGENT_OS_NAME -> "user_agent.os_name";
            case Filter.Name.HTTP_USER_AGENT_DEVICE -> "user_agent.device.name";
            case Filter.Name.LLM_PROXY_MODEL -> "additional-metrics.keyword_llm-proxy_model";
            case Filter.Name.LLM_PROXY_PROVIDER -> "additional-metrics.keyword_llm-proxy_provider";
            default -> throw new UnsupportedOperationException("not an HTTP filter");
        };
    }

    @Override
    public String fromFacet(Facet facet) {
        return switch (facet) {
            case API -> "api-id";
            case APPLICATION -> "application-id";
            case PLAN -> "plan-id";
            case GATEWAY -> "gateway";
            case HOST -> "host";
            case TENANT -> "tenant";
            case ZONE -> "zone";
            case HTTP_METHOD -> "http-method";
            case HTTP_STATUS_CODE_GROUP, HTTP_STATUS -> "status";
            case HTTP_PATH -> "path";
            case HTTP_PATH_MAPPING -> "mapped-path";
            case GEO_IP_COUNTRY -> "geoip.country_iso_code";
            case GEO_IP_REGION -> "geoip.region_name";
            case GEO_IP_CITY -> "geoip.city_name";
            case GEO_IP_CONTINENT -> "geoip.continent_name";
            case CONSUMER_IP -> "remote-address";
            case HTTP_USER_AGENT_OS_NAME -> "user_agent.os_name";
            case HTTP_USER_AGENT_DEVICE -> "user_agent.device.name";
            case LLM_PROXY_MODEL -> "additional-metrics.keyword_llm-proxy_model";
            case LLM_PROXY_PROVIDER -> "additional-metrics.keyword_llm-proxy_provider";
            default -> throw new UnsupportedOperationException("not an HTTP facet");
        };
    }
}
