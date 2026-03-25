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
import io.gravitee.apim.core.analytics.use_case.SearchAnalyticsStatsUseCase.Input;
import io.gravitee.apim.core.analytics.use_case.SearchAnalyticsStatsUseCase.Output;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.rest.api.model.v4.analytics.StatsResult;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAnalyticsStatsUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT_NOW;

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchAnalyticsStatsUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchAnalyticsStatsUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    @Test
    void should_throw_if_field_is_null() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() -> cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, null, FROM, TO)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid field");
    }

    @Test
    void should_throw_if_field_is_not_allowed() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, "invalid-field", FROM, TO))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid field");
    }

    @Test
    void should_throw_if_api_not_found() {
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "gateway-response-time-ms", FROM, TO)
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
                    new Input(MY_API, ENV_ID, "gateway-response-time-ms", FROM, TO)
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
                    new Input(MY_API, ENV_ID, "gateway-response-time-ms", FROM, TO)
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Analytics are not supported for TCP Proxy APIs");
    }

    @Test
    void should_return_default_stats_when_no_data() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.statsResult = null;
        final Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, "gateway-response-time-ms", FROM, TO)
        );
        assertThat(result.stats().getCount()).isEqualTo(0L);
        assertThat(result.stats().getAvg()).isEqualTo(0.0);
    }

    @Test
    void should_return_stats_for_valid_field() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.statsResult = StatsResult.builder().count(100).min(1.0).max(500.0).avg(42.5).sum(4250.0).build();
        final Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, "gateway-response-time-ms", FROM, TO)
        );
        assertThat(result.stats().getCount()).isEqualTo(100L);
        assertThat(result.stats().getMin()).isEqualTo(1.0);
        assertThat(result.stats().getMax()).isEqualTo(500.0);
        assertThat(result.stats().getAvg()).isEqualTo(42.5);
        assertThat(result.stats().getSum()).isEqualTo(4250.0);
    }

    @Test
    void should_accept_endpoint_response_time_field() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.statsResult = StatsResult.builder().count(10).min(0).max(100).avg(50).sum(500).build();
        final Output result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, "endpoint-response-time-ms", FROM, TO)
        );
        assertThat(result.stats().getCount()).isEqualTo(10L);
    }
}
