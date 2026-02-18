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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class HTTPFieldResolverTest {

    private final HTTPFieldResolver fieldResolver = new HTTPFieldResolver();

    @ParameterizedTest
    @EnumSource(
        value = Metric.class,
        names = {
            "HTTP_REQUESTS",
            "HTTP_ERRORS",
            "HTTP_REQUEST_CONTENT_LENGTH",
            "HTTP_RESPONSE_CONTENT_LENGTH",
            "HTTP_ENDPOINT_RESPONSE_TIME",
            "HTTP_GATEWAY_RESPONSE_TIME",
            "HTTP_GATEWAY_LATENCY",
            "LLM_PROMPT_TOKEN_SENT",
            "LLM_PROMPT_TOKEN_RECEIVED",
            "LLM_PROMPT_TOKEN_SENT_COST",
            "LLM_PROMPT_TOKEN_RECEIVED_COST",
        }
    )
    void should_resolve_from_http_metric(Metric metric) {
        assertThat(fieldResolver.fromMetric(metric)).isNotBlank();
    }

    @Test
    void should_throw_unsupported_operation_exception_for_unknown_metric() {
        assertThrows(UnsupportedOperationException.class, () -> fieldResolver.fromMetric(Metric.MESSAGE_ERRORS));
    }

    @ParameterizedTest
    @EnumSource(
        value = Filter.Name.class,
        names = {
            "API",
            "APPLICATION",
            "PLAN",
            "GATEWAY",
            "ZONE",
            "TENANT",
            "HTTP_METHOD",
            "HTTP_STATUS",
            "HTTP_STATUS_CODE_GROUP",
            "HTTP_PATH",
            "HTTP_PATH_MAPPING",
            "GEO_IP_COUNTRY",
            "CONSUMER_IP",
            "MCP_PROXY_METHOD",
            "MCP_PROXY_TOOL",
            "MCP_PROXY_RESOURCE",
            "MCP_PROXY_PROMPT",
        }
    )
    void should_resolve_from_http_filter(Filter.Name name) {
        assertThat(fieldResolver.fromFilter(new Filter(name, Filter.Operator.EQ, "value"))).isNotBlank();
    }

    @Test
    void should_throw_unsupported_operation_exception_for_unknown_filter() {
        assertThrows(UnsupportedOperationException.class, () ->
            fieldResolver.fromFilter(new Filter(Filter.Name.KAFKA_TOPIC, Filter.Operator.EQ, "value"))
        );
    }

    @ParameterizedTest
    @EnumSource(
        value = Facet.class,
        names = {
            "API",
            "APPLICATION",
            "PLAN",
            "GATEWAY",
            "ZONE",
            "TENANT",
            "HTTP_METHOD",
            "HTTP_STATUS",
            "HTTP_STATUS_CODE_GROUP",
            "HTTP_PATH",
            "HTTP_PATH_MAPPING",
            "GEO_IP_COUNTRY",
            "CONSUMER_IP",
            "MCP_PROXY_METHOD",
            "MCP_PROXY_TOOL",
            "MCP_PROXY_RESOURCE",
            "MCP_PROXY_PROMPT",
        }
    )
    void should_resolve_from_http_facet(Facet name) {
        assertThat(fieldResolver.fromFacet(name)).isNotBlank();
    }

    @Test
    void should_resolve_mcp_proxy_filter_to_expected_es_fields() {
        assertThat(fieldResolver.fromFilter(new Filter(Filter.Name.MCP_PROXY_METHOD, Filter.Operator.EQ, "initialize"))).isEqualTo(
            "additional-metrics.keyword_mcp-proxy_method"
        );
        assertThat(fieldResolver.fromFilter(new Filter(Filter.Name.MCP_PROXY_TOOL, Filter.Operator.EQ, "tool"))).isEqualTo(
            "additional-metrics.keyword_mcp-proxy_tools/call"
        );
        assertThat(fieldResolver.fromFilter(new Filter(Filter.Name.MCP_PROXY_RESOURCE, Filter.Operator.EQ, "resource"))).isEqualTo(
            "additional-metrics.keyword_mcp-proxy_resources/read"
        );
        assertThat(fieldResolver.fromFilter(new Filter(Filter.Name.MCP_PROXY_PROMPT, Filter.Operator.EQ, "prompt"))).isEqualTo(
            "additional-metrics.keyword_mcp-proxy_prompts/get"
        );
    }

    @Test
    void should_resolve_mcp_proxy_facet_to_expected_es_fields() {
        assertThat(fieldResolver.fromFacet(Facet.MCP_PROXY_METHOD)).isEqualTo("additional-metrics.keyword_mcp-proxy_method");
        assertThat(fieldResolver.fromFacet(Facet.MCP_PROXY_TOOL)).isEqualTo("additional-metrics.keyword_mcp-proxy_tools/call");
        assertThat(fieldResolver.fromFacet(Facet.MCP_PROXY_RESOURCE)).isEqualTo("additional-metrics.keyword_mcp-proxy_resources/read");
        assertThat(fieldResolver.fromFacet(Facet.MCP_PROXY_PROMPT)).isEqualTo("additional-metrics.keyword_mcp-proxy_prompts/get");
    }

    @Test
    void should_throw_unsupported_operation_exception_for_unknown_facet() {
        assertThrows(UnsupportedOperationException.class, () -> fieldResolver.fromFacet(Facet.KAFKA_TOPIC));
    }
}
