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
import io.gravitee.gateway.core.reactor.handler.ContextHandlerFactory;
import io.gravitee.gateway.core.reactor.handler.ContextReactorHandler;
import io.gravitee.gateway.core.reactor.handler.ReactorHandler;
import io.gravitee.gateway.core.reactor.handler.impl.ApiReactorHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class GraviteeReactorMockedTest {

    @InjectMocks
    private GraviteeReactor reactor = new GraviteeReactor();

    @Mock
    private EventManager eventManager;

    @Mock
    private ContextHandlerFactory contextHandlerFactory;

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

        ContextReactorHandler handler = (ContextReactorHandler) reactor.bestHandler(request);
        Assert.assertEquals("/teams", handler.getContextPath());
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

        ContextReactorHandler handler = (ContextReactorHandler) reactor.bestHandler(request);
        Assert.assertEquals("/teams", handler.getContextPath());
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

        ContextReactorHandler handler = (ContextReactorHandler) reactor.bestHandler(request);
        Assert.assertEquals("/teams", handler.getContextPath());
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

            ApiReactorHandler apiReactorHandler = Mockito.mock(ApiReactorHandler.class);

            Mockito.when(apiReactorHandler.start()).thenReturn(apiReactorHandler);
            Mockito.when(contextHandlerFactory.create(Mockito.any(Api.class))).thenReturn(apiReactorHandler);
            Mockito.when(apiReactorHandler.getContextPath()).thenReturn(contextPath);
            reactor.createHandler(api);
        } catch (Exception e) {
            // Do nothing
        }
    }
}
