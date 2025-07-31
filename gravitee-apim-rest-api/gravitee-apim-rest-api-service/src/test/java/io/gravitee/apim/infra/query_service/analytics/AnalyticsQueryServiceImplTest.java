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
package io.gravitee.apim.infra.query_service.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.model.Bucket;
import io.gravitee.apim.core.analytics.model.HistogramAnalytics;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.model.Timestamp;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.CountByAggregate;
import io.gravitee.repository.log.v4.model.analytics.HistogramAggregate;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.rest.api.model.analytics.TopHitsApps;
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.model.v4.analytics.TopFailedApis;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceImplTest {

    private static final String ORGANIZATION_ID = "org#1";
    private static final String ENVIRONMENT_ID = "env#1";

    @Mock
    AnalyticsRepository analyticsRepository;

    AnalyticsQueryService cut;

    @Captor
    ArgumentCaptor<QueryContext> queryContextCaptor;

    @BeforeEach
    void setUp() {
        cut = new AnalyticsQueryServiceImpl(analyticsRepository);

        GraviteeContext.setCurrentOrganization(ORGANIZATION_ID);
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Nested
    class RequestsCountAnalytics {

        @Test
        void should_return_empty_requests_count() {
            when(analyticsRepository.searchRequestsCount(any(QueryContext.class), any())).thenReturn(Optional.empty());
            assertThat(cut.searchRequestsCount(GraviteeContext.getExecutionContext(), "api#1", null, null)).isEmpty();
        }

        @Test
        void should_map_repository_response_to_requests_count() {
            when(analyticsRepository.searchRequestsCount(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(CountAggregate.builder().total(10).countBy(Map.of("first", 3L, "second", 4L, "third", 3L)).build())
                );
            assertThat(cut.searchRequestsCount(GraviteeContext.getExecutionContext(), "api#1", null, null))
                .hasValueSatisfying(requestsCount -> {
                    assertThat(requestsCount.getTotal()).isEqualTo(10);
                    assertThat(requestsCount.getCountsByEntrypoint()).containsAllEntriesOf(Map.of("first", 3L, "second", 4L, "third", 3L));
                });
        }

        @Test
        void should_return_request_status_ranges() {
            var queryParameters = AnalyticsQueryParameters.builder().apiIds(List.of("api#1")).build();
            when(analyticsRepository.searchResponseStatusRanges(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(
                        ResponseStatusRangesAggregate
                            .builder()
                            .ranges(Map.of("100.0-200.0", 1L, "200.0-300.0", 2L))
                            .statusRangesCountByEntrypoint(
                                Map.of(
                                    "http-post",
                                    Map.of("100.0-200.0", 1L, "200.0-300.0", 1L),
                                    "http-get",
                                    Map.of("100.0-200.0", 0L, "200.0-300.0", 1L)
                                )
                            )
                            .build()
                    )
                );
            assertThat(cut.searchResponseStatusRanges(GraviteeContext.getExecutionContext(), queryParameters))
                .hasValueSatisfying(responseStatusRanges -> {
                    assertThat(responseStatusRanges.getRanges()).containsAllEntriesOf(Map.of("100.0-200.0", 1L, "200.0-300.0", 2L));
                    assertThat(responseStatusRanges.getStatusRangesCountByEntrypoint().get("http-post"))
                        .containsAllEntriesOf(Map.of("100.0-200.0", 1L, "200.0-300.0", 1L));
                    assertThat(responseStatusRanges.getStatusRangesCountByEntrypoint().get("http-get"))
                        .containsAllEntriesOf(Map.of("100.0-200.0", 0L, "200.0-300.0", 1L));
                });
        }

        @Test
        void should_return_top_hits() {
            var queryParameters = AnalyticsQueryParameters.builder().apiIds(List.of("api#1")).build();
            when(analyticsRepository.searchTopHitsApi(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(TopHitsAggregate.builder().topHitsCounts(Map.of("api-id-1", 15L, "api-id-2", 2L, "api-id-3", 17L)).build())
                );

            var result = cut.searchTopHitsApis(GraviteeContext.getExecutionContext(), queryParameters);

            assertThat(result)
                .hasValueSatisfying(topHits ->
                    assertThat(topHits.getData())
                        .containsExactlyInAnyOrder(
                            TopHitsApis.TopHitApi.builder().id("api-id-1").count(15L).build(),
                            TopHitsApis.TopHitApi.builder().id("api-id-2").count(2L).build(),
                            TopHitsApis.TopHitApi.builder().id("api-id-3").count(17L).build()
                        )
                );
        }

        @Test
        void should_return_request_response_time() {
            var queryParameters = AnalyticsQueryParameters.builder().apiIds(List.of("api#1")).build();
            when(analyticsRepository.searchRequestResponseTimes(any(QueryContext.class), any()))
                .thenReturn(
                    RequestResponseTimeAggregate
                        .builder()
                        .requestsPerSecond(3.7d)
                        .requestsTotal(25600L)
                        .responseMinTime(32.5d)
                        .responseMaxTime(1220.87d)
                        .responseAvgTime(159.2d)
                        .build()
                );

            var result = cut.searchRequestResponseTime(GraviteeContext.getExecutionContext(), queryParameters);

            assertThat(result)
                .isNotNull()
                .isEqualTo(
                    RequestResponseTime
                        .builder()
                        .requestsPerSecond(3.7d)
                        .requestsTotal(25600L)
                        .responseMinTime(32.5d)
                        .responseMaxTime(1220.87d)
                        .responseAvgTime(159.2d)
                        .build()
                );
        }
    }

    @Nested
    class SearchResponseStatusOvertime {

        private static final String API_ID = "api#1";
        private static final Instant INSTANT = Instant.parse("2023-10-22T10:15:30Z");
        private static final ResponseStatusOvertime.TimeRange TIME_RANGE = new ResponseStatusOvertime.TimeRange(
            INSTANT.minus(1, ChronoUnit.DAYS),
            INSTANT,
            Duration.ofMinutes(10)
        );

        @Test
        void should_call_analytics_repository() {
            var queryCaptor = ArgumentCaptor.forClass(ResponseStatusOverTimeQuery.class);
            var expectedMap = Map.of("200", List.of(0L, 0L));

            when(analyticsRepository.searchResponseStatusOvertime(any(), any()))
                .thenReturn(new ResponseStatusOverTimeAggregate(expectedMap));

            var result = cut.searchResponseStatusOvertime(
                GraviteeContext.getExecutionContext(),
                new AnalyticsQueryService.ResponseStatusOverTimeQuery(
                    List.of(API_ID),
                    TIME_RANGE.from(),
                    TIME_RANGE.to(),
                    TIME_RANGE.interval(),
                    List.of(DefinitionVersion.V4)
                )
            );

            verify(analyticsRepository).searchResponseStatusOvertime(queryContextCaptor.capture(), queryCaptor.capture());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getData()).isSameAs(expectedMap);
                softly.assertThat(result.getTimeRange()).isEqualTo(TIME_RANGE);
                softly.assertThat(queryContextCaptor.getValue()).isEqualTo(new QueryContext(ORGANIZATION_ID, ENVIRONMENT_ID));
                softly.assertThat(queryCaptor.getValue().apiIds()).isEqualTo(List.of(API_ID));
                softly.assertThat(queryCaptor.getValue().from()).isEqualTo(TIME_RANGE.from());
                softly.assertThat(queryCaptor.getValue().to()).isEqualTo(TIME_RANGE.to());
                softly.assertThat(queryCaptor.getValue().interval()).isEqualTo(TIME_RANGE.interval());
            });
        }
    }

    @Nested
    class SearchTopApps {

        @Test
        void should_search_top_apps() {
            var queryParameters = AnalyticsQueryParameters.builder().apiIds(List.of("api#1")).build();
            when(analyticsRepository.searchTopApps(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(TopHitsAggregate.builder().topHitsCounts(Map.of("app-id-1", 15L, "app-id-2", 2L, "app-id-3", 17L)).build())
                );

            var result = cut.searchTopHitsApps(GraviteeContext.getExecutionContext(), queryParameters);

            assertThat(result)
                .hasValueSatisfying(topHits ->
                    assertThat(topHits.getData())
                        .containsExactlyInAnyOrder(
                            TopHitsApps.TopHitApp.builder().id("app-id-1").count(15L).build(),
                            TopHitsApps.TopHitApp.builder().id("app-id-2").count(2L).build(),
                            TopHitsApps.TopHitApp.builder().id("app-id-3").count(17L).build()
                        )
                );
        }
    }

    @Nested
    class SearchTopFailedApis {

        @Test
        void should_search_top_failed_apis() {
            var queryParameters = AnalyticsQueryParameters.builder().apiIds(List.of("api#1")).build();
            when(analyticsRepository.searchTopFailedApis(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(
                        TopFailedAggregate
                            .builder()
                            .failedApis(
                                Map.of(
                                    "app-id-1",
                                    new TopFailedAggregate.FailedApiInfo(13L, 0.3),
                                    "app-id-2",
                                    new TopFailedAggregate.FailedApiInfo(7L, 0.2),
                                    "app-id-3",
                                    new TopFailedAggregate.FailedApiInfo(3L, 0.1)
                                )
                            )
                            .build()
                    )
                );

            var result = cut.searchTopFailedApis(GraviteeContext.getExecutionContext(), queryParameters);

            assertThat(result)
                .hasValueSatisfying(topHits ->
                    assertThat(topHits.getData())
                        .containsExactlyInAnyOrder(
                            TopFailedApis.TopFailedApi.builder().id("app-id-1").failedCalls(13L).failedCallsRatio(0.3).build(),
                            TopFailedApis.TopFailedApi.builder().id("app-id-2").failedCalls(7L).failedCallsRatio(0.2).build(),
                            TopFailedApis.TopFailedApi.builder().id("app-id-3").failedCalls(3L).failedCallsRatio(0.1).build()
                        )
                );
        }
    }

    @Nested
    class HistogramAnalyticsTest {

        @Test
        void should_map_histogram_aggregate_to_histogram_analytics() {
            Instant from = Instant.parse("2024-01-01T00:00:00Z");
            Instant to = Instant.parse("2024-01-02T00:00:00Z");
            Duration interval = Duration.ofHours(1);

            HistogramAggregate rootAggregate = new HistogramAggregate(
                "rootField",
                "rootName",
                Map.of("200", List.of(0L, 0L, 1L, 0L), "202", List.of(0L, 0L, 0L, 1L), "404", List.of(0L, 0L, 0L, 2L))
            );

            when(analyticsRepository.searchHistogram(any(QueryContext.class), any(HistogramQuery.class)))
                .thenReturn(List.of(rootAggregate));

            AnalyticsQueryService.HistogramQuery query = new AnalyticsQueryService.HistogramQuery(
                "api-1",
                from,
                to,
                interval,
                null,
                Optional.empty()
            );

            Optional<HistogramAnalytics> result = cut.searchHistogramAnalytics(GraviteeContext.getExecutionContext(), query);

            assertThat(result).isPresent();
            HistogramAnalytics analytics = result.get();
            assertThat(analytics.getTimestamp()).isEqualTo(new Timestamp(from, to, interval));
            assertThat(analytics.getValues()).hasSize(1);

            Bucket rootBucket = analytics.getValues().getFirst();
            assertThat(rootBucket.getField()).isEqualTo("rootField");
            assertThat(rootBucket.getName()).isEqualTo("rootName");
            assertThat(rootBucket.getBuckets()).hasSize(3);

            assertThat(rootBucket.getBuckets().stream().map(Bucket::getName)).containsExactlyInAnyOrder("200", "202", "404");

            Bucket bucket200 = rootBucket.getBuckets().stream().filter(b -> "200".equals(b.getName())).findFirst().orElseThrow();
            Bucket bucket202 = rootBucket.getBuckets().stream().filter(b -> "202".equals(b.getName())).findFirst().orElseThrow();
            Bucket bucket404 = rootBucket.getBuckets().stream().filter(b -> "404".equals(b.getName())).findFirst().orElseThrow();

            assertThat(bucket200.getData()).containsExactly(0L, 0L, 1L, 0L);
            assertThat(bucket202.getData()).containsExactly(0L, 0L, 0L, 1L);
            assertThat(bucket404.getData()).containsExactly(0L, 0L, 0L, 2L);
        }
    }

    @Nested
    class SearchGroupByAnalyticsTest {

        @Test
        void should_map_group_by_aggregate_to_group_by_analytics() {
            var from = Instant.parse("2024-01-01T00:00:00Z");
            var to = Instant.parse("2024-01-02T00:00:00Z");
            var apiId = "api-1";
            var field = "status";
            var values = Map.of("200", 10L, "404", 2L);
            var order = List.of("200", "404");

            var repoAggregate = new io.gravitee.repository.log.v4.model.analytics.GroupByAggregate("aggName", field, values, order);

            when(analyticsRepository.searchGroupBy(any(QueryContext.class), any())).thenReturn(Optional.of(repoAggregate));

            var groupByQuery = new AnalyticsQueryService.GroupByQuery(
                apiId,
                from,
                to,
                field,
                Collections.emptyList(),
                Optional.empty(),
                Optional.empty()
            );

            var result = cut.searchGroupByAnalytics(GraviteeContext.getExecutionContext(), groupByQuery);

            assertThat(result).isPresent();
            var analytics = result.get();
            assertThat(analytics.getValues()).containsExactlyInAnyOrderEntriesOf(values);
        }

        @Test
        void should_pass_query_parameter_to_repository() {
            var from = Instant.parse("2024-01-01T00:00:00Z");
            var to = Instant.parse("2024-01-02T00:00:00Z");
            var apiId = "api-1";
            var field = "status";
            var queryString = "status:200 AND method:GET";
            var order = List.of("200");

            var repoAggregate = new io.gravitee.repository.log.v4.model.analytics.GroupByAggregate(
                "aggName",
                field,
                Map.of("200", 10L),
                order
            );
            when(analyticsRepository.searchGroupBy(any(QueryContext.class), any())).thenReturn(Optional.of(repoAggregate));

            var groupByQuery = new AnalyticsQueryService.GroupByQuery(
                apiId,
                from,
                to,
                field,
                Collections.emptyList(),
                Optional.empty(),
                Optional.of(queryString)
            );

            cut.searchGroupByAnalytics(GraviteeContext.getExecutionContext(), groupByQuery);

            ArgumentCaptor<io.gravitee.repository.log.v4.model.analytics.GroupByQuery> repoQueryCaptor = ArgumentCaptor.forClass(
                io.gravitee.repository.log.v4.model.analytics.GroupByQuery.class
            );
            verify(analyticsRepository).searchGroupBy(any(QueryContext.class), repoQueryCaptor.capture());
            assertThat(repoQueryCaptor.getValue().query()).hasValue(queryString);
        }
    }

    @Nested
    class SearchStatsAnalyticsTest {

        @Test
        void should_return_empty_when_repository_returns_empty() {
            when(analyticsRepository.searchStats(any(QueryContext.class), any())).thenReturn(Optional.empty());

            var statsQuery = new AnalyticsQueryService.StatsQuery("api#1", "field", Instant.now(), Instant.now(), Optional.empty());
            var result = cut.searchStatsAnalytics(GraviteeContext.getExecutionContext(), statsQuery);

            assertThat(result).isEmpty();
        }

        @Test
        void should_map_stats_aggregate_to_stats_analytics() {
            var repoAggregate = new io.gravitee.repository.log.v4.model.analytics.StatsAggregate(
                "field",
                10L,
                100L,
                10L,
                1L,
                20L,
                2L,
                3L,
                4L
            );
            when(analyticsRepository.searchStats(any(QueryContext.class), any())).thenReturn(Optional.of(repoAggregate));

            var statsQuery = new AnalyticsQueryService.StatsQuery("api#1", "field", Instant.now(), Instant.now(), Optional.empty());
            var result = cut.searchStatsAnalytics(GraviteeContext.getExecutionContext(), statsQuery);

            assertThat(result)
                .hasValueSatisfying(statsAnalytics -> {
                    assertThat(statsAnalytics.count()).isEqualTo(10);
                    assertThat(statsAnalytics.sum()).isEqualTo(100L);
                    assertThat(statsAnalytics.avg()).isEqualTo(10L);
                    assertThat(statsAnalytics.min()).isEqualTo(1L);
                    assertThat(statsAnalytics.max()).isEqualTo(20L);
                    assertThat(statsAnalytics.rps()).isEqualTo(2L);
                    assertThat(statsAnalytics.rpm()).isEqualTo(3L);
                    assertThat(statsAnalytics.rph()).isEqualTo(4L);
                });
        }

        @Test
        void should_pass_query_argument_to_repository() {
            var repoAggregate = new io.gravitee.repository.log.v4.model.analytics.StatsAggregate("field", 5, 50L, 10L, 2L, 20L, 1L, 2L, 3L);
            when(analyticsRepository.searchStats(any(QueryContext.class), any())).thenReturn(Optional.of(repoAggregate));

            var queryString = "status:200 AND method:GET";
            var statsQuery = new AnalyticsQueryService.StatsQuery("api#1", "field", Instant.now(), Instant.now(), Optional.of(queryString));
            var result = cut.searchStatsAnalytics(GraviteeContext.getExecutionContext(), statsQuery);

            assertThat(result)
                .hasValueSatisfying(statsAnalytics -> {
                    assertThat(statsAnalytics.count()).isEqualTo(5);
                    assertThat(statsAnalytics.sum()).isEqualTo(50L);
                    assertThat(statsAnalytics.avg()).isEqualTo(10L);
                    assertThat(statsAnalytics.min()).isEqualTo(2L);
                    assertThat(statsAnalytics.max()).isEqualTo(20L);
                    assertThat(statsAnalytics.rps()).isEqualTo(1L);
                });
            verify(analyticsRepository)
                .searchStats(any(QueryContext.class), argThat(arg -> arg.query().isPresent() && arg.query().get().equals(queryString)));
        }
    }

    @Nested
    class SearchRequestCountsAnalytics {

        @Test
        void should_return_empty_requests_count() {
            var from = Instant.parse("2024-01-01T00:00:00Z");
            var to = Instant.parse("2024-01-02T00:00:00Z");
            var query = new AnalyticsQueryService.CountQuery(Map.of("api", "api#1"), from, to);
            when(analyticsRepository.searchRequestsCountByEvent(any(QueryContext.class), any())).thenReturn(Optional.empty());
            assertThat(cut.searchRequestsCountByEvent(GraviteeContext.getExecutionContext(), query)).isEmpty();
        }

        @Test
        void should_map_repository_response_to_requests_count() {
            var from = Instant.parse("2024-01-01T00:00:00Z");
            var to = Instant.parse("2024-01-02T00:00:00Z");
            var query = new AnalyticsQueryService.CountQuery(Map.of("api", "api#1"), from, to);
            when(analyticsRepository.searchRequestsCountByEvent(any(QueryContext.class), any()))
                .thenReturn(Optional.of(new CountByAggregate(10)));
            assertThat(cut.searchRequestsCountByEvent(GraviteeContext.getExecutionContext(), query))
                .hasValueSatisfying(requestsCount -> {
                    assertThat(requestsCount.getTotal()).isEqualTo(10);
                });
        }

        @Test
        void should_return_empty_requests_count_by_event() {
            when(analyticsRepository.searchRequestsCountByEvent(any(QueryContext.class), any())).thenReturn(Optional.empty());
            assertThat(
                cut.searchRequestsCountByEvent(
                    GraviteeContext.getExecutionContext(),
                    new AnalyticsQueryService.CountQuery(Map.of("api", "api#1"), null, null)
                )
            )
                .isEmpty();
        }
    }
}
