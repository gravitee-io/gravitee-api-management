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

import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.*;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.CategoryMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
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
    private PlanService planService;

    @Mock
    private EventService eventService;

    @Mock
    private FlowService flowService;

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

    @Mock
    private ApiConverter apiConverter;

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

        ApiMapper apiMapper = new ApiMapper(
            new ObjectMapper(),
            planService,
            flowService,
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
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("apiId");
        apiEntity.setName("Api name");

        EventEntity eventEntity = new EventEntity();
        eventEntity.setType(EventType.PUBLISH_API);
        eventEntity.setPayload(objectMapper.writeValueAsString(apiEntity));

        when(eventService.search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any()))
            .thenReturn(new Page<>(singletonList(eventEntity), 0, 1, 1));

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isTrue();

        verify(eventService, times(1))
            .search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any());
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
    }

    @Test
    public void should_return_true_for_V2_API() throws JsonProcessingException {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        apiEntity.setId("apiId");
        apiEntity.setName("Api name");

        // Add flow
        io.gravitee.definition.model.flow.Flow flow = new io.gravitee.definition.model.flow.Flow();
        apiEntity.setFlows(Collections.singletonList(flow));

        EventEntity eventEntity = new EventEntity();
        eventEntity.setType(EventType.PUBLISH_API);
        eventEntity.setPayload(objectMapper.writeValueAsString(apiEntity));

        when(eventService.search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any()))
            .thenReturn(new Page<>(singletonList(eventEntity), 0, 1, 1));

        // Mock apiConverter to return the defined apiEntity
        when(apiConverter.toApiEntity(any(), eq(null), eq(null), eq(false))).thenReturn(apiEntity);

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isTrue();

        verify(eventService, times(1))
            .search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any());
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
    }

    @Test
    public void should_return_false_for_V2_API() throws JsonProcessingException {
        io.gravitee.rest.api.model.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.api.ApiEntity();
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());
        apiEntity.setId("apiId");
        apiEntity.setName("Api name");

        // Add flow
        io.gravitee.definition.model.flow.Flow flow = new io.gravitee.definition.model.flow.Flow();
        apiEntity.setFlows(Collections.singletonList(flow));

        EventEntity eventEntity = new EventEntity();
        eventEntity.setType(EventType.PUBLISH_API);
        eventEntity.setPayload(objectMapper.writeValueAsString(apiEntity));

        when(eventService.search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any()))
            .thenReturn(new Page<>(singletonList(eventEntity), 0, 1, 1));

        // Mock apiConverter to return the defined apiEntity
        when(apiConverter.toApiEntity(any(), eq(null), eq(null), eq(false))).thenReturn(apiEntity);

        // Add second flow to simulate a change
        io.gravitee.definition.model.flow.Flow secondFlow = new io.gravitee.definition.model.flow.Flow();
        apiEntity.setFlows(List.of(flow, secondFlow));

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isTrue();

        verify(eventService, times(1))
            .search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any());
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
    }

    @Test
    public void should_return_false_for_V4_API() throws JsonProcessingException {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("apiId");
        apiEntity.setName("Api name");

        EventEntity eventEntity = new EventEntity();
        eventEntity.setType(EventType.PUBLISH_API);
        eventEntity.setPayload(objectMapper.writeValueAsString(apiEntity));

        // Add Flows to make API not synchronized
        List<Flow> apiFlows = List.of(mock(Flow.class), mock(Flow.class));
        apiEntity.setFlows(apiFlows);

        when(eventService.search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any()))
            .thenReturn(new Page<>(singletonList(eventEntity), 0, 1, 1));

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isFalse();

        verify(eventService, times(1))
            .search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any());
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
    }

    @Test
    public void should_return_true_when_no_plan_change() throws JsonProcessingException {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("apiId");
        apiEntity.setName("Api name");
        apiEntity.setDeployedAt(nowDate);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setType(EventType.PUBLISH_API);
        eventEntity.setPayload(objectMapper.writeValueAsString(apiEntity));

        when(eventService.search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any()))
            .thenReturn(new Page<>(singletonList(eventEntity), 0, 1, 1));

        final PlanEntity planPublished = new PlanEntity();
        planPublished.setStatus(PlanStatus.PUBLISHED);
        // Same date as API -> No redeploy needed
        planPublished.setNeedRedeployAt(nowDate);

        final PlanEntity planStaging = new PlanEntity();
        planStaging.setStatus(PlanStatus.STAGING);
        // Date after API but not published -> No redeploy needed
        planStaging.setNeedRedeployAt(Date.from(now.plus(1, ChronoUnit.DAYS)));

        when(planSearchService.findByApi(eq(apiEntity.getId()))).thenReturn(Set.of(planPublished, planStaging));

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isTrue();

        verify(eventService, times(1))
            .search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any());
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
        verify(planSearchService, times(1)).findByApi(any());
    }

    @Test
    public void should_return_false_when_plan_change() throws JsonProcessingException {
        Instant now = Instant.now();
        Date nowDate = Date.from(now);

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("apiId");
        apiEntity.setName("Api name");
        apiEntity.setDeployedAt(nowDate);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setType(EventType.PUBLISH_API);
        eventEntity.setPayload(objectMapper.writeValueAsString(apiEntity));

        when(eventService.search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any()))
            .thenReturn(new Page<>(singletonList(eventEntity), 0, 1, 1));

        final PlanEntity planPublished = new PlanEntity();
        planPublished.setStatus(PlanStatus.PUBLISHED);
        // Date after API -> Redeploy needed
        planPublished.setNeedRedeployAt(Date.from(now.plus(1, ChronoUnit.DAYS)));

        when(planSearchService.findByApi(eq(apiEntity.getId()))).thenReturn(Set.of(planPublished));

        final boolean isSynchronized = apiStateService.isSynchronized(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(isSynchronized).isFalse();

        verify(eventService, times(1))
            .search(eq(Arrays.asList(PUBLISH_API, EventType.UNPUBLISH_API)), any(), eq(0L), eq(0L), eq(0), eq(1), any());
        verify(synchronizationService, times(1)).checkSynchronization(any(), any(), any());
        verify(planSearchService, times(1)).findByApi(any());
    }
}
