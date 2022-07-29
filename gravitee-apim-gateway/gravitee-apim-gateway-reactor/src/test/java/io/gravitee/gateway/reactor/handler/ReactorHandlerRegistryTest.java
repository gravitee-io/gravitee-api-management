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
    private ReactorHandlerFactoryManager reactorHandlerFactoryManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHaveOneEntrypoint() {
        Reactable reactable = createReactable("reactable1", "/");

        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(1, entrypoints.size());
        assertEntryPoint(null, "/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveTwoEntrypoints_singleReactable() {
        Reactable reactable = createReactable("reactable1", new VirtualHost("/products/v1"), new VirtualHost("/products/v2"));

        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(2, entrypoints.size());
        assertEntryPoint(null, "/products/v1/", entrypointIterator.next());
        assertEntryPoint(null, "/products/v2/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveTwoEntrypoints_singleReactable_withVirtualHost() {
        Reactable reactable = createReactable(
            "reactable1",
            new VirtualHost("/products/v1"),
            new VirtualHost("api.gravitee.io", "/products/v2")
        );

        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);

        reactorHandlerRegistry.create(reactable);

        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(2, entrypoints.size());
        assertEntryPoint("api.gravitee.io", "/products/v2/", entrypointIterator.next());
        assertEntryPoint(null, "/products/v1/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveTwoEntrypoints_duplicateContextPath() {
        Reactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable2", "/");
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);

        reactorHandlerRegistry.create(reactable2);

        Assert.assertEquals(2, reactorHandlerRegistry.getEntrypoints().size());
        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(2, entrypoints.size());
        assertEntryPoint(null, "/", entrypointIterator.next());
        assertEntryPoint(null, "/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveTwoEntrypoints() {
        Reactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable2", "/products");
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);
        reactorHandlerRegistry.create(reactable2);

        Assert.assertEquals(2, reactorHandlerRegistry.getEntrypoints().size());

        // Paths are sorted in natural order so "/" takes priority over "/products".
        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(2, entrypoints.size());
        assertEntryPoint(null, "/", entrypointIterator.next());
        assertEntryPoint(null, "/products/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveTwoEntrypoints_duplicateContextPath_withVirtualHost() {
        Reactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable2", "api.gravitee.io", "/");
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);
        reactorHandlerRegistry.create(reactable2);

        // Paths "/" are equivalent but virtualhost takes priority over simple path.
        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(2, entrypoints.size());
        final HttpAcceptorHandler first = entrypointIterator.next();
        assertEntryPoint("api.gravitee.io", "/", first);
        Assert.assertEquals(1001, first.priority());
        assertEntryPoint(null, "/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveTwoEntrypoints_updateReactable() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable2", "api.gravitee.io", "/");
        ReactorHandler handler2 = createReactorHandler(updateReactable);
        when(reactorHandlerFactoryManager.create(updateReactable)).thenReturn(handler2);
        reactorHandlerRegistry.update(updateReactable);

        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(2, entrypoints.size());
        assertEntryPoint("api.gravitee.io", "/", entrypointIterator.next());
        assertEntryPoint(null, "/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveMultipleEntrypoints_multipleCreateReactable() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable(
            "reactable2",
            new VirtualHost("api.gravitee.io", "/a"),
            new VirtualHost("api1.gravitee.io", "/a"),
            new VirtualHost("api2.gravitee.io", "/a"),
            new VirtualHost("api3.gravitee.io", "/a"),
            new VirtualHost("api4.gravitee.io", "/a"),
            new VirtualHost("apiX.gravitee.io", "/a"),
            new VirtualHost("api10.gravitee.io", "/a"),
            new VirtualHost("api11.gravitee.io", "/a")
        );
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable(
            "reactable3",
            new VirtualHost("api.gravitee.io", "/a-v1"),
            new VirtualHost("api1.gravitee.io", "/a-v1"),
            new VirtualHost("api2.gravitee.io", "/a-v1"),
            new VirtualHost("api3.gravitee.io", "/a-v1"),
            new VirtualHost("api4.gravitee.io", "/a-v1"),
            new VirtualHost("apiX.gravitee.io", "/a-v1"),
            new VirtualHost("api10.gravitee.io", "/a-v1"),
            new VirtualHost("api11.gravitee.io", "/a-v1")
        );
        ReactorHandler handler3 = createReactorHandler(reactable3);
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(handler3);
        reactorHandlerRegistry.create(reactable3);

        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(17, entrypoints.size());
        assertEntryPoint("api.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint(null, "/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveMultipleEntrypoints_multipleVhostsWithSubPaths() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable(
            "reactable2",
            new VirtualHost("api.gravitee.io", "/a/b/c"),
            new VirtualHost("api1.gravitee.io", "/a/b/c"),
            new VirtualHost("api2.gravitee.io", "/a/b/c"),
            new VirtualHost("api3.gravitee.io", "/a/b/c"),
            new VirtualHost("api4.gravitee.io", "/a/b/c"),
            new VirtualHost("apiX.gravitee.io", "/a/b/c"),
            new VirtualHost("api10.gravitee.io", "/a/b/c"),
            new VirtualHost("api11.gravitee.io", "/a/b/c")
        );
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable(
            "reactable3",
            new VirtualHost("api.gravitee.io", "/a/b/a"),
            new VirtualHost("api1.gravitee.io", "/a/b/b"),
            new VirtualHost("api2.gravitee.io", "/a/b/d"),
            new VirtualHost("api3.gravitee.io", "/a/b/e"),
            new VirtualHost("api4.gravitee.io", "/a/b/f"),
            new VirtualHost("apiX.gravitee.io", "/a/b/c1/sub"),
            new VirtualHost("api10.gravitee.io", "/a/b/c1/sub"),
            new VirtualHost("api11.gravitee.io", "/a/b/c1/sub")
        );
        ReactorHandler handler3 = createReactorHandler(reactable3);
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(handler3);
        reactorHandlerRegistry.create(reactable3);

        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(17, entrypoints.size());
        assertEntryPoint("api.gravitee.io", "/a/b/a/", entrypointIterator.next());
        assertEntryPoint("api.gravitee.io", "/a/b/c/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/b/b/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/b/c/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/b/c/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/b/c1/sub/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/b/c/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/b/c1/sub/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/b/c/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/b/d/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/b/c/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/b/e/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/b/c/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/b/f/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/b/c/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/b/c1/sub/", entrypointIterator.next());
        assertEntryPoint(null, "/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveMultipleEntrypoints_multipleUpdateReactable() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable(
            "reactable2",
            new VirtualHost("api.gravitee.io", "/a"),
            new VirtualHost("api1.gravitee.io", "/a"),
            new VirtualHost("api2.gravitee.io", "/a"),
            new VirtualHost("api3.gravitee.io", "/a"),
            new VirtualHost("api4.gravitee.io", "/a"),
            new VirtualHost("apiX.gravitee.io", "/a"),
            new VirtualHost("api10.gravitee.io", "/a"),
            new VirtualHost("api11.gravitee.io", "/a")
        );
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable(
            "reactable3",
            new VirtualHost("api.gravitee.io", "/a-v1"),
            new VirtualHost("api1.gravitee.io", "/a-v1"),
            new VirtualHost("api2.gravitee.io", "/a-v1"),
            new VirtualHost("api3.gravitee.io", "/a-v1"),
            new VirtualHost("api4.gravitee.io", "/a-v1"),
            new VirtualHost("apiX.gravitee.io", "/a-v1"),
            new VirtualHost("api10.gravitee.io", "/a-v1"),
            new VirtualHost("api11.gravitee.io", "/a-v1")
        );
        ReactorHandler handler3 = createReactorHandler(reactable3);
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(handler3);
        reactorHandlerRegistry.create(reactable3);

        reactable2 =
            createReactable(
                "reactable2",
                new VirtualHost("api.gravitee.io", "/b"),
                new VirtualHost("api1.gravitee.io", "/b"),
                new VirtualHost("api2.gravitee.io", "/b"),
                new VirtualHost("api3.gravitee.io", "/b"),
                new VirtualHost("api4.gravitee.io", "/b"),
                new VirtualHost("apiX.gravitee.io", "/b"),
                new VirtualHost("api10.gravitee.io", "/b"),
                new VirtualHost("api11.gravitee.io", "/b")
            );
        handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);
        reactorHandlerRegistry.update(reactable2);

        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(17, entrypoints.size());
        assertEntryPoint("api.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a-v1/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint(null, "/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveOneEntrypoint_updateSameReactableWithVHost() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        Assert.assertEquals(1, reactorHandlerRegistry.getEntrypoints().size());

        for (int i = 0; i < 100; i++) {
            final DummyReactable toUpdate = createReactable("reactable1", "api.gravitee.io", "/new-path");
            final ReactorHandler handlerUpdate = createReactorHandler(toUpdate);
            when(reactorHandlerFactoryManager.create(toUpdate)).thenReturn(handlerUpdate);
            reactorHandlerRegistry.update(toUpdate);
            Assert.assertEquals("Size of entrypoints list should be 1 (i=" + i + ")", 1, reactorHandlerRegistry.getEntrypoints().size());
        }

        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(1, entrypoints.size());
        assertEntryPoint("api.gravitee.io", "/new-path/", entrypointIterator.next());
    }

    @Test
    public void shouldHaveOneEntrypoint_updateSameReactableWithContextPath() throws InterruptedException {
        DummyReactable reactable = createReactable("reactable", "/c");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        Assert.assertEquals(1, reactorHandlerRegistry.getEntrypoints().size());

        for (int i = 0; i < 100; i++) {
            final Reactable toUpdate = createReactable("reactable", "/c");
            final ReactorHandler handlerUpdate = createReactorHandler(toUpdate);
            when(reactorHandlerFactoryManager.create(toUpdate)).thenReturn(handlerUpdate);
            reactorHandlerRegistry.update(toUpdate);
            Assert.assertEquals("Size of entrypoints list should be 1 (i=" + i + ")", 1, reactorHandlerRegistry.getEntrypoints().size());
        }

        final Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        final Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(1, entrypoints.size());
        assertEntryPoint(null, "/c/", entrypointIterator.next());
    }

    @Test
    public void shouldHave100Entrypoints_createThenUpdateMultiThreads() throws InterruptedException {
        ExecutorService executorService = null;

        try {
            executorService = Executors.newFixedThreadPool(10);
            List<Runnable> runnables = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                final DummyReactable toCreate = createReactable("reactable" + i, "api.gravitee.io", "/new-path" + i);
                final ReactorHandler handler = createReactorHandler(toCreate);
                when(reactorHandlerFactoryManager.create(toCreate)).thenReturn(handler);
                runnables.add(() -> reactorHandlerRegistry.create(toCreate));
            }

            for (Runnable r : runnables) {
                executorService.submit(r);
            }

            // Wait for all creations before going further.
            executorService.shutdown();
            assertTrue(executorService.awaitTermination(10000, TimeUnit.MILLISECONDS));

            Assert.assertEquals(100, reactorHandlerRegistry.getEntrypoints().size());

            executorService = Executors.newFixedThreadPool(10);
            runnables = new ArrayList<>();

            for (int i = 0; i < 100; i++) {
                final DummyReactable toUpdate = createReactable("reactable" + i, "api.gravitee.io", "/new-path" + i);
                final ReactorHandler handler = createReactorHandler(toUpdate);
                when(reactorHandlerFactoryManager.create(toUpdate)).thenReturn(handler);
                runnables.add(() -> reactorHandlerRegistry.update(toUpdate));
            }

            for (Runnable r : runnables) {
                executorService.submit(r);
            }

            executorService.shutdown();
            assertTrue(executorService.awaitTermination(10000, TimeUnit.MILLISECONDS));

            Assert.assertEquals(100, reactorHandlerRegistry.getEntrypoints().size());
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
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable1", "/");
        reactorHandlerRegistry.remove(updateReactable);

        Assert.assertEquals(0, reactorHandlerRegistry.getEntrypoints().size());
    }

    @Test
    public void shouldHaveNoEntrypoint_removeUnknownEntrypoint() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.remove(reactable);

        Assert.assertEquals(0, reactorHandlerRegistry.getEntrypoints().size());
    }

    @Test
    public void shouldHaveMultipleEntrypoints_multipleRemoveReactable() {
        DummyReactable reactable = createReactable("reactable1", "/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        DummyReactable reactable2 = createReactable(
            "reactable2",
            new VirtualHost("api.gravitee.io", "/a"),
            new VirtualHost("api1.gravitee.io", "/a"),
            new VirtualHost("api2.gravitee.io", "/a"),
            new VirtualHost("api3.gravitee.io", "/a"),
            new VirtualHost("api4.gravitee.io", "/a"),
            new VirtualHost("apiX.gravitee.io", "/a"),
            new VirtualHost("api10.gravitee.io", "/a"),
            new VirtualHost("api11.gravitee.io", "/a")
        );
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);
        reactorHandlerRegistry.create(reactable2);

        DummyReactable reactable3 = createReactable(
            "reactable3",
            new VirtualHost("api.gravitee.io", "/b"),
            new VirtualHost("api1.gravitee.io", "/b"),
            new VirtualHost("api2.gravitee.io", "/b"),
            new VirtualHost("api3.gravitee.io", "/b"),
            new VirtualHost("api4.gravitee.io", "/b"),
            new VirtualHost("apiX.gravitee.io", "/b"),
            new VirtualHost("api10.gravitee.io", "/b"),
            new VirtualHost("api11.gravitee.io", "/b")
        );
        ReactorHandler handler3 = createReactorHandler(reactable3);
        when(reactorHandlerFactoryManager.create(reactable3)).thenReturn(handler3);
        reactorHandlerRegistry.create(reactable3);

        Collection<HttpAcceptorHandler> entrypoints = reactorHandlerRegistry.getEntrypoints();
        Iterator<HttpAcceptorHandler> entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(17, entrypoints.size());
        assertEntryPoint("api.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint(null, "/", entrypointIterator.next());

        reactorHandlerRegistry.remove(reactable);
        entrypoints = reactorHandlerRegistry.getEntrypoints();
        entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(16, entrypoints.size());
        assertEntryPoint("api.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/a/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/b/", entrypointIterator.next());

        reactorHandlerRegistry.remove(reactable2);
        entrypoints = reactorHandlerRegistry.getEntrypoints();
        entrypointIterator = entrypoints.iterator();
        Assert.assertEquals(8, entrypoints.size());
        assertEntryPoint("api.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api1.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api10.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api11.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api2.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api3.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("api4.gravitee.io", "/b/", entrypointIterator.next());
        assertEntryPoint("apiX.gravitee.io", "/b/", entrypointIterator.next());

        reactorHandlerRegistry.remove(reactable3);
        Assert.assertEquals(0, reactorHandlerRegistry.getEntrypoints().size());
    }

    private void assertEntryPoint(String host, String path, HttpAcceptor httpAcceptor) {
        Assert.assertEquals(host, httpAcceptor.host());
        Assert.assertEquals(path, httpAcceptor.path());
    }

    private DummyReactable createReactable(String id, VirtualHost... virtualHosts) {
        return new DummyReactable(id, Arrays.asList(virtualHosts));
    }

    private DummyReactable createReactable(String id, String contextPath) {
        return createReactable(id, null, contextPath);
    }

    private DummyReactable createReactable(String id, String virtualHost, String contextPath) {
        return createReactable(id, new VirtualHost(virtualHost, contextPath));
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
        public List<HttpAcceptor> entrypoints() {
            return httpAcceptors;
        }
    }
}
