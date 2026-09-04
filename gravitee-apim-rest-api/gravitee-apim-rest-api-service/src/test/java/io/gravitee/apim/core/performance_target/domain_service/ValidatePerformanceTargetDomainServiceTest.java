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
package io.gravitee.apim.core.performance_target.domain_service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.performance_target.exception.InvalidPerformanceTargetException;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.definition.model.v4.ApiType;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ValidatePerformanceTargetDomainServiceTest {

    private static final String ENV_ID = "environment-id";
    private static final String A2A_API = "a2a-api";
    private static final String LLM_API = "llm-api";
    private static final String MCP_API = "mcp-api";

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final ValidatePerformanceTargetDomainService service = new ValidatePerformanceTargetDomainService(
        apiCrudService,
        new AnalyticsDefinitionYAMLQueryService()
    );

    @BeforeEach
    void setUp() {
        apiCrudService.initWith(
            List.of(
                ApiFixtures.anA2AProxyApiV4().toBuilder().id(A2A_API).build(),
                ApiFixtures.aLLMProxyApiV4().toBuilder().id(LLM_API).build(),
                ApiFixtures.aMCPProxyApiV4().toBuilder().id(MCP_API).build()
            )
        );
    }

    @Test
    void should_accept_a_latency_rule_on_an_a2a_subject() {
        var target = aTarget(List.of(A2A_API), aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 2000));

        assertThatCode(() -> service.validate(target)).doesNotThrowAnyException();
    }

    @Test
    void should_accept_a_throughput_floor_and_a_server_error_ceiling_on_an_mcp_subject() {
        var target = aTarget(
            List.of(MCP_API),
            aRule(MetricSpec.Name.HTTP_REQUESTS_PER_SECOND, MetricSpec.Measure.RATE, 2),
            aRule(MetricSpec.Name.HTTP_SERVER_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 1)
        );

        assertThatCode(() -> service.validate(target)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_an_llm_only_metric_on_an_a2a_only_subject() {
        var target = aTarget(List.of(A2A_API), aRule(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST, MetricSpec.Measure.AVG, 0.01));

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("LLM_PROMPT_TOKEN_TOTAL_COST")
            .hasMessageContaining("A2A_PROXY");
    }

    @Test
    void should_reject_a_measure_the_metric_does_not_support() {
        var target = aTarget(List.of(A2A_API), aRule(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.P95, 5));

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("P95")
            .hasMessageContaining("HTTP_ERROR_RATE");
    }

    @Test
    void should_accept_an_llm_metric_restricted_to_the_llm_ids_of_a_mixed_subject() {
        var rule = aRule(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST, MetricSpec.Measure.AVG, 0.01)
            .toBuilder()
            .apiTypes(Set.of(ApiType.LLM_PROXY))
            .build();
        var target = aTarget(List.of(A2A_API, LLM_API), rule);

        assertThatCode(() -> service.validate(target)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_an_llm_metric_on_a_mixed_subject_when_the_rule_is_not_restricted() {
        var target = aTarget(List.of(A2A_API, LLM_API), aRule(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST, MetricSpec.Measure.AVG, 0.01));

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("A2A_PROXY");
    }

    @Test
    void should_reject_rule_api_types_that_are_not_in_the_subject() {
        var rule = aRule(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 5)
            .toBuilder()
            .apiTypes(Set.of(ApiType.LLM_PROXY))
            .build();
        var target = aTarget(List.of(A2A_API), rule);

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("LLM_PROXY")
            .hasMessageContaining("subject");
    }

    @Test
    void should_accept_a_filter_available_for_the_rule_api_types() {
        var rule = aRule(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 5)
            .toBuilder()
            .filters(List.of(new Filter(FilterSpec.Name.MCP_PROXY_TOOL, FilterOperator.EQ, "search")))
            .build();
        var target = aTarget(List.of(MCP_API), rule);

        assertThatCode(() -> service.validate(target)).doesNotThrowAnyException();
    }

    @Test
    void should_reject_a_filter_not_available_for_the_rule_api_types() {
        var rule = aRule(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 5)
            .toBuilder()
            .filters(List.of(new Filter(FilterSpec.Name.MCP_PROXY_TOOL, FilterOperator.EQ, "search")))
            .build();
        var target = aTarget(List.of(A2A_API), rule);

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("MCP_PROXY_TOOL")
            .hasMessageContaining("A2A_PROXY");
    }

    @Test
    void should_reject_a_filter_operator_the_filter_does_not_support() {
        var rule = aRule(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 5)
            .toBuilder()
            .filters(List.of(new Filter(FilterSpec.Name.MCP_PROXY_TOOL, FilterOperator.CONTAINS, "sea")))
            .build();
        var target = aTarget(List.of(MCP_API), rule);

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("CONTAINS")
            .hasMessageContaining("MCP_PROXY_TOOL");
    }

    @Test
    void should_reject_a_filter_without_value() {
        var rule = aRule(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 5)
            .toBuilder()
            .filters(List.of(new Filter(FilterSpec.Name.MCP_PROXY_TOOL, FilterOperator.EQ, null)))
            .build();
        var target = aTarget(List.of(MCP_API), rule);

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("MCP_PROXY_TOOL")
            .hasMessageContaining("value");
    }

    @Test
    void should_reject_a_percent_threshold_above_100() {
        var target = aTarget(List.of(A2A_API), aRule(MetricSpec.Name.HTTP_ERROR_RATE, MetricSpec.Measure.PERCENTAGE, 150));

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("150")
            .hasMessageContaining("PERCENT");
    }

    @Test
    void should_reject_a_negative_threshold() {
        var target = aTarget(List.of(A2A_API), aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, -1));

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("-1")
            .hasMessageContaining("MILLISECONDS");
    }

    @Test
    void should_reject_a_window_shorter_than_the_interval() {
        var target = aTarget(List.of(A2A_API), aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 2000))
            .toBuilder()
            .window(Duration.ofMinutes(1))
            .interval(Duration.ofMinutes(5))
            .build();

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("window")
            .hasMessageContaining("interval");
    }

    @Test
    void should_reject_a_non_positive_interval() {
        var target = aTarget(List.of(A2A_API), aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 2000))
            .toBuilder()
            .interval(Duration.ZERO)
            .build();

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("interval");
    }

    @Test
    void should_reject_a_min_sample_size_below_1() {
        var target = aTarget(List.of(A2A_API), aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 2000))
            .toBuilder()
            .minSampleSize(0)
            .build();

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("minSampleSize");
    }

    @Test
    void should_reject_a_target_without_rules() {
        var target = aTarget(List.of(A2A_API));

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("rule");
    }

    @Test
    void should_reject_a_blank_reference() {
        var target = aTarget(List.of(A2A_API), aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 2000))
            .toBuilder()
            .subject(new PerformanceTarget.Subject(List.of(A2A_API), " "))
            .build();

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("reference");
    }

    @Test
    void should_reject_an_unknown_api_in_the_subject() {
        var target = aTarget(
            List.of(A2A_API, "unknown-api"),
            aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 2000)
        );

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("unknown-api");
    }

    @Test
    void should_reject_an_api_of_another_environment() {
        apiCrudService.create(ApiFixtures.aProxyApiV4().toBuilder().id("foreign-api").environmentId("other-env").build());
        var target = aTarget(List.of("foreign-api"), aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 2000));

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("foreign-api");
    }

    @Test
    void should_reject_an_api_that_is_not_v4() {
        apiCrudService.create(ApiFixtures.aProxyApiV2().toBuilder().id("v2-api").build());
        var target = aTarget(List.of("v2-api"), aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 2000));

        assertThatThrownBy(() -> service.validate(target))
            .isInstanceOf(InvalidPerformanceTargetException.class)
            .hasMessageContaining("v2-api")
            .hasMessageContaining("v4");
    }

    @Test
    void should_accept_a_subject_without_api_ids_yet() {
        var target = aTarget(List.of(), aRule(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME, MetricSpec.Measure.P95, 2000));

        assertThatCode(() -> service.validate(target)).doesNotThrowAnyException();
    }

    private static PerformanceTarget aTarget(List<String> apiIds, PerformanceTarget.Rule... rules) {
        return PerformanceTarget.builder()
            .id("target-id")
            .environmentId(ENV_ID)
            .subject(new PerformanceTarget.Subject(apiIds, "agent-42"))
            .window(Duration.ofMinutes(15))
            .interval(Duration.ofMinutes(5))
            .minSampleSize(20)
            .rules(List.of(rules))
            .build();
    }

    private static PerformanceTarget.Rule aRule(MetricSpec.Name metric, MetricSpec.Measure measure, double threshold) {
        return PerformanceTarget.Rule.builder()
            .metric(metric)
            .measure(measure)
            .operator(PerformanceTarget.Operator.LTE)
            .threshold(threshold)
            .build();
    }
}
