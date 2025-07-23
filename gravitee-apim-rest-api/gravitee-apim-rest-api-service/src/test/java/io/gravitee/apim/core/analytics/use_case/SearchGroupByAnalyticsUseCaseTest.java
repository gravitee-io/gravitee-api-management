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
import io.gravitee.apim.core.analytics.domain_service.AnalyticsMetadataProvider;
import io.gravitee.apim.core.analytics.exception.IllegalTimeRangeException;
import io.gravitee.apim.core.analytics.model.GroupByAnalytics;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.common.utils.TimeProvider;
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

class SearchGroupByAnalyticsUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ENV_ID = "environment-id";

    private final FakeAnalyticsQueryService analyticsQueryService = Mockito.spy(new FakeAnalyticsQueryService());
    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private SearchGroupByAnalyticsUseCase useCase;

    private final AnalyticsMetadataProvider provider = new AnalyticsMetadataProvider() {
        @Override
        public boolean appliesTo(Field field) {
            return field == Field.API;
        }

        @Override
        public Map<String, String> provide(String key, String environmentId) {
            return Map.of("name", "api-" + key);
        }
    };

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
        useCase = new SearchGroupByAnalyticsUseCase(apiCrudService, analyticsQueryService, List.of(provider));
    }

    @AfterEach
    void tearDown() {
        apiCrudService.reset();
        analyticsQueryService.reset();
    }

    @Test
    void should_throw_exception_if_api_is_tcp() {
        apiCrudService.initWith(List.of(ApiFixtures.aTcpApiV4()));
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchGroupByAnalyticsUseCase.Input(
                    MY_API,
                    INSTANT_NOW.minus(Duration.ofDays(1)).toEpochMilli(),
                    INSTANT_NOW.toEpochMilli(),
                    "api-id",
                    null,
                    null,
                    null
                )
            )
        );
        assertThat(throwable).isInstanceOf(TcpProxyNotSupportedException.class);
    }

    @Test
    void should_throw_if_api_doesnt_belong_to_environment() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId("other-env").build()));
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchGroupByAnalyticsUseCase.Input(
                    MY_API,
                    INSTANT_NOW.minus(Duration.ofDays(1)).toEpochMilli(),
                    INSTANT_NOW.toEpochMilli(),
                    "api-id",
                    null,
                    null,
                    null
                )
            )
        );
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_definition_is_not_v4() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2()));
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchGroupByAnalyticsUseCase.Input(
                    MY_API,
                    INSTANT_NOW.minus(Duration.ofDays(1)).toEpochMilli(),
                    INSTANT_NOW.toEpochMilli(),
                    "api-id",
                    null,
                    null,
                    null
                )
            )
        );
        assertThat(throwable).isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_throw_if_api_is_not_found() {
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchGroupByAnalyticsUseCase.Input(
                    MY_API,
                    INSTANT_NOW.minus(Duration.ofDays(1)).toEpochMilli(),
                    INSTANT_NOW.toEpochMilli(),
                    "api-id",
                    null,
                    null,
                    null
                )
            )
        );
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_dates_are_invalid() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        var throwable = catchThrowable(() ->
            useCase.execute(
                GraviteeContext.getExecutionContext(),
                new SearchGroupByAnalyticsUseCase.Input(
                    MY_API,
                    INSTANT_NOW.toEpochMilli(),
                    INSTANT_NOW.minus(Duration.ofDays(1)).toEpochMilli(),
                    "api-id",
                    null,
                    null,
                    null
                )
            )
        );
        assertThat(throwable).isInstanceOf(IllegalTimeRangeException.class);
    }

    @Test
    void should_return_group_by_analytics_and_metadata_for_specified_date_range() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        var values = Map.of("k1", 10L, "k2", 20L);
        analyticsQueryService.groupByAnalytics = new GroupByAnalytics();
        analyticsQueryService.groupByAnalytics.setValues(values);

        var result = useCase.execute(
            GraviteeContext.getExecutionContext(),
            new SearchGroupByAnalyticsUseCase.Input(
                MY_API,
                INSTANT_NOW.minus(3, ChronoUnit.DAYS).toEpochMilli(),
                INSTANT_NOW.minus(1, ChronoUnit.DAYS).toEpochMilli(),
                "api-id",
                null,
                null,
                null
            )
        );

        assertThat(result.analytics()).isEqualTo(analyticsQueryService.groupByAnalytics);
        assertThat(result.metadata()).containsKeys("k1", "k2");
        assertThat(result.metadata().get("k1")).containsEntry("name", "api-k1");
        assertThat(result.metadata().get("k2")).containsEntry("name", "api-k2");

        var queryCaptor = ArgumentCaptor.forClass(io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService.GroupByQuery.class);
        verify(analyticsQueryService).searchGroupByAnalytics(any(), queryCaptor.capture());
        assertSoftly(softly -> {
            softly.assertThat(queryCaptor.getValue().apiId()).isEqualTo(MY_API);
            softly.assertThat(queryCaptor.getValue().from()).isEqualTo(INSTANT_NOW.minus(3, ChronoUnit.DAYS));
            softly.assertThat(queryCaptor.getValue().to()).isEqualTo(INSTANT_NOW.minus(1, ChronoUnit.DAYS));
            softly.assertThat(queryCaptor.getValue().field()).isEqualTo("api-id");
        });
    }

    @Test
    void should_propagate_query_parameter_to_group_by_query() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        GraviteeContext.setCurrentEnvironment(ENV_ID);
        String queryString = "status:200 AND method:GET";
        analyticsQueryService.groupByAnalytics = new GroupByAnalytics();

        useCase.execute(
            GraviteeContext.getExecutionContext(),
            new SearchGroupByAnalyticsUseCase.Input(
                MY_API,
                INSTANT_NOW.minus(2, ChronoUnit.DAYS).toEpochMilli(),
                INSTANT_NOW.minus(1, ChronoUnit.DAYS).toEpochMilli(),
                "api-id",
                null,
                null,
                queryString
            )
        );

        var queryCaptor = ArgumentCaptor.forClass(io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService.GroupByQuery.class);
        verify(analyticsQueryService).searchGroupByAnalytics(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue().query()).isEqualTo(queryString);
    }
}
