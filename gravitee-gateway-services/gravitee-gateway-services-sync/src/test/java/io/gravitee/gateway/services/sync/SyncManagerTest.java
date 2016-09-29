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
package io.gravitee.gateway.services.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.services.sync.builder.RepositoryApiBuilder;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.LifecycleState;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class SyncManagerTest {

    @InjectMocks
    private SyncManager syncManager = new SyncManager();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ApiManager apiManager;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void test_empty() throws TechnicalException {
        when(apiRepository.findAll()).thenReturn(Collections.emptySet());

        syncManager.refresh();

        verify(apiManager, never()).deploy(any(Api.class));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_newApi() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);
        
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.singletonList(mockEvent));

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithSameApi() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.singletonList(mockEvent));

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));
        when(apiManager.get(api.getId())).thenReturn(null);

        syncManager.refresh();

        when(apiManager.get(api.getId())).thenReturn(mockApi);

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test-2").updatedAt(new Date()).definition("test2").build();

        final Api mockApi = mockApi(api);

        final Api mockApi2 = mockApi(api2);
        
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.PUBLISH_API);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.singletonList(mockEvent));

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        Set<io.gravitee.repository.management.model.Api> apis = new HashSet<>();
        apis.add(api);
        apis.add(api2);
        
        List<Event> events = new ArrayList<>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(events);

        when(apiRepository.findAll()).thenReturn(apis);

        syncManager.refresh();

        verify(apiManager, times(2)).deploy(argThat(new ArgumentMatcher<Api>() {
            @Override
            public boolean matches(Object argument) {
                final Api api = (Api) argument;
                return api.getId().equals(mockApi.getId()) || api2.getId().equals(mockApi2.getId());
            }
        }));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis_apiToRemove() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test-2").updatedAt(new Date()).definition("test2").build();

        final Api mockApi = mockApi(api);

        final Api mockApi2 = mockApi(api2);
        
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        
        final Event mockEvent2 = mockEvent(api2, EventType.PUBLISH_API);

        List<Event> events = new ArrayList<Event>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(events);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));
        when(apiManager.apis()).thenReturn(Collections.singleton(mockApi));

        syncManager.refresh();

        verify(apiManager, times(2)).deploy(argThat(new ArgumentMatcher<Api>() {
            @Override
            public boolean matches(Object argument) {
                final Api api = (Api) argument;
                return api.getId().equals(mockApi.getId()) || api2.getId().equals(mockApi2.getId());
            }
        }));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager).undeploy(api.getId());
        verify(apiManager, never()).undeploy(api2.getId());
    }

    @Test
    public void test_twiceWithTwoApis_apiToUpdate() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        Instant updateDateInst = api.getUpdatedAt().toInstant().plus(Duration.ofHours(1));
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test").updatedAt(Date.from(updateDateInst)).definition("test2").build();

        final Api mockApi = mockApi(api);
        mockApi(api2);
        
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.PUBLISH_API);

        List<Event> events = new ArrayList<Event>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(events);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));
        when(apiManager.get(api.getId())).thenReturn(mockApi);

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager).update(mockApi);
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_twiceWithTwoApis_api_noUpdate() throws Exception {
        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
        io.gravitee.repository.management.model.Api api2 =
                new RepositoryApiBuilder().id("api-test").updatedAt(api.getUpdatedAt()).definition("test").build();

        final Api mockApi = mockApi(api);
        
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        
        final Event mockEvent2 = mockEvent(api2, EventType.PUBLISH_API);

        List<Event> events = new ArrayList<Event>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(events);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));

        syncManager.refresh();

        verify(apiManager, times(2)).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_throwTechnicalException() throws TechnicalException {
        when(apiRepository.findAll()).thenThrow(TechnicalException.class);

        syncManager.refresh();

        verify(apiManager, never()).deploy(any(Api.class));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_deployApiWithTag() throws Exception {
        shouldDeployApiWithTags(new String[]{"test"});
    }

    @Test
    public void test_deployApiWithUpperCasedTag() throws Exception {
        shouldDeployApiWithTags(new String[]{"Test"});
    }

    @Test
    public void test_deployApiWithAccentTag() throws Exception {
        shouldDeployApiWithTags(new String[]{"tést"});
    }

    @Test
    public void test_deployApiWithUpperCasedAndAccentTag() throws Exception {
        shouldDeployApiWithTags(new String[]{"Tést"});
    }

    public void shouldDeployApiWithTags(final String[] apiTags) throws Exception {
        System.setProperty(SyncManager.TAGS_PROP, "test,toto");

        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api, apiTags);
        
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.singletonList(mockEvent));

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
        System.clearProperty(SyncManager.TAGS_PROP);
    }

    @Test
    public void test_not_deployApiWithoutTag() throws Exception {
        System.setProperty(SyncManager.TAGS_PROP, "test,toto");

        io.gravitee.repository.management.model.Api api =
                new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager, never()).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
        System.clearProperty(SyncManager.TAGS_PROP);
    }
    
    @Test
    public void test_not_deployApiWithoutEvent() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.emptyList());

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager, never()).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_deployOnlyOneApiWithTwoApisAndOneEvent() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder().id("api-test-2").updatedAt(new Date()).definition("test2").build();

        final Api mockApi = mockApi(api);

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);

        Set<io.gravitee.repository.management.model.Api> apis = new HashSet<>();
        apis.add(api);
        apis.add(api2);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.singletonList(mockEvent));

        when(apiRepository.findAll()).thenReturn(apis);

        syncManager.refresh();

        verify(apiManager).deploy(argThat(new ArgumentMatcher<Api>() {
            @Override
            public boolean matches(Object argument) {
                final Api api = (Api) argument;
                return api.getId().equals(mockApi.getId());
            }
        }));
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager, never()).undeploy(any(String.class));
    }

    @Test
    public void test_shouldUndeployIfLastEventIsUnpublishAPI() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder().id("api-test").updatedAt(new Date()).definition("test").build();

        final Api mockApi = mockApi(api);

        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);

        final Event mockEvent2 = mockEvent(api, EventType.UNPUBLISH_API);

        Set<Event> events = new HashSet<Event>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.singletonList(mockEvent));

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(apiManager.apis()).thenReturn(Collections.singleton(mockApi));

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.singletonList(mockEvent2));

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager, never()).update(any(Api.class));
        verify(apiManager).undeploy(mockApi.getId());

    }
    
    @Test
    public void test_shouldUpdateIfLastEventIsStartAPI() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
                                                            .id("api-test")
                                                            .updatedAt(new Date())
                                                            .definition("test")
                                                            .lifecycleState(LifecycleState.STOPPED)
                                                            .build();
        
        Instant updateDateInst = api.getUpdatedAt().toInstant().plus(Duration.ofHours(1));
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder()
                                                            .id("api-test")
                                                            .updatedAt(Date.from(updateDateInst))
                                                            .definition("test")
                                                            .lifecycleState(LifecycleState.STARTED)
                                                            .build();
        
        final Api mockApi = mockApi(api);
        mockApi(api2);
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.START_API);
        
        List<Event> events = new ArrayList<Event>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.singletonList(mockEvent));

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(events);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));
        when(apiManager.get(api.getId())).thenReturn(mockApi);

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager).update(mockApi);
        verify(apiManager, never()).undeploy(any(String.class));

    }
    
    @Test
    public void test_shouldUpdateIfLastEventIsStopAPI() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
                                                            .id("api-test")
                                                            .updatedAt(new Date())
                                                            .definition("test")
                                                            .lifecycleState(LifecycleState.STARTED)
                                                            .build();
        
        Instant updateDateInst = api.getUpdatedAt().toInstant().plus(Duration.ofHours(1));
        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder()
                                                            .id("api-test")
                                                            .updatedAt(Date.from(updateDateInst))
                                                            .definition("test")
                                                            .lifecycleState(LifecycleState.STOPPED)
                                                            .build();
        
        final Api mockApi = mockApi(api);
        mockApi(api2);
        final Event mockEvent = mockEvent(api, EventType.PUBLISH_API);
        final Event mockEvent2 = mockEvent(api2, EventType.STOP_API);
        
        List<Event> events = new ArrayList<Event>();
        events.add(mockEvent);
        events.add(mockEvent2);

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(Collections.singletonList(mockEvent));

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api));

        syncManager.refresh();

        when(eventRepository.search(
                new EventCriteria.Builder()
                        .types(EventType.PUBLISH_API, EventType.UNPUBLISH_API, EventType.START_API, EventType.STOP_API)
                        .build()
        )).thenReturn(events);

        when(apiRepository.findAll()).thenReturn(Collections.singleton(api2));
        when(apiManager.get(api.getId())).thenReturn(mockApi);

        syncManager.refresh();

        verify(apiManager).deploy(mockApi);
        verify(apiManager).update(mockApi);
        verify(apiManager, never()).undeploy(any(String.class));

    }

    private Api mockApi(final io.gravitee.repository.management.model.Api api, final String[] tags) throws Exception {
        final Api mockApi = mockApi(api);
        mockApi.setTags(new HashSet<>(Arrays.asList(tags)));
        return mockApi;
    }

    private Api mockApi(final io.gravitee.repository.management.model.Api api) throws Exception {
        final Api mockApi = new Api();
        mockApi.setId(api.getId());
        mockApi.setDeployedAt(api.getUpdatedAt());
        when(objectMapper.readValue(api.getDefinition(), Api.class)).thenReturn(mockApi);
        return mockApi;
    }
    
    private Event mockEvent(final io.gravitee.repository.management.model.Api api, EventType eventType) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), api.getId());

        Event event = new Event();
        event.setType(eventType);
        event.setCreatedAt(new Date());
        event.setProperties(properties);

        when(objectMapper.readValue(event.getPayload(), io.gravitee.repository.management.model.Api.class)).thenReturn(api);

        return event;
    }
}
