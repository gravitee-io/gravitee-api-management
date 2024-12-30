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
package io.gravitee.apim.infra.query_service.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.WorkflowRepository;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WorkflowQueryServiceImplTest {

    private static final String API_ID = "api-id";

    WorkflowRepository workflowRepository;

    WorkflowQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);

        service = new WorkflowQueryServiceImpl(workflowRepository);
    }

    @Nested
    class FindAllByApiIdAndType {

        @Test
        @SneakyThrows
        void should_find_workflows() {
            // Given
            when(workflowRepository.findByReferenceAndType(eq("API"), eq(API_ID), eq("REVIEW")))
                .thenReturn(
                    List.of(
                        io.gravitee.repository.management.model.Workflow
                            .builder()
                            .id("workflow-1")
                            .referenceId(API_ID)
                            .referenceType("API")
                            .type("REVIEW")
                            .state("DRAFT")
                            .build(),
                        io.gravitee.repository.management.model.Workflow
                            .builder()
                            .id("workflow-2")
                            .referenceId(API_ID)
                            .referenceType("API")
                            .type("REVIEW")
                            .state("IN_REVIEW")
                            .build()
                    )
                );

            // When
            var results = service.findAllByApiIdAndType(API_ID, Workflow.Type.REVIEW);

            // Then
            assertThat(results)
                .usingRecursiveComparison()
                .isEqualTo(
                    List.of(
                        Workflow
                            .builder()
                            .id("workflow-1")
                            .type(Workflow.Type.REVIEW)
                            .referenceId(API_ID)
                            .referenceType(Workflow.ReferenceType.API)
                            .state(Workflow.State.DRAFT)
                            .build(),
                        Workflow
                            .builder()
                            .id("workflow-2")
                            .type(Workflow.Type.REVIEW)
                            .referenceId(API_ID)
                            .referenceType(Workflow.ReferenceType.API)
                            .state(Workflow.State.IN_REVIEW)
                            .build()
                    )
                );
        }

        @Test
        @SneakyThrows
        void should_return_empty_list() {
            // Given
            when(workflowRepository.findByReferenceAndType(any(), any(), any())).thenReturn(List.of());

            // When
            var results = service.findAllByApiIdAndType(API_ID, Workflow.Type.REVIEW);

            // Then
            assertThat(results).isEmpty();
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(workflowRepository.findByReferenceAndType(any(), any(), any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.findAllByApiIdAndType(API_ID, Workflow.Type.REVIEW));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurred while finding [API] workflows with apiId [api-id] and status [REVIEW]");
        }
    }
}
