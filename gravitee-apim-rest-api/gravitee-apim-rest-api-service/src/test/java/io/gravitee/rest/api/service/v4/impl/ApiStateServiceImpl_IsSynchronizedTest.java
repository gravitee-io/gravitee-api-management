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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.*;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiStateServiceImpl_IsSynchronizedTest {

    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private io.gravitee.rest.api.service.PlanService planService;

    @Mock
    private PlanService planServiceV4;

    @Mock
    private EventService eventService;

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private io.gravitee.rest.api.service.configuration.flow.FlowService flowService;

    @Mock
    private FlowService flowServiceV4;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private ApiValidationService apiValidationService;

    private ApiConverter apiConverter;

    private ApiMapper apiMapper;

    @Mock
    private PlanSearchService planSearchService;

    @InjectMocks
    private SynchronizationService synchronizationService = Mockito.spy(new SynchronizationService(this.objectMapper));

    private ApiStateService apiStateService;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Before
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );

        apiConverter =
            new ApiConverter(
                new GraviteeMapper(),
                planService,
                flowService,
                new CategoryMapper(categoryService),
                parameterService,
                workflowService
            );

        apiMapper =
            new ApiMapper(
                new GraviteeMapper(),
                planServiceV4,
                flowServiceV4,
                parameterService,
                workflowService,
                new CategoryMapper(categoryService)
            );
        GenericApiMapper genericApiMapper = new GenericApiMapper(apiMapper, apiConverter);
        apiStateService =
            new ApiStateServiceImpl(
                apiSearchService,
                apiRepository,
                apiMapper,
                genericApiMapper,
                apiNotificationService,
                primaryOwnerService,
                auditService,
                eventService,
                eventLatestRepository,
                objectMapper,
                apiMetadataService,
                apiValidationService,
                planSearchService,
                apiConverter,
                synchronizationService
            );
        reset(searchEngineService);
    }

    @Test
    public void should_return_true_for_V4_API() throws JsonProcessingException {
        io.gravitee.definition.model.v4.Api apiDefinition = io.gravitee.definition.model.v4.Api
            .builder()
            .id("apiId")
            .name("Api name")
            .definitionVersion(DefinitionVersion.V4)
            .build();
        Api api = new Api();
        api.setId("apiId");
        api.setName("Api name");
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        Event event = new Event();
        event.setType(io.gravitee.repository.management.model.EventType.PUBLISH_API);
        event.setPayload(objectMapper.writeValueAsString(api));

        when(
            eventLatestRepository.search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            )
        )
            .thenReturn(List.of(event));

        ApiEntity apiEntity = apiMapper.toEntity(GraviteeContext.getExecutionContext(), api, null, false);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isTrue();

        verify(eventLatestRepository, times(1))
            .search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            );
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
    }

    @Test
    public void should_return_false_for_V4_API() throws JsonProcessingException {
        io.gravitee.definition.model.v4.Api apiDefinition = io.gravitee.definition.model.v4.Api
            .builder()
            .id("apiId")
            .name("Api name")
            .definitionVersion(DefinitionVersion.V4)
            .build();
        Api api = new Api();
        api.setId("apiId");
        api.setName("Api name");
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        Event event = new Event();
        event.setType(io.gravitee.repository.management.model.EventType.PUBLISH_API);
        event.setPayload(objectMapper.writeValueAsString(api));

        when(
            eventLatestRepository.search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            )
        )
            .thenReturn(List.of(event));

        ApiEntity apiEntity = apiMapper.toEntity(GraviteeContext.getExecutionContext(), api, null, false);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        // Add Flows to make API not synchronized
        List<Flow> apiFlows = List.of(mock(Flow.class), mock(Flow.class));
        apiEntity.setFlows(apiFlows);

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isFalse();

        verify(eventLatestRepository, times(1))
            .search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            );
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
    }

    @Test
    public void should_return_true_for_V2_API() throws JsonProcessingException {
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(List.of(new VirtualHost("/api")));
        io.gravitee.definition.model.Api apiDefinition = io.gravitee.definition.model.Api
            .builder()
            .id("apiId")
            .name("Api name")
            .definitionVersion(DefinitionVersion.V2)
            .plans(Map.of())
            .proxy(proxy)
            .build();
        Api api = new Api();
        api.setId("apiId");
        api.setName("Api name");
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        Event event = new Event();
        event.setType(io.gravitee.repository.management.model.EventType.PUBLISH_API);
        event.setPayload(objectMapper.writeValueAsString(api));

        when(
            eventLatestRepository.search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            )
        )
            .thenReturn(List.of(event));

        io.gravitee.rest.api.model.api.ApiEntity apiEntity = apiConverter.toApiEntity(
            GraviteeContext.getExecutionContext(),
            api,
            null,
            false
        );
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isTrue();

        verify(eventLatestRepository, times(1))
            .search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            );
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
    }

    @Test
    public void should_return_false_for_V2_API() throws JsonProcessingException {
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(List.of(new VirtualHost("/api")));
        io.gravitee.definition.model.Api apiDefinition = io.gravitee.definition.model.Api
            .builder()
            .id("apiId")
            .name("Api name")
            .definitionVersion(DefinitionVersion.V2)
            .plans(Map.of())
            .proxy(proxy)
            .build();
        Api api = new Api();
        api.setId("apiId");
        api.setName("Api name");
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        Event event = new Event();
        event.setType(io.gravitee.repository.management.model.EventType.PUBLISH_API);
        event.setPayload(objectMapper.writeValueAsString(api));

        when(
            eventLatestRepository.search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            )
        )
            .thenReturn(List.of(event));

        io.gravitee.rest.api.model.api.ApiEntity apiEntity = apiConverter.toApiEntity(
            GraviteeContext.getExecutionContext(),
            api,
            null,
            false
        );
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        // Add Flows to make API not synchronized
        io.gravitee.definition.model.flow.Flow flow = new io.gravitee.definition.model.flow.Flow();
        apiEntity.setFlows(Collections.singletonList(flow));

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isFalse();

        verify(eventLatestRepository, times(1))
            .search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            );
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
    }

    @Test
    public void should_return_true_when_no_plan_change() throws JsonProcessingException {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        io.gravitee.definition.model.v4.Api apiDefinition = io.gravitee.definition.model.v4.Api
            .builder()
            .id("apiId")
            .name("Api name")
            .definitionVersion(DefinitionVersion.V4)
            .build();
        Api api = new Api();
        api.setId("apiId");
        api.setName("Api name");
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));
        api.setDeployedAt(nowDate);

        Event event = new Event();
        event.setType(io.gravitee.repository.management.model.EventType.PUBLISH_API);
        event.setPayload(objectMapper.writeValueAsString(api));

        when(
            eventLatestRepository.search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            )
        )
            .thenReturn(List.of(event));

        ApiEntity apiEntity = apiMapper.toEntity(GraviteeContext.getExecutionContext(), api, null, false);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        final PlanEntity planPublished = new PlanEntity();
        planPublished.setStatus(PlanStatus.PUBLISHED);
        // Same date as API -> No redeploy needed
        planPublished.setNeedRedeployAt(nowDate);

        final PlanEntity planStaging = new PlanEntity();
        planStaging.setStatus(PlanStatus.STAGING);
        // Date after API but not published -> No redeploy needed
        planStaging.setNeedRedeployAt(Date.from(now.plus(1, ChronoUnit.DAYS)));

        when(planSearchService.findByApi(eq(GraviteeContext.getExecutionContext()), eq(apiEntity.getId())))
            .thenReturn(Set.of(planPublished, planStaging));

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isTrue();

        verify(eventLatestRepository, times(1))
            .search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            );
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
        verify(planSearchService, times(1)).findByApi(any(), any());
    }

    @Test
    public void should_return_false_when_plan_change() throws JsonProcessingException {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        io.gravitee.definition.model.v4.Api apiDefinition = io.gravitee.definition.model.v4.Api
            .builder()
            .id("apiId")
            .name("Api name")
            .definitionVersion(DefinitionVersion.V4)
            .build();
        Api api = new Api();
        api.setId("apiId");
        api.setName("Api name");
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));
        api.setDeployedAt(nowDate);

        Event event = new Event();
        event.setType(io.gravitee.repository.management.model.EventType.PUBLISH_API);
        event.setPayload(objectMapper.writeValueAsString(api));

        when(
            eventLatestRepository.search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            )
        )
            .thenReturn(List.of(event));

        ApiEntity apiEntity = apiMapper.toEntity(GraviteeContext.getExecutionContext(), api, null, false);
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);

        final PlanEntity planPublished = new PlanEntity();
        planPublished.setStatus(PlanStatus.PUBLISHED);
        // Date after API -> Redeploy needed
        planPublished.setNeedRedeployAt(Date.from(now.plus(1, ChronoUnit.DAYS)));

        when(planSearchService.findByApi(eq(GraviteeContext.getExecutionContext()), eq(apiEntity.getId())))
            .thenReturn(Set.of(planPublished));

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isFalse();

        verify(eventLatestRepository, times(1))
            .search(
                EventCriteria
                    .builder()
                    .types(
                        List.of(
                            io.gravitee.repository.management.model.EventType.PUBLISH_API,
                            io.gravitee.repository.management.model.EventType.UNPUBLISH_API
                        )
                    )
                    .properties(Map.of(Event.EventProperties.API_ID.getValue(), "apiId"))
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                1L
            );
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
        verify(planSearchService, times(1)).findByApi(any(), any());
    }
}
