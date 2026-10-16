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
package io.gravitee.gamma.rest.core.observability.analytics.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.DataKind;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.ObservabilityAnalyticsDataPort.MeasuresQuery;
import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import io.gravitee.gamma.rest.core.observability.filter.domain_service.ObservabilityFilterValidator;
import io.gravitee.gamma.rest.core.observability.filter.exception.UnsupportedObservabilityFilterException;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterCondition;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterOperator;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterSpec;
import io.gravitee.gamma.rest.core.observability.filter.model.FilterType;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;
import io.gravitee.gamma.rest.core.observability.filter.port.service_provider.FilterRegistry;
import io.gravitee.gamma.rest.core.observability.logs.domain_service.AccessibleApiScopeDomainService;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import io.gravitee.gamma.rest.infra.adapter.EntrypointScopeProviderAdapter;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ComputeObservabilityMeasuresUseCaseTest {

    private static final String ORG_ID = "org-1";
    private static final String ENV_ID = "env-1";
    private static final Instant FROM = Instant.parse("2026-06-10T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-06-11T00:00:00Z");
    private static final FilterCondition STATUS_200 = new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("200"));
    private static final FilterCondition STATUS_500 = new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of("500"));
    private static final FilterCondition SERVER_ERRORS = new FilterCondition("HTTP_STATUS", FilterOperator.GTE, List.of("500"));

    @Mock
    private ObservabilityAnalyticsDataPort analyticsDataPort;

    @Mock
    private FilterRegistry filterRegistry;

    private ComputeObservabilityMeasuresUseCase useCase;

    @BeforeEach
    void setUp() {
        var accessibleApiScope = new AccessibleApiScopeDomainService();
        var filterValidator = new ObservabilityFilterValidator(filterRegistry);
        var pipeline = new AnalyticsRequestPipeline(filterValidator, accessibleApiScope, new EntrypointScopeProviderAdapter());
        useCase = new ComputeObservabilityMeasuresUseCase(analyticsDataPort, pipeline);

        lenient()
            .when(filterRegistry.getFilters(any(), any()))
            .thenReturn(
                List.of(
                    new FilterSpec(
                        "API",
                        "API",
                        FilterType.KEYWORD,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        null,
                        null,
                        Set.of(Signal.LOGS, Signal.ANALYTICS),
                        ApiType.ALL
                    ),
                    new FilterSpec(
                        "HTTP_STATUS",
                        "Status Code",
                        FilterType.NUMBER,
                        List.of(FilterOperator.EQ, FilterOperator.GTE, FilterOperator.LTE),
                        null,
                        new FilterSpec.Range(100, 599),
                        Set.of(Signal.LOGS, Signal.ANALYTICS),
                        Set.of(ApiType.HTTP_PROXY)
                    ),
                    new FilterSpec(
                        "CONSUMER_IP",
                        "Consumer IP",
                        FilterType.KEYWORD,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        null,
                        null,
                        Set.of(Signal.ANALYTICS),
                        Set.of(ApiType.HTTP_PROXY)
                    ),
                    new FilterSpec(
                        "RECORD_TYPE",
                        "Record Type",
                        FilterType.ENUM,
                        List.of(FilterOperator.EQ, FilterOperator.IN),
                        List.of(
                            new FilterSpec.EnumValue("REQUEST", "Request"),
                            new FilterSpec.EnumValue("AUTHZ_DECISION", "Authz decision")
                        ),
                        null,
                        Set.of(Signal.LOGS, Signal.ANALYTICS),
                        ApiType.API_KINDS
                    )
                )
            );
        lenient()
            .when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES))
            .thenReturn(
                List.of(new AccessibleApi("api-1", "API 1", ApiType.HTTP_PROXY), new AccessibleApi("api-2", "API 2", ApiType.HTTP_PROXY))
            );
    }

    /** A metric condition identifying callers makes the whole request per-request data. */
    @Test
    void should_count_the_apis_whose_requests_the_caller_reads_when_a_metric_condition_identifies_callers() {
        when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.PER_REQUEST)).thenReturn(
            List.of(new AccessibleApi("logged-api", "Logged API", ApiType.HTTP_PROXY))
        );
        when(analyticsDataPort.computeMeasures(any())).thenReturn(JsonNodeFactory.instance.objectNode());
        var byConsumerIp = new FilterCondition("CONSUMER_IP", FilterOperator.EQ, List.of("10.0.0.1"));

        useCase.execute(
            new ComputeObservabilityMeasuresUseCase.Input(
                ORG_ID,
                ENV_ID,
                List.of(),
                FROM,
                TO,
                List.of(new AnalyticsMetricQuery(null, "HTTP_REQUESTS", List.of("COUNT"), List.of(byConsumerIp)))
            )
        );

        var captor = ArgumentCaptor.forClass(ObservabilityAnalyticsDataPort.MeasuresQuery.class);
        verify(analyticsDataPort).computeMeasures(captor.capture());
        assertThat(captor.getValue().groups()).allSatisfy(group -> assertThat(group.apiIds()).containsExactly("logged-api"));
    }

    @Nested
    class HappyPath {

        @Test
        void should_delegate_to_port_with_prepared_scope_and_metrics() {
            JsonNode fakeResponse = JsonNodeFactory.instance.objectNode().put("metrics", "ok");
            when(analyticsDataPort.computeMeasures(any())).thenReturn(fakeResponse);

            var output = execute(List.of(), metric("HTTP_REQUESTS"));

            assertThat(output.response()).isEqualTo(fakeResponse);
            var query = computedQuery();
            assertThat(query.metrics())
                .extracting(m -> m.query().metricName())
                .containsExactly("HTTP_REQUESTS");
            assertThat(query.groups())
                .singleElement()
                .satisfies(group -> assertThat(group.apiIds()).containsExactlyInAnyOrder("api-1", "api-2"));
            assertThat(query.identified()).isFalse();
        }
    }

    @Nested
    class MetricConditionsApplied {

        @Test
        void should_run_one_query_per_distinct_condition_set() {
            execute(List.of(), metric("total", "HTTP_REQUESTS"), metric("errors", "HTTP_REQUESTS", STATUS_500));

            var query = computedQuery();
            assertThat(query.groups()).hasSize(2);
            assertThat(query.groups().get(0).filters()).noneMatch(condition -> "HTTP_STATUS".equals(condition.name()));
            assertThat(query.groups().get(1).filters()).contains(STATUS_500);
            assertThat(query.metrics()).extracting(MeasuresQuery.Metric::group).containsExactly(0, 1);
            assertThat(query.metrics()).allSatisfy(metric -> assertThat(metric.conditionsNotApplied()).isEmpty());
        }

        @Test
        void should_and_a_metric_condition_with_the_request_condition_on_the_same_field() {
            execute(List.of(STATUS_200), metric("errors", "HTTP_REQUESTS", STATUS_500));

            assertThat(computedQuery().groups())
                .singleElement()
                .satisfies(group -> assertThat(group.filters()).containsSubsequence(STATUS_200, STATUS_500));
        }

        @Test
        void should_and_two_request_conditions_on_the_same_field() {
            execute(List.of(STATUS_200, STATUS_500), metric("HTTP_REQUESTS"));

            assertThat(computedQuery().groups())
                .singleElement()
                .satisfies(group -> assertThat(group.filters()).containsSubsequence(STATUS_200, STATUS_500));
        }

        @Test
        void should_share_one_query_between_metrics_carrying_the_same_conditions_in_any_order() {
            execute(
                List.of(),
                metric("a", "HTTP_REQUESTS", STATUS_500, SERVER_ERRORS),
                metric("b", "HTTP_ERRORS", SERVER_ERRORS, STATUS_500)
            );

            var query = computedQuery();
            assertThat(query.groups()).hasSize(1);
            assertThat(query.metrics()).extracting(MeasuresQuery.Metric::group).containsExactly(0, 0);
        }

        @Test
        void should_share_one_query_between_in_conditions_listing_their_values_in_another_order() {
            execute(
                List.of(),
                metric("a", "HTTP_REQUESTS", new FilterCondition("API", FilterOperator.IN, List.of("api-1", "api-2"))),
                metric("b", "HTTP_ERRORS", new FilterCondition("API", FilterOperator.IN, List.of("api-2", "api-1")))
            );

            assertThat(computedQuery().groups()).hasSize(1);
        }

        // A library already deployed sends metric conditions without ids: on distinct metrics they now apply.
        @Test
        void should_apply_the_conditions_of_distinct_metrics_without_ids() {
            execute(List.of(), metric("HTTP_REQUESTS", STATUS_500), metric("HTTP_ERRORS"));

            var query = computedQuery();
            assertThat(query.identified()).isFalse();
            assertThat(query.groups()).hasSize(2);
            assertThat(query.groups().get(0).filters()).contains(STATUS_500);
            assertThat(query.metrics()).allSatisfy(metric -> assertThat(metric.conditionsNotApplied()).isEmpty());
        }

        // Answered by name, the two would share one entry: the 5xx count must never show on the total.
        @Test
        void should_report_without_applying_the_conditions_of_a_metric_queried_twice_without_ids() {
            execute(List.of(), metric("HTTP_REQUESTS"), metric("HTTP_REQUESTS", STATUS_500, SERVER_ERRORS));

            var query = computedQuery();
            assertThat(query.groups())
                .singleElement()
                .satisfies(group -> assertThat(group.filters()).noneMatch(condition -> "HTTP_STATUS".equals(condition.name())));
            assertThat(query.metrics())
                .extracting(MeasuresQuery.Metric::conditionsNotApplied)
                .containsExactly(List.of(), List.of("HTTP_STATUS"));
        }

        @Test
        void should_apply_the_conditions_of_a_metric_queried_twice_with_ids() {
            execute(List.of(), metric("total", "HTTP_REQUESTS"), metric("errors", "HTTP_REQUESTS", STATUS_500));

            assertThat(computedQuery().metrics()).allSatisfy(metric -> assertThat(metric.conditionsNotApplied()).isEmpty());
        }

        @Test
        void should_accept_ten_condition_sets_with_ids() {
            execute(List.of(), metricsWithDistinctConditions(10, true));

            assertThat(computedQuery().groups()).hasSize(10);
        }

        @Test
        void should_refuse_more_than_ten_condition_sets_with_ids_before_any_data_access() {
            assertThatThrownBy(() -> execute(List.of(), metricsWithDistinctConditions(11, true)))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.too_many_metric_condition_sets");
            verifyNoInteractions(analyticsDataPort);
        }

        @Test
        void should_report_without_applying_more_than_ten_condition_sets_without_ids() {
            execute(List.of(), metricsWithDistinctConditions(11, false));

            var query = computedQuery();
            assertThat(query.groups()).hasSize(1);
            assertThat(query.metrics()).allSatisfy(metric -> assertThat(metric.conditionsNotApplied()).containsExactly("HTTP_STATUS"));
        }

        @Test
        void should_report_a_record_type_that_does_not_select_the_metric_before_its_own_conditions() {
            var decisions = new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("AUTHZ_DECISION"));

            execute(List.of(decisions), metric("HTTP_REQUESTS"), metric("HTTP_REQUESTS", STATUS_500), metric("AUTHZ_DECISIONS"));

            assertThat(computedQuery().metrics())
                .extracting(MeasuresQuery.Metric::conditionsNotApplied)
                .containsExactly(List.of("RECORD_TYPE"), List.of("RECORD_TYPE", "HTTP_STATUS"), List.of());
        }

        @Test
        void should_refuse_a_record_type_at_metric_level_before_any_data_access() {
            var decisions = new FilterCondition("RECORD_TYPE", FilterOperator.EQ, List.of("AUTHZ_DECISION"));

            assertThatThrownBy(() -> execute(List.of(), metric("HTTP_REQUESTS", decisions)))
                .isInstanceOf(UnsupportedObservabilityFilterException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.filter.record_type_on_metric");
            verifyNoInteractions(analyticsDataPort);
        }

        private AnalyticsMetricQuery[] metricsWithDistinctConditions(int count, boolean withIds) {
            return IntStream.range(0, count)
                .mapToObj(i ->
                    new AnalyticsMetricQuery(
                        withIds ? "m" + i : null,
                        "METRIC_" + i,
                        List.of("COUNT"),
                        List.of(new FilterCondition("HTTP_STATUS", FilterOperator.EQ, List.of(String.valueOf(200 + i))))
                    )
                )
                .toArray(AnalyticsMetricQuery[]::new);
        }
    }

    @Nested
    class MetricIds {

        @Test
        void should_mark_the_query_identified_when_every_metric_carries_an_id() {
            execute(List.of(), metric("total", "HTTP_REQUESTS"), metric("errors", "HTTP_ERRORS"));

            assertThat(computedQuery().identified()).isTrue();
        }

        @Test
        void should_refuse_ids_on_some_metrics_only_before_any_data_access() {
            assertThatThrownBy(() -> execute(List.of(), metric("total", "HTTP_REQUESTS"), metric("HTTP_ERRORS")))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.partial_metric_ids");
            verifyNoInteractions(analyticsDataPort);
        }

        @Test
        void should_refuse_an_id_used_twice_before_any_data_access() {
            assertThatThrownBy(() -> execute(List.of(), metric("tile", "HTTP_REQUESTS"), metric("tile", "HTTP_ERRORS")))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .hasMessageContaining("tile")
                .extracting("technicalCode")
                .isEqualTo("observability.query.duplicate_metric_id");
            verifyNoInteractions(analyticsDataPort);
        }
    }

    @Nested
    class FilterValidation {

        @Test
        void should_reject_unknown_filter() {
            var filters = List.of(new FilterCondition("UNKNOWN", FilterOperator.EQ, List.of("val")));

            assertThatThrownBy(() -> execute(filters, metric("HTTP_REQUESTS"))).isInstanceOf(UnsupportedObservabilityFilterException.class);
        }

        @Test
        void should_reject_an_unknown_metric_level_filter_before_any_data_access() {
            var unknown = new FilterCondition("UNKNOWN", FilterOperator.EQ, List.of("val"));

            assertThatThrownBy(() -> execute(List.of(), metric("HTTP_REQUESTS", unknown))).isInstanceOf(
                UnsupportedObservabilityFilterException.class
            );
            verifyNoInteractions(analyticsDataPort);
        }
    }

    @Nested
    class QueryValidation {

        @Test
        void should_reject_a_request_without_metrics_before_loading_the_accessible_apis() {
            var input = new ComputeObservabilityMeasuresUseCase.Input(ORG_ID, ENV_ID, List.of(), FROM, TO, List.of());

            assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.metrics_required");
            verifyNoInteractions(analyticsDataPort);
        }

        // The engine validation names the missing metric; grouping the metrics must not fail on it first.
        @Test
        void should_leave_a_metric_without_a_name_to_the_engine_validation() {
            doThrow(InvalidObservabilityQueryException.unknownMetric(null)).when(analyticsDataPort).validate(any(MeasuresQuery.class));

            assertThatThrownBy(() -> execute(List.of(), metric(null), metric(null, STATUS_500)))
                .isInstanceOf(InvalidObservabilityQueryException.class)
                .extracting("technicalCode")
                .isEqualTo("observability.query.unknown_metric");
        }

        // The answer to a malformed query must not depend on what the caller can read.
        @Test
        void should_refuse_a_malformed_query_even_when_the_caller_can_read_no_api() {
            when(analyticsDataPort.loadAccessibleApis(ORG_ID, ENV_ID, DataKind.AGGREGATES)).thenReturn(List.of());
            doThrow(InvalidObservabilityQueryException.unknownMetric("BOGUS")).when(analyticsDataPort).validate(any(MeasuresQuery.class));

            assertThatThrownBy(() -> execute(List.of(), metric("BOGUS"))).isInstanceOf(InvalidObservabilityQueryException.class);
            verify(analyticsDataPort, never()).computeMeasures(any());
        }
    }

    @Nested
    class RBACScoping {

        @Test
        void should_intersect_user_api_filter_with_accessible_scope() {
            execute(List.of(new FilterCondition("API", FilterOperator.IN, List.of("api-1"))), metric("HTTP_REQUESTS"));

            assertThat(computedQuery().groups())
                .singleElement()
                .satisfies(group -> assertThat(group.apiIds()).containsExactly("api-1"));
        }

        @Test
        void should_hand_an_empty_group_to_the_port_when_user_filters_unknown_api() {
            execute(List.of(new FilterCondition("API", FilterOperator.IN, List.of("non-existent-api"))), metric("HTTP_REQUESTS"));

            assertThat(computedQuery().groups())
                .singleElement()
                .satisfies(group -> assertThat(group.isEmpty()).isTrue());
        }

        @Test
        void should_hand_an_empty_group_to_the_port_for_disjoint_api_conditions() {
            var filters = List.of(
                new FilterCondition("API", FilterOperator.EQ, List.of("api-1")),
                new FilterCondition("API", FilterOperator.EQ, List.of("api-2"))
            );

            execute(filters, metric("HTTP_REQUESTS"));

            assertThat(computedQuery().groups())
                .singleElement()
                .satisfies(group -> assertThat(group.isEmpty()).isTrue());
        }

        @Test
        void should_empty_only_the_group_whose_api_condition_is_disjoint_from_the_request_one() {
            execute(
                List.of(new FilterCondition("API", FilterOperator.EQ, List.of("api-1"))),
                metric("all", "HTTP_REQUESTS"),
                metric("other", "HTTP_REQUESTS", new FilterCondition("API", FilterOperator.EQ, List.of("api-2")))
            );

            var groups = computedQuery().groups();
            assertThat(groups.get(0).apiIds()).containsExactly("api-1");
            assertThat(groups.get(1).isEmpty()).isTrue();
        }
    }

    private ComputeObservabilityMeasuresUseCase.Output execute(List<FilterCondition> filters, AnalyticsMetricQuery... metrics) {
        return useCase.execute(new ComputeObservabilityMeasuresUseCase.Input(ORG_ID, ENV_ID, filters, FROM, TO, List.of(metrics)));
    }

    private MeasuresQuery computedQuery() {
        var captor = ArgumentCaptor.forClass(MeasuresQuery.class);
        verify(analyticsDataPort).computeMeasures(captor.capture());
        return captor.getValue();
    }

    private static AnalyticsMetricQuery metric(String name, FilterCondition... conditions) {
        return new AnalyticsMetricQuery(null, name, List.of("COUNT"), List.of(conditions));
    }

    private static AnalyticsMetricQuery metric(String id, String name, FilterCondition... conditions) {
        return new AnalyticsMetricQuery(id, name, List.of("COUNT"), List.of(conditions));
    }
}
