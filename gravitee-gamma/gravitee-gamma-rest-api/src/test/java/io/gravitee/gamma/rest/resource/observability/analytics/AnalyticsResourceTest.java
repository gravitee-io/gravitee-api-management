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
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import io.gravitee.gamma.rest.infra.adapter.EntrypointScopeProviderAdapter;
import io.gravitee.gamma.rest.infra.adapter.SpiFilterRegistry;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsResourceTest.AnalyticsTestConfiguration;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
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
    private static final String OTHER_ENVIRONMENT = "other-env";
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
        when(analyticsDataPort.loadAccessibleApis(any(), any(), any())).thenReturn(
            List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY))
        );
        when(analyticsDataPort.computeMeasures(any())).thenReturn(JsonNodeFactory.instance.objectNode());
        when(analyticsDataPort.computeFacets(any())).thenReturn(JsonNodeFactory.instance.objectNode());
        when(analyticsDataPort.computeTimeSeries(any())).thenReturn(JsonNodeFactory.instance.objectNode());
        when(analyticsDataPort.emptyTimeSeriesResponse()).thenReturn(JsonNodeFactory.instance.objectNode());
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

        // A widget's own filters are appended to the dashboard's: the conditions are ANDed, not refused.
        @Test
        void should_hand_a_repeated_filter_over_to_be_anded() {
            var response = post(
                "measures",
                measures(List.of(filter("HTTP_METHOD", "IN", List.of("GET")), filter("HTTP_METHOD", "IN", List.of("POST"))))
            );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.MeasuresQuery.class);
            verify(analyticsDataPort).computeMeasures(captor.capture());
            assertThat(captor.getValue().groups().getFirst().filters())
                .filteredOn(c -> "HTTP_METHOD".equals(c.name()))
                .hasSize(2);
        }

        @Test
        void should_answer_200_to_a_closed_numeric_range() {
            var response = post("measures", measures(List.of(filter("HTTP_STATUS", "GTE", 400), filter("HTTP_STATUS", "LTE", 499))));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_apply_a_metric_level_condition() {
            var metric = Map.of("name", "HTTP_REQUESTS", "measures", List.of("COUNT"), "filters", List.of(filter("PLAN", "EQ", "plan-1")));

            var response = post("measures", Map.of("timeRange", TIME_RANGE, "metrics", List.of(metric)));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.MeasuresQuery.class);
            verify(analyticsDataPort).computeMeasures(captor.capture());
            assertThat(captor.getValue().groups().getFirst().filters()).anyMatch(c -> "PLAN".equals(c.name()));
            assertThat(captor.getValue().metrics().getFirst().conditionsNotApplied()).isEmpty();
        }

        @Test
        void should_hand_the_metric_ids_over() {
            var total = Map.of("id", "total", "name", "HTTP_REQUESTS", "measures", List.of("COUNT"));
            var errors = Map.of(
                "id",
                "errors",
                "name",
                "HTTP_REQUESTS",
                "measures",
                List.of("COUNT"),
                "filters",
                List.of(filter("HTTP_STATUS", "GTE", 500))
            );

            var response = post("measures", Map.of("timeRange", TIME_RANGE, "metrics", List.of(total, errors)));

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.MeasuresQuery.class);
            verify(analyticsDataPort).computeMeasures(captor.capture());
            assertThat(captor.getValue().metrics())
                .extracting(m -> m.query().id())
                .containsExactly("total", "errors");
            assertThat(captor.getValue().groups()).hasSize(2);
        }

        @Test
        void should_answer_400_to_ids_on_some_metrics_only() {
            var total = Map.of("id", "total", "name", "HTTP_REQUESTS", "measures", List.of("COUNT"));
            var errors = Map.of("name", "HTTP_ERRORS", "measures", List.of("COUNT"));

            var response = post("measures", Map.of("timeRange", TIME_RANGE, "metrics", List.of(total, errors)));

            assertBadRequest(response, "observability.query.partial_metric_ids");
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

    @Nested
    class StrictBody {

        private static final Map<String, Object> LIBRARY_TIME_RANGE = Map.of(
            "from",
            "2026-10-11T12:00:00.000Z",
            "to",
            "2026-10-12T12:00:00.000Z"
        );
        private static final List<Map<String, Object>> LIBRARY_FILTERS = List.of(
            Map.of("name", "API", "operator", "IN", "value", List.of("api-1"))
        );
        private static final List<Map<String, Object>> LIBRARY_RANGES = List.of(
            Map.of("from", 100, "to", 400),
            Map.of("from", 400, "to", 600)
        );

        @Test
        void should_answer_200_to_the_library_measures_payload() {
            var response = post(
                "measures",
                Map.of(
                    "timeRange",
                    LIBRARY_TIME_RANGE,
                    "filters",
                    LIBRARY_FILTERS,
                    "metrics",
                    List.of(
                        Map.of(
                            "name",
                            "HTTP_REQUESTS",
                            "measures",
                            List.of("COUNT"),
                            "filters",
                            List.of(Map.of("name", "HTTP_STATUS", "operator", "GTE", "value", 500))
                        ),
                        Map.of("name", "HTTP_GATEWAY_RESPONSE_TIME", "measures", List.of("AVG", "P90"))
                    )
                )
            );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_answer_200_to_the_library_facets_payload() {
            var response = post(
                "facets",
                Map.of(
                    "timeRange",
                    LIBRARY_TIME_RANGE,
                    "filters",
                    LIBRARY_FILTERS,
                    "by",
                    List.of("HTTP_STATUS"),
                    "metrics",
                    List.of(librarySortedMetric()),
                    "ranges",
                    LIBRARY_RANGES,
                    "limit",
                    10
                )
            );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @Test
        void should_answer_200_to_the_library_time_series_payload() {
            var response = post(
                "time-series",
                Map.of(
                    "timeRange",
                    LIBRARY_TIME_RANGE,
                    "filters",
                    LIBRARY_FILTERS,
                    "interval",
                    3_600_000,
                    "metrics",
                    List.of(librarySortedMetric()),
                    "by",
                    List.of("API"),
                    "ranges",
                    LIBRARY_RANGES,
                    "limit",
                    5
                )
            );

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        @ParameterizedTest
        @ValueSource(strings = { "measures", "facets", "time-series" })
        void should_answer_400_naming_an_unknown_top_level_key(String path) {
            var response = post(path, Map.of("timeRange", TIME_RANGE, "metrics", REQUEST_COUNT, "apiId", "api-1"));

            assertUnknownKey(response, "apiId");
        }

        @ParameterizedTest
        @CsvSource({ "timeRange, form", "metrics, measure", "sorts, direction", "ranges, min", "metricFilters, values" })
        void should_answer_400_naming_an_unknown_nested_key(String location, String unknownKey) {
            var metric = new HashMap<String, Object>(Map.of("name", "HTTP_REQUESTS", "measures", List.of("COUNT")));
            var body = new HashMap<String, Object>(
                Map.of("timeRange", TIME_RANGE, "interval", 3_600_000, "metrics", List.of(metric), "by", List.of("HTTP_STATUS"))
            );
            switch (location) {
                case "timeRange" -> body.put("timeRange", Map.of(unknownKey, "2026-06-10T00:00:00Z", "to", "2026-06-11T00:00:00Z"));
                case "metrics" -> metric.put(unknownKey, "COUNT");
                case "sorts" -> metric.put("sorts", List.of(Map.of("measure", "COUNT", unknownKey, "DESC")));
                case "ranges" -> body.put("ranges", List.of(Map.of(unknownKey, 100, "to", 400)));
                default -> metric.put("filters", List.of(Map.of("name", "HTTP_METHOD", "operator", "IN", unknownKey, List.of("GET"))));
            }

            var response = post("time-series", body);

            assertUnknownKey(response, unknownKey);
        }

        private static Map<String, Object> librarySortedMetric() {
            return Map.of(
                "name",
                "HTTP_REQUESTS",
                "measures",
                List.of("COUNT"),
                "filters",
                List.of(Map.of("name", "HTTP_METHOD", "operator", "IN", "value", List.of("GET", "POST"))),
                "sorts",
                List.of(Map.of("measure", "COUNT", "order", "DESC"))
            );
        }

        private void assertUnknownKey(Response response, String unknownKey) {
            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
            assertThat(response.readEntity(JsonNode.class).get("message").asText()).isEqualTo(
                "Property [" + unknownKey + "] is not recognized as a valid property"
            );
            verifyNoInteractions(analyticsDataPort);
        }
    }

    /**
     * The rows a caller reads decide what analytics count, never an environment permission: a caller whose rows are
     * empty (no permission at all, ENVIRONMENT_API[READ] only, or the platform permission of another environment) gets
     * an empty answer, and no API reaches the engine.
     */
    @Nested
    class RowRule {

        @ParameterizedTest
        @MethodSource("io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsResourceTest#wellFormedRequests")
        void should_answer_without_querying_any_api_when_caller_holds_no_permission(String path, Map<String, Object> body) {
            when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(false);
            givenTheCallerReadsNoApi();

            var response = post(path, body);

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertNoApiQueried(path);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsResourceTest#wellFormedRequests")
        void should_answer_without_querying_any_api_when_caller_only_holds_environment_api_read(String path, Map<String, Object> body) {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_API, ENVIRONMENT);
            givenTheCallerReadsNoApi();

            var response = post(path, body);

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertNoApiQueried(path);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsResourceTest#wellFormedRequests")
        void should_answer_without_querying_any_api_when_environment_platform_read_is_held_on_another_environment(
            String path,
            Map<String, Object> body
        ) {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_PLATFORM, OTHER_ENVIRONMENT);
            givenTheCallerReadsNoApi();

            var response = post(path, body);

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
            assertNoApiQueried(path);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.gamma.rest.resource.observability.analytics.AnalyticsResourceTest#wellFormedRequests")
        void should_answer_200_when_caller_holds_environment_platform_read(String path, Map<String, Object> body) {
            givenOnlyReadPermission(RolePermission.ENVIRONMENT_PLATFORM, ENVIRONMENT);

            var response = post(path, body);

            assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        }

        private void givenTheCallerReadsNoApi() {
            when(analyticsDataPort.loadAccessibleApis(any(), any(), any())).thenReturn(List.of());
            when(analyticsDataPort.emptyFacetsResponse()).thenReturn(JsonNodeFactory.instance.objectNode());
        }

        private void assertNoApiQueried(String path) {
            switch (path) {
                case "measures" -> {
                    var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.MeasuresQuery.class);
                    verify(analyticsDataPort).computeMeasures(captor.capture());
                    assertThat(captor.getValue().groups()).allSatisfy(group -> assertThat(group.isEmpty()).isTrue());
                }
                case "facets" -> verify(analyticsDataPort, never()).computeFacets(any());
                default -> verify(analyticsDataPort, never()).computeTimeSeries(any());
            }
        }
    }

    static Stream<Arguments> wellFormedRequests() {
        return Stream.of(
            arguments("measures", Map.of("timeRange", TIME_RANGE, "metrics", REQUEST_COUNT)),
            arguments("facets", Map.of("timeRange", TIME_RANGE, "metrics", REQUEST_COUNT, "by", List.of("HTTP_STATUS"))),
            arguments(
                "time-series",
                Map.of("timeRange", TIME_RANGE, "interval", 3_600_000, "metrics", REQUEST_COUNT, "by", List.of("HTTP_STATUS"))
            )
        );
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
                new AccessibleApiScopeDomainService(),
                new EntrypointScopeProviderAdapter()
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
