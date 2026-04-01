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
import io.gravitee.apim.core.analytics.model.AnalyticsStatsResponse;
import io.gravitee.apim.core.analytics.use_case.SearchAnalyticsStatsUseCase.Input;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
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
class SearchAnalyticsStatsUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT_NOW;
    private static final String FIELD = "gateway-response-time-ms";

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
    void should_throw_if_api_not_found() {
        assertThatThrownBy(() ->
                cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, FIELD, Optional.of(FROM), Optional.of(TO)))
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_not_v4() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
        assertThatThrownBy(() ->
                cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, FIELD, Optional.of(FROM), Optional.of(TO)))
            )
            .isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_throw_if_api_is_tcp() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4()));
        assertThatThrownBy(() ->
                cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, FIELD, Optional.of(FROM), Optional.of(TO)))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Analytics are not supported for TCP Proxy APIs");
    }

    @Test
    void should_throw_if_api_not_in_environment() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, "another-environment", FIELD, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_field_is_null() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(GraviteeContext.getExecutionContext(), new Input(MY_API, ENV_ID, null, Optional.of(FROM), Optional.of(TO)))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("field is required for STATS analytics");
    }

    @Test
    void should_throw_if_field_not_in_allowlist() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "unsupported-field", Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported field for STATS analytics");
    }

    @Test
    void should_return_stats_for_supported_field() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.stats = new AnalyticsStatsResponse(10L, 5.0, 500.0, 100.0, 1000.0);

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, FIELD, Optional.of(FROM), Optional.of(TO))
        );

        assertThat(result.stats()).isPresent();
        assertThat(result.stats().get().count()).isEqualTo(10L);
        assertThat(result.stats().get().min()).isEqualTo(5.0);
        assertThat(result.stats().get().max()).isEqualTo(500.0);
        assertThat(result.stats().get().avg()).isEqualTo(100.0);
        assertThat(result.stats().get().sum()).isEqualTo(1000.0);
    }

    @Test
    void should_return_empty_when_no_data() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.stats = null;

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, FIELD, Optional.of(FROM), Optional.of(TO))
        );

        assertThat(result.stats()).isEmpty();
    }
}
