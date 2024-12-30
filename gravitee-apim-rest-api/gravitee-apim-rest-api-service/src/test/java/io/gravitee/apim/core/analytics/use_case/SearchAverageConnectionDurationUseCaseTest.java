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
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.rest.api.model.v4.analytics.AverageConnectionDuration;
import io.gravitee.rest.api.model.v4.analytics.AverageMessagesPerRequest;
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

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchAverageConnectionDurationUseCaseTest {

    private static final String ENV_ID = "environment-id";
    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final Instant FROM = INSTANT_NOW.minus(1, ChronoUnit.DAYS);
    private static final Instant TO = INSTANT_NOW;

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiCrudServiceInMemory apiCrudServiceInMemory = new ApiCrudServiceInMemory();
    private SearchAverageConnectionDurationUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchAverageConnectionDurationUseCase(analyticsQueryService, apiCrudServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiCrudServiceInMemory.reset();
    }

    @Test
    void should_throw_if_no_api_does_not_belong_to_current_environment() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        assertThatThrownBy(() ->
                cut.execute(
                    null,
                    new SearchAverageConnectionDurationUseCase.Input(MY_API, "another-environment", Optional.of(FROM), Optional.of(TO))
                )
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_no_api_found() {
        assertThatThrownBy(() ->
                cut.execute(null, new SearchAverageConnectionDurationUseCase.Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO)))
            )
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_throw_if_api_definition_not_v4() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aProxyApiV2()));
        assertThatThrownBy(() ->
                cut.execute(null, new SearchAverageConnectionDurationUseCase.Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO)))
            )
            .isInstanceOf(ApiInvalidDefinitionVersionException.class);
    }

    @Test
    void should_throw_if_api_is_tcp() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aTcpApiV4()));
        assertThatThrownBy(() ->
                cut.execute(null, new SearchAverageConnectionDurationUseCase.Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO)))
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Analytics are not supported for TCP Proxy APIs");
    }

    @Test
    void should_not_find_average_messages_per_request() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.averageMessagesPerRequest = new AverageMessagesPerRequest();
        final SearchAverageConnectionDurationUseCase.Output result = cut.execute(
            null,
            new SearchAverageConnectionDurationUseCase.Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO))
        );
        assertThat(result.averageConnectionDuration())
            .hasValueSatisfying(averageConnectionDuration -> {
                assertThat(averageConnectionDuration.getAveragesByEntrypoint()).isNull();
                assertThat(averageConnectionDuration.getGlobalAverage()).isNull();
            });
    }

    @Test
    void should_get_average_connection_duration_for_a_v4_api() {
        apiCrudServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4()));
        analyticsQueryService.averageConnectionDuration =
            AverageConnectionDuration
                .builder()
                .globalAverage(250.0)
                .averagesByEntrypoint(Map.of("http-get", 499.0, "http-post", 1.0))
                .build();
        final SearchAverageConnectionDurationUseCase.Output result = cut.execute(
            null,
            new SearchAverageConnectionDurationUseCase.Input(MY_API, ENV_ID, Optional.of(FROM), Optional.of(TO))
        );
        assertThat(result.averageConnectionDuration())
            .hasValueSatisfying(averageMessagesPerRequest -> {
                assertThat(averageMessagesPerRequest.getGlobalAverage()).isEqualTo(250.0);
                assertThat(averageMessagesPerRequest.getAveragesByEntrypoint()).isEqualTo(Map.of("http-get", 499.0, "http-post", 1.0));
            });
    }
}
