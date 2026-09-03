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
package io.gravitee.apim.infra.crud_service.performance_target;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PerformanceTargetRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Duration;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerformanceTargetCrudServiceImplTest {

    PerformanceTargetRepository repository;
    PerformanceTargetCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PerformanceTargetRepository.class);
        service = new PerformanceTargetCrudServiceImpl(repository);
    }

    @Test
    @SneakyThrows
    void should_create_a_target_and_return_what_was_stored() {
        when(repository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
        var target = aTarget();

        var created = service.create(target);

        assertThat(created).isEqualTo(target);
    }

    @Test
    @SneakyThrows
    void should_return_touched_target_ids_when_removing_an_api() {
        when(repository.removeApiId("api-id")).thenReturn(List.of("target-1", "target-2"));

        assertThat(service.removeApiId("api-id")).containsExactly("target-1", "target-2");
    }

    @Test
    @SneakyThrows
    void should_wrap_technical_exceptions() {
        when(repository.create(any())).thenThrow(TechnicalException.class);
        when(repository.removeApiId(any())).thenThrow(TechnicalException.class);

        assertThatThrownBy(() -> service.create(aTarget())).isInstanceOf(TechnicalManagementException.class);
        assertThatThrownBy(() -> service.removeApiId("api-id")).isInstanceOf(TechnicalManagementException.class);
    }

    private static PerformanceTarget aTarget() {
        return PerformanceTarget.builder()
            .id("target-id")
            .environmentId("environment-id")
            .subject(new PerformanceTarget.Subject(List.of("api-id"), "api-id"))
            .window(Duration.ofMinutes(15))
            .interval(Duration.ofMinutes(5))
            .minSampleSize(20)
            .rules(
                List.of(
                    PerformanceTarget.Rule.builder()
                        .metric(MetricSpec.Name.HTTP_ERROR_RATE)
                        .measure(MetricSpec.Measure.PERCENTAGE)
                        .operator(PerformanceTarget.Operator.LTE)
                        .threshold(5)
                        .build()
                )
            )
            .build();
    }
}
