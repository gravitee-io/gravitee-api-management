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
package io.gravitee.apim.infra.query_service.api_health;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_health.model.AvailabilityHealthCheck;
import io.gravitee.apim.core.api_health.model.HealthCheckLog;
import io.gravitee.apim.core.api_health.query_service.ApiHealthQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.healthcheck.v4.api.HealthCheckRepository;
import io.gravitee.repository.healthcheck.v4.model.ApiFieldPeriod;
import io.gravitee.repository.healthcheck.v4.model.AvailabilityResponse;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTime;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeOvertimeQuery;
import io.gravitee.repository.healthcheck.v4.model.HealthCheckLogQuery;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiHealthQueryServiceImplTest {

    private static final String ORGANIZATION_ID = "org#1";
    private static final String ENVIRONMENT_ID = "env#1";
    private static final String API_ID = "api#1";
    private static final Instant INSTANT = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT;
    private static final Duration INTERVAL = Duration.ofMinutes(10);

    @Mock
    HealthCheckRepository healthCheckRepository;

    @Captor
    ArgumentCaptor<QueryContext> queryContextCaptor;

    ApiHealthQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ApiHealthQueryServiceImpl(healthCheckRepository);
    }

    @Nested
    class AverageResponseTime {

        @Test
        void should_call_repository() {
            var queryCaptor = ArgumentCaptor.forClass(ApiFieldPeriod.class);
            when(healthCheckRepository.averageResponseTime(any(), any()))
                .thenReturn(Maybe.just(new AverageHealthCheckResponseTime(2L, Map.of("default", 2L))));

            var result = service
                .averageResponseTime(
                    new ApiHealthQueryService.ApiFieldPeriodQuery(ORGANIZATION_ID, ENVIRONMENT_ID, API_ID, "endpoint", FROM, TO)
                )
                .blockingGet();

            verify(healthCheckRepository).averageResponseTime(queryContextCaptor.capture(), queryCaptor.capture());
            SoftAssertions.assertSoftly(softly -> {
                softly
                    .assertThat(result)
                    .isEqualTo(new io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTime(2L, Map.of("default", 2L)));
                softly.assertThat(queryContextCaptor.getValue()).isEqualTo(new QueryContext(ORGANIZATION_ID, ENVIRONMENT_ID));
                softly.assertThat(queryCaptor.getValue()).isEqualTo(new ApiFieldPeriod(API_ID, "endpoint", FROM, TO));
            });
        }
    }

    @Nested
    class Availability {

        @Test
        void should_call_repository() {
            var queryCaptor = ArgumentCaptor.forClass(ApiFieldPeriod.class);
            when(healthCheckRepository.availability(any(), any()))
                .thenReturn(Maybe.just(new AvailabilityResponse(.75f, Map.of("default", .75f))));

            var result = service
                .availability(new ApiHealthQueryService.ApiFieldPeriodQuery(ORGANIZATION_ID, ENVIRONMENT_ID, API_ID, "endpoint", FROM, TO))
                .blockingGet();

            verify(healthCheckRepository).availability(queryContextCaptor.capture(), queryCaptor.capture());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result).isEqualTo(new AvailabilityHealthCheck(.75f, Map.of("default", .75f)));
                softly.assertThat(queryContextCaptor.getValue()).isEqualTo(new QueryContext(ORGANIZATION_ID, ENVIRONMENT_ID));
                softly.assertThat(queryCaptor.getValue()).isEqualTo(new ApiFieldPeriod(API_ID, "endpoint", FROM, TO));
            });
        }
    }

    @Nested
    class AverageResponseTimeOvertime {

        @Test
        void should_call_repository() {
            var queryCaptor = ArgumentCaptor.forClass(AverageHealthCheckResponseTimeOvertimeQuery.class);
            when(healthCheckRepository.averageResponseTimeOvertime(any(), any()))
                .thenReturn(Maybe.just(new AverageHealthCheckResponseTimeOvertime(Map.of("default", 2L))));

            var result = service
                .averageResponseTimeOvertime(
                    new ApiHealthQueryService.AverageHealthCheckResponseTimeOvertimeQuery(
                        ORGANIZATION_ID,
                        ENVIRONMENT_ID,
                        API_ID,
                        FROM,
                        TO,
                        INTERVAL
                    )
                )
                .blockingGet();

            verify(healthCheckRepository).averageResponseTimeOvertime(queryContextCaptor.capture(), queryCaptor.capture());
            SoftAssertions.assertSoftly(softly -> {
                softly
                    .assertThat(result)
                    .isEqualTo(
                        new io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTimeOvertime(
                            new io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTimeOvertime.TimeRange(FROM, TO, INTERVAL),
                            List.of(2L)
                        )
                    );
                softly.assertThat(queryContextCaptor.getValue()).isEqualTo(new QueryContext(ORGANIZATION_ID, ENVIRONMENT_ID));
                softly
                    .assertThat(queryCaptor.getValue())
                    .isEqualTo(new AverageHealthCheckResponseTimeOvertimeQuery(API_ID, FROM, TO, INTERVAL));
            });
        }
    }

    @Nested
    class SearchLogs {

        @Test
        void should_call_repository() {
            var queryCaptor = ArgumentCaptor.forClass(HealthCheckLogQuery.class);
            when(healthCheckRepository.searchLogs(any(), any()))
                .thenReturn(
                    Maybe.just(
                        new Page<>(
                            List.of(
                                new io.gravitee.repository.healthcheck.v4.model.HealthCheckLog(
                                    "id",
                                    INSTANT,
                                    "api-id",
                                    "endpoint",
                                    "gateway",
                                    21L,
                                    false,
                                    List.of()
                                )
                            ),
                            1,
                            5,
                            1
                        )
                    )
                );

            var result = service
                .searchLogs(
                    new ApiHealthQueryService.SearchLogsQuery(
                        ORGANIZATION_ID,
                        ENVIRONMENT_ID,
                        API_ID,
                        FROM,
                        TO,
                        Optional.of(false),
                        new PageableImpl(1, 5)
                    )
                )
                .blockingGet();

            verify(healthCheckRepository).searchLogs(queryContextCaptor.capture(), queryCaptor.capture());
            SoftAssertions.assertSoftly(softly -> {
                softly
                    .assertThat(result)
                    .extracting(Page::getContent, Page::getPageNumber, Page::getTotalElements)
                    .contains(List.of(new HealthCheckLog("id", INSTANT, "api-id", "endpoint", "gateway", 21L, false, List.of())), 1, 1L);
                softly.assertThat(queryContextCaptor.getValue()).isEqualTo(new QueryContext(ORGANIZATION_ID, ENVIRONMENT_ID));
                softly
                    .assertThat(queryCaptor.getValue())
                    .isEqualTo(
                        new HealthCheckLogQuery(
                            API_ID,
                            FROM,
                            TO,
                            Optional.of(false),
                            new PageableBuilder().pageNumber(1).pageSize(5).build()
                        )
                    );
            });
        }
    }
}
