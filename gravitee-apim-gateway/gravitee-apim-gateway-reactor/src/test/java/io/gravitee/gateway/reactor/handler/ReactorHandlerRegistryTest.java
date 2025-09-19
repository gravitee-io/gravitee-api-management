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
package io.gravitee.gateway.reactor.handler;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ReactorHandlerRegistryTest {

    @InjectMocks
    private DefaultReactorHandlerRegistry reactorHandlerRegistry;

    @Mock
    private ReactorFactoryManager reactorHandlerFactoryManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHaveOneHttpAcceptor() {
        Reactable reactable = createReactable("reactable1");

        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(1, httpAcceptorHandlers.size());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_singleReactable() {
        Reactable reactable = createReactable("reactable1");

        ReactorHandler handler = createReactorHandler(
            new OverlappingHttpAcceptor("/products/v1"),
            new OverlappingHttpAcceptor("/products/v2")
        );
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint(null, "/products/v2/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/products/v1/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_singleReactable_withVirtualHost() {
        Reactable reactable = createReactable("reactable1");

        ReactorHandler handler = createReactorHandler(
            new DefaultHttpAcceptor("/products/v1"),
            new DefaultHttpAcceptor("api.gravitee.io", "/products/v2")
        );
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/products/v2/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/products/v1/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_duplicateContextPath() {
        Reactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable2");
        ReactorHandler handler2 = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));

        reactorHandlerRegistry.create(reactable2);

        Assert.assertEquals(2, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());
        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoEntrypoints() {
        Reactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable2");
        ReactorHandler handler2 = createReactorHandler("/products");
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        Assert.assertEquals(2, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());

        // Paths are sorted in natural order so "/" takes priority over "/products".
        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/products/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_duplicateContextPath_withVirtualHost() {
        Reactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable2");
        ReactorHandler handler2 = createReactorHandler("api.gravitee.io", "/");
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        // Paths "/" are equivalent but virtualhost takes priority over simple path.
        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        final HttpAcceptor first = httpAcceptorHandlerIterator.next();
        assertEntryPoint("api.gravitee.io", "/", first);
        Assert.assertEquals(1001, first.priority());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_updateReactable() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable2");
        ReactorHandler handler2 = createReactorHandler("api.gravitee.io", "/");
        when(reactorHandlerFactoryManager.create(updateReactable)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.update(updateReactable);

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveMultipleHttpAcceptors_multipleCreateReactable() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable("reactable2");
        ReactorHandler handler2 = createReactorHandler(
            new OverlappingHttpAcceptor("api.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api1.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api2.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api3.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api4.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("apiX.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api10.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api11.gravitee.io", "/a")
        );
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable("reactable3");
        ReactorHandler handler3 = createReactorHandler(
            new OverlappingHttpAcceptor("api.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api1.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api2.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api3.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api4.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("apiX.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api10.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api11.gravitee.io", "/a-v1")
        );
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(List.of(handler3));
        reactorHandlerRegistry.create(reactable3);

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(17, httpAcceptorHandlers.size());
        assertEntryPoint("apiX.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveMultipleHttpAcceptors_multipleVhostsWithSubPaths() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable("reactable2");
        ReactorHandler handler2 = createReactorHandler(
            new OverlappingHttpAcceptor("api.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("api1.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("api2.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("api3.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("api4.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("apiX.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("api10.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("api11.gravitee.io", "/a/b/c")
        );
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable("reactable3");
        ReactorHandler handler3 = createReactorHandler(
            new OverlappingHttpAcceptor("api.gravitee.io", "/a/b/a"),
            new OverlappingHttpAcceptor("api1.gravitee.io", "/a/b/b"),
            new OverlappingHttpAcceptor("api2.gravitee.io", "/a/b/d"),
            new OverlappingHttpAcceptor("api3.gravitee.io", "/a/b/e"),
            new OverlappingHttpAcceptor("api4.gravitee.io", "/a/b/f"),
            new OverlappingHttpAcceptor("apiX.gravitee.io", "/a/b/c1/sub"),
            new OverlappingHttpAcceptor("api10.gravitee.io", "/a/b/c1/sub"),
            new OverlappingHttpAcceptor("api11.gravitee.io", "/a/b/c1/sub")
        );

        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(List.of(handler3));
        reactorHandlerRegistry.create(reactable3);

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(17, httpAcceptorHandlers.size());
        assertEntryPoint("apiX.gravitee.io", "/a/b/c1/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/a/b/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/b/f/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/b/e/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/b/d/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/b/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/b/c1/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/b/c1/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveMultipleHttpAcceptors_multipleVhostsWithSubPathsAndWildcards() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler(
            new OverlappingHttpAcceptor(null, "/"),
            new OverlappingHttpAcceptor("*.gravitee.io", "/"),
            new OverlappingHttpAcceptor("test.gravitee.io", "/")
        );
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable("reactable2");
        ReactorHandler handler2 = createReactorHandler(
            new OverlappingHttpAcceptor("test.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("test.gravitee.io", "/a/b"),
            new OverlappingHttpAcceptor("test.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("1.test.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("2.test.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("3.test.gravitee.io", "/a/b/c")
        );
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable("reactable3");
        ReactorHandler handler3 = createReactorHandler(
            new OverlappingHttpAcceptor("*.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("*.gravitee.io", "/a/b/c/sub"),
            new OverlappingHttpAcceptor("*.foo.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("*.foo.gravitee.io", "/a/b/c/sub"),
            new OverlappingHttpAcceptor("*.test.gravitee.io", "/a/b/c"),
            new OverlappingHttpAcceptor("*.test.gravitee.io", "/a/b/c/sub")
        );
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(List.of(handler3));
        reactorHandlerRegistry.create(reactable3);

        DummyReactable reactable4 = createReactable("reactable4");
        ReactorHandler handler4 = createReactorHandler(
            new OverlappingHttpAcceptor(null, "/a/b/a"),
            new OverlappingHttpAcceptor(null, "/a/b/b"),
            new OverlappingHttpAcceptor(null, "/a/b/c"),
            new OverlappingHttpAcceptor(null, "/a/b/a/sub"),
            new OverlappingHttpAcceptor(null, "/a/b/b/sub"),
            new OverlappingHttpAcceptor(null, "/a/b/c/sub")
        );

        when(reactorHandlerFactoryManager.create(reactable4)).thenReturn(List.of(handler4));
        reactorHandlerRegistry.create(reactable4);

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(21, httpAcceptorHandlers.size());
        assertEntryPoint("3.test.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("2.test.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("1.test.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(".test.gravitee.io", "/a/b/c/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(".test.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("test.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("test.gravitee.io", "/a/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("test.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("test.gravitee.io", "/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(".foo.gravitee.io", "/a/b/c/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(".foo.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(".gravitee.io", "/a/b/c/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(".gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(".gravitee.io", "/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/a/b/c/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/a/b/b/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/a/b/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/a/b/a/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/a/b/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveMultipleHttpAcceptors_multipleUpdateReactable() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable("reactable2");
        ReactorHandler handler2 = createReactorHandler(
            new OverlappingHttpAcceptor("api.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api1.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api2.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api3.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api4.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("apiX.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api10.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api11.gravitee.io", "/a")
        );
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable("reactable3");
        ReactorHandler handler3 = createReactorHandler(
            new OverlappingHttpAcceptor("api.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api1.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api2.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api3.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api4.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("apiX.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api10.gravitee.io", "/a-v1"),
            new OverlappingHttpAcceptor("api11.gravitee.io", "/a-v1")
        );
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(List.of(handler3));
        reactorHandlerRegistry.create(reactable3);

        reactable2 = createReactable("reactable2");
        handler2 = createReactorHandler(
            new OverlappingHttpAcceptor("api.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api1.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api2.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api3.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api4.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("apiX.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api10.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api11.gravitee.io", "/b")
        );
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.update(reactable2);

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(17, httpAcceptorHandlers.size());
        assertEntryPoint("apiX.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveOneEntrypoint_updateSameReactableWithVHost() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Assert.assertEquals(1, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());

        for (int i = 0; i < 100; i++) {
            final DummyReactable toUpdate = createReactable("reactable1");
            final ReactorHandler handlerUpdate = createReactorHandler("api.gravitee.io", "/new-path");
            when(reactorHandlerFactoryManager.create(toUpdate)).thenReturn(List.of(handlerUpdate));
            reactorHandlerRegistry.update(toUpdate);
            Assert.assertEquals(
                "Size of acceptors list should be 1 (i=" + i + ")",
                1,
                reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size()
            );
        }

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(1, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/new-path/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveOneEntrypoint_updateSameReactableWithContextPath() throws InterruptedException {
        DummyReactable reactable = createReactable("reactable");
        ReactorHandler handler = createReactorHandler("/c");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Assert.assertEquals(1, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());

        for (int i = 0; i < 100; i++) {
            final Reactable toUpdate = createReactable("reactable");
            final ReactorHandler handlerUpdate = createReactorHandler("/c");
            when(reactorHandlerFactoryManager.create(toUpdate)).thenReturn(List.of(handlerUpdate));
            reactorHandlerRegistry.update(toUpdate);
            Assert.assertEquals(
                "Size of acceptors list should be 1 (i=" + i + ")",
                1,
                reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size()
            );
        }

        final Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(1, httpAcceptorHandlers.size());
        assertEntryPoint(null, "/c/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHave100HttpAcceptors_createThenUpdateMultiThreads() throws InterruptedException {
        ExecutorService executorService = null;

        try {
            executorService = Executors.newFixedThreadPool(10);
            List<Runnable> runnables = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                final DummyReactable toCreate = createReactable("reactable" + i);
                final ReactorHandler handler = createReactorHandler("api.gravitee.io", "/new-path" + i);
                when(reactorHandlerFactoryManager.create(toCreate)).thenReturn(List.of(handler));
                runnables.add(() -> reactorHandlerRegistry.create(toCreate));
            }

            for (Runnable r : runnables) {
                executorService.submit(r);
            }

            // Wait for all creations before going further.
            executorService.shutdown();
            assertTrue(executorService.awaitTermination(10000, TimeUnit.MILLISECONDS));

            Assert.assertEquals(100, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());

            executorService = Executors.newFixedThreadPool(10);
            runnables = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                final DummyReactable toUpdate = createReactable("reactable" + i);
                final ReactorHandler handler = createReactorHandler("api.gravitee.io", "/new-path" + i);
                when(reactorHandlerFactoryManager.create(toUpdate)).thenReturn(List.of(handler));
                runnables.add(() -> reactorHandlerRegistry.update(toUpdate));
            }

            for (Runnable r : runnables) {
                executorService.submit(r);
            }

            executorService.shutdown();
            assertTrue(executorService.awaitTermination(10000, TimeUnit.MILLISECONDS));

            Assert.assertEquals(100, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());
        } finally {
            if (executorService != null) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    public void shouldHaveNoEntrypoint_removeSameReactable() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable1");
        reactorHandlerRegistry.remove(updateReactable);

        Assert.assertEquals(0, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());
    }

    @Test
    public void shouldHaveNoEntrypoint_removeUnknownEntrypoint() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.remove(reactable);

        Assert.assertEquals(0, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());
    }

    @Test
    public void shouldHaveMultipleHttpAcceptors_multipleRemoveReactable() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler("/");
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable("reactable2");
        ReactorHandler handler2 = createReactorHandler(
            new OverlappingHttpAcceptor("api.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api1.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api2.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api3.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api4.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("apiX.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api10.gravitee.io", "/a"),
            new OverlappingHttpAcceptor("api11.gravitee.io", "/a")
        );
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable("reactable3");
        ReactorHandler handler3 = createReactorHandler(
            new OverlappingHttpAcceptor("api.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api1.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api2.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api3.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api4.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("apiX.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api10.gravitee.io", "/b"),
            new OverlappingHttpAcceptor("api11.gravitee.io", "/b")
        );
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(List.of(handler3));
        reactorHandlerRegistry.create(reactable3);

        Collection<HttpAcceptor> httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        Iterator<HttpAcceptor> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(17, httpAcceptorHandlers.size());
        assertEntryPoint("apiX.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());

        reactorHandlerRegistry.remove(reactable);
        httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(16, httpAcceptorHandlers.size());
        assertEntryPoint("apiX.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());

        reactorHandlerRegistry.remove(reactable2);
        httpAcceptorHandlers = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(8, httpAcceptorHandlers.size());
        assertEntryPoint("apiX.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());

        reactorHandlerRegistry.remove(reactable3);
        Assert.assertEquals(0, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());
    }

    @Test
    public void shouldHaveOneHttpAcceptorAndOneDummyAcceptor_singleReactable() {
        Reactable reactable = createReactable("reactable1");

        ReactorHandler handler = createReactorHandler(new DefaultHttpAcceptor("/products/v1"), new DefaultDummyAcceptor("api1"));
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptor> httpAcceptors = reactorHandlerRegistry.getAcceptors(HttpAcceptor.class);
        final Iterator<HttpAcceptor> httpAcceptorsIterator = httpAcceptors.iterator();
        Assert.assertEquals(1, httpAcceptors.size());
        assertEntryPoint(null, "/products/v1/", httpAcceptorsIterator.next());
        final Collection<DummyAcceptor> dummyAcceptors = reactorHandlerRegistry.getAcceptors(DummyAcceptor.class);
        final Iterator<DummyAcceptor> dummyAcceptorsIterator = dummyAcceptors.iterator();
        Assert.assertEquals(1, httpAcceptors.size());
        Assert.assertEquals("api1", dummyAcceptorsIterator.next().apiId());
    }

    @Test
    public void shouldHaveNoEntrypoints_removeSameReactableWithHttpAndDummyAcceptors() {
        DummyReactable reactable = createReactable("reactable1");
        ReactorHandler handler = createReactorHandler(new DefaultHttpAcceptor("/products/v1"), new DefaultDummyAcceptor("api1"));
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable1");
        reactorHandlerRegistry.remove(updateReactable);

        Assert.assertEquals(0, reactorHandlerRegistry.getAcceptors(HttpAcceptor.class).size());
        Assert.assertEquals(0, reactorHandlerRegistry.getAcceptors(DummyAcceptor.class).size());
    }

    private void assertEntryPoint(String host, String path, HttpAcceptor httpAcceptor) {
        Assert.assertEquals(host, httpAcceptor.host());
        Assert.assertEquals(path, httpAcceptor.path());
    }

    private DummyReactable createReactable(String id) {
        return new DummyReactable(id);
    }

    private ReactorHandler createReactorHandler(String contextPath) {
        return createReactorHandler(new DefaultHttpAcceptor(contextPath));
    }

    private ReactorHandler createReactorHandler(String virtualHost, String contextPath) {
        return createReactorHandler(new DefaultHttpAcceptor(virtualHost, contextPath));
    }

    private ReactorHandler createReactorHandler(Acceptor<?>... httpAcceptors) {
        ReactorHandler handler = mock(ReactorHandler.class);

        List<Acceptor<?>> acceptors = Arrays.stream(httpAcceptors)
            .peek(acceptor -> {
                if (acceptor instanceof DefaultHttpAcceptor) {
                    ((DefaultHttpAcceptor) acceptor).reactor(handler);
                } else if (acceptor instanceof DefaultDummyAcceptor) {
                    ((DefaultDummyAcceptor) acceptor).reactor(handler);
                }
            })
            .collect(Collectors.toList());

        when(handler.acceptors()).thenReturn(acceptors);

        return handler;
    }

    private static class DefaultDummyAcceptor implements DummyAcceptor {

        private final String apiId;
        private ReactorHandler reactor;

        public DefaultDummyAcceptor(final ReactorHandler reactor, final String apiId) {
            this.reactor = reactor;
            this.apiId = apiId;
        }

        public DefaultDummyAcceptor(final String apiId) {
            this(null, apiId);
        }

        public void reactor(ReactorHandler reactor) {
            this.reactor = reactor;
        }

        @Override
        public String apiId() {
            return apiId;
        }

        @Override
        public ReactorHandler reactor() {
            return reactor;
        }

        @Override
        public int compareTo(@Nonnull DummyAcceptor o) {
            return 0;
        }
    }

    private interface DummyAcceptor extends Acceptor<DummyAcceptor> {
        String apiId();
    }

    private static class DummyReactable implements Reactable {

        private final String id;

        public DummyReactable(String id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DummyReactable that = (DummyReactable) o;
            return id.equals(that.id);
        }

        @Override
        public boolean enabled() {
            return false;
        }

        @Override
        public <D> Set<D> dependencies(Class<D> type) {
            return null;
        }
    }
}
