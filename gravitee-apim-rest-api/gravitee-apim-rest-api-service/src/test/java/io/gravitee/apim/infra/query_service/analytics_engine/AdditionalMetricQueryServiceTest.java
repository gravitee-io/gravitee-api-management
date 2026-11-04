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
package io.gravitee.apim.infra.query_service.analytics_engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.AuditInfoFixtures;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoaderResolver;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.exception.InvalidQueryException;
import io.gravitee.apim.core.analytics_engine.exception.UnsupportedMetricException;
import io.gravitee.apim.core.analytics_engine.model.AdditionalMetricName;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetName;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.observability.model.AdditionalMetricField;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.infra.adapter.AnalyticsMeasuresAdapter;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import io.gravitee.apim.infra.domain_service.analytics_engine.processors.UnitEnrichmentPostProcessorImpl;
import io.gravitee.repository.analytics.engine.api.metric.AdditionalMetric;
import io.gravitee.repository.analytics.engine.api.metric.Measure;
import io.gravitee.repository.analytics.engine.api.query.AnalyticsSearchPath;
import io.gravitee.repository.analytics.engine.api.query.FilterOutcome;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricMeasuresResult;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * An additional metric field reaches the engine through the request documents only: the HTTP and Edge metrics group by it,
 * every other service refuses to, a condition on it is reported as skipped where the path skips it, and a field holding
 * a number is measured by the HTTP data plane alone.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AdditionalMetricQueryServiceTest {

    private static final TimeRange TIME_RANGE = new TimeRange(Instant.EPOCH, Instant.EPOCH.plusSeconds(60));
    private static final AdditionalMetricName TIER = new AdditionalMetricName(
        new AdditionalMetricField(AdditionalMetricField.ValueType.KEYWORD, "keyword_fraud-check_tier")
    );

    private final AnalyticsRepository repository = mock(AnalyticsRepository.class);

    @Nested
    class Grouping {

        private final AnalyticsQueryContextProvider provider = new AnalyticsQueryContextProvider(
            List.of(
                new HTTPDataPlaneAnalyticsQueryService(repository),
                new NativeApiAnalyticsQueryService(repository),
                new MessageDataPlaneQueryService(repository),
                new AuthzTrafficAnalyticsQueryService(repository)
            )
        );

        @Test
        void should_group_an_http_metric_by_an_additional_metric_field() {
            assertThatCode(() ->
                provider.resolve(facets(MetricSpec.Name.HTTP_REQUESTS, List.of(FacetSpec.Name.API, TIER)))
            ).doesNotThrowAnyException();
            assertThatCode(() ->
                provider.resolve(timeSeries(MetricSpec.Name.LLM_PROMPT_TOKEN_SENT, List.of(TIER)))
            ).doesNotThrowAnyException();
            assertThatCode(() -> provider.resolve(facets(MetricSpec.Name.EDGE_DETECTION_COUNT, List.of(TIER)))).doesNotThrowAnyException();
        }

        @Test
        void should_refuse_to_group_a_metric_read_from_other_documents_naming_the_field() {
            var metrics = List.of(MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY, MetricSpec.Name.MESSAGES, MetricSpec.Name.AUTHZ_OPERATIONS);

            assertThat(metrics).allSatisfy(metric -> {
                assertThatThrownBy(() -> provider.resolve(facets(metric, List.of(TIER))))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessage("Facet additional-metrics.keyword_fraud-check_tier is not supported for metric " + metric.name());
                assertThatThrownBy(() -> provider.resolve(timeSeries(metric, List.of(TIER))))
                    .isInstanceOf(InvalidQueryException.class)
                    .hasMessage("Facet additional-metrics.keyword_fraud-check_tier is not supported for metric " + metric.name());
            });
        }

        @Test
        void should_route_built_in_facets_as_before() {
            assertThatCode(() ->
                provider.resolve(facets(MetricSpec.Name.NATIVE_CONNECTIONS_SUMMARY, List.of(FacetSpec.Name.API)))
            ).doesNotThrowAnyException();
        }

        private static FacetsRequest facets(MetricSpec.Name metric, List<FacetName> facets) {
            return new FacetsRequest(TIME_RANGE, List.of(), List.of(count(metric)), facets, null, List.of());
        }

        private static TimeSeriesRequest timeSeries(MetricSpec.Name metric, List<FacetName> facets) {
            return new TimeSeriesRequest(TIME_RANGE, 1000L, List.of(), List.of(count(metric)), facets, null, List.of());
        }

        private static FacetMetricMeasuresRequest count(MetricSpec.Name metric) {
            return new FacetMetricMeasuresRequest(metric, List.of(MetricSpec.Measure.COUNT), List.of());
        }
    }

    @Nested
    class Measuring {

        private static final AdditionalMetricName FAILOVERS = new AdditionalMetricName(
            new AdditionalMetricField(AdditionalMetricField.ValueType.LONG, "long_failover_count")
        );

        private final HTTPDataPlaneAnalyticsQueryService http = new HTTPDataPlaneAnalyticsQueryService(repository);

        private final List<AnalyticsEngineQueryService> services = List.of(
            http,
            new NativeApiAnalyticsQueryService(repository),
            new MessageDataPlaneQueryService(repository),
            new EventMetricsAnalyticsQueryService(repository),
            new AuthzAnalyticsQueryService(repository),
            new AuthzTrafficAnalyticsQueryService(repository)
        );

        @Test
        void should_measure_a_number_field_on_the_http_data_plane_only() {
            var provider = new AnalyticsQueryContextProvider(services);

            assertThat(provider.resolve(FAILOVERS)).isSameAs(http);
            assertThat(services).filteredOn(AnalyticsEngineQueryService::measuresAdditionalMetrics).containsExactly(http);
        }

        @Test
        void should_group_a_number_field_by_another_field() {
            var provider = new AnalyticsQueryContextProvider(services);
            var request = new FacetsRequest(
                TIME_RANGE,
                List.of(),
                List.of(new FacetMetricMeasuresRequest(FAILOVERS, List.of(MetricSpec.Measure.AVG), List.of())),
                List.of(TIER),
                null,
                List.of()
            );

            assertThat(provider.resolve(request)).containsOnlyKeys(http);
        }

        @Test
        void should_narrow_a_number_field_by_a_condition_as_the_http_path_does() {
            var provider = new AnalyticsQueryContextProvider(services);

            when(repository.filterOutcome(eq(AnalyticsSearchPath.HTTP), any())).thenReturn(FilterOutcome.APPLIED);
            assertThat(provider.narrowsBy(FAILOVERS, FilterSpec.Name.API_PRODUCT)).isTrue();

            when(repository.filterOutcome(eq(AnalyticsSearchPath.HTTP), any())).thenReturn(FilterOutcome.IGNORED);
            assertThat(provider.narrowsBy(FAILOVERS, FilterSpec.Name.API_PRODUCT)).isFalse();
        }

        @Test
        void should_refuse_a_number_field_when_no_service_reads_the_request_documents() {
            var provider = new AnalyticsQueryContextProvider(services.subList(1, services.size()));

            assertThatThrownBy(() -> provider.resolve(FAILOVERS))
                .isInstanceOf(UnsupportedMetricException.class)
                .hasMessage("Metric additional-metrics.long_failover_count is not supported at the moment");
        }

        @Test
        void should_refuse_two_services_measuring_the_fields() {
            var duplicate = new HTTPDataPlaneAnalyticsQueryService(repository);

            assertThatThrownBy(() -> new AnalyticsQueryContextProvider(List.of(http, duplicate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("More than one analytics service measures additional metrics");
        }

        @Test
        void should_send_the_field_to_the_repository_and_read_its_measures_back_under_its_name() {
            var engineFailovers = AnalyticsMeasuresAdapter.INSTANCE.toMetricName(FAILOVERS);
            var measures = new HashMap<Measure, Number>();
            measures.put(Measure.AVG, null);
            measures.put(Measure.COUNT, 0);
            when(repository.searchHTTPMeasures(any(), any())).thenReturn(
                new MeasuresResult(List.of(new MetricMeasuresResult(engineFailovers, measures)))
            );
            var request = new MeasuresRequest(
                TIME_RANGE,
                List.of(),
                List.of(new MetricMeasuresRequest(FAILOVERS, List.of(MetricSpec.Measure.AVG, MetricSpec.Measure.COUNT)))
            );

            var response = http.searchMeasures(new ExecutionContext("DEFAULT", "DEFAULT"), request);

            var query = ArgumentCaptor.forClass(MeasuresQuery.class);
            verify(repository).searchHTTPMeasures(any(), query.capture());
            assertThat(query.getValue().metrics()).extracting(MetricMeasuresQuery::metric).containsExactly(engineFailovers);
            assertThat(engineFailovers).isEqualTo(
                new AdditionalMetric(
                    new io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField(
                        io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField.ValueType.LONG,
                        "long_failover_count"
                    )
                )
            );
            var metric = response.metrics().getFirst();
            assertThat(metric.name()).isEqualTo(FAILOVERS);
            assertThat(metric.measures()).containsExactlyInAnyOrder(
                new io.gravitee.apim.core.analytics_engine.model.Measure(MetricSpec.Measure.AVG, null),
                new io.gravitee.apim.core.analytics_engine.model.Measure(MetricSpec.Measure.COUNT, 0)
            );
        }
    }

    /** The whole measures path with the real catalog: not its 500 for a name it does not hold, and no unit. */
    @Nested
    class MeasuresUseCase {

        private static final AdditionalMetricName FAILOVERS = new AdditionalMetricName(
            new AdditionalMetricField(AdditionalMetricField.ValueType.LONG, "long_failover_count")
        );

        @Test
        void should_answer_a_well_formed_number_field_no_request_carries_with_no_value_and_no_unit() {
            var noValue = new HashMap<Measure, Number>();
            noValue.put(Measure.AVG, null);
            noValue.put(Measure.COUNT, 0);
            when(repository.searchHTTPMeasures(any(), any())).thenReturn(
                new MeasuresResult(List.of(new MetricMeasuresResult(AnalyticsMeasuresAdapter.INSTANCE.toMetricName(FAILOVERS), noValue)))
            );
            var catalog = new AnalyticsDefinitionYAMLQueryService();
            var useCase = new ComputeMeasuresUseCase(
                new AnalyticsQueryContextProvider(List.of(new HTTPDataPlaneAnalyticsQueryService(repository))),
                new AnalyticsQueryValidator(catalog),
                List.of(),
                new UnitEnrichmentPostProcessorImpl(catalog),
                mock(AnalyticsQueryContextLoaderResolver.class)
            );
            var request = new MeasuresRequest(
                TIME_RANGE,
                List.of(),
                List.of(new MetricMeasuresRequest(FAILOVERS, List.of(MetricSpec.Measure.AVG, MetricSpec.Measure.COUNT)))
            );

            var response = useCase.executeForApis(AuditInfoFixtures.anAuditInfo("org", "env", "user"), request, Set.of("api")).response();

            assertThat(response.metrics())
                .singleElement()
                .satisfies(metric -> {
                    assertThat(metric.name()).isEqualTo(FAILOVERS);
                    assertThat(metric.unit()).isNull();
                    assertThat(metric.measures()).containsExactlyInAnyOrder(
                        new io.gravitee.apim.core.analytics_engine.model.Measure(MetricSpec.Measure.AVG, null),
                        new io.gravitee.apim.core.analytics_engine.model.Measure(MetricSpec.Measure.COUNT, 0)
                    );
                });
        }
    }

    @Nested
    class SkippedConditions {

        private final io.gravitee.repository.analytics.engine.api.query.FilterName engineTier =
            AnalyticsMeasuresAdapter.INSTANCE.toFilterName(TIER);

        @Test
        void should_report_an_additional_metric_condition_the_path_skips() {
            when(repository.filterOutcome(any(), any())).thenReturn(FilterOutcome.APPLIED);
            when(repository.filterOutcome(eq(AnalyticsSearchPath.NATIVE), eq(engineTier))).thenReturn(FilterOutcome.IGNORED);

            var ignored = IgnoredFilters.of(repository, AnalyticsSearchPath.NATIVE, query());

            assertThat(ignored).containsExactly(TIER);
        }

        @Test
        void should_report_nothing_when_the_condition_empties_the_result() {
            when(repository.filterOutcome(any(), any())).thenReturn(FilterOutcome.APPLIED);
            when(repository.filterOutcome(eq(AnalyticsSearchPath.AUTHZ_TRAFFIC), eq(engineTier))).thenReturn(FilterOutcome.EMPTIES);

            assertThat(IgnoredFilters.of(repository, AnalyticsSearchPath.AUTHZ_TRAFFIC, query())).isEmpty();
        }

        private static io.gravitee.repository.analytics.engine.api.query.Query query() {
            return AnalyticsMeasuresAdapter.INSTANCE.fromRequest(
                new io.gravitee.apim.core.analytics_engine.model.MeasuresRequest(
                    TIME_RANGE,
                    List.of(
                        new Filter(TIER, FilterOperator.EQ, "high"),
                        new Filter(FilterSpec.Name.API, FilterOperator.IN, List.of("api"))
                    ),
                    List.of()
                )
            );
        }
    }

    @Nested
    class CallerIdentification {

        private static final AdditionalMetricField CLIENT_ID = new AdditionalMetricField(
            AdditionalMetricField.ValueType.KEYWORD,
            "keyword_native-kafka_client-id"
        );
        private static final AdditionalMetricField MODEL = new AdditionalMetricField(
            AdditionalMetricField.ValueType.KEYWORD,
            "keyword_llm-proxy_model"
        );

        private final AnalyticsQueryContextProvider provider = new AnalyticsQueryContextProvider(
            List.of(
                new HTTPDataPlaneAnalyticsQueryService(repository),
                new NativeApiAnalyticsQueryService(repository),
                new MessageDataPlaneQueryService(repository),
                new AuthzTrafficAnalyticsQueryService(repository)
            )
        );

        @Test
        void should_identify_a_caller_through_the_key_an_identifying_built_in_reads() {
            when(repository.builtInAdditionalMetricKeys()).thenReturn(Map.of("keyword_native-kafka_client-id", Set.of("NATIVE_CLIENT_ID")));

            assertThat(provider.identifiesCaller(CLIENT_ID)).isTrue();
        }

        @Test
        void should_not_identify_a_caller_through_the_key_a_non_identifying_built_in_reads() {
            when(repository.builtInAdditionalMetricKeys()).thenReturn(Map.of("keyword_llm-proxy_model", Set.of("LLM_PROXY_MODEL")));

            assertThat(provider.identifiesCaller(MODEL)).isFalse();
        }

        @Test
        void should_not_identify_a_caller_through_a_key_no_built_in_reads() {
            assertThat(provider.identifiesCaller(TIER.field())).isFalse();
        }
    }
}
