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

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.gateway.debug.handler.definition.DebugApi;
import io.gravitee.gateway.debug.utils.RepositoryApiBuilder;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.manager.ApiManager;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.sync.synchronizer.PlanFetcher;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.Plan;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class DebugApiSynchronizerTest extends TestCase {

    private static final String GATEWAY_ID = "gateway-id";

    @InjectMocks
    private DebugApiSynchronizer debugApiSynchronizer = new DebugApiSynchronizer();

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventManager eventManager;

    @Mock
    private PlanFetcher planFetcher;

    @Mock
    private ObjectMapper objectMapper;

    @Spy
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    @Mock
    private Node node;

    static final List<String> ENVIRONMENTS = Arrays.asList("DEFAULT", "OTHER_ENV");

    @Before
    public void setup() {
        when(node.id()).thenReturn(GATEWAY_ID);
        when(planFetcher.fetchApiPlans(any()))
            .thenAnswer(
                (Answer) invocation -> {
                    Flowable<DebugApi> argument = (Flowable<DebugApi>) invocation.getArguments()[0];
                    return argument
                        .groupBy(io.gravitee.definition.model.Api::getDefinitionVersion)
                        .flatMap(
                            apisByDefinitionVersion ->
                                apisByDefinitionVersion.flatMapSingle(
                                    (Function<DebugApi, SingleSource<?>>) api -> {
                                        api.setPlans(api.getPlans());
                                        return Single.just(api);
                                    }
                                )
                        );
                }
            );
    }

    @Test
    public void debugWithDefinitionV2() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
            .id("api-test")
            .updatedAt(new Date())
            .definition("test")
            .build();

        final io.gravitee.definition.model.DebugApi mockApi = mockApi(api);

        final Event mockEvent = mockEvent(mockApi, EventType.DEBUG_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().size() == 1 &&
                        criteria.getTypes().contains(EventType.DEBUG_API) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS) &&
                        criteria
                            .getProperties()
                            .get(Event.EventProperties.API_DEBUG_STATUS.name().toLowerCase())
                            .equals(ApiDebugStatus.TO_DEBUG.name()) &&
                        criteria.getProperties().get(Event.EventProperties.GATEWAY_ID.name().toLowerCase()).equals(node.id())
                ),
                eq(Event.EventProperties.API_DEBUG_ID),
                anyLong(),
                anyLong()
            )
        )
            .thenReturn(singletonList(mockEvent));

        debugApiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(eventManager, times(1)).publishEvent(eq(ReactorEvent.DEBUG), any(DebugApi.class));
    }

    @Test
    public void publishWithPagination() throws Exception {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
            .id("api-test")
            .updatedAt(new Date())
            .definition("api-test")
            .build();

        io.gravitee.repository.management.model.Api api2 = new RepositoryApiBuilder()
            .id("api2-test")
            .updatedAt(new Date())
            .definition("api2-test")
            .build();

        final io.gravitee.definition.model.DebugApi mockApi = mockApi(api);
        final io.gravitee.definition.model.DebugApi mockApi2 = mockApi(api2);

        // Force bulk size to 1.
        debugApiSynchronizer.bulkItems = 1;

        final Event mockEvent = mockEvent(mockApi, EventType.DEBUG_API);
        final Event mockEvent2 = mockEvent(mockApi2, EventType.DEBUG_API);
        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().size() == 1 &&
                        criteria.getTypes().contains(EventType.DEBUG_API) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS) &&
                        criteria
                            .getProperties()
                            .get(Event.EventProperties.API_DEBUG_STATUS.name().toLowerCase())
                            .equals(ApiDebugStatus.TO_DEBUG.name()) &&
                        criteria.getProperties().get(Event.EventProperties.GATEWAY_ID.name().toLowerCase()).equals(node.id())
                ),
                eq(Event.EventProperties.API_DEBUG_ID),
                eq(0L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent));

        when(
            eventRepository.searchLatest(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().size() == 1 &&
                        criteria.getTypes().contains(EventType.DEBUG_API) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS) &&
                        criteria
                            .getProperties()
                            .get(Event.EventProperties.API_DEBUG_STATUS.name().toLowerCase())
                            .equals(ApiDebugStatus.TO_DEBUG.name()) &&
                        criteria.getProperties().get(Event.EventProperties.GATEWAY_ID.name().toLowerCase()).equals(node.id())
                ),
                eq(Event.EventProperties.API_DEBUG_ID),
                eq(1L),
                eq(1L)
            )
        )
            .thenReturn(singletonList(mockEvent2));

        debugApiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(eventManager, times(2)).publishEvent(eq(ReactorEvent.DEBUG), any(DebugApi.class));
        verify(eventManager, times(2)).publishEvent(eq(ReactorEvent.DEBUG), any(DebugApi.class));
    }

    @Test
    public void synchronizeWithLotOfApiEvents() throws Exception {
        long page = 0;
        List<Event> eventAccumulator = new ArrayList<>(100);

        for (int i = 1; i <= 500; i++) {
            io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
                .id("api" + i + "-test")
                .updatedAt(new Date())
                .definition("api" + i + "-test")
                .build();

            final io.gravitee.definition.model.DebugApi mockApi = mockApi(api);
            eventAccumulator.add(mockEvent(mockApi, EventType.DEBUG_API));

            debugApiSynchronizer.bulkItems = 100;

            if (i % 100 == 0) {
                when(
                    eventRepository.searchLatest(
                        argThat(
                            criteria ->
                                criteria != null &&
                                criteria.getTypes().containsAll(Arrays.asList(EventType.DEBUG_API)) &&
                                criteria.getEnvironments().containsAll(ENVIRONMENTS)
                        ),
                        eq(Event.EventProperties.API_DEBUG_ID),
                        eq(page),
                        eq(100L)
                    )
                )
                    .thenReturn(eventAccumulator);

                page++;
                eventAccumulator = new ArrayList<>();
            }
        }

        debugApiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(eventManager, times(500)).publishEvent(eq(ReactorEvent.DEBUG), any(DebugApi.class));
    }

    private io.gravitee.definition.model.DebugApi mockApi(final io.gravitee.repository.management.model.Api api) throws Exception {
        return mockApi(api, new String[] {});
    }

    private io.gravitee.definition.model.DebugApi mockApi(final io.gravitee.repository.management.model.Api api, final String[] tags)
        throws Exception {
        final io.gravitee.definition.model.DebugApi mockApi = new io.gravitee.definition.model.DebugApi();
        mockApi.setId(api.getId());
        mockApi.setTags(new HashSet<>(Arrays.asList(tags)));
        mockApi.setDefinitionVersion(DefinitionVersion.V2);
        io.gravitee.definition.model.Plan plan = mock(io.gravitee.definition.model.Plan.class);
        mockApi.setPlans(Arrays.asList(plan));
        final HttpRequest httpRequest = new HttpRequest();
        httpRequest.setMethod("GET");
        httpRequest.setPath("/path1");
        httpRequest.setBody("request body");
        mockApi.setRequest(httpRequest);
        when(objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.DebugApi.class)).thenReturn(mockApi);
        return mockApi;
    }

    private Event mockEvent(final io.gravitee.definition.model.DebugApi api, EventType eventType) throws Exception {
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_DEBUG_ID.getValue(), api.getId());
        properties.put(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name());
        properties.put(Event.EventProperties.GATEWAY_ID.getValue(), node.id());
        Event event = new Event();
        event.setType(eventType);
        event.setCreatedAt(new Date());
        event.setProperties(properties);
        event.setPayload(api.getId());

        when(objectMapper.readValue(event.getPayload(), io.gravitee.definition.model.DebugApi.class)).thenReturn(api);

        return event;
    }
}
