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
package io.gravitee.apim.rest.api.mcp.core.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.rest.api.mcp.core.TriageException;
import io.gravitee.apim.rest.api.mcp.infra.DefinitionAnalyticsCatalog;
import org.junit.jupiter.api.Test;

/**
 * Over the shipped definition, so that what the tool describes is what the engine takes.
 */
class DescribeMetricsUseCaseTest {

    private static final AnalyticsDefinitionYAMLQueryService DEFINITION = new AnalyticsDefinitionYAMLQueryService();

    private final DescribeMetricsUseCase useCase = new DescribeMetricsUseCase(
        new DefinitionAnalyticsCatalog(
            new GetApiSpecUseCase(DEFINITION),
            new GetApiMetricSpecUseCase(DEFINITION),
            new GetMetricFacetSpecUseCase(DEFINITION),
            new GetMetricFilterSpecUseCase(DEFINITION)
        )
    );

    @Test
    void should_list_every_api_type_with_its_metrics_units_and_measures() {
        var output = useCase.execute(new DescribeMetricsUseCase.Input(null, null));

        assertThat(output.metric()).isNull();
        assertThat(output.apiTypes())
            .extracting(DescribeMetricsUseCase.ApiTypeMetrics::name)
            .contains("HTTP_PROXY", "MESSAGE", "LLM", "NATIVE", "AUTHZ");
        var http = output
            .apiTypes()
            .stream()
            .filter(type -> type.name().equals("HTTP_PROXY"))
            .findFirst()
            .orElseThrow();
        assertThat(http.metrics()).anySatisfy(metric -> {
            assertThat(metric.name()).isEqualTo("HTTP_GATEWAY_RESPONSE_TIME");
            assertThat(metric.unit()).isEqualTo("MILLISECONDS");
            assertThat(metric.measures()).contains("AVG", "P95");
        });
        assertThat(output.summary()).contains("Call describe_metrics with metric=<name>");
    }

    @Test
    void should_narrow_to_one_api_type() {
        var output = useCase.execute(new DescribeMetricsUseCase.Input("llm", null));

        assertThat(output.apiTypes()).extracting(DescribeMetricsUseCase.ApiTypeMetrics::name).containsExactly("LLM");
    }

    @Test
    void should_describe_a_metric_with_its_facets_and_filters() {
        var output = useCase.execute(new DescribeMetricsUseCase.Input(null, "http_requests"));

        assertThat(output.apiTypes()).isNull();
        assertThat(output.metric().metric().name()).isEqualTo("HTTP_REQUESTS");
        assertThat(output.metric().apiTypes()).contains("HTTP_PROXY");
        assertThat(output.metric().facets())
            .extracting(DescribeMetricsUseCase.Facet::name)
            .contains("API", "APPLICATION", "HTTP_STATUS_CODE_GROUP", "CONSUMER_IP");
        assertThat(output.metric().filters()).anySatisfy(filter -> {
            assertThat(filter.name()).isEqualTo("HTTP_STATUS_CODE_GROUP");
            assertThat(filter.operators()).contains("EQ", "IN");
            assertThat(filter.values()).contains("4XX", "5XX");
        });
    }

    @Test
    void should_name_the_catalog_when_the_metric_or_type_is_unknown() {
        assertThatThrownBy(() -> useCase.execute(new DescribeMetricsUseCase.Input(null, "REQUESTS")))
            .isInstanceOf(TriageException.class)
            .hasMessage("Unknown metric 'REQUESTS'. Call describe_metrics to list them.");
        assertThatThrownBy(() -> useCase.execute(new DescribeMetricsUseCase.Input("GRAPHQL", null)))
            .isInstanceOf(TriageException.class)
            .hasMessageContaining("Unknown API type 'GRAPHQL'");
    }
}
