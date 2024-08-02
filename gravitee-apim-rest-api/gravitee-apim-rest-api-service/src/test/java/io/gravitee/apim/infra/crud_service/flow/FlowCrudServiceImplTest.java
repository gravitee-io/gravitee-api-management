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
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.adapter.FlowAdapter;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FlowCrudServiceImplTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    public static final String API_ID = "api-id";
    public static final String PLAN_ID = "plan-id";
    FlowRepository flowRepository;

    FlowCrudServiceImpl service;

    @BeforeAll
    static void beforeAll() {
        UuidString.overrideGenerator(() -> "generated-id");
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        UuidString.reset();
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        flowRepository = mock(FlowRepository.class);

        service = new FlowCrudServiceImpl(flowRepository);
    }

    @Nested
    class SaveApiFlows {

        @Test
        @SneakyThrows
        void should_delete_existing_flows() {
            // Given

            // When
            service.saveApiFlows(API_ID, List.of());

            // Then
            verify(flowRepository).deleteByReferenceIdAndReferenceType(API_ID, FlowReferenceType.API);
        }

        @Test
        @SneakyThrows
        void should_save_flows() {
            // Given
            var flows = List.of(Flow.builder().name("My flow").enabled(true).build());

            // When
            service.saveApiFlows(API_ID, flows);

            // Then
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.flow.Flow.class);
            verify(flowRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.flow.Flow
                        .builder()
                        .referenceType(FlowReferenceType.API)
                        .referenceId(API_ID)
                        .order(0)
                        .id("generated-id")
                        .enabled(true)
                        .name("My flow")
                        .createdAt(Date.from(INSTANT_NOW))
                        .updatedAt(Date.from(INSTANT_NOW))
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_created_flows() {
            // Given
            when(flowRepository.create(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
            var flows = List.of(Flow.builder().build());

            // When
            var result = service.saveApiFlows(API_ID, flows);

            // Then
            flows.get(0).setId(result.get(0).getId());
            assertThat(result).isEqualTo(flows);
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            when(flowRepository.create(any())).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.saveApiFlows(API_ID, List.of(Flow.builder().build())));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to save flows for API: api-id");
        }
    }

    @Nested
    class SavePlanFlows {

        @Test
        @SneakyThrows
        void should_delete_existing_flows() {
            // Given

            // When
            service.savePlanFlows(PLAN_ID, List.of());

            // Then
            verify(flowRepository).deleteByReferenceIdAndReferenceType(PLAN_ID, FlowReferenceType.PLAN);
            verify(flowRepository, never()).delete(any());
            verify(flowRepository, never()).create(any());
        }

        @Test
        @SneakyThrows
        void should_save_flows() {
            // Given
            var flows = List.of(Flow.builder().name("My flow").enabled(true).build());

            // When
            service.savePlanFlows(PLAN_ID, flows);

            // Then
            var captor = ArgumentCaptor.forClass(io.gravitee.repository.management.model.flow.Flow.class);
            verify(flowRepository).create(captor.capture());

            assertThat(captor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(
                    io.gravitee.repository.management.model.flow.Flow
                        .builder()
                        .referenceType(FlowReferenceType.PLAN)
                        .referenceId(PLAN_ID)
                        .order(0)
                        .id("generated-id")
                        .enabled(true)
                        .name("My flow")
                        .createdAt(Date.from(INSTANT_NOW))
                        .updatedAt(Date.from(INSTANT_NOW))
                        .build()
                );
        }

        @Test
        @SneakyThrows
        void should_return_created_flows() {
            // Given
            when(flowRepository.create(any())).thenAnswer(invocationOnMock -> invocationOnMock.getArgument(0));
            var flows = List.of(Flow.builder().build());

            // When
            var result = service.savePlanFlows(PLAN_ID, flows);
            flows.get(0).setId(result.get(0).getId());

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

        @Test
        void should_delete_one_existing_and_create_new_flow() throws TechnicalException {
            Flow flow1 = new Flow();
            flow1.setId("id1");
            flow1.setName("flow1");
            io.gravitee.repository.management.model.flow.Flow repoFlow1 = FlowAdapter.INSTANCE.toRepository(
                flow1,
                FlowReferenceType.API,
                API_ID,
                0
            );
            repoFlow1.setId(flow1.getId());
            io.gravitee.definition.model.v4.flow.Flow flow2 = new io.gravitee.definition.model.v4.flow.Flow();
            flow2.setName("flow2");

            when(flowRepository.findByReference(any(), eq(API_ID))).thenAnswer(invocation -> List.of(repoFlow1));
            when(flowRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

            List<io.gravitee.definition.model.v4.flow.Flow> flowServiceByReference = service.saveApiFlows(API_ID, List.of(flow2));
            assertThat(flowServiceByReference).isNotNull();
            assertThat(flowServiceByReference.size()).isEqualTo(1);
            assertThat(flowServiceByReference.get(0).getName()).isEqualTo("flow2");

            verify(flowRepository, never()).deleteByReferenceIdAndReferenceType(API_ID, FlowReferenceType.API);
            verify(flowRepository, times(1)).deleteAllById(Set.of(flow1.getId()));
            verify(flowRepository, times(1)).create(any());
        }

        @Test
        void should_update_one_and_create_flow() throws TechnicalException {
            io.gravitee.definition.model.v4.flow.Flow flow1 = new io.gravitee.definition.model.v4.flow.Flow();
            flow1.setId("id1");
            flow1.setName("flow1");
            io.gravitee.repository.management.model.flow.Flow repoFlow1 = FlowAdapter.INSTANCE.toRepository(
                flow1,
                FlowReferenceType.API,
                API_ID,
                0
            );
            repoFlow1.setId(flow1.getId());
            io.gravitee.definition.model.v4.flow.Flow flow2 = new io.gravitee.definition.model.v4.flow.Flow();
            flow2.setName("flow2");

            when(flowRepository.findByReference(any(), eq(API_ID))).thenAnswer(invocation -> List.of(repoFlow1));
            when(flowRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(flowRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

            List<io.gravitee.definition.model.v4.flow.Flow> flowServiceByReference = service.saveApiFlows(API_ID, List.of(flow1, flow2));
            assertThat(flowServiceByReference).isNotNull();
            assertThat(flowServiceByReference.size()).isEqualTo(2);
            assertThat(flowServiceByReference.get(0).getName()).isEqualTo("flow1");
            assertThat(flowServiceByReference.get(1).getName()).isEqualTo("flow2");

            verify(flowRepository, never()).deleteByReferenceIdAndReferenceType(API_ID, FlowReferenceType.API);
            verify(flowRepository, never()).deleteAllById(anySet());
            verify(flowRepository, times(1)).create(any());
            verify(flowRepository, times(1)).update(any());
        }
    }

    @Nested
    class GetApiV4Flows {

        @Test
        @SneakyThrows
        void should_return_flows() {
            // Given
            var repoFlows = List.of(
                io.gravitee.repository.management.model.flow.Flow
                    .builder()
                    .id("flow-id")
                    .referenceType(FlowReferenceType.API)
                    .referenceId(API_ID)
                    .order(0)
                    .name("My flow")
                    .enabled(true)
                    .createdAt(Date.from(INSTANT_NOW))
                    .updatedAt(Date.from(INSTANT_NOW))
                    .build()
            );
            when(flowRepository.findByReference(FlowReferenceType.API, API_ID)).thenReturn(repoFlows);

            // When
            var result = service.getApiV4Flows(API_ID);

            // Then
            assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(List.of(Flow.builder().id("flow-id").name("My flow").enabled(true).build()));
        }

        @Test
        @SneakyThrows
        void should_return_empty_list_when_no_flows() {
            // Given
            when(flowRepository.findByReference(FlowReferenceType.API, API_ID)).thenReturn(List.of());

            // When
            var result = service.getApiV4Flows(API_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            when(flowRepository.findByReference(FlowReferenceType.API, API_ID)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.getApiV4Flows(API_ID));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to get flows for API: api-id");
        }
    }

    @Nested
    class GetPlanV4Flows {

        @Test
        @SneakyThrows
        void should_return_flows() {
            // Given
            var repoFlows = List.of(
                io.gravitee.repository.management.model.flow.Flow
                    .builder()
                    .id("flow-id")
                    .referenceType(FlowReferenceType.PLAN)
                    .referenceId(PLAN_ID)
                    .order(0)
                    .name("My flow")
                    .enabled(true)
                    .createdAt(Date.from(INSTANT_NOW))
                    .updatedAt(Date.from(INSTANT_NOW))
                    .build()
            );
            when(flowRepository.findByReference(FlowReferenceType.PLAN, PLAN_ID)).thenReturn(repoFlows);

            // When
            var result = service.getPlanV4Flows(PLAN_ID);

            // Then
            assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(List.of(Flow.builder().id("flow-id").name("My flow").enabled(true).build()));
        }

        @Test
        @SneakyThrows
        void should_return_empty_list_when_no_flows() {
            // Given
            when(flowRepository.findByReference(FlowReferenceType.PLAN, PLAN_ID)).thenReturn(List.of());

            // When
            var result = service.getPlanV4Flows(PLAN_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            when(flowRepository.findByReference(FlowReferenceType.PLAN, PLAN_ID)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.getPlanV4Flows(PLAN_ID));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to get flows for PLAN: plan-id");
        }
    }

    @Nested
    class GetApiV2Flows {

        @Test
        @SneakyThrows
        void should_return_flows() {
            // Given
            var repoFlows = List.of(
                io.gravitee.repository.management.model.flow.Flow
                    .builder()
                    .id("flow-id")
                    .referenceType(FlowReferenceType.API)
                    .referenceId(API_ID)
                    .order(0)
                    .name("My flow")
                    .enabled(true)
                    .createdAt(Date.from(INSTANT_NOW))
                    .updatedAt(Date.from(INSTANT_NOW))
                    .build()
            );
            when(flowRepository.findByReference(FlowReferenceType.API, API_ID)).thenReturn(repoFlows);

            // When
            var result = service.getApiV2Flows(API_ID);

            // Then
            assertThat(result.get(0)).isNotNull();
            assertThat(result.get(0).getName()).isEqualTo("My flow");
        }

        @Test
        @SneakyThrows
        void should_return_empty_list_when_no_flows() {
            // Given
            when(flowRepository.findByReference(FlowReferenceType.API, API_ID)).thenReturn(List.of());

            // When
            var result = service.getApiV2Flows(API_ID);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_throw_when_technical_exception_occurs() {
            // Given
            when(flowRepository.findByReference(FlowReferenceType.API, API_ID)).thenThrow(TechnicalException.class);

            // When
            Throwable throwable = catchThrowable(() -> service.getApiV2Flows(API_ID));

            // Then
            assertThat(throwable)
                .isInstanceOf(TechnicalDomainException.class)
                .hasMessage("An error occurs while trying to get flows for API: api-id");
        }
    }
}
