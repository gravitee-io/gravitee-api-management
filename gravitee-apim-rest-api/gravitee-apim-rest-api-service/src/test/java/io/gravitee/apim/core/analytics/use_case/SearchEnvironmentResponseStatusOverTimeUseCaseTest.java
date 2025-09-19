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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import fakes.FakeAnalyticsQueryService;
import fixtures.core.model.ApiFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.query_service.AnalyticsQueryService;
import io.gravitee.apim.core.analytics.use_case.SearchEnvironmentResponseStatusOverTimeUseCase.Input;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SearchEnvironmentResponseStatusOverTimeUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String ENV_ID = "environment-id";

    private final FakeAnalyticsQueryService analyticsQueryService = Mockito.spy(new FakeAnalyticsQueryService());
    private final ApiQueryServiceInMemory apiQueryServiceInMemory = new ApiQueryServiceInMemory();
    private SearchEnvironmentResponseStatusOverTimeUseCase useCase;

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
        useCase = new SearchEnvironmentResponseStatusOverTimeUseCase(analyticsQueryService, apiQueryServiceInMemory);
    }

    @AfterEach
    void tearDown() {
        apiQueryServiceInMemory.reset();
        analyticsQueryService.reset();
    }

    @Test
    void should_return_latest_24_hours_data() {
        // Given
        apiQueryServiceInMemory.initWith(List.of(ApiFixtures.aMessageApiV4().setId(MY_API)));
        var expectedData = new ResponseStatusOvertime();
        analyticsQueryService.responseStatusOvertime = expectedData;

        // When
        var output = useCase.execute(
            GraviteeContext.getExecutionContext(),
            new Input(ENV_ID, TimeProvider.instantNow().minus(Duration.ofDays(1)), TimeProvider.instantNow())
        );

        // Then
        assertThat(output.responseStatusOvertime()).isSameAs(expectedData);

        var queryCaptor = ArgumentCaptor.forClass(AnalyticsQueryService.ResponseStatusOverTimeQuery.class);
        verify(analyticsQueryService).searchResponseStatusOvertime(any(), queryCaptor.capture());
        assertThat(queryCaptor.getValue()).satisfies(query -> {
            assertSoftly(softly -> {
                softly.assertThat(query.apiIds()).isEqualTo(List.of(MY_API));
                softly.assertThat(query.from()).isEqualTo(INSTANT_NOW.minus(1, ChronoUnit.DAYS));
                softly.assertThat(query.to()).isEqualTo(INSTANT_NOW);
                softly.assertThat(query.interval()).isEqualTo(Duration.ofMinutes(10));
            });
        });
    }
}
