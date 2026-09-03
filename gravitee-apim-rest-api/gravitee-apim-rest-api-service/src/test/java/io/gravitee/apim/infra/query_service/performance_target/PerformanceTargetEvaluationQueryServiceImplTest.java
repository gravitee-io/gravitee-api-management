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
package io.gravitee.apim.infra.query_service.performance_target;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.performance_target.model.PerformanceTargetEnvironmentSummary;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PerformanceTargetEvaluationRepository;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PerformanceTargetEvaluationQueryServiceImplTest {

    PerformanceTargetEvaluationRepository repository;
    PerformanceTargetEvaluationQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(PerformanceTargetEvaluationRepository.class);
        service = new PerformanceTargetEvaluationQueryServiceImpl(repository);
    }

    @Test
    @SneakyThrows
    void should_map_latest_evaluations_of_a_reference() {
        when(repository.findLatestByReference("environment-id", "agent-42")).thenReturn(List.of(aRepositoryEvaluation("eval-1")));

        var found = service.findLatestByReference("environment-id", "agent-42");

        assertThat(found)
            .singleElement()
            .satisfies(evaluation -> {
                assertThat(evaluation.id()).isEqualTo("eval-1");
                assertThat(evaluation.status()).isEqualTo(PerformanceTargetEvaluation.Status.PASS);
                assertThat(evaluation.evaluatedAt()).isEqualTo(Instant.parse("2020-02-01T21:00:00Z"));
                assertThat(evaluation.latest()).isTrue();
            });
    }

    @Test
    @SneakyThrows
    void should_page_latest_evaluations_of_an_environment() {
        when(repository.findEnvironmentLatest(eq("environment-id"), any())).thenReturn(
            new Page<>(List.of(aRepositoryEvaluation("eval-1"), aRepositoryEvaluation("eval-2")), 0, 2, 7)
        );

        var page = service.findEnvironmentLatest("environment-id", new PageableImpl(1, 2));

        assertThat(page.getTotalElements()).isEqualTo(7);
        assertThat(page.getContent()).extracting(PerformanceTargetEvaluation::id).containsExactly("eval-1", "eval-2");
    }

    @Test
    @SneakyThrows
    void should_map_environment_summary() {
        when(repository.getEnvironmentSummary("environment-id")).thenReturn(
            new io.gravitee.repository.management.model.PerformanceTargetEnvironmentSummary("environment-id", 4, 2, 1)
        );

        assertThat(service.getEnvironmentSummary("environment-id")).isEqualTo(
            new PerformanceTargetEnvironmentSummary("environment-id", 4, 2, 1)
        );
    }

    @Test
    @SneakyThrows
    void should_wrap_technical_exceptions() {
        when(repository.findLatestByReference(any(), any())).thenThrow(TechnicalException.class);

        assertThatThrownBy(() -> service.findLatestByReference("environment-id", "agent-42")).isInstanceOf(
            TechnicalManagementException.class
        );
    }

    private static io.gravitee.repository.management.model.PerformanceTargetEvaluation aRepositoryEvaluation(String id) {
        return io.gravitee.repository.management.model.PerformanceTargetEvaluation.builder()
            .id(id)
            .targetId("target-id")
            .environmentId("environment-id")
            .reference("agent-42")
            .status(io.gravitee.repository.management.model.PerformanceTargetEvaluation.Status.PASS)
            .windowFrom(Date.from(Instant.parse("2020-02-01T20:00:00Z")))
            .windowTo(Date.from(Instant.parse("2020-02-01T21:00:00Z")))
            .coveredApiIds(List.of("api-id"))
            .evaluatedAt(Date.from(Instant.parse("2020-02-01T21:00:00Z")))
            .latest(true)
            .build();
    }
}
