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
package io.gravitee.gateway.services.sync.synchronizer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.builder.RepositoryApiBuilder;
import io.gravitee.gateway.services.sync.cache.ApiKeysCacheService;
import io.gravitee.gateway.services.sync.cache.SubscriptionsCacheService;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.Plan;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.*;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiSynchronizerTest extends TestCase {

    @InjectMocks
    private ApiSynchronizer apiSynchronizer = new ApiSynchronizer();

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ApiManager apiManager;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ApiKeysCacheService apiKeysCacheService;

    @Mock
    private SubscriptionsCacheService subscriptionsCacheService;

    @Mock
    private ObjectMapper objectMapper;

    @Spy
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);


    @Test
    public void initialSynchronize() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final io.gravitee.definition.model.Api mockApi = mockApi(api);

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API))),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
        )).thenReturn(singletonList(mockEvent));

        apiSynchronizer.synchronize(-1, System.currentTimeMillis());

        verify(apiManager).register(new Api(mockApi));
        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService).register(singletonList(new Api(mockApi)));
        verify(subscriptionsCacheService).register(singletonList(new Api(mockApi)));
    }

    @Test
    public void publishWithDefinitionV2() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final io.gravitee.definition.model.Api mockApi = mockApi(api);

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
        )).thenReturn(singletonList(mockEvent));

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis());

        verify(apiManager).register(new Api(mockApi));
        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService).register(singletonList(new Api(mockApi)));
        verify(subscriptionsCacheService).register(singletonList(new Api(mockApi)));
    }

    @Test
    public void publishWithDefinitionV1() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final io.gravitee.definition.model.Api mockApi = mockApi(api);
        mockApi.setDefinitionVersion(DefinitionVersion.V1);

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
        )).thenReturn(singletonList(mockEvent));

        final Plan plan = new Plan();
        plan.setApi(mockApi.getId());
        when(planRepository.findByApis(anyList())).thenReturn(singletonList(plan));

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis());

        verify(apiManager).register(new Api(mockApi));
        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, times(1)).findByApis(anyList());
        verify(apiKeysCacheService).register(singletonList(new Api(mockApi)));
        verify(subscriptionsCacheService).register(singletonList(new Api(mockApi)));
    }

    @Test
    public void publishWithPagination() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("api-test").build();

        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api2-test").updatedAt(new Date()).definition("api2-test").build();

        final io.gravitee.definition.model.Api mockApi = mockApi(api);
        final io.gravitee.definition.model.Api mockApi2 = mockApi(api2);

        // Force bulk size to 1.
        apiSynchronizer.bulkItems = 1;

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.PUBLISH_API);
        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                eq(Event.EventProperties.API_ID),
                eq(0L),
                eq(1L)
        )).thenReturn(singletonList(mockEvent));

        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                eq(Event.EventProperties.API_ID),
                eq(1L),
                eq(1L)
        )).thenReturn(singletonList(mockEvent2));

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis());

        verify(apiManager, times(1)).register(new Api(mockApi));
        verify(apiManager, times(1)).register(new Api(mockApi2));
        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService).register(singletonList(new Api(mockApi)));
        verify(apiKeysCacheService).register(singletonList(new Api(mockApi2)));
        verify(subscriptionsCacheService).register(singletonList(new Api(mockApi)));
        verify(subscriptionsCacheService).register(singletonList(new Api(mockApi2)));
    }

    @Test
    public void publishWithPaginationAndDefinitionV1AndV2() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("api-test").build();

        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api2-test").updatedAt(new Date()).definition("api2-test").build();

        final io.gravitee.definition.model.Api mockApi = mockApi(api);
        mockApi.setDefinitionVersion(DefinitionVersion.V1);
        final io.gravitee.definition.model.Api mockApi2 = mockApi(api2);
        mockApi2.setDefinitionVersion(DefinitionVersion.V2);

        // Force bulk size to 1.
        apiSynchronizer.bulkItems = 1;

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.PUBLISH_API);
        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                eq(Event.EventProperties.API_ID),
                eq(0L),
                eq(1L)
        )).thenReturn(singletonList(mockEvent));

        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                eq(Event.EventProperties.API_ID),
                eq(1L),
                eq(1L)
        )).thenReturn(singletonList(mockEvent2));

        final Plan plan = new Plan();
        plan.setApi(mockApi.getId());
        when(planRepository.findByApis(singletonList(plan.getApi()))).thenReturn(singletonList(plan));

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis());

        verify(apiManager, times(1)).register(new Api(mockApi));
        verify(apiManager, times(1)).register(new Api(mockApi2));
        verify(apiManager, never()).unregister(any(String.class));
        verify(planRepository, times(1)).findByApis(singletonList(plan.getApi()));
        verify(apiKeysCacheService).register(singletonList(new Api(mockApi)));
        verify(apiKeysCacheService).register(singletonList(new Api(mockApi2)));
        verify(subscriptionsCacheService).register(singletonList(new Api(mockApi)));
        verify(subscriptionsCacheService).register(singletonList(new Api(mockApi2)));
    }

    @Test
    public void unpublish() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final io.gravitee.definition.model.Api mockApi = mockApi(api);

        final Event mockEvent = mockEvent(api, EventType.UNPUBLISH_API);
        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                eq(Event.EventProperties.API_ID),
                anyLong(),
                anyLong()
        )).thenReturn(singletonList(mockEvent));

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis());

        verify(apiManager, never()).register(new Api(mockApi));
        verify(apiManager).unregister(mockApi.getId());
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService, never()).register(singletonList(new Api(mockApi)));
        verify(subscriptionsCacheService, never()).register(singletonList(new Api(mockApi)));
    }

    @Test
    public void unpublishWithPagination() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("api-test").build();

        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api2-test").updatedAt(new Date()).definition("api2-test").build();

        final io.gravitee.definition.model.Api mockApi = mockApi(api);
        mockApi.setDefinitionVersion(DefinitionVersion.V1);
        final io.gravitee.definition.model.Api mockApi2 = mockApi(api2);
        mockApi2.setDefinitionVersion(DefinitionVersion.V2);

        // Force bulk size to 1.
        apiSynchronizer.bulkItems = 1;

        final Event mockEvent = mockEvent(api, EventType.UNPUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.UNPUBLISH_API);
        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                eq(Event.EventProperties.API_ID),
                eq(0L),
                eq(1L)
        )).thenReturn(singletonList(mockEvent));

        when(eventRepository.searchLatest(
                argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                eq(Event.EventProperties.API_ID),
                eq(1L),
                eq(1L)
        )).thenReturn(singletonList(mockEvent2));

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis());

        verify(apiManager, never()).register(new Api(mockApi));
        verify(apiManager).unregister(mockApi.getId());
        verify(apiManager).unregister(mockApi2.getId());
        verify(planRepository, never()).findByApis(anyList());
        verify(apiKeysCacheService, never()).register(singletonList(new Api(mockApi)));
        verify(subscriptionsCacheService, never()).register(singletonList(new Api(mockApi)));
    }


    @Test
    public void synchronizeWithLotOfApiEvents() throws Exception {
        long page = 0;
        List<Event> eventAccumulator = new ArrayList<>(100);

        for (int i = 1; i <= 500; i++) {
            io.gravitee.repository.management.model.Api api =
                    new RepositoryApiBuilder().id("api" + i + "-test").updatedAt(new Date()).definition("api" + i + "-test").build();

            final io.gravitee.definition.model.Api mockApi = mockApi(api);
            mockApi.setDefinitionVersion(DefinitionVersion.V1);

            if (i % 2 == 0) {
                eventAccumulator.add(mockEvent(api, EventType.START_API));
            } else {
                eventAccumulator.add(mockEvent(api, EventType.STOP_API));
            }

            if (i % 100 == 0) {
                when(eventRepository.searchLatest(
                        argThat(criteria -> criteria != null && criteria.getTypes().containsAll(Arrays.asList(EventType.PUBLISH_API, EventType.START_API, EventType.UNPUBLISH_API, EventType.STOP_API))),
                        eq(Event.EventProperties.API_ID),
                        eq(page),
                        eq(100L)
                )).thenReturn(eventAccumulator);

                page++;
                eventAccumulator = new ArrayList<>();
            }
        }

        apiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis());

        verify(planRepository, times(3)).findByApis(anyList());
        verify(apiKeysCacheService, times(3)).register(anyList());
        verify(subscriptionsCacheService, times(3)).register(anyList());
        verify(apiManager, times(250)).register(any(Api.class));
        verify(apiManager, times(250)).unregister(anyString());
    }

    private io.gravitee.definition.model.Api mockApi(final io.gravitee.repository.management.model.Api api) throws Exception {
        return mockApi(api, new String[]{});
    }

    private io.gravitee.definition.model.Api mockApi(final io.gravitee.repository.management.model.Api api, final String[] tags) throws Exception {
        final io.gravitee.definition.model.Api mockApi = new io.gravitee.definition.model.Api();
        mockApi.setId(api.getId());
        mockApi.setTags(new HashSet<>(Arrays.asList(tags)));
        mockApi.setDefinitionVersion(DefinitionVersion.V2);
        when(objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class)).thenReturn(mockApi);
        return mockApi;
    }

    private Event mockEvent(final io.gravitee.repository.management.model.Api api, EventType eventType) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), api.getId());

        Event event = new Event();
        event.setType(eventType);
        event.setCreatedAt(new Date());
        event.setProperties(properties);
        event.setPayload(api.getId());

        when(objectMapper.readValue(event.getPayload(), io.gravitee.repository.management.model.Api.class)).thenReturn(api);

        return event;
    }
}