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

    @Mock
    private Node node;

    static final List<String> ENVIRONMENTS = Arrays.asList("DEFAULT", "OTHER_ENV");

    @Before
    public void setup() {
        debugApiSynchronizer.setExecutor(Executors.newFixedThreadPool(1));
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
            eventRepository.search(
                argThat(
                    criteria ->
                        criteria != null &&
                        criteria.getTypes().size() == 1 &&
                        criteria.getTypes().contains(EventType.DEBUG_API) &&
                        criteria.getEnvironments().containsAll(ENVIRONMENTS) &&
                        criteria
                            .getProperties()
                            .get(Event.EventProperties.API_DEBUG_STATUS.name().toLowerCase())
                            .equals(ApiDebugStatus.TO_DEBUG.name())
                )
            )
        )
            .thenReturn(singletonList(mockEvent));

        debugApiSynchronizer.synchronize(System.currentTimeMillis() - 5000, System.currentTimeMillis(), ENVIRONMENTS);

        verify(eventManager, times(1)).publishEvent(eq(ReactorEvent.DEBUG), any(ReactableWrapper.class));
    }

    private Event mockEvent(final Api api, EventType eventType) {
        Map<String, String> properties = new HashMap<>();
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
