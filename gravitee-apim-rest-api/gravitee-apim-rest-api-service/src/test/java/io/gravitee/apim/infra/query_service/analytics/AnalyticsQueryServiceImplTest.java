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

import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
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
            when(analyticsRepository.searchRequestsCount(any())).thenReturn(Optional.empty());
            assertThat(cut.searchRequestsCount(any())).isEmpty();
        }

        @Test
        void should_map_repository_response_to_requests_count() {
            when(analyticsRepository.searchRequestsCount(any()))
                .thenReturn(
                    Optional.of(CountAggregate.builder().total(10).countBy(Map.of("first", 3L, "second", 4L, "third", 3L)).build())
                );
            assertThat(cut.searchRequestsCount(any()))
                .hasValueSatisfying(requestsCount -> {
                    assertThat(requestsCount.getTotal()).isEqualTo(10);
                    assertThat(requestsCount.getCountsByEntrypoint()).containsAllEntriesOf(Map.of("first", 3L, "second", 4L, "third", 3L));
                });
        }
    }
}
