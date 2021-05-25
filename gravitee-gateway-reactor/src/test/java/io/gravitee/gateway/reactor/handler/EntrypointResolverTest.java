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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactor.handler.impl.DefaultEntrypointResolver;
import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EntrypointResolverTest {

    @Mock
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @InjectMocks
    private DefaultEntrypointResolver handlerResolver;

    @Mock
    private ExecutionContext context;

    @Mock
    private Request request;

    @Mock
    private HttpHeaders httpHeaders;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(context.request()).thenReturn(request);
        when(request.headers()).thenReturn(httpHeaders);
    }

    @After
    public void tearDown() {
        reactorHandlerRegistry.clear();
    }

    @Test
    public void test_uniqContextPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));

        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(Collections.singletonList(entrypoint1));
        when(request.path()).thenReturn("/teams");

        assertEquals(entrypoint1, handlerResolver.resolve(context));
        verify(context, times(1)).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), eq(entrypoint1));
    }

    @Test
    public void test_uniqContextPath_unknownRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));

        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(Collections.singletonList(entrypoint1));
        when(request.path()).thenReturn("/team");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleContextPath_validRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(Arrays.asList(entrypoint1, entrypoint2));
        when(request.path()).thenReturn("/teams");

        assertEquals(entrypoint1, handlerResolver.resolve(context));
        verify(context, times(1)).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), eq(entrypoint1));
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(Arrays.asList(entrypoint1, entrypoint2));
        when(request.path()).thenReturn("/team");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleContextPath_unknownRequestPath2() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(Arrays.asList(entrypoint1, entrypoint2));
        when(request.path()).thenReturn("/teamss");

        assertNull(handlerResolver.resolve(context));
        verify(context, never()).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), any());
    }

    @Test
    public void test_multipleContextPath_extraSeparatorRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(Arrays.asList(entrypoint1, entrypoint2));
        when(request.path()).thenReturn("/teams/");

        assertEquals(entrypoint1, handlerResolver.resolve(context));
        verify(context, times(1)).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), eq(entrypoint1));
    }

    @Test
    public void test_multipleContextPath_extraSeparatorUnknownRequestPath() {
        DummyReactorandlerEntrypoint entrypoint1 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams"));
        DummyReactorandlerEntrypoint entrypoint2 = new DummyReactorandlerEntrypoint(new VirtualHost("/teams2"));

        when(reactorHandlerRegistry.getEntrypoints()).thenReturn(Arrays.asList(entrypoint1, entrypoint2));

        when(request.path()).thenReturn("/teamss/");

        verify(context, never()).setAttribute(eq(DefaultEntrypointResolver.ATTR_ENTRYPOINT), any());
        assertNull(handlerResolver.resolve(context));
    }

    private class DummyReactorandlerEntrypoint implements HandlerEntrypoint {

        private final Entrypoint entrypoint;

        public DummyReactorandlerEntrypoint(Entrypoint entrypoint) {
            this.entrypoint = entrypoint;
        }

        @Override
        public ReactorHandler target() {
            return null;
        }

        @Override
        public String path() {
            return null;
        }

        @Override
        public int priority() {
            return entrypoint.priority();
        }

        @Override
        public boolean accept(Request request) {
            return entrypoint.accept(request);
        }
    }
}
