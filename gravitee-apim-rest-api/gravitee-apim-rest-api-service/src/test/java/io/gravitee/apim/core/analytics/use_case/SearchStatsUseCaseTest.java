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

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.exception.IllegalTimeRangeException;
import io.gravitee.apim.core.analytics.model.StatsAnalytics;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchStatsUseCaseTest {

    private final ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private SearchStatsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SearchStatsUseCase(apiCrudService, analyticsQueryService);
    }

    @AfterEach
    void tearDown() {
        apiCrudService.reset();
        analyticsQueryService.reset();
    }

    @Test
    void should_return_stats_analytics_when_valid() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        GraviteeContext.setCurrentEnvironment("environment-id");
        analyticsQueryService.statsAnalytics = new StatsAnalytics(1L, 2L, 3L, 4L, 5, 6L, 7L, 8L);

        var input = new SearchStatsUseCase.Input(
            MY_API,
            Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
            Instant.now().toEpochMilli(),
            "field",
            Optional.empty()
        );
        var output = useCase.execute(GraviteeContext.getExecutionContext(), input);

        assertThat(output.analytics()).isEqualTo(analyticsQueryService.statsAnalytics);
    }

    @Test
    void should_return_null_when_no_stats_found() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        GraviteeContext.setCurrentEnvironment("environment-id");
        analyticsQueryService.statsAnalytics = null;

        var input = new SearchStatsUseCase.Input(
            MY_API,
            Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
            Instant.now().toEpochMilli(),
            "field",
            Optional.empty()
        );
        var output = useCase.execute(GraviteeContext.getExecutionContext(), input);

        assertThat(output.analytics()).isNull();
    }

    @Test
    void should_throw_exception_for_invalid_time_range() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        GraviteeContext.setCurrentEnvironment("environment-id");
        var input = new SearchStatsUseCase.Input(
            MY_API,
            Instant.now().toEpochMilli(),
            Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
            "field",
            Optional.empty()
        );
        var throwable = catchThrowable(() -> useCase.execute(GraviteeContext.getExecutionContext(), input));
        assertThat(throwable).isInstanceOf(IllegalTimeRangeException.class);
    }

    @Test
    void should_throw_exception_for_non_v4_api() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV2()));
        GraviteeContext.setCurrentEnvironment("environment-id");
        var input = new SearchStatsUseCase.Input(
            MY_API,
            Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
            Instant.now().toEpochMilli(),
            "field",
            Optional.empty()
        );
        var throwable = catchThrowable(() -> useCase.execute(GraviteeContext.getExecutionContext(), input));
        assertThat(throwable).isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_throw_exception_for_tcp_proxy_api() {
        apiCrudService.initWith(List.of(ApiFixtures.aTcpApiV4()));
        GraviteeContext.setCurrentEnvironment("environment-id");
        var input = new SearchStatsUseCase.Input(
            MY_API,
            Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
            Instant.now().toEpochMilli(),
            "field",
            Optional.empty()
        );
        var throwable = catchThrowable(() -> useCase.execute(GraviteeContext.getExecutionContext(), input));
        assertThat(throwable).isInstanceOf(TcpProxyNotSupportedException.class);
    }

    @Test
    void should_throw_exception_for_wrong_environment() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4().toBuilder().environmentId("other-env").build()));
        GraviteeContext.setCurrentEnvironment("environment-id");
        var input = new SearchStatsUseCase.Input(
            MY_API,
            Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
            Instant.now().toEpochMilli(),
            "field",
            Optional.empty()
        );
        var throwable = catchThrowable(() -> useCase.execute(GraviteeContext.getExecutionContext(), input));
        assertThat(throwable).isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_pass_query_argument_to_stats_query() {
        apiCrudService.initWith(List.of(ApiFixtures.aProxyApiV4()));
        GraviteeContext.setCurrentEnvironment("environment-id");
        var queryString = "status:200 AND method:GET";
        analyticsQueryService.statsAnalytics = new StatsAnalytics(1L, 2L, 3L, 4L, 5, 6L, 7L, 8L);
        var input = new SearchStatsUseCase.Input(
            MY_API,
            Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli(),
            Instant.now().toEpochMilli(),
            "field",
            Optional.of(queryString)
        );
        var result = useCase.execute(GraviteeContext.getExecutionContext(), input);
        assertThat(result.analytics()).isNotNull();
    }
}
