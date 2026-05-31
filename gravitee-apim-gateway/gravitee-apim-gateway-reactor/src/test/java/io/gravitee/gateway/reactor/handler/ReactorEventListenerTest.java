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
package io.gravitee.gateway.reactor.handler;

import static org.mockito.Mockito.*;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReactorEventListenerTest {

    private ReactorEventListener reactorEventListener;

    @Mock
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Mock
    private EventManager eventManager;

    @BeforeEach
    public void setUp() throws Exception {
        reactorEventListener = new ReactorEventListener(eventManager, reactorHandlerRegistry);
        reactorEventListener.start();
    }

    @Test
    public void shouldCreateToHandlerRegistryWhenDeployApiEvent() {
        final Event<ReactorEvent, Reactable> event = mock(Event.class);
        final Reactable api = mock(Reactable.class);

        when(event.type()).thenReturn(ReactorEvent.DEPLOY);
        when(event.content()).thenReturn(api);
        reactorEventListener.onEvent(event);

        verify(reactorHandlerRegistry).create(api);
        verifyNoMoreInteractions(reactorHandlerRegistry);
    }

    @Test
    public void shouldUpdateToHandlerRegistryWhenUpdateApiEvent() {
        final Event<ReactorEvent, Reactable> event = mock(Event.class);
        final Reactable api = mock(Reactable.class);

        when(event.type()).thenReturn(ReactorEvent.UPDATE);
        when(event.content()).thenReturn(api);
        reactorEventListener.onEvent(event);

        verify(reactorHandlerRegistry).update(api);
        verifyNoMoreInteractions(reactorHandlerRegistry);
    }

    @Test
    public void shouldRemoveToHandlerRegistryWhenUpdateApiEvent() {
        final Event<ReactorEvent, Reactable> event = mock(Event.class);
        final Reactable api = mock(Reactable.class);

        when(event.type()).thenReturn(ReactorEvent.UNDEPLOY);
        when(event.content()).thenReturn(api);
        reactorEventListener.onEvent(event);

        verify(reactorHandlerRegistry).remove(api);
        verifyNoMoreInteractions(reactorHandlerRegistry);
    }

    @Test
    public void shouldClearHandlerRegistryWhenStopping() throws Exception {
        reactorEventListener.stop();
        verify(reactorHandlerRegistry).clear();
    }
}
