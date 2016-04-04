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
package io.gravitee.gateway.reactor.handler;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerResolver;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ReactorHandlerResolverTest {

    @Mock
    private ReactorHandlerRegistry handlerRegistry;

    @InjectMocks
    private DefaultReactorHandlerResolver handlerResolver;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        handlerRegistry.clear();
    }

    @Test
    public void test_uniqContextPath() {
        Collection<ReactorHandler> handlers = new ArrayList<>(
                new ArrayList<ReactorHandler>() {
                    {
                        add(createMockHandler("/teams"));
                    }
                }
        );
        when(handlerRegistry.getReactorHandlers()).thenReturn(handlers);

        Request request = Mockito.mock(Request.class);
        when(request.path()).thenReturn("/teams");

        ReactorHandler handler = handlerResolver.resolve(request);
        Assert.assertEquals("/teams/", handler.contextPath());
    }

    @Test
    public void test_uniqContextPath_unknownRequestPath() {
        Collection<ReactorHandler> handlers = new ArrayList<>(
                new ArrayList<ReactorHandler>() {
                    {
                        add(createMockHandler("/teams"));
                    }
                }
        );
        when(handlerRegistry.getReactorHandlers()).thenReturn(handlers);

        Request request = Mockito.mock(Request.class);
        when(request.path()).thenReturn("/team");

        ReactorHandler handler = handlerResolver.resolve(request);
        Assert.assertNull(handler);
    }

    @Test
    public void test_multipleContextPath_validRequestPath() {
        Collection<ReactorHandler> handlers = new ArrayList<>(
                new ArrayList<ReactorHandler>() {
                    {
                        add(createMockHandler("/teams"));
                        add(createMockHandler("/teams2"));
                    }
                }
        );
        when(handlerRegistry.getReactorHandlers()).thenReturn(handlers);

        Request request = Mockito.mock(Request.class);
        when(request.path()).thenReturn("/teams");

        ReactorHandler handler = handlerResolver.resolve(request);
        Assert.assertEquals("/teams/", handler.contextPath());
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath() {
        Collection<ReactorHandler> handlers = new ArrayList<>(
                new ArrayList<ReactorHandler>() {
                    {
                        add(createMockHandler("/teams"));
                        add(createMockHandler("/teams2"));
                    }
                }
        );
        when(handlerRegistry.getReactorHandlers()).thenReturn(handlers);

        Request request = Mockito.mock(Request.class);
        when(request.path()).thenReturn("/team");

        ReactorHandler handler = handlerResolver.resolve(request);
        Assert.assertNull(handler);
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath2() {
        Collection<ReactorHandler> handlers = new ArrayList<>(
                new ArrayList<ReactorHandler>() {
                    {
                        add(createMockHandler("/teams"));
                        add(createMockHandler("/teams2"));
                    }
                }
        );
        when(handlerRegistry.getReactorHandlers()).thenReturn(handlers);

        Request request = Mockito.mock(Request.class);
        when(request.path()).thenReturn("/teamss");

        ReactorHandler handler = handlerResolver.resolve(request);
        Assert.assertNull(handler);
    }

    @Test
    public void test_multipleContextPath_extraSeparatorRequestPath() {
        Collection<ReactorHandler> handlers = new ArrayList<>(
                new ArrayList<ReactorHandler>() {
                    {
                        add(createMockHandler("/teams2"));
                        add(createMockHandler("/teams"));
                    }
                }
        );
        when(handlerRegistry.getReactorHandlers()).thenReturn(handlers);

        Request request = Mockito.mock(Request.class);
        when(request.path()).thenReturn("/teams/");

        ReactorHandler handler = handlerResolver.resolve(request);
        Assert.assertEquals("/teams/", handler.contextPath());
    }

    @Test
    public void test_multipleContextPath_extraSeparatorUnknownRequestPath() {
        Collection<ReactorHandler> handlers = new ArrayList<>(
                new ArrayList<ReactorHandler>() {
                    {
                        add(createMockHandler("/teams2"));
                        add(createMockHandler("/teams"));
                    }
                }
        );
        when(handlerRegistry.getReactorHandlers()).thenReturn(handlers);

        Request request = Mockito.mock(Request.class);
        Mockito.when(request.path()).thenReturn("/teamss/");

        ReactorHandler handler = handlerResolver.resolve(request);
        Assert.assertNull(handler);
    }

    private ReactorHandler createMockHandler(String contextPath) {
        ReactorHandler handler = mock(ReactorHandler.class);
        when(handler.contextPath()).thenReturn(contextPath + '/');
        return handler;
    }
}
