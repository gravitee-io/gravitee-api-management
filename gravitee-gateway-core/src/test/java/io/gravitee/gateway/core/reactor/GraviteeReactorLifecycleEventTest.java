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

import io.gravitee.gateway.core.AbstractCoreTest;
import io.gravitee.gateway.core.builder.ApiBuilder;
import io.gravitee.gateway.core.event.Event;
import io.gravitee.gateway.core.event.impl.SimpleEvent;
import io.gravitee.gateway.core.handler.impl.ApiHandlerFactory;
import io.gravitee.gateway.core.model.Api;
import io.gravitee.gateway.core.service.ApiLifecycleEvent;
import org.junit.Test;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactorLifecycleEventTest extends AbstractCoreTest {

    @Test
    public void handleStartApiEvent() {
        GraviteeReactor reactor = spy(new AsyncGraviteeReactor());

        ApiHandlerFactory handlerFactory = new ApiHandlerFactory();
        handlerFactory.setApplicationContext(applicationContext);
        reactor.setHandlerFactory(handlerFactory);
        reactor.setApplicationContext(applicationContext);

        Api api = new ApiBuilder().name("my-api").origin("http://localhost/team").build();
        Event<ApiLifecycleEvent, Api> evt = new SimpleEvent<>(ApiLifecycleEvent.START, api);

        reactor.onEvent(evt);

        verify(reactor).addHandler(eq(api));
        verify(reactor, never()).removeHandler(eq(api));
    }

    @Test
    public void handleStopApiEvent() {
        GraviteeReactor reactor = spy(new AsyncGraviteeReactor());
        reactor.setApplicationContext(applicationContext);

        Api api = new ApiBuilder().name("my-api").origin("http://localhost/team").build();
        Event<ApiLifecycleEvent, Api> evt = new SimpleEvent<>(ApiLifecycleEvent.STOP, api);

        reactor.onEvent(evt);

        verify(reactor).removeHandler(eq(api));
        verify(reactor, never()).addHandler(eq(api));
    }
}
