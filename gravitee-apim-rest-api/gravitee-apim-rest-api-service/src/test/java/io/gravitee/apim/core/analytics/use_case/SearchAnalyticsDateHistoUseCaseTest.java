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
import io.gravitee.apim.core.analytics.model.AnalyticsDateHistoResponse;
import io.gravitee.apim.core.analytics.use_case.SearchAnalyticsDateHistoUseCase.Input;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAnalyticsDateHistoUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT_NOW;
    private static final long INTERVAL_MS = 3_600_000L;

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchAnalyticsDateHistoUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchAnalyticsDateHistoUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    @Test
    void should_throw_if_api_not_found() {
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "status", INTERVAL_MS, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_not_v4() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "status", INTERVAL_MS, Optional.of(FROM), Optional.of(TO))
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
                    new Input(MY_API, ENV_ID, "status", INTERVAL_MS, Optional.of(FROM), Optional.of(TO))
                )
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
                    new Input(MY_API, "another-environment", "status", INTERVAL_MS, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_field_is_null() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, null, INTERVAL_MS, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("field is required for DATE_HISTO analytics");
    }

    @Test
    void should_throw_if_field_not_in_allowlist() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "unsupported-field", INTERVAL_MS, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported field for DATE_HISTO analytics");
    }

    @Test
    void should_throw_if_interval_too_small() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "status", 500L, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("interval must be >=");
    }

    @Test
    void should_throw_if_interval_too_large() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    GraviteeContext.getExecutionContext(),
                    new Input(MY_API, ENV_ID, "status", 2_000_000_000L, Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("interval must be >=");
    }

    @Test
    void should_return_date_histo_for_status_field() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.dateHistoResponse =
            new AnalyticsDateHistoResponse(
                List.of(1697932800000L, 1697936400000L),
                List.of(
                    new AnalyticsDateHistoResponse.DateHistoBucket("200", List.of(10L, 20L), Map.of()),
                    new AnalyticsDateHistoResponse.DateHistoBucket("404", List.of(1L, 2L), Map.of())
                )
            );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, "status", INTERVAL_MS, Optional.of(FROM), Optional.of(TO))
        );

        assertThat(result.dateHisto().timestamps()).containsExactly(1697932800000L, 1697936400000L);
        assertThat(result.dateHisto().values()).hasSize(2);
        assertThat(result.dateHisto().values().get(0).field()).isEqualTo("200");
        assertThat(result.dateHisto().values().get(0).buckets()).containsExactly(10L, 20L);
    }

    @Test
    void should_return_date_histo_for_response_time_field() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.dateHistoResponse =
            new AnalyticsDateHistoResponse(
                List.of(1697932800000L, 1697936400000L),
                List.of(new AnalyticsDateHistoResponse.DateHistoBucket("_metric", List.of(150L, 200L), Map.of()))
            );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new Input(MY_API, ENV_ID, "gateway-response-time-ms", INTERVAL_MS, Optional.of(FROM), Optional.of(TO))
        );

        assertThat(result.dateHisto().timestamps()).hasSize(2);
        assertThat(result.dateHisto().values()).hasSize(1);
        assertThat(result.dateHisto().values().get(0).buckets()).containsExactly(150L, 200L);
    }
}
