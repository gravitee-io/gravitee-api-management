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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoQuery;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByQuery;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsStatsQuery;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.DateHistoAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.rest.api.model.analytics.TopHitsApps;
import io.gravitee.rest.api.model.v4.analytics.RequestResponseTime;
import io.gravitee.rest.api.model.v4.analytics.TopFailedApis;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
import io.gravitee.rest.api.model.v4.analytics.V4AnalyticsCount;
import io.gravitee.rest.api.model.v4.analytics.V4AnalyticsDateHisto;
import io.gravitee.rest.api.model.v4.analytics.V4AnalyticsGroupBy;
import io.gravitee.rest.api.model.v4.analytics.V4AnalyticsStats;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    class SearchV4AnalyticsCount {

        @Test
        void should_return_empty_when_repository_returns_empty() {
            when(analyticsRepository.searchRequestsCount(any(QueryContext.class), any())).thenReturn(Optional.empty());

            assertThat(cut.searchV4AnalyticsCount(GraviteeContext.getExecutionContext(), "api#1", 1000L, 2000L))
                .isEmpty();
        }

        @Test
        void should_map_repository_count_to_v4_analytics_count() {
            when(analyticsRepository.searchRequestsCount(any(QueryContext.class), any()))
                .thenReturn(Optional.of(CountAggregate.builder().total(42L).build()));

            assertThat(cut.searchV4AnalyticsCount(GraviteeContext.getExecutionContext(), "api#1", 1000L, 2000L))
                .hasValueSatisfying(v4 -> assertThat(v4.getCount()).isEqualTo(42L));
        }

        @Test
        void should_call_repository_with_requests_count_query() {
            var queryCaptor = ArgumentCaptor.forClass(RequestsCountQuery.class);
            when(analyticsRepository.searchRequestsCount(any(QueryContext.class), queryCaptor.capture()))
                .thenReturn(Optional.of(CountAggregate.builder().total(1L).build()));

            cut.searchV4AnalyticsCount(GraviteeContext.getExecutionContext(), "api-123", 1000L, 2000L);

            assertThat(queryCaptor.getValue().apiId()).contains("api-123");
            assertThat(queryCaptor.getValue().from()).contains(Instant.ofEpochMilli(1000L));
            assertThat(queryCaptor.getValue().to()).contains(Instant.ofEpochMilli(2000L));
        }
    }

    @Nested
    class SearchV4AnalyticsStats {

        @Test
        void should_return_empty_when_repository_returns_empty() {
            when(analyticsRepository.searchStats(any(QueryContext.class), any())).thenReturn(Optional.empty());

            assertThat(
                cut.searchV4AnalyticsStats(
                    GraviteeContext.getExecutionContext(),
                    "api#1",
                    1000L,
                    2000L,
                    "gateway-response-time-ms"
                )
            ).isEmpty();
        }

        @Test
        void should_map_repository_stats_to_v4_analytics_stats() {
            when(analyticsRepository.searchStats(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(
                        StatsAggregate.builder().count(10L).min(1.0).max(100.0).avg(50.5).sum(505.0).build()
                    )
                );

            assertThat(
                cut.searchV4AnalyticsStats(
                    GraviteeContext.getExecutionContext(),
                    "api#1",
                    1000L,
                    2000L,
                    "gateway-response-time-ms"
                )
            )
                .hasValueSatisfying(v4 -> {
                    assertThat(v4.getCount()).isEqualTo(10L);
                    assertThat(v4.getMin()).isEqualTo(1.0);
                    assertThat(v4.getMax()).isEqualTo(100.0);
                    assertThat(v4.getAvg()).isEqualTo(50.5);
                    assertThat(v4.getSum()).isEqualTo(505.0);
                });
        }

        @Test
        void should_call_repository_with_stats_query() {
            var queryCaptor = ArgumentCaptor.forClass(ApiAnalyticsStatsQuery.class);
            when(analyticsRepository.searchStats(any(QueryContext.class), queryCaptor.capture()))
                .thenReturn(Optional.of(StatsAggregate.builder().count(1L).min(0).max(0).avg(0).sum(0).build()));

            cut.searchV4AnalyticsStats(
                GraviteeContext.getExecutionContext(),
                "api-123",
                1000L,
                2000L,
                "endpoint-response-time-ms"
            );

            assertThat(queryCaptor.getValue().getApiId()).isEqualTo("api-123");
            assertThat(queryCaptor.getValue().getFrom()).isEqualTo(Instant.ofEpochMilli(1000L));
            assertThat(queryCaptor.getValue().getTo()).isEqualTo(Instant.ofEpochMilli(2000L));
            assertThat(queryCaptor.getValue().getField()).isEqualTo("endpoint-response-time-ms");
        }
    }

    @Nested
    class SearchV4AnalyticsGroupBy {

        @Test
        void should_return_empty_when_repository_returns_empty() {
            when(analyticsRepository.searchGroupBy(any(QueryContext.class), any())).thenReturn(Optional.empty());

            assertThat(
                cut.searchV4AnalyticsGroupBy(
                    GraviteeContext.getExecutionContext(),
                    "api#1",
                    1000L,
                    2000L,
                    "status",
                    10,
                    null
                )
            ).isEmpty();
        }

        @Test
        void should_map_repository_group_by_to_v4_analytics_group_by() {
            when(analyticsRepository.searchGroupBy(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(
                        GroupByAggregate.builder().values(Map.of("200", 80L, "404", 10L)).metadata(Map.of()).build()
                    )
                );

            assertThat(
                cut.searchV4AnalyticsGroupBy(
                    GraviteeContext.getExecutionContext(),
                    "api#1",
                    1000L,
                    2000L,
                    "status",
                    10,
                    null
                )
            )
                .hasValueSatisfying(v4 -> {
                    assertThat(v4.getValues()).containsAllEntriesOf(Map.of("200", 80L, "404", 10L));
                });
        }

        @Test
        void should_call_repository_with_group_by_query() {
            var queryCaptor = ArgumentCaptor.forClass(ApiAnalyticsGroupByQuery.class);
            when(analyticsRepository.searchGroupBy(any(QueryContext.class), queryCaptor.capture()))
                .thenReturn(Optional.of(GroupByAggregate.builder().values(Map.of()).metadata(Map.of()).build()));

            cut.searchV4AnalyticsGroupBy(
                GraviteeContext.getExecutionContext(),
                "api-123",
                1000L,
                2000L,
                "status",
                20,
                "-count"
            );

            assertThat(queryCaptor.getValue().getApiId()).isEqualTo("api-123");
            assertThat(queryCaptor.getValue().getField()).isEqualTo("status");
            assertThat(queryCaptor.getValue().getSize()).isEqualTo(20);
            assertThat(queryCaptor.getValue().getOrder()).isEqualTo("-count");
        }
    }

    @Nested
    class SearchV4AnalyticsDateHisto {

        @Test
        void should_return_empty_when_repository_returns_empty() {
            when(analyticsRepository.searchDateHisto(any(QueryContext.class), any())).thenReturn(Optional.empty());

            assertThat(
                cut.searchV4AnalyticsDateHisto(
                    GraviteeContext.getExecutionContext(),
                    "api#1",
                    1000L,
                    2000L,
                    "status",
                    3600000L
                )
            ).isEmpty();
        }

        @Test
        void should_map_repository_date_histo_to_v4_analytics_date_histo() {
            when(analyticsRepository.searchDateHisto(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(
                        DateHistoAggregate
                            .builder()
                            .timestamp(List.of(1000L, 2000L))
                            .values(
                                List.of(
                                    DateHistoAggregate.DateHistoValue
                                        .builder()
                                        .field("status")
                                        .buckets(List.of(5L, 10L))
                                        .metadata(Map.of())
                                        .build()
                                )
                            )
                            .build()
                    )
                );

            assertThat(
                cut.searchV4AnalyticsDateHisto(
                    GraviteeContext.getExecutionContext(),
                    "api#1",
                    1000L,
                    2000L,
                    "status",
                    3600000L
                )
            )
                .hasValueSatisfying(v4 -> {
                    assertThat(v4.getTimestamp()).containsExactly(1000L, 2000L);
                    assertThat(v4.getValues()).hasSize(1);
                    assertThat(v4.getValues().get(0).getField()).isEqualTo("status");
                    assertThat(v4.getValues().get(0).getBuckets()).containsExactly(5L, 10L);
                });
        }

        @Test
        void should_call_repository_with_date_histo_query() {
            var queryCaptor = ArgumentCaptor.forClass(ApiAnalyticsDateHistoQuery.class);
            when(analyticsRepository.searchDateHisto(any(QueryContext.class), queryCaptor.capture()))
                .thenReturn(
                    Optional.of(
                        DateHistoAggregate.builder().timestamp(List.of()).values(List.of()).build()
                    )
                );

            cut.searchV4AnalyticsDateHisto(
                GraviteeContext.getExecutionContext(),
                "api-123",
                1000L,
                2000L,
                "status",
                3600000L
            );

            assertThat(queryCaptor.getValue().getApiId()).isEqualTo("api-123");
            assertThat(queryCaptor.getValue().getField()).isEqualTo("status");
            assertThat(queryCaptor.getValue().getInterval()).isEqualTo(3600000L);
        }
    }
}
