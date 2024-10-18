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
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics.model.StatusRangesQueryParameters;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceImplTest {

    @Mock
    AnalyticsRepository analyticsRepository;

    AnalyticsQueryService cut;

    @BeforeEach
    void setUp() {
        cut = new AnalyticsQueryServiceImpl(analyticsRepository);
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
            var queryParameters = StatusRangesQueryParameters.builder().apiIds(List.of("api#1")).build();
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
    }
}
