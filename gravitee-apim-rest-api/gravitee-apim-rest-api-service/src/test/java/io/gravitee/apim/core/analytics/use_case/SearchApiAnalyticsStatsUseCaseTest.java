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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsStatsUseCase.Input;
import io.gravitee.apim.core.analytics.use_case.SearchApiAnalyticsStatsUseCase.Output;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsField;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsStatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsStatsQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchApiAnalyticsStatsUseCaseTest {

    private static final class TrackingFakeAnalyticsQueryService extends FakeAnalyticsQueryService {

        int searchApiAnalyticsStatsCalls;

        @Override
        public Optional<ApiAnalyticsStatsAggregate> searchApiAnalyticsStats(
            ExecutionContext executionContext,
            ApiAnalyticsStatsQuery query
        ) {
            searchApiAnalyticsStatsCalls++;
            return super.searchApiAnalyticsStats(executionContext, query);
        }
    }

    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT_NOW;

    private TrackingFakeAnalyticsQueryService analyticsQueryService;
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchApiAnalyticsStatsUseCase cut;

    @BeforeEach
    void setUp() {
        analyticsQueryService = new TrackingFakeAnalyticsQueryService();
        cut = new SearchApiAnalyticsStatsUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    @Test
    void should_throw_if_no_api_does_not_belong_to_current_environment() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4WithAnalyticsEnabled()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, "another-environment", Optional.of(FROM), Optional.of(TO), Optional.of(ApiAnalyticsField.STATUS))
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_no_api_found() {
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO), Optional.of(ApiAnalyticsField.STATUS))
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_definition_not_v4() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO), Optional.of(ApiAnalyticsField.STATUS))
                )
            )
            .isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_throw_if_api_is_tcp() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO), Optional.of(ApiAnalyticsField.STATUS))
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Analytics are not supported for TCP Proxy APIs");
    }

    @Test
    void should_return_empty_when_analytics_disabled_without_querying_repository() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        final Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO), Optional.of(ApiAnalyticsField.STATUS))
        );
        assertThat(result.aggregate()).isEmpty();
        assertThat(analyticsQueryService.searchApiAnalyticsStatsCalls).isZero();
    }

    @Test
    void should_not_find_stats_when_repository_empty() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4WithAnalyticsEnabled()));
        analyticsQueryService.apiAnalyticsStats = null;
        final Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO), Optional.of(ApiAnalyticsField.GATEWAY_RESPONSE_TIME_MS))
        );
        assertThat(result.aggregate()).isEmpty();
        assertThat(analyticsQueryService.searchApiAnalyticsStatsCalls).isEqualTo(1);
    }

    @Test
    void should_get_stats_for_a_v4_api_with_analytics_enabled() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4WithAnalyticsEnabled()));
        analyticsQueryService.apiAnalyticsStats =
            ApiAnalyticsStatsAggregate.builder().count(10).min(1.0).max(9.0).avg(5.0).sum(50.0).build();
        final Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO), Optional.of(ApiAnalyticsField.STATUS))
        );
        assertThat(result.aggregate())
            .hasValueSatisfying(agg -> {
                assertThat(agg.getCount()).isEqualTo(10);
                assertThat(agg.getAvg()).isEqualTo(5.0);
            });
    }
}
