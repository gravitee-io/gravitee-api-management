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
import io.gravitee.gateway.core.builder.ApiDefinitionBuilder;
import io.gravitee.gateway.core.builder.ProxyDefinitionBuilder;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.event.ApiEvent;
import io.gravitee.gateway.core.reactor.handler.impl.api.ApiContextHandlerFactory;
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
        reactor = spy(new GraviteeReactor());

        handlerFactory = spy(new ApiContextHandlerFactory());
        handlerFactory.setApplicationContext(applicationContext);
        reactor.setContextHandlerFactory(handlerFactory);
        reactor.setApplicationContext(applicationContext);
    }

    @Test
    public void handleApiEvent_create_started() {
        Api api = new ApiDefinitionBuilder().name("my-api")
                .proxy(new ProxyDefinitionBuilder().contextPath("/team").target("http://localhost:8083").build()).build();

        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.DEPLOY, api);

        reactor.onEvent(evt);

        verify(reactor).createHandler(api);
        verify(handlerFactory).create(api);
        verify(reactor, never()).removeHandler(api);
    }

    @Test
    public void handleApiEvent_create_stopped() {
        Api api = new ApiDefinitionBuilder().name("my-api")
                .proxy(new ProxyDefinitionBuilder().contextPath("/team").build()).enabled(false).build();

        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.DEPLOY, api);

        reactor.onEvent(evt);

        verify(reactor).createHandler(api);
        verify(handlerFactory, never()).create(api);
        verify(reactor, never()).removeHandler(api);
    }

    @Test
    public void handleApiEvent_update_stopped() {
        Api api = new ApiDefinitionBuilder().name("my-api")
                .proxy(new ProxyDefinitionBuilder().contextPath("/team").build()).enabled(false).build();

        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.UPDATE, api);

        reactor.onEvent(evt);

//        verify(reactor).removeHandler(api);
        verify(reactor).createHandler(api);
        verify(handlerFactory, never()).create(api);
    }

    @Test
    public void handleApiEvent_update_started() {
        Api api = new ApiDefinitionBuilder().name("my-api")
                .proxy(new ProxyDefinitionBuilder().contextPath("/team").target("http://localhost:8083").build()).build();

        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.UPDATE, api);

        reactor.onEvent(evt);

//        verify(reactor).removeHandler(api);
        verify(reactor).createHandler(api);
        verify(handlerFactory).create(api);
    }

    @Test
    public void handleApiEvent_remove() {
        Api api = new ApiDefinitionBuilder().name("my-api")
                .proxy(new ProxyDefinitionBuilder().contextPath("/team").build()).build();

        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.UNDEPLOY, api);

        reactor.onEvent(evt);

        verify(reactor).removeHandler(api);
        verify(reactor, never()).createHandler(api);
    }
}
