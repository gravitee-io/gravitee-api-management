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
package io.gravitee.gateway.core.reactor;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.gateway.core.AbstractCoreTest;
import io.gravitee.gateway.core.builder.ApiBuilder;
import io.gravitee.gateway.core.reactor.handler.impl.ApiContextHandlerFactory;
import io.gravitee.gateway.core.manager.ApiEvent;
import io.gravitee.gateway.core.model.Api;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactorApiEventTest extends AbstractCoreTest {

    private GraviteeReactor reactor;
    private ApiContextHandlerFactory handlerFactory;

    @Before
    public void setUp() {
        reactor = spy(new AsyncGraviteeReactor());

        handlerFactory = spy(new ApiContextHandlerFactory());
        handlerFactory.setApplicationContext(applicationContext);
        reactor.setContextHandlerFactory(handlerFactory);
        reactor.setApplicationContext(applicationContext);
    }

    @Test
    public void handleApiEvent_create_started() {
        Api api = new ApiBuilder().name("my-api").origin("http://localhost/team").start().build();
        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.CREATE, api);

        reactor.onEvent(evt);

        verify(reactor).addHandler(api);
        verify(handlerFactory).create(api);
        verify(reactor, never()).removeHandler(api);
    }

    @Test
    public void handleApiEvent_create_stopped() {
        Api api = new ApiBuilder().name("my-api").origin("http://localhost/team").build();
        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.CREATE, api);

        reactor.onEvent(evt);

        verify(reactor).addHandler(api);
        verify(handlerFactory, never()).create(api);
        verify(reactor, never()).removeHandler(api);
    }

    @Test
    public void handleApiEvent_update_stopped() {
        Api api = new ApiBuilder().name("my-api").origin("http://localhost/team").build();
        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.UPDATE, api);

        reactor.onEvent(evt);

        verify(reactor).removeHandler(api);
        verify(reactor).addHandler(api);
        verify(handlerFactory, never()).create(api);
    }

    @Test
    public void handleApiEvent_update_started() {
        Api api = new ApiBuilder().name("my-api").origin("http://localhost/team").start().build();
        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.UPDATE, api);

        reactor.onEvent(evt);

        verify(reactor).removeHandler(api);
        verify(reactor).addHandler(api);
        verify(handlerFactory).create(api);
    }

    @Test
    public void handleApiEvent_remove() {
        Api api = new ApiBuilder().name("my-api").origin("http://localhost/team").build();
        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.REMOVE, api);

        reactor.onEvent(evt);

        verify(reactor).removeHandler(api);
        verify(reactor, never()).addHandler(api);
    }
}
