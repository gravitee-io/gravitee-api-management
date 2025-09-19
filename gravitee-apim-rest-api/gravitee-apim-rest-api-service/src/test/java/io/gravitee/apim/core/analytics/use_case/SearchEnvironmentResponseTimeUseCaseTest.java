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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import fakes.FakeAnalyticsQueryService;
import inmemory.ApiQueryServiceInMemory;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchEnvironmentResponseTimeUseCaseTest {

    public static final String ENV_ID = "environment-id";
    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();

    SearchEnvironmentResponseTimeOverTimeUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new SearchEnvironmentResponseTimeOverTimeUseCase(analyticsQueryService, apiQueryService);
    }

    @AfterEach
    void tearDown() {
        apiQueryService.reset();
    }

    @Test
    void nominal_case() {
        // Given
        var input = new SearchEnvironmentResponseTimeOverTimeUseCase.Input(ENV_ID, Instant.now().minus(Duration.ofDays(1)), Instant.now());
        var definition = io.gravitee.definition.model.v4.Api.builder()
            .type(ApiType.MESSAGE)
            .definitionVersion(DefinitionVersion.V4)
            .build();
        apiQueryService.initWith(List.of(Api.builder().id("MyApiID").apiDefinitionHttpV4(definition).environmentId(ENV_ID).build()));
        // the order of keys is important
        analyticsQueryService.averageAggregate = new LinkedHashMap<>();
        analyticsQueryService.averageAggregate.put("1970-01-01T00:00:00", 1.2D);
        analyticsQueryService.averageAggregate.put("1970-01-01T00:30:00", 1.6D);

        // When
        var output = cut.execute(new ExecutionContext(), input).blockingGet();

        // Then
        assertThat(output.data()).containsExactly(1L, 2L);
        assertThat(Duration.between(output.from(), output.to())).isCloseTo(Duration.ofDays(1), Duration.ofSeconds(1));
        assertThat(output.interval()).isEqualTo(Duration.ofMinutes(10));
        assertThat(output.to()).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
    }
}
