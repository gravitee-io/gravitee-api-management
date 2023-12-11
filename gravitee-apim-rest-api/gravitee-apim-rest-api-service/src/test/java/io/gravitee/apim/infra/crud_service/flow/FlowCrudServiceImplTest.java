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
package io.gravitee.apim.infra.crud_service.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FlowCrudServiceImplTest {

    public static final String PLAN_ID = "plan-id";
    FlowRepository flowRepository;

    FlowCrudServiceImpl service;

    @BeforeEach
    void setUp() {
        flowRepository = mock(FlowRepository.class);

        service = new FlowCrudServiceImpl(flowRepository);
    }

    @Nested
    class Save {

        @Test
        @SneakyThrows
        void should_delete_existing_flows() {
            // Given

            // When
            service.savePlanFlows(PLAN_ID, List.of(Flow.builder().build()));

            // Then
            verify(flowRepository).deleteByReference(FlowReferenceType.PLAN, PLAN_ID);
        }

        @Test
        @SneakyThrows
        void should_save_flows() {
            // Given
            when(flowRepository.create(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
            var flows = List.of(Flow.builder().build());

            // When
            var result = service.savePlanFlows(PLAN_ID, flows);

            // Then
            assertThat(result).isEqualTo(flows);
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            when(flowRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.savePlanFlows(PLAN_ID, List.of(Flow.builder().build())));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to save flows for PLAN: plan-id");
        }
    }
}
