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
import io.gravitee.repository.elasticsearch.v4.analytics.engine.LlmProxyFields;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HTTPFieldResolver implements FieldResolver {

    public String fromMetric(Metric metric) {
        return switch (metric) {
            case HTTP_REQUESTS, HTTP_ERRORS, HTTP_REQUESTS_PER_SECOND -> "@timestamp";
            case HTTP_REQUEST_CONTENT_LENGTH -> "request-content-length";
            case HTTP_RESPONSE_CONTENT_LENGTH -> "response-content-length";
            case HTTP_ENDPOINT_RESPONSE_TIME -> "endpoint-response-time-ms";
            case HTTP_GATEWAY_RESPONSE_TIME -> "gateway-response-time-ms";
            case HTTP_GATEWAY_LATENCY -> "gateway-latency-ms";
            case LLM_PROMPT_TOKEN_SENT -> LlmProxyFields.TOKENS_SENT;
            case LLM_PROMPT_TOKEN_RECEIVED -> LlmProxyFields.TOKENS_RECEIVED;
            case LLM_PROMPT_TOKEN_SENT_COST -> LlmProxyFields.SENT_COST;
            case LLM_PROMPT_TOKEN_RECEIVED_COST -> LlmProxyFields.RECEIVED_COST;
            case LLM_PROMPT_TOKEN_REASONING -> LlmProxyFields.TOKENS_REASONING;
            case LLM_PROMPT_TOKEN_REASONING_COST -> LlmProxyFields.REASONING_COST;
            case MCP_PROXY_TOOL_COST -> "additional-metrics.double_mcp-proxy_tool-cost";
            case LLM_LARGEST_TURN -> LlmProxyFields.CALL_NUMBER;
            case EDGE_DETECTION_COUNT -> "additional-metrics.long_edge_count";
            case EDGE_TOKENS_IN -> "additional-metrics.long_edge_tokens_in";
            case EDGE_TOKENS_OUT -> "additional-metrics.long_edge_tokens_out";
            case EDGE_HEARTBEAT_COUNT -> "@timestamp";
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
            // the gateway reports the path after the context path as path-info; `path` is declared but never written
            case Filter.Name.HTTP_PATH -> "path-info.keyword";
            case Filter.Name.HTTP_PATH_MAPPING -> "mapped-path";
            case Filter.Name.GEO_IP_COUNTRY -> "geoip.country_iso_code";
            case Filter.Name.GEO_IP_REGION -> "geoip.region_name";
            case Filter.Name.GEO_IP_CITY -> "geoip.city_name";
            case Filter.Name.GEO_IP_CONTINENT -> "geoip.continent_name";
            case Filter.Name.CONSUMER_IP -> "remote-address";
            case Filter.Name.HTTP_USER_AGENT_OS_NAME -> "user_agent.os_name";
            case Filter.Name.HTTP_USER_AGENT_DEVICE -> "user_agent.device.name";
            case Filter.Name.LLM_PROXY_MODEL -> LlmProxyFields.MODEL;
            case Filter.Name.LLM_PROXY_PROVIDER -> LlmProxyFields.PROVIDER;
            case Filter.Name.LLM_PROXY_CONVERSATION -> LlmProxyFields.CONVERSATION_ID;
            case Filter.Name.LLM_PROXY_REQUEST_KIND -> LlmProxyFields.REQUEST_KIND;
            case Filter.Name.LLM_PROXY_TOOL -> LlmProxyFields.TOOL_NAMES;
            case Filter.Name.MCP_PROXY_METHOD -> "additional-metrics.keyword_mcp-proxy_method";
            case Filter.Name.MCP_PROXY_TOOL -> "additional-metrics.keyword_mcp-proxy_tools/call";
            case Filter.Name.MCP_PROXY_RESOURCE -> "additional-metrics.keyword_mcp-proxy_resources/read";
            case Filter.Name.MCP_PROXY_PROMPT -> "additional-metrics.keyword_mcp-proxy_prompts/get";
            case Filter.Name.MCP_PROXY_TOOL_PRICE_STATUS -> "additional-metrics.keyword_mcp-proxy_tool-price";
            case Filter.Name.MCP_PROXY_TOOL_BILLED -> "additional-metrics.keyword_mcp-proxy_tool-billed";
            case Filter.Name.EDGE_PROVIDER -> "additional-metrics.keyword_edge_provider";
            case Filter.Name.EDGE_PROCESS -> "additional-metrics.keyword_edge_process";
            case Filter.Name.EDGE_CLIENT -> "client-identifier";
            case Filter.Name.EDGE_TYPE -> "additional-metrics.keyword_edge_type";
            case Filter.Name.EDGE_VERSION -> "additional-metrics.keyword_edge_version";
            case Filter.Name.EDGE_MODEL -> "additional-metrics.keyword_edge_model";
            case Filter.Name.EDGE_TOOL -> "additional-metrics.keyword_edge_tool";
            case Filter.Name.API_PRODUCT -> "api-product-id";
            case Filter.Name.HTTP_ENDPOINT_RESPONSE_TIME -> "endpoint-response-time-ms";
            case Filter.Name.HTTP_GATEWAY_RESPONSE_TIME -> "gateway-response-time-ms";
            case Filter.Name.HTTP_GATEWAY_LATENCY -> "gateway-latency-ms";
            case Filter.Name.HTTP_REQUEST_CONTENT_LENGTH -> "request-content-length";
            case Filter.Name.HTTP_RESPONSE_CONTENT_LENGTH -> "response-content-length";
            case Filter.Name.URI -> "uri";
            case Filter.Name.ENTRYPOINT -> "entrypoint-id";
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
            case HTTP_PATH -> "path-info.keyword";
            case HTTP_PATH_MAPPING -> "mapped-path";
            case GEO_IP_COUNTRY -> "geoip.country_iso_code";
            case GEO_IP_REGION -> "geoip.region_name";
            case GEO_IP_CITY -> "geoip.city_name";
            case GEO_IP_CONTINENT -> "geoip.continent_name";
            case CONSUMER_IP -> "remote-address";
            case HTTP_USER_AGENT_OS_NAME -> "user_agent.os_name";
            case HTTP_USER_AGENT_DEVICE -> "user_agent.device.name";
            case LLM_PROXY_MODEL -> LlmProxyFields.MODEL;
            case LLM_PROXY_PROVIDER -> LlmProxyFields.PROVIDER;
            case LLM_PROXY_CONVERSATION -> LlmProxyFields.CONVERSATION_ID;
            case LLM_PROXY_REQUEST_KIND -> LlmProxyFields.REQUEST_KIND;
            case LLM_PROXY_TOOL -> LlmProxyFields.TOOL_NAMES;
            case MCP_PROXY_METHOD -> "additional-metrics.keyword_mcp-proxy_method";
            case MCP_PROXY_TOOL -> "additional-metrics.keyword_mcp-proxy_tools/call";
            case MCP_PROXY_RESOURCE -> "additional-metrics.keyword_mcp-proxy_resources/read";
            case MCP_PROXY_PROMPT -> "additional-metrics.keyword_mcp-proxy_prompts/get";
            case MCP_PROXY_TOOL_PRICE_STATUS -> "additional-metrics.keyword_mcp-proxy_tool-price";
            case MCP_PROXY_TOOL_BILLED -> "additional-metrics.keyword_mcp-proxy_tool-billed";
            case EDGE_PROVIDER -> "additional-metrics.keyword_edge_provider";
            case EDGE_PROCESS -> "additional-metrics.keyword_edge_process";
            case EDGE_CLIENT -> "client-identifier";
            case EDGE_TYPE -> "additional-metrics.keyword_edge_type";
            case EDGE_VERSION -> "additional-metrics.keyword_edge_version";
            case EDGE_MODEL -> "additional-metrics.keyword_edge_model";
            case EDGE_TOOL -> "additional-metrics.keyword_edge_tool";
            case API_PRODUCT -> "api-product-id";
            default -> throw new UnsupportedOperationException("not an HTTP facet");
        };
    }
}
