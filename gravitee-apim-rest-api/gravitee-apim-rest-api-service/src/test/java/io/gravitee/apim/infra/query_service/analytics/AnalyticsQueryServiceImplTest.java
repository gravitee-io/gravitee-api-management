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

import io.gravitee.apim.core.analytics.model.EnvironmentAnalyticsQueryParameters;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.rest.api.model.v4.analytics.TopHitsApis;
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
            assertThat(cut.searchRequestsCount(GraviteeContext.getExecutionContext(), "api#1")).isEmpty();
        }

        @Test
        void should_map_repository_response_to_requests_count() {
            when(analyticsRepository.searchRequestsCount(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(CountAggregate.builder().total(10).countBy(Map.of("first", 3L, "second", 4L, "third", 3L)).build())
                );
            assertThat(cut.searchRequestsCount(GraviteeContext.getExecutionContext(), "api#1"))
                .hasValueSatisfying(requestsCount -> {
                    assertThat(requestsCount.getTotal()).isEqualTo(10);
                    assertThat(requestsCount.getCountsByEntrypoint()).containsAllEntriesOf(Map.of("first", 3L, "second", 4L, "third", 3L));
                });
        }

        @Test
        void should_return_request_status_ranges() {
            var queryParameters = EnvironmentAnalyticsQueryParameters.builder().apiIds(List.of("api#1")).build();
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
            var queryParameters = EnvironmentAnalyticsQueryParameters.builder().apiIds(List.of("api#1")).build();
            when(analyticsRepository.searchTopHitsApi(any(QueryContext.class), any()))
                .thenReturn(
                    Optional.of(TopHitsAggregate.builder().topHitsCounts(Map.of("api-id-1", 15L, "api-id-2", 2L, "api-id-3", 17L)).build())
                );

            var result = cut.searchTopHitsApis(GraviteeContext.getExecutionContext(), queryParameters);

            assertThat(result)
                .isNotNull()
                .isPresent()
                .hasValueSatisfying(topHits ->
                    assertThat(topHits.getData())
                        .containsExactlyInAnyOrder(
                            TopHitsApis.TopHitApi.builder().id("api-id-1").count(15L).build(),
                            TopHitsApis.TopHitApi.builder().id("api-id-2").count(2L).build(),
                            TopHitsApis.TopHitApi.builder().id("api-id-3").count(17L).build()
                        )
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
                new AnalyticsQueryService.ResponseStatusOverTimeQuery(API_ID, TIME_RANGE.from(), TIME_RANGE.to(), TIME_RANGE.interval())
            );

            verify(analyticsRepository).searchResponseStatusOvertime(queryContextCaptor.capture(), queryCaptor.capture());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.getData()).isSameAs(expectedMap);
                softly.assertThat(result.getTimeRange()).isEqualTo(TIME_RANGE);
                softly.assertThat(queryContextCaptor.getValue()).isEqualTo(new QueryContext(ORGANIZATION_ID, ENVIRONMENT_ID));
                softly.assertThat(queryCaptor.getValue().apiId()).isEqualTo(API_ID);
                softly.assertThat(queryCaptor.getValue().from()).isEqualTo(TIME_RANGE.from());
                softly.assertThat(queryCaptor.getValue().to()).isEqualTo(TIME_RANGE.to());
                softly.assertThat(queryCaptor.getValue().interval()).isEqualTo(TIME_RANGE.interval());
            });
        }
    }
}
