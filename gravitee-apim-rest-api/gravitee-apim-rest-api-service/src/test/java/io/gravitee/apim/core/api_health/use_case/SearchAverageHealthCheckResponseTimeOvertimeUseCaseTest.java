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
package io.gravitee.apim.core.api_health.use_case;

import static fixtures.core.model.ApiFixtures.MY_API;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.verify;

import fakes.FakeApiHealthQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTimeOvertime;
import io.gravitee.apim.core.api_health.query_service.ApiHealthQueryService;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SearchAverageHealthCheckResponseTimeOvertimeUseCaseTest {

    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant TO = INSTANT_NOW;
    private static final Instant FROM = TO.minus(1, ChronoUnit.DAYS);
    private static final Duration INTERVAL = Duration.ofMinutes(10);

    private final FakeApiHealthQueryService apiHealthQueryService = Mockito.spy(new FakeApiHealthQueryService());
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    private SearchAverageHealthCheckResponseTimeOvertimeUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(TO, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase = new SearchAverageHealthCheckResponseTimeOvertimeUseCase(apiHealthQueryService, apiCrudService);
    }

    @AfterEach
    void tearDown() {
        apiCrudService.reset();
        apiHealthQueryService.reset();
    }

    @Test
    void should_return_average_response_time_for_the_given_period() {
        // Given
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        var expectedData = new AverageHealthCheckResponseTimeOvertime(
            new AverageHealthCheckResponseTimeOvertime.TimeRange(FROM, TO, INTERVAL),
            List.of(3L)
        );
        apiHealthQueryService.averageHealthCheckResponseTimeOvertime = expectedData;

        // When
        var output = useCase
            .execute(new SearchAverageHealthCheckResponseTimeOvertimeUseCase.Input(ORGANIZATION_ID, ENV_ID, MY_API, FROM, TO, INTERVAL))
            .blockingGet();

        // Then
        assertThat(requireNonNull(output).averageHealthCheckResponseTimeOvertime()).isEqualTo(expectedData);

        var queryCaptor = ArgumentCaptor.forClass(ApiHealthQueryService.AverageHealthCheckResponseTimeOvertimeQuery.class);
        verify(apiHealthQueryService).averageResponseTimeOvertime(queryCaptor.capture());
        assertThat(queryCaptor.getValue())
            .satisfies(query -> {
                assertSoftly(softly -> {
                    softly.assertThat(query.organizationId()).isEqualTo(ORGANIZATION_ID);
                    softly.assertThat(query.environmentId()).isEqualTo(ENV_ID);
                    softly.assertThat(query.apiId()).isEqualTo(MY_API);
                    softly.assertThat(query.from()).isEqualTo(FROM);
                    softly.assertThat(query.to()).isEqualTo(TO);
                    softly.assertThat(query.interval()).isEqualTo(INTERVAL);
                });
            });
    }

    @Test
    void should_throw_if_no_api_found() {
        // Given

        // When
        var throwable = catchThrowable(() ->
            useCase
                .execute(new SearchAverageHealthCheckResponseTimeOvertimeUseCase.Input(ORGANIZATION_ID, ENV_ID, MY_API, FROM, TO, INTERVAL))
                .blockingGet()
        );

        // Then
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_no_api_does_not_belong_to_current_environment() {
        // Given
        apiCrudService.initWith(List.of(ApiFixtures.aMessageApiV4()));

        // When
        var throwable = catchThrowable(() ->
            useCase
                .execute(
                    new SearchAverageHealthCheckResponseTimeOvertimeUseCase.Input(ORGANIZATION_ID, "another", MY_API, FROM, TO, INTERVAL)
                )
                .blockingGet()
        );

        // Then
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_is_tcp() {
        // Given
        apiCrudService.initWith(List.of(ApiFixtures.aTcpApiV4()));

        // When
        var throwable = catchThrowable(() ->
            useCase
                .execute(new SearchAverageHealthCheckResponseTimeOvertimeUseCase.Input(ORGANIZATION_ID, ENV_ID, MY_API, FROM, TO, INTERVAL))
                .blockingGet()
        );

        // Then
        assertThat(throwable).isInstanceOf(TcpProxyNotSupportedException.class);
    }
}
