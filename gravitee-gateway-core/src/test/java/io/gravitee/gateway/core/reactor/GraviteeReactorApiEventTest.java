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
import io.gravitee.gateway.core.builder.ApiDefinitionBuilder;
import io.gravitee.gateway.core.builder.ProxyDefinitionBuilder;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.event.ApiEvent;
import io.gravitee.gateway.core.reactor.handler.ReactorHandlerRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactorApiEventTest {

    @InjectMocks
    private GraviteeReactor reactor;

    @Mock
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void handleEvent_create() {
        Api api = new ApiDefinitionBuilder().name("my-api")
                .proxy(new ProxyDefinitionBuilder().contextPath("/team").target("http://localhost:8083").build()).build();

        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.DEPLOY, api);

        reactor.onEvent(evt);

        verify(reactorHandlerRegistry).create(api);
    }

    @Test
    public void handleEvent_update() {
        Api api = new ApiDefinitionBuilder().name("my-api")
                .proxy(new ProxyDefinitionBuilder().contextPath("/team").target("http://localhost:8083").build()).build();

        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.UPDATE, api);

        reactor.onEvent(evt);

        verify(reactorHandlerRegistry).update(api);
    }

    @Test
    public void handleEvent_remove() {
        Api api = new ApiDefinitionBuilder().name("my-api")
                .proxy(new ProxyDefinitionBuilder().contextPath("/team").build()).build();

        Event<ApiEvent, Api> evt = new SimpleEvent<>(ApiEvent.UNDEPLOY, api);

        reactor.onEvent(evt);

        verify(reactorHandlerRegistry).remove(api);
    }
}
