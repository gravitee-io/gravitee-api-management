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

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.impl.ReactableWrapper;
import io.gravitee.gateway.services.sync.builder.RepositoryApiBuilder;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

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

    @Spy
    private ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    @Mock
    private Node node;

    static final List<String> ENVIRONMENTS = Arrays.asList("DEFAULT", "OTHER_ENV");

    @Before
    public void setup() {
        when(node.id()).thenReturn(GATEWAY_ID);
    }

    @Test
    public void shouldSynchronizeDebugEvents() {
        io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
            .id("api-test")
            .updatedAt(new Date())
            .definition("test")
            .build();

        final Event mockEvent = mockEvent(api, EventType.DEBUG_API);

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

        verify(eventManager, times(1)).publishEvent(eq(ReactorEvent.DEBUG), any(ReactableWrapper.class));
    }

    @Test
    public void shouldSynchronizeDebugEventsWithPagination() {
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

        // Force bulk size to 1.
        debugApiSynchronizer.bulkItems = 1;

        final Event mockEvent = mockEvent(api, EventType.DEBUG_API);
        final Event mockEvent2 = mockEvent(api2, EventType.DEBUG_API);
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

        verify(eventManager, times(2)).publishEvent(eq(ReactorEvent.DEBUG), any(ReactableWrapper.class));
        verify(eventManager, times(2)).publishEvent(eq(ReactorEvent.DEBUG), any(ReactableWrapper.class));
    }

    @Test
    public void shouldSynchronizeALotOfDebugEvents() throws Exception {
        long page = 0;
        List<Event> eventAccumulator = new ArrayList<>(100);

        for (int i = 1; i <= 500; i++) {
            io.gravitee.repository.management.model.Api api = new RepositoryApiBuilder()
                .id("api" + i + "-test")
                .updatedAt(new Date())
                .definition("api" + i + "-test")
                .build();

            eventAccumulator.add(mockEvent(api, EventType.DEBUG_API));

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

        verify(eventManager, times(500)).publishEvent(eq(ReactorEvent.DEBUG), any(ReactableWrapper.class));
    }

    private Event mockEvent(final Api api, EventType eventType) {
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_DEBUG_ID.getValue(), api.getId());
        properties.put(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name());
        properties.put(Event.EventProperties.GATEWAY_ID.getValue(), node.id());
        Event event = new Event();
        event.setType(eventType);
        event.setCreatedAt(new Date());
        event.setProperties(properties);
        event.setPayload(api.getId());
        event.setId("eventId");
        return event;
    }
}
