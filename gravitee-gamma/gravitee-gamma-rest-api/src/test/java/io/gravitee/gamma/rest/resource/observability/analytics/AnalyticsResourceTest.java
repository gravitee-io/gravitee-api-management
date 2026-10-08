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
package io.gravitee.gamma.rest.resource.observability.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityFacetsUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityMeasuresUseCase;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.ComputeObservabilityTimeSeriesUseCase;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsResourceTest.AnalyticsTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

/**
 * Real payloads through Jersey and the real use cases and filter catalog; only the analytics engine behind
 * the data port is mocked.
 */
@ContextConfiguration(classes = { ResourceContextConfiguration.class, AnalyticsTestConfiguration.class })
class AnalyticsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "fake-env";
    private static final Map<String, String> TIME_RANGE = Map.of("from", "2026-06-10T00:00:00Z", "to", "2026-06-11T00:00:00Z");
    private static final List<Map<String, Object>> REQUEST_COUNT = List.of(Map.of("name", "HTTP_REQUESTS", "measures", List.of("COUNT")));

    @Inject
    private ObservabilityAnalyticsDataPort analyticsDataPort;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/observability/analytics";
    }

    @BeforeEach
    void prepareEnvironment() {
        EnvironmentEntity env = new EnvironmentEntity();
        env.setId(ENVIRONMENT);
        env.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(env);
        when(analyticsDataPort.loadAccessibleApis(any(), any())).thenReturn(
            List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
        );
        when(analyticsDataPort.computeMeasures(any())).thenReturn(JsonNodeFactory.instance.objectNode());
        when(analyticsDataPort.computeFacets(any())).thenReturn(JsonNodeFactory.instance.objectNode());
        when(analyticsDataPort.computeTimeSeries(any())).thenReturn(JsonNodeFactory.instance.objectNode());
    }

    @AfterEach
    void resetDataPort() {
        reset(analyticsDataPort);
    }

    @Nested
    class QueryShape {

        @Test
        void should_answer_200_to_a_well_formed_measures_request() {
            var response = post("measures", Map.of("timeRange", TIME_RANGE, "metrics", REQUEST_COUNT));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_answer_400_when_the_time_range_misses_a_bound() {
            var response = post("measures", Map.of("timeRange", Map.of("from", "2026-06-10T00:00:00Z"), "metrics", REQUEST_COUNT));

            assertBadRequest(response, "observability.query.time_range_required");
        }

        @Test
        void should_answer_400_when_no_metric_is_requested() {
            var response = post("facets", Map.of("timeRange", TIME_RANGE, "metrics", List.of(), "by", List.of("HTTP_STATUS")));

            assertBadRequest(response, "observability.query.metrics_required");
        }

        @ParameterizedTest
        @ValueSource(strings = { "filters", "metrics", "sorts", "ranges" })
        void should_answer_400_rather_than_500_when_an_array_holds_a_null(String field) {
            var metric = new HashMap<String, Object>(Map.of("name", "HTTP_REQUESTS", "measures", List.of("COUNT")));
            var body = new HashMap<String, Object>(
                Map.of("timeRange", TIME_RANGE, "metrics", List.of(metric), "by", List.of("HTTP_STATUS"))
            );
            switch (field) {
                case "sorts" -> metric.put("sorts", Arrays.asList((Object) null));
                case "metrics" -> body.put("metrics", Arrays.asList(metric, null));
                default -> body.put(field, Arrays.asList((Object) null));
            }

            var response = post("facets", body);

            assertBadRequest(response, "observability.query.null_entry");
        }

        @Test
        void should_answer_400_rather_than_500_when_a_time_series_has_no_interval() {
            var response = post("time-series", Map.of("timeRange", TIME_RANGE, "metrics", REQUEST_COUNT));

            assertBadRequest(response, "observability.query.invalid_interval");
        }
    }

    @Nested
    class FilterContract {

        @Test
        void should_answer_400_when_eq_carries_several_values() {
            var response = post("measures", measures(List.of(filter("HTTP_METHOD", "EQ", List.of("GET", "POST")))));

            assertBadRequest(response, "observability.filter.invalid_arity");
        }

        @Test
        void should_answer_400_when_a_value_array_holds_a_null() {
            var response = post("measures", measures(List.of(filter("HTTP_METHOD", "IN", Arrays.asList("GET", null)))));

            assertBadRequest(response, "observability.filter.invalid_value_shape");
        }

        @Test
        void should_answer_400_when_a_filter_is_repeated() {
            var response = post(
                "measures",
                measures(List.of(filter("HTTP_METHOD", "IN", List.of("GET")), filter("HTTP_METHOD", "IN", List.of("POST"))))
            );

            assertBadRequest(response, "observability.filter.repeated");
        }

        @Test
        void should_answer_200_to_a_closed_numeric_range() {
            var response = post("measures", measures(List.of(filter("HTTP_STATUS", "GTE", 400), filter("HTTP_STATUS", "LTE", 499))));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_hand_a_metric_level_condition_over_as_not_applied() {
            var metric = Map.of("name", "HTTP_REQUESTS", "measures", List.of("COUNT"), "filters", List.of(filter("PLAN", "EQ", "plan-1")));

            var response = post("measures", Map.of("timeRange", TIME_RANGE, "metrics", List.of(metric)));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.MeasuresQuery.class);
            verify(analyticsDataPort).computeMeasures(captor.capture());
            assertThat(captor.getValue().conditionsNotApplied()).isEqualTo(Map.of("HTTP_REQUESTS", List.of("PLAN")));
        }

        @Test
        void should_answer_400_to_a_metric_level_condition_the_catalog_refuses() {
            var metric = Map.of(
                "name",
                "HTTP_REQUESTS",
                "measures",
                List.of("COUNT"),
                "filters",
                List.of(filter("HTTP_METHOD", "EQ", List.of("GET", "POST")))
            );

            var response = post("measures", Map.of("timeRange", TIME_RANGE, "metrics", List.of(metric)));

            assertBadRequest(response, "observability.filter.invalid_arity");
        }

        private static Map<String, Object> measures(List<Map<String, Object>> filters) {
            return Map.of("timeRange", TIME_RANGE, "metrics", REQUEST_COUNT, "filters", filters);
        }

        private static Map<String, Object> filter(String name, String operator, Object value) {
            var filter = new HashMap<String, Object>();
            filter.put("name", name);
            filter.put("operator", operator);
            filter.put("value", value);
            return filter;
        }
    }

    private Response post(String path, Map<String, ?> body) {
        return rootTarget(path).request().post(Entity.entity(body, MediaType.APPLICATION_JSON_TYPE));
    }

    private void assertBadRequest(Response response, String technicalCode) {
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        assertThat(response.readEntity(JsonNode.class).get("technicalCode").asText()).isEqualTo(technicalCode);
        verifyNoInteractions(analyticsDataPort);
    }

    @Configuration
    static class AnalyticsTestConfiguration {

        @Bean
        ObservabilityAnalyticsDataPort observabilityAnalyticsDataPort() {
            return mock(ObservabilityAnalyticsDataPort.class);
        }

        @Bean
        AnalyticsRequestPipeline analyticsRequestPipeline() {
            return new AnalyticsRequestPipeline(
                new ObservabilityFilterValidator(new SpiFilterRegistry()),
                new AccessibleApiScopeDomainService()
            );
        }

        @Bean
        ComputeObservabilityMeasuresUseCase computeObservabilityMeasuresUseCase(
            ObservabilityAnalyticsDataPort port,
            AnalyticsRequestPipeline pipeline
        ) {
            return new ComputeObservabilityMeasuresUseCase(port, pipeline);
        }

        @Bean
        ComputeObservabilityFacetsUseCase computeObservabilityFacetsUseCase(
            ObservabilityAnalyticsDataPort port,
            AnalyticsRequestPipeline pipeline
        ) {
            return new ComputeObservabilityFacetsUseCase(port, pipeline);
        }

        @Bean
        ComputeObservabilityTimeSeriesUseCase computeObservabilityTimeSeriesUseCase(
            ObservabilityAnalyticsDataPort port,
            AnalyticsRequestPipeline pipeline
        ) {
            return new ComputeObservabilityTimeSeriesUseCase(port, pipeline);
        }
    }
}
