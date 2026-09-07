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
package io.gravitee.apim.core.performance_target.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.ApiFixtures;
import fixtures.core.model.PerformanceTargetFixtures;
import inmemory.ApiCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.PerformanceTargetCrudServiceInMemory;
import io.gravitee.apim.core.performance_target.domain_service.PerformanceTargetScheduleStateDomainService;
import io.gravitee.apim.core.performance_target.domain_service.PerformanceTargetScheduleStateDomainService.State;
import io.gravitee.apim.core.performance_target.domain_service.ValidatePerformanceTargetDomainService;
import io.gravitee.apim.infra.domain_service.analytics_engine.definition.AnalyticsDefinitionYAMLQueryService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdatePerformanceTargetUseCaseTest {

    private static final String TARGET_ID = "target-id";

    ApiCrudServiceInMemory apiCrudService = new ApiCrudServiceInMemory();
    PerformanceTargetCrudServiceInMemory targetCrudService = new PerformanceTargetCrudServiceInMemory();
    PerformanceTargetScheduleStateDomainService scheduleState = new PerformanceTargetScheduleStateDomainService();

    UpdatePerformanceTargetUseCase useCase = new UpdatePerformanceTargetUseCase(
        targetCrudService,
        new ValidatePerformanceTargetDomainService(apiCrudService, new AnalyticsDefinitionYAMLQueryService()),
        scheduleState
    );

    @BeforeEach
    void setUp() {
        apiCrudService.initWith(List.of(ApiFixtures.anA2AProxyApiV4().toBuilder().id(PerformanceTargetFixtures.A2A_API_ID).build()));
        targetCrudService.initWith(List.of(PerformanceTargetFixtures.aTarget(TARGET_ID)));
    }

    @AfterEach
    void tearDown() {
        Stream.of(apiCrudService, targetCrudService).forEach(InMemoryAlternative::reset);
    }

    @Test
    void should_replace_the_schedule_and_rules_and_start_a_fresh_scheduler_state() {
        scheduleState.stateOf(TARGET_ID, () -> new State(Instant.parse("2021-06-01T10:00:00Z"), 6));
        var redefined = PerformanceTargetFixtures.aTarget(TARGET_ID).toBuilder().interval(Duration.ofMinutes(1)).build();

        var output = useCase.execute(
            new UpdatePerformanceTargetUseCase.Input(PerformanceTargetFixtures.ENVIRONMENT_ID, TARGET_ID, redefined)
        );

        assertThat(output.target().interval()).isEqualTo(Duration.ofMinutes(1));
        assertThat(targetCrudService.storage())
            .singleElement()
            .extracting(t -> t.interval())
            .isEqualTo(Duration.ofMinutes(1));
        assertThat(scheduleState.current(TARGET_ID)).contains(State.FRESH);
    }
}
