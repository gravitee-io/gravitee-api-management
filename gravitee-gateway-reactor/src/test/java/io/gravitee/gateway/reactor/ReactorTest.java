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
package io.gravitee.gateway.reactor;

import io.gravitee.common.event.Event;
import io.gravitee.common.event.impl.SimpleEvent;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.reactor.handler.AbstractReactorHandler;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.handler.ReactorHandlerResolver;
import io.gravitee.gateway.reactor.impl.DefaultReactor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;


/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ReactorTest {

    @InjectMocks
    private DefaultReactor reactor;

    @Mock
    private ReactorHandlerResolver handlerResolver;

    @Mock
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void processRequest_startedApi() throws Exception {
        Request request = mock(Request.class);
        when(request.method()).thenReturn(HttpMethod.GET);
        when(request.path()).thenReturn("/team");

        when(handlerResolver.resolve(request)).thenReturn(new AbstractReactorHandler() {
            @Override
            public CompletableFuture<Response> handle(Request request, Response response) {
                Response proxyResponse = mock(Response.class);
                when(proxyResponse.status()).thenReturn(HttpStatusCode.OK_200);

                return CompletableFuture.completedFuture(proxyResponse);
            }
        });

        final CountDownLatch lock = new CountDownLatch(1);
        reactor.process(request, mock(Response.class)).thenAccept(resp -> {
            Assert.assertEquals(HttpStatusCode.OK_200, resp.status());
            lock.countDown();
        });

        Assert.assertEquals(true, lock.await(10000, TimeUnit.MILLISECONDS));
    }

    @Test
    public void handleEvent_create() {
        Reactable reactable = mock(Reactable.class);
        Event<ReactorEvent, Reactable> evt = new SimpleEvent<>(ReactorEvent.DEPLOY, reactable);

        reactor.onEvent(evt);

        verify(reactorHandlerRegistry).create(reactable);
    }

    @Test
    public void handleEvent_update() {
        Reactable reactable = mock(Reactable.class);
        Event<ReactorEvent, Reactable> evt = new SimpleEvent<>(ReactorEvent.UPDATE, reactable);

        reactor.onEvent(evt);

        verify(reactorHandlerRegistry).update(reactable);
    }

    @Test
    public void handleEvent_remove() {
        Reactable reactable = mock(Reactable.class);
        Event<ReactorEvent, Reactable> evt = new SimpleEvent<>(ReactorEvent.UNDEPLOY, reactable);

        reactor.onEvent(evt);

        verify(reactorHandlerRegistry).remove(reactable);
    }
}
