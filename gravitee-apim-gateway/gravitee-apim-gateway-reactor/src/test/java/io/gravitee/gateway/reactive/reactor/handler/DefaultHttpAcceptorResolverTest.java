/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.reactive.reactor.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactor.handler.HttpAcceptor;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultHttpAcceptorResolverTest {

    protected static final String HOST = "gravitee.io";
    protected static final String PATH = "/path";
    private static final String SERVER_ID = "http";

    @Mock
    private ReactorHandlerRegistry handlerRegistry;

    private DefaultHttpAcceptorResolver cut;

    @BeforeEach
    public void init() {
        cut = new DefaultHttpAcceptorResolver(handlerRegistry);
    }

    @Test
    public void shouldIteratorOverEntrypointToResolveHandler() {
        final HttpAcceptor handler1 = mock(HttpAcceptor.class);
        final HttpAcceptor handler2 = mock(HttpAcceptor.class);
        final HttpAcceptor handler3 = mock(HttpAcceptor.class);

        when(handler1.accept(HOST, PATH, SERVER_ID)).thenReturn(false);
        when(handler2.accept(HOST, PATH, SERVER_ID)).thenReturn(false);
        when(handler3.accept(HOST, PATH, SERVER_ID)).thenReturn(true);

        when(handlerRegistry.getAcceptors(HttpAcceptor.class)).thenReturn(List.of(handler1, handler2, handler3));

        final HttpAcceptor resolvedHandler = cut.resolve(HOST, PATH, SERVER_ID);

        assertEquals(handler3, resolvedHandler);
    }

    @Test
    public void shouldReturnNullHanlderWhenNoHandlerCanHandleTheRequest() {
        final HttpAcceptor handler1 = mock(HttpAcceptor.class);
        final HttpAcceptor handler2 = mock(HttpAcceptor.class);
        final HttpAcceptor handler3 = mock(HttpAcceptor.class);

        when(handler1.accept(HOST, PATH, SERVER_ID)).thenReturn(false);
        when(handler2.accept(HOST, PATH, SERVER_ID)).thenReturn(false);
        when(handler3.accept(HOST, PATH, SERVER_ID)).thenReturn(false);

        when(handlerRegistry.getAcceptors(HttpAcceptor.class)).thenReturn(List.of(handler1, handler2, handler3));

        final HttpAcceptor resolvedHandler = cut.resolve(HOST, PATH, SERVER_ID);

        assertNull(resolvedHandler);
    }
}
