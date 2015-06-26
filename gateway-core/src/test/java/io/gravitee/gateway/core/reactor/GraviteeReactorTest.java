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

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.core.AbstractCoreTest;
import io.gravitee.gateway.core.builder.ApiBuilder;
import io.gravitee.gateway.core.event.Event;
import io.gravitee.gateway.core.event.impl.SimpleEvent;
import io.gravitee.gateway.core.external.ApiExternalResource;
import io.gravitee.gateway.core.external.ApiServlet;
import io.gravitee.gateway.core.http.ServerRequest;
import io.gravitee.gateway.core.service.ApiLifecycleEvent;
import io.gravitee.model.Api;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import rx.Observable;

import java.net.URI;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactorTest extends AbstractCoreTest {

    @ClassRule
    public static final ApiExternalResource SERVER_MOCK = new ApiExternalResource("8083", ApiServlet.class, "/*", null);

    @Autowired
    private GraviteeReactor<Observable<Response>> reactor;

    @Before
    public void setUp() {
        reactor.clearHandlers();
    }

    @Test
    public void handleStartApiEvent() {
        GraviteeReactor reactor = spy(new AsyncGraviteeReactor());
        Api api = new ApiBuilder().name("my-api").origin("http://localhost/team").build();
        Event<ApiLifecycleEvent, Api> evt = new SimpleEvent<>(ApiLifecycleEvent.START, api);

        reactor.onEvent(evt);

        verify(reactor).addHandler(eq(api));
        verify(reactor, never()).removeHandler(eq(api));
    }

    @Test
    public void handleStopApiEvent() {
        GraviteeReactor reactor = spy(new AsyncGraviteeReactor());
        Api api = new ApiBuilder().name("my-api").origin("http://localhost/team").build();
        Event<ApiLifecycleEvent, Api> evt = new SimpleEvent<>(ApiLifecycleEvent.STOP, api);

        reactor.onEvent(evt);

        verify(reactor).removeHandler(eq(api));
        verify(reactor, never()).addHandler(eq(api));
    }

    @Test
    public void handleNotFoundRequest() {
        // Register new API endpoint
        reactor.onEvent(new Event<ApiLifecycleEvent, Api>() {
            @Override
            public Api content() {
                return new ApiBuilder()
                        .name("my-team-api")
                        .origin("http://localhost/team")
                        .target("http://localhost/myapi")
                        .build();
            }

            @Override
            public ApiLifecycleEvent type() {
                return ApiLifecycleEvent.START;
            }
        });

        ServerRequest req = new ServerRequest();
        req.setRequestURI(URI.create("http://localhost/unknown_path"));
        req.setMethod(HttpMethod.GET);

        Response resp = reactor.process(req).toBlocking().single();
        Assert.assertEquals(HttpStatusCode.NOT_FOUND_404, resp.status());
    }

    @Test
    public void handleCorrectRequest() {
        // Register new API endpoint
        reactor.onEvent(new Event<ApiLifecycleEvent, Api>() {
            @Override
            public Api content() {
                return new ApiBuilder()
                        .name("my-team-api")
                        .origin("http://localhost/team")
                        .target("http://localhost:8083/myapi")
                        .build();
            }

            @Override
            public ApiLifecycleEvent type() {
                return ApiLifecycleEvent.START;
            }
        });

        ServerRequest req = new ServerRequest();
        req.setRequestURI(URI.create("http://localhost/team"));
        req.setMethod(HttpMethod.GET);

        Response resp = reactor.process(req).toBlocking().single();
        Assert.assertEquals(HttpStatusCode.OK_200, resp.status());
    }
}
