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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.jupiter.reactor.v4.reactor.ReactorFactoryManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerRegistry;
import java.util.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
        Reactable reactable = createReactable("reactable1", "/");

        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(1, httpAcceptorHandlers.size());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_singleReactable() {
        Reactable reactable = createReactable(
            "reactable1",
            new DefaultHttpAcceptor("/products/v1"),
            new DefaultHttpAcceptor("/products/v2")
        );

        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint(null, "/products/v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/products/v2/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_singleReactable_withVirtualHost() {
        Reactable reactable = createReactable(
            "reactable1",
            new DefaultHttpAcceptor("/products/v1"),
            new DefaultHttpAcceptor("api.gravitee.io", "/products/v2")
        );

        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/products/v2/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/products/v1/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_duplicateContextPath() {
        Reactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable2", "/");
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));

        reactorHandlerRegistry.create(reactable2);

        Assert.assertEquals(2, reactorHandlerRegistry.getHttpAcceptorHandlers().size());
        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoEntrypoints() {
        Reactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable2", "/products");
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        Assert.assertEquals(2, reactorHandlerRegistry.getHttpAcceptorHandlers().size());

        // Paths are sorted in natural order so "/" takes priority over "/products".
        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/products/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_duplicateContextPath_withVirtualHost() {
        Reactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable2", "api.gravitee.io", "/");
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        // Paths "/" are equivalent but virtualhost takes priority over simple path.
        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        final HttpAcceptorHandler first = httpAcceptorHandlerIterator.next();
        assertEntryPoint("api.gravitee.io", "/", first);
        Assert.assertEquals(1001, first.priority());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveTwoHttpAcceptors_updateReactable() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable2", "api.gravitee.io", "/");
        ReactorHandler handler2 = createReactorHandler(updateReactable);
        when(reactorHandlerFactoryManager.create(updateReactable)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.update(updateReactable);

        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(2, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveMultipleHttpAcceptors_multipleCreateReactable() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable(
            "reactable2",
            new DefaultHttpAcceptor("api.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api1.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api2.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api3.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api4.gravitee.io", "/a"),
            new DefaultHttpAcceptor("apiX.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api10.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api11.gravitee.io", "/a")
        );
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable(
            "reactable3",
            new DefaultHttpAcceptor("api.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api1.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api2.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api3.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api4.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("apiX.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api10.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api11.gravitee.io", "/a-v1")
        );
        ReactorHandler handler3 = createReactorHandler(reactable3);
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(List.of(handler3));
        reactorHandlerRegistry.create(reactable3);

        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(17, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveMultipleHttpAcceptors_multipleVhostsWithSubPaths() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable(
            "reactable2",
            new DefaultHttpAcceptor("api.gravitee.io", "/a/b/c"),
            new DefaultHttpAcceptor("api1.gravitee.io", "/a/b/c"),
            new DefaultHttpAcceptor("api2.gravitee.io", "/a/b/c"),
            new DefaultHttpAcceptor("api3.gravitee.io", "/a/b/c"),
            new DefaultHttpAcceptor("api4.gravitee.io", "/a/b/c"),
            new DefaultHttpAcceptor("apiX.gravitee.io", "/a/b/c"),
            new DefaultHttpAcceptor("api10.gravitee.io", "/a/b/c"),
            new DefaultHttpAcceptor("api11.gravitee.io", "/a/b/c")
        );
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable(
            "reactable3",
            new DefaultHttpAcceptor("api.gravitee.io", "/a/b/a"),
            new DefaultHttpAcceptor("api1.gravitee.io", "/a/b/b"),
            new DefaultHttpAcceptor("api2.gravitee.io", "/a/b/d"),
            new DefaultHttpAcceptor("api3.gravitee.io", "/a/b/e"),
            new DefaultHttpAcceptor("api4.gravitee.io", "/a/b/f"),
            new DefaultHttpAcceptor("apiX.gravitee.io", "/a/b/c1/sub"),
            new DefaultHttpAcceptor("api10.gravitee.io", "/a/b/c1/sub"),
            new DefaultHttpAcceptor("api11.gravitee.io", "/a/b/c1/sub")
        );
        ReactorHandler handler3 = createReactorHandler(reactable3);
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(List.of(handler3));
        reactorHandlerRegistry.create(reactable3);

        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(17, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/a/b/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/b/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/b/c1/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/b/c1/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/b/d/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/b/e/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/b/f/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/b/c/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/b/c1/sub/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveMultipleHttpAcceptors_multipleUpdateReactable() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable(
            "reactable2",
            new DefaultHttpAcceptor("api.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api1.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api2.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api3.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api4.gravitee.io", "/a"),
            new DefaultHttpAcceptor("apiX.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api10.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api11.gravitee.io", "/a")
        );
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable(
            "reactable3",
            new DefaultHttpAcceptor("api.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api1.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api2.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api3.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api4.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("apiX.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api10.gravitee.io", "/a-v1"),
            new DefaultHttpAcceptor("api11.gravitee.io", "/a-v1")
        );
        ReactorHandler handler3 = createReactorHandler(reactable3);
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(List.of(handler3));
        reactorHandlerRegistry.create(reactable3);

        reactable2 =
            createReactable(
                "reactable2",
                new DefaultHttpAcceptor("api.gravitee.io", "/b"),
                new DefaultHttpAcceptor("api1.gravitee.io", "/b"),
                new DefaultHttpAcceptor("api2.gravitee.io", "/b"),
                new DefaultHttpAcceptor("api3.gravitee.io", "/b"),
                new DefaultHttpAcceptor("api4.gravitee.io", "/b"),
                new DefaultHttpAcceptor("apiX.gravitee.io", "/b"),
                new DefaultHttpAcceptor("api10.gravitee.io", "/b"),
                new DefaultHttpAcceptor("api11.gravitee.io", "/b")
            );
        handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.update(reactable2);

        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(17, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a-v1/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveOneEntrypoint_updateSameReactableWithVHost() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Assert.assertEquals(1, reactorHandlerRegistry.getHttpAcceptorHandlers().size());

        for (int i = 0; i < 100; i++) {
            final DummyReactable toUpdate = createReactable("reactable1", "api.gravitee.io", "/new-path");
            final ReactorHandler handlerUpdate = createReactorHandler(toUpdate);
            when(reactorHandlerFactoryManager.create(toUpdate)).thenReturn(List.of(handlerUpdate));
            reactorHandlerRegistry.update(toUpdate);
            Assert.assertEquals(
                "Size of acceptors list should be 1 (i=" + i + ")",
                1,
                reactorHandlerRegistry.getHttpAcceptorHandlers().size()
            );
        }

        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(1, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/new-path/", httpAcceptorHandlerIterator.next());
    }

    @Test
    public void shouldHaveOneEntrypoint_updateSameReactableWithContextPath() throws InterruptedException {
        DummyReactable reactable = createReactable("reactable", "/c");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        Assert.assertEquals(1, reactorHandlerRegistry.getHttpAcceptorHandlers().size());

        for (int i = 0; i < 100; i++) {
            final Reactable toUpdate = createReactable("reactable", "/c");
            final ReactorHandler handlerUpdate = createReactorHandler(toUpdate);
            when(reactorHandlerFactoryManager.create(toUpdate)).thenReturn(List.of(handlerUpdate));
            reactorHandlerRegistry.update(toUpdate);
            Assert.assertEquals(
                "Size of acceptors list should be 1 (i=" + i + ")",
                1,
                reactorHandlerRegistry.getHttpAcceptorHandlers().size()
            );
        }

        final Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        final Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
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
                final DummyReactable toCreate = createReactable("reactable" + i, "api.gravitee.io", "/new-path" + i);
                final ReactorHandler handler = createReactorHandler(toCreate);
                when(reactorHandlerFactoryManager.create(toCreate)).thenReturn(List.of(handler));
                runnables.add(() -> reactorHandlerRegistry.create(toCreate));
            }

            for (Runnable r : runnables) {
                executorService.submit(r);
            }

            // Wait for all creations before going further.
            executorService.shutdown();
            assertTrue(executorService.awaitTermination(10000, TimeUnit.MILLISECONDS));

            Assert.assertEquals(100, reactorHandlerRegistry.getHttpAcceptorHandlers().size());

            executorService = Executors.newFixedThreadPool(10);
            runnables = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                final DummyReactable toUpdate = createReactable("reactable" + i, "api.gravitee.io", "/new-path" + i);
                final ReactorHandler handler = createReactorHandler(toUpdate);
                when(reactorHandlerFactoryManager.create(toUpdate)).thenReturn(List.of(handler));
                runnables.add(() -> reactorHandlerRegistry.update(toUpdate));
            }

            for (Runnable r : runnables) {
                executorService.submit(r);
            }

            executorService.shutdown();
            assertTrue(executorService.awaitTermination(10000, TimeUnit.MILLISECONDS));

            Assert.assertEquals(100, reactorHandlerRegistry.getHttpAcceptorHandlers().size());
        } finally {
            if (executorService != null) {
                executorService.shutdownNow();
            }
        }
    }

    @Test
    public void shouldHaveNoEntrypoint_removeSameReactable() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable1", "/");
        reactorHandlerRegistry.remove(updateReactable);

        Assert.assertEquals(0, reactorHandlerRegistry.getHttpAcceptorHandlers().size());
    }

    @Test
    public void shouldHaveNoEntrypoint_removeUnknownEntrypoint() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.remove(reactable);

        Assert.assertEquals(0, reactorHandlerRegistry.getHttpAcceptorHandlers().size());
    }

    @Test
    public void shouldHaveMultipleHttpAcceptors_multipleRemoveReactable() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(List.of(handler));
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable(
            "reactable2",
            new DefaultHttpAcceptor("api.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api1.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api2.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api3.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api4.gravitee.io", "/a"),
            new DefaultHttpAcceptor("apiX.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api10.gravitee.io", "/a"),
            new DefaultHttpAcceptor("api11.gravitee.io", "/a")
        );
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(List.of(handler2));
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable(
            "reactable3",
            new DefaultHttpAcceptor("api.gravitee.io", "/b"),
            new DefaultHttpAcceptor("api1.gravitee.io", "/b"),
            new DefaultHttpAcceptor("api2.gravitee.io", "/b"),
            new DefaultHttpAcceptor("api3.gravitee.io", "/b"),
            new DefaultHttpAcceptor("api4.gravitee.io", "/b"),
            new DefaultHttpAcceptor("apiX.gravitee.io", "/b"),
            new DefaultHttpAcceptor("api10.gravitee.io", "/b"),
            new DefaultHttpAcceptor("api11.gravitee.io", "/b")
        );
        ReactorHandler handler3 = createReactorHandler(reactable3);
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(List.of(handler3));
        reactorHandlerRegistry.create(reactable3);

        Collection<HttpAcceptorHandler> httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        Iterator<HttpAcceptorHandler> httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(17, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint(null, "/", httpAcceptorHandlerIterator.next());

        reactorHandlerRegistry.remove(reactable);
        httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(16, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());

        reactorHandlerRegistry.remove(reactable2);
        httpAcceptorHandlers = reactorHandlerRegistry.getHttpAcceptorHandlers();
        httpAcceptorHandlerIterator = httpAcceptorHandlers.iterator();
        Assert.assertEquals(8, httpAcceptorHandlers.size());
        assertEntryPoint("api.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/b/", httpAcceptorHandlerIterator.next());

        reactorHandlerRegistry.remove(reactable3);
        Assert.assertEquals(0, reactorHandlerRegistry.getHttpAcceptorHandlers().size());
    }

    private void assertEntryPoint(String host, String path, HttpAcceptor httpAcceptor) {
        Assert.assertEquals(host, httpAcceptor.host());
        Assert.assertEquals(path, httpAcceptor.path());
    }

    private DummyReactable createReactable(String id, DefaultHttpAcceptor... virtualHosts) {
        return new DummyReactable(id, Arrays.asList(virtualHosts));
    }

    private DummyReactable createReactable(String id, String contextPath) {
        return createReactable(id, null, contextPath);
    }

    private DummyReactable createReactable(String id, String virtualHost, String contextPath) {
        return createReactable(id, new DefaultHttpAcceptor(virtualHost, contextPath));
    }

    private ReactorHandler createReactorHandler(Reactable reactable) {
        ReactorHandler handler = mock(ReactorHandler.class);
        when(handler.reactable()).thenReturn(reactable);
        return handler;
    }

    private class DummyReactable implements Reactable {

        private final String id;
        private final List<HttpAcceptor> httpAcceptors;

        public DummyReactable(String id, List<HttpAcceptor> httpAcceptors) {
            this.id = id;
            this.httpAcceptors = httpAcceptors;
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

        @Override
        public List<HttpAcceptor> httpAcceptors() {
            return httpAcceptors;
        }
    }
}
