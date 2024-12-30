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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import fakes.FakeApiHealthQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api_health.model.AverageHealthCheckResponseTime;
import io.gravitee.apim.core.api_health.model.HealthCheckLog;
import io.gravitee.apim.core.api_health.query_service.ApiHealthQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.common.PageableImpl;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SearchHealthCheckLogsUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String ENV_ID = "environment-id";

    private final FakeApiHealthQueryService apiHealthQueryService = Mockito.spy(new FakeApiHealthQueryService());
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();

    private SearchHealthCheckLogsUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase = new SearchHealthCheckLogsUseCase(apiHealthQueryService, apiCrudService);
    }

    @AfterEach
    void tearDown() {
        apiCrudService.reset();
        apiHealthQueryService.reset();
    }

    @Test
    void should_return_the_request_page_of_health_check_logs() {
        // Given
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        var expectedData = new Page<HealthCheckLog>(List.of(), 1, 10, 0);
        apiHealthQueryService.healthCheckLogs = expectedData;

        // When
        var output = useCase
            .execute(
                new SearchHealthCheckLogsUseCase.Input(
                    ORGANIZATION_ID,
                    ENV_ID,
                    MY_API,
                    INSTANT_NOW.minus(1, ChronoUnit.DAYS),
                    INSTANT_NOW,
                    Optional.of(false),
                    Optional.of(new PageableImpl(2, 5))
                )
            )
            .blockingGet();

        // Then
        assertThat(requireNonNull(output).logs()).isSameAs(expectedData);

        var queryCaptor = ArgumentCaptor.forClass(ApiHealthQueryService.SearchLogsQuery.class);
        verify(apiHealthQueryService).searchLogs(queryCaptor.capture());
        assertThat(queryCaptor.getValue())
            .satisfies(query -> {
                assertSoftly(softly -> {
                    softly.assertThat(query.organizationId()).isEqualTo(ORGANIZATION_ID);
                    softly.assertThat(query.environmentId()).isEqualTo(ENV_ID);
                    softly.assertThat(query.apiId()).isEqualTo(MY_API);
                    softly.assertThat(query.from()).isEqualTo(INSTANT_NOW.minus(1, ChronoUnit.DAYS));
                    softly.assertThat(query.to()).isEqualTo(INSTANT_NOW);
                    softly.assertThat(query.success()).isEqualTo(Optional.of(false));
                    softly.assertThat(query.pageable()).isEqualTo(new PageableImpl(2, 5));
                });
            });
    }

    @Test
    void should_return_the_1st_age() {
        // Given
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        var expectedData = new Page<HealthCheckLog>(List.of(), 1, 10, 0);
        apiHealthQueryService.healthCheckLogs = expectedData;

        // When
        var output = useCase
            .execute(
                new SearchHealthCheckLogsUseCase.Input(
                    ORGANIZATION_ID,
                    ENV_ID,
                    MY_API,
                    INSTANT_NOW.minus(1, ChronoUnit.DAYS),
                    INSTANT_NOW,
                    Optional.of(false),
                    Optional.empty()
                )
            )
            .blockingGet();

        // Then
        assertThat(requireNonNull(output).logs()).isSameAs(expectedData);

        var queryCaptor = ArgumentCaptor.forClass(ApiHealthQueryService.SearchLogsQuery.class);
        verify(apiHealthQueryService).searchLogs(queryCaptor.capture());
        assertThat(queryCaptor.getValue())
            .satisfies(query -> {
                assertSoftly(softly -> {
                    softly.assertThat(query.organizationId()).isEqualTo(ORGANIZATION_ID);
                    softly.assertThat(query.environmentId()).isEqualTo(ENV_ID);
                    softly.assertThat(query.apiId()).isEqualTo(MY_API);
                    softly.assertThat(query.from()).isEqualTo(INSTANT_NOW.minus(1, ChronoUnit.DAYS));
                    softly.assertThat(query.to()).isEqualTo(INSTANT_NOW);
                    softly.assertThat(query.success()).isEqualTo(Optional.of(false));
                    softly.assertThat(query.pageable()).isEqualTo(new PageableImpl(1, 10));
                });
            });
    }

    @Test
    void should_throw_if_no_api_found() {
        // Given

        // When
        var throwable = catchThrowable(() ->
            useCase
                .execute(
                    new SearchHealthCheckLogsUseCase.Input(
                        ORGANIZATION_ID,
                        ENV_ID,
                        MY_API,
                        INSTANT_NOW.minus(1, ChronoUnit.DAYS),
                        INSTANT_NOW,
                        Optional.of(false),
                        Optional.empty()
                    )
                )
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
                    new SearchHealthCheckLogsUseCase.Input(
                        ORGANIZATION_ID,
                        "another",
                        MY_API,
                        INSTANT_NOW.minus(1, ChronoUnit.DAYS),
                        INSTANT_NOW,
                        Optional.of(false),
                        Optional.empty()
                    )
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
                .execute(
                    new SearchHealthCheckLogsUseCase.Input(
                        ORGANIZATION_ID,
                        ENV_ID,
                        MY_API,
                        INSTANT_NOW.minus(1, ChronoUnit.DAYS),
                        INSTANT_NOW,
                        Optional.of(false),
                        Optional.empty()
                    )
                )
                .blockingGet()
        );

        // Then
        assertThat(throwable).isInstanceOf(TcpProxyNotSupportedException.class);
    }
}
