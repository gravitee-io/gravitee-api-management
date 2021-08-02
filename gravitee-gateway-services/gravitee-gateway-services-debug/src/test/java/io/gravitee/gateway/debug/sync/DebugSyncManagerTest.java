/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.debug.sync;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Plan;
import io.gravitee.gateway.debug.handler.definition.DebugApi;
import io.gravitee.gateway.debug.utils.RepositoryApiBuilder;
import io.gravitee.gateway.debug.utils.Stubs;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.node.api.cluster.ClusterManager;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldExclusionFilter;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DebugSyncManagerTest {

    private static final String EVENT_ID = "evt-id";

    @InjectMocks
    private final DebugSyncManager syncManager = new DebugSyncManager();

    @Mock
    private ClusterManager clusterManager;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EventManager eventManager;

    private List<String> environments;

    @Before
    public void setUp() {
        when(clusterManager.isMasterNode()).thenReturn(true);
        environments = Arrays.asList("DEFAULT", "ENVIRONMENT_2");
    }

    @Test
    public void shouldNotSync_notMasterNode() throws TechnicalException {
        when(clusterManager.isMasterNode()).thenReturn(false);
        syncManager.setDistributed(true);

        syncManager.refresh(this.environments);

        verify(apiRepository, never()).search(any(ApiCriteria.class), any(ApiFieldExclusionFilter.class));
    }

    @Test
    public void shouldSync_notMasterNode_notDistributed() throws TechnicalException {
        when(clusterManager.isMasterNode()).thenReturn(false);
        syncManager.setDistributed(false);

        when(
            apiRepository.search(
                new ApiCriteria.Builder().environments(environments).build(),
                new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build()
            )
        )
            .thenReturn(emptyList());

        syncManager.refresh(this.environments);

        verify(apiRepository, times(1)).search(any(ApiCriteria.class), any(ApiFieldExclusionFilter.class));
    }

    @Test
    public void shouldSyncAndNotEmitDebugEventBecauseEmpty() throws TechnicalException {
        when(
            apiRepository.search(
                new ApiCriteria.Builder().environments(environments).build(),
                new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build()
            )
        )
            .thenReturn(emptyList());

        syncManager.refresh(this.environments);

        verify(eventManager, never()).publishEvent(eq(ReactorEvent.DEBUG), any(DebugApi.class));
    }

    @Test
    public void shouldSyncAndPublishDebugEventStagingPlan() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
            .id("api-test")
            .updatedAt(new Date())
            .definition("test")
            .build();

        final io.gravitee.definition.model.DebugApi mockApi = mockDebugApi();

        final Plan plan = new Plan();
        plan.setStatus("staging");

        mockApi.setPlans(singletonList(plan));

        final Event mockEvent = mockEvent(mockApi, EventType.DEBUG_API);
        when(eventRepository.search(any(EventCriteria.class), any(Pageable.class)))
            .thenReturn(new Page<>(singletonList(mockEvent), 0, 0, 1));

        when(
            apiRepository.search(
                new ApiCriteria.Builder().environments(environments).build(),
                new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build()
            )
        )
            .thenReturn(singletonList(api));

        syncManager.refresh(this.environments);

        verify(eventManager, times(1)).publishEvent(eq(ReactorEvent.DEBUG), any(DebugApi.class));
    }

    @Test
    public void shouldSyncAndPublishDebugEventPublishedPlan() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
            .id("api-test")
            .updatedAt(new Date())
            .definition("test")
            .build();

        final io.gravitee.definition.model.DebugApi mockApi = mockDebugApi();

        final Plan plan = new Plan();
        plan.setStatus("published");

        mockApi.setPlans(singletonList(plan));

        final Event mockEvent = mockEvent(mockApi, EventType.DEBUG_API);
        when(eventRepository.search(any(EventCriteria.class), any(Pageable.class)))
            .thenReturn(new Page<>(singletonList(mockEvent), 0, 0, 1));

        when(
            apiRepository.search(
                new ApiCriteria.Builder().environments(environments).build(),
                new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build()
            )
        )
            .thenReturn(singletonList(api));

        syncManager.refresh(this.environments);

        verify(eventManager, times(1)).publishEvent(eq(ReactorEvent.DEBUG), any(DebugApi.class));
    }

    @Test
    public void shouldNotSyncAndPublishDebugEventIfNoPlanInGoodStatus() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
            .id("api-test")
            .updatedAt(new Date())
            .definition("test")
            .build();

        final io.gravitee.definition.model.DebugApi mockApi = mockDebugApi();

        final Event mockEvent = mockEvent(mockApi, EventType.DEBUG_API);
        when(eventRepository.search(any(EventCriteria.class), any(Pageable.class)))
            .thenReturn(new Page<>(singletonList(mockEvent), 0, 0, 1));

        when(
            apiRepository.search(
                new ApiCriteria.Builder().environments(environments).build(),
                new ApiFieldExclusionFilter.Builder().excludeDefinition().excludePicture().build()
            )
        )
            .thenReturn(singletonList(api));

        syncManager.refresh(this.environments);

        verify(eventManager, never()).publishEvent(eq(ReactorEvent.DEBUG), any(DebugApi.class));
    }

    private io.gravitee.definition.model.DebugApi mockDebugApi() {
        final io.gravitee.definition.model.DebugApi mockApi = Stubs.getADebugApiDefinition();

        mockApi.setDefinitionVersion(DefinitionVersion.V2);

        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody("request body");
        mockApi.setRequest(httpRequest);
        return mockApi;
    }

    private Event mockEvent(final io.gravitee.definition.model.DebugApi debugApi, EventType eventType) throws Exception {
        final Event event = Stubs.getAnEvent();
        event.setType(eventType);
        event.setCreatedAt(new Date());
        event.getProperties().put(Event.EventProperties.API_ID.getValue(), debugApi.getId());
        event.setId(EVENT_ID);

        when(objectMapper.readValue(event.getPayload(), io.gravitee.definition.model.DebugApi.class)).thenReturn(debugApi);

        return event;
    }
}
