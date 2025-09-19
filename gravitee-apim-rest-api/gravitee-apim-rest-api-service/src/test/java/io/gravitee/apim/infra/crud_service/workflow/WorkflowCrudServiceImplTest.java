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
package io.gravitee.apim.infra.crud_service.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.WorkflowRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WorkflowCrudServiceImplTest {

    WorkflowRepository workflowRepository;

    WorkflowCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        workflowRepository = mock(WorkflowRepository.class);

        service = new WorkflowCrudServiceImpl(workflowRepository);
    }

    @Nested
    class Create {

        @Test
        @SneakyThrows
        void should_create_a_workflow() {
            var config = aWorkflow();
            service.create(config);

            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.Workflow.class);
            verify(workflowRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.Workflow.builder()
                        .id("workflow-id")
                        .referenceType("API")
                        .referenceId("api-id")
                        .type("REVIEW")
                        .state("DRAFT")
                        .user("user-id")
                        .comment("my-comment")
                        .createdAt(Date.from(Instant.parse("2020-02-01T20:22:02.00Z")))
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_the_created_workflow() {
            when(workflowRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            var toCreate = aWorkflow();
            var result = service.create(toCreate);

            assertThat(result).isEqualTo(toCreate);
        }

        @Test
        void should_throw_when_technical_exception_occurs() throws TechnicalException {
            // Given
            when(workflowRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.create(aWorkflow()));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to create the REVIEW workflow of [apiId=api-id]");
        }
    }

    private static Workflow aWorkflow() {
        return Workflow.builder()
            .id("workflow-id")
            .referenceType(Workflow.ReferenceType.API)
            .referenceId("api-id")
            .type(Workflow.Type.REVIEW)
            .state(Workflow.State.DRAFT)
            .user("user-id")
            .comment("my-comment")
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .build();
    }
}
