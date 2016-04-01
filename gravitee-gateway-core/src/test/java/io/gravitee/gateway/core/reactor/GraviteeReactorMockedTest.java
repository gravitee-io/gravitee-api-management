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

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.Proxy;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.definition.Api;
import io.gravitee.gateway.core.reactor.handler.ReactorHandler;
import io.gravitee.gateway.core.reactor.handler.ReactorHandlerManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactorMockedTest {

    @InjectMocks
    private GraviteeReactor reactor = new GraviteeReactor();

    @Mock
    private EventManager eventManager;

    @Mock
    private ReactorHandlerManager reactorHandlerManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        reactor.clearHandlers();
    }

    @Test
    public void test_uniqContextPath() {
        addMockApiWithContextPath("Teams", "/teams");

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.path()).thenReturn("/teams");

        ReactorHandler handler = reactor.bestHandler(request);
        Assert.assertEquals("/teams/", handler.contextPath());
    }

    @Test
    public void test_uniqContextPath_unknownRequestPath() {
        addMockApiWithContextPath("Teams", "/teams");

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.path()).thenReturn("/team");

        ReactorHandler handler = reactor.bestHandler(request);
        // assertNull means NotFoundHandler (status code 404)
        Assert.assertNull(handler);
    }

    @Test
    public void test_multipleContextPath_validRequestPath() {
        addMockApiWithContextPath("Teams 1", "/teams");
        addMockApiWithContextPath("Teams 2", "/teams2");

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.path()).thenReturn("/teams");

        ReactorHandler handler = reactor.bestHandler(request);
        Assert.assertEquals("/teams/", handler.contextPath());
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath() {
        addMockApiWithContextPath("Teams 1", "/teams");
        addMockApiWithContextPath("Teams 2", "/teams2");

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.path()).thenReturn("/team");

        ReactorHandler handler = reactor.bestHandler(request);
        // assertNull means NotFoundHandler (status code 404)
        Assert.assertNull(handler);
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath2() {
        addMockApiWithContextPath("Teams 1", "/teams");
        addMockApiWithContextPath("Teams 2", "/teams2");

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.path()).thenReturn("/teamss");

        ReactorHandler handler = reactor.bestHandler(request);
        // assertNull means NotFoundHandler (status code 404)
        Assert.assertNull(handler);
    }

    @Test
    public void test_multipleContextPath_extraSeparatorRequestPath() {
        addMockApiWithContextPath("Teams 2", "/teams2");
        addMockApiWithContextPath("Teams 1", "/teams");

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.path()).thenReturn("/teams/");

        ReactorHandler handler = reactor.bestHandler(request);
        Assert.assertEquals("/teams/", handler.contextPath());
    }

    @Test
    public void test_multipleContextPath_extraSeparatorUnknownRequestPath() {
        addMockApiWithContextPath("Teams 2", "/teams2");
        addMockApiWithContextPath("Teams 1", "/teams");

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.path()).thenReturn("/teamss/");

        ReactorHandler handler = reactor.bestHandler(request);
        // assertNull means NotFoundHandler (status code 404)
        Assert.assertNull(handler);
    }

    private void addMockApiWithContextPath(String apiName, String contextPath) {
        try {
            Api api = Mockito.mock(Api.class);
            Proxy proxy = Mockito.mock(Proxy.class);

            Mockito.when(api.getProxy()).thenReturn(proxy);
            Mockito.when(api.getId()).thenReturn(apiName);
            Mockito.when(api.isEnabled()).thenReturn(true);
            Mockito.when(proxy.getContextPath()).thenReturn(contextPath);

            ReactorHandler reactorHandler = Mockito.mock(ReactorHandler.class);

            Mockito.when(reactorHandler.start()).thenReturn(reactorHandler);
            Mockito.when(reactorHandlerManager.create(Mockito.any(Api.class))).thenReturn(reactorHandler);
            Mockito.when(reactorHandler.contextPath()).thenReturn(contextPath + '/');
            reactor.createHandler(api);
        } catch (Exception e) {
            // Do nothing
        }
    }
}
