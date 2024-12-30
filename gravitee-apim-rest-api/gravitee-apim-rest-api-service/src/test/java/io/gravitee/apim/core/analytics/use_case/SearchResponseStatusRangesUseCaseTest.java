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
package io.gravitee.apim.core.analytics.use_case;

import static fixtures.core.model.ApiFixtures.MY_API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.model.AnalyticsQueryParameters;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SearchResponseStatusRangesUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ENV_ID = "environment-id";

    private final FakeAnalyticsQueryService analyticsQueryService = Mockito.spy(new FakeAnalyticsQueryService());
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private SearchResponseStatusRangesUseCase useCase;

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
        useCase = new SearchResponseStatusRangesUseCase(analyticsQueryService, apiCrudService);
    }

    @AfterEach
    void tearDown() {
        apiCrudService.reset();
        analyticsQueryService.reset();
    }

    @Test
    void should_throw_exception_if_api_is_tcp() {
        //Given
        apiCrudService.initWith(List.of(ApiFixtures.aTcpApiV4()));

        //When
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchResponseStatusRangesUseCase.Input(
                    MY_API,
                    ENV_ID,
                    TimeProvider.instantNow().minus(Duration.ofDays(1)).toEpochMilli(),
                    TimeProvider.instantNow().toEpochMilli()
                )
            )
        );

        // Then
        assertThat(throwable).isInstanceOf(TcpProxyNotSupportedException.class);
    }

    @Test
    void should_throw_if_api_doesnt_belong_to_environment() {
        //Given
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId("other-env").build()));

        //When
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchResponseStatusRangesUseCase.Input(
                    MY_API,
                    ENV_ID,
                    TimeProvider.instantNow().minus(Duration.ofDays(1)).toEpochMilli(),
                    TimeProvider.instantNow().toEpochMilli()
                )
            )
        );

        // Then
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_definition_is_not_v4() {
        //Given
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2()));

        //When
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchResponseStatusRangesUseCase.Input(
                    MY_API,
                    ENV_ID,
                    TimeProvider.instantNow().minus(Duration.ofDays(1)).toEpochMilli(),
                    TimeProvider.instantNow().toEpochMilli()
                )
            )
        );

        // Then
        assertThat(throwable).isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_throw_if_api_is_not_found() {
        //When
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchResponseStatusRangesUseCase.Input(
                    MY_API,
                    ENV_ID,
                    TimeProvider.instantNow().minus(Duration.ofDays(1)).toEpochMilli(),
                    TimeProvider.instantNow().toEpochMilli()
                )
            )
        );

        // Then
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_dates_are_invalid() {
        //When
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchResponseStatusRangesUseCase.Input(
                    MY_API,
                    ENV_ID,
                    TimeProvider.instantNow().toEpochMilli(),
                    TimeProvider.instantNow().minus(Duration.ofDays(1)).toEpochMilli()
                )
            )
        );

        // Then
        assertThat(throwable).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_return_status_ranges_for_specified_date_range() {
        //Given
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        var expectedResponseStatusRanges = ResponseStatusRanges
            .builder()
            .ranges(Map.of("100.0-200.0", 1L, "200.0-300.0", 17L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L))
            .build();
        analyticsQueryService.responseStatusRanges = expectedResponseStatusRanges;

        //When
        var result = useCase.execute(
            GraviteeContext.getExecutionContext(),
            new SearchResponseStatusRangesUseCase.Input(
                MY_API,
                ENV_ID,
                TimeProvider.instantNow().minus(Duration.ofDays(3)).toEpochMilli(),
                TimeProvider.instantNow().minus(Duration.ofDays(1)).toEpochMilli()
            )
        );

        // Then
        assertThat(result.responseStatusRanges()).hasValue(expectedResponseStatusRanges);

        var queryCaptor = ArgumentCaptor.forClass(AnalyticsQueryParameters.class);
        verify(analyticsQueryService).searchResponseStatusRanges(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue())
            .satisfies(query ->
                assertSoftly(softly -> {
                    softly.assertThat(query.getApiIds()).isEqualTo(List.of(MY_API));
                    softly.assertThat(query.getFrom()).isEqualTo(INSTANT_NOW.minus(3, ChronoUnit.DAYS).toEpochMilli());
                    softly.assertThat(query.getTo()).isEqualTo(INSTANT_NOW.minus(1, ChronoUnit.DAYS).toEpochMilli());
                })
            );
    }

    @Test
    void should_return_status_ranges_for_default_date_range() {
        //Given
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        var expectedResponseStatusRanges = ResponseStatusRanges
            .builder()
            .ranges(Map.of("100.0-200.0", 1L, "200.0-300.0", 17L, "300.0-400.0", 0L, "400.0-500.0", 0L, "500.0-600.0", 0L))
            .build();
        analyticsQueryService.responseStatusRanges = expectedResponseStatusRanges;

        //When
        var result = useCase.execute(
            GraviteeContext.getExecutionContext(),
            new SearchResponseStatusRangesUseCase.Input(MY_API, ENV_ID, null, null)
        );

        // Then
        assertThat(result.responseStatusRanges()).hasValue(expectedResponseStatusRanges);

        var queryCaptor = ArgumentCaptor.forClass(AnalyticsQueryParameters.class);
        verify(analyticsQueryService).searchResponseStatusRanges(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue())
            .satisfies(query ->
                assertSoftly(softly -> {
                    softly.assertThat(query.getApiIds()).isEqualTo(List.of(MY_API));
                    softly.assertThat(query.getFrom()).isEqualTo(INSTANT_NOW.minus(1, ChronoUnit.DAYS).toEpochMilli());
                    softly.assertThat(query.getTo()).isEqualTo(INSTANT_NOW.toEpochMilli());
                })
            );
    }
}
