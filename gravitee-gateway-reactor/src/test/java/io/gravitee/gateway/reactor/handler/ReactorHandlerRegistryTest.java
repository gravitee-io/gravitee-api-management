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

import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.impl.DefaultReactorHandlerRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        Assert.assertEquals(1, reactorHandlerRegistry.getEntrypoints().size());
    }

    @Test
    public void shouldHaveTwoEntrypoints_singleReactable() {
        Reactable reactable = createReactable("reactable1",
                new VirtualHost("/products/v1"), new VirtualHost("/products/v2"));

        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);

        reactorHandlerRegistry.create(reactable);

        Assert.assertEquals(2, reactorHandlerRegistry.getEntrypoints().size());
    }

    @Test
    public void shouldHaveTwoEntrypoints_singleReactable_withVirtualHost() {
        Reactable reactable = createReactable("reactable1",
                new VirtualHost("/products/v1"), new VirtualHost("api.gravitee.io", "/products/v2"));

        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);

        reactorHandlerRegistry.create(reactable);

        Assert.assertEquals(2, reactorHandlerRegistry.getEntrypoints().size());
        Assert.assertEquals("/products/v2/", reactorHandlerRegistry.getEntrypoints().iterator().next().path());
    }

    @Test
    public void shouldHaveTwoEntrypoints_duplicateContextPath() {
        Reactable reactable = createReactable("reactable1","/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable1","/");
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);

        reactorHandlerRegistry.create(reactable2);

        Assert.assertEquals(2, reactorHandlerRegistry.getEntrypoints().size());
    }

    @Test
    public void shouldHaveTwoEntrypoints() {
        Reactable reactable = createReactable("reactable1","/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable1","/products");
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);
        reactorHandlerRegistry.create(reactable2);

        Assert.assertEquals(2, reactorHandlerRegistry.getEntrypoints().size());
        Assert.assertEquals("/products/", reactorHandlerRegistry.getEntrypoints().iterator().next().path());
    }

    @Test
    public void shouldHaveTwoEntrypoints_duplicateContextPath_withVirtualHost() {
        Reactable reactable = createReactable("reactable1","/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        Reactable reactable2 = createReactable("reactable1","api.gravitee.io", "/");
        ReactorHandler handler2 = createReactorHandler(reactable2);
        when(reactorHandlerFactoryManager.create(reactable2)).thenReturn(handler2);
        reactorHandlerRegistry.create(reactable2);

        Assert.assertEquals(2, reactorHandlerRegistry.getEntrypoints().size());
        Assert.assertEquals("/", reactorHandlerRegistry.getEntrypoints().iterator().next().path());
    }

    @Test
    public void shouldHaveTwoEntrypoints_updateReactable() {
        DummyReactable reactable = createReactable("reactable1","/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable2","api.gravitee.io", "/");
        ReactorHandler handler2 = createReactorHandler(updateReactable);
        when(reactorHandlerFactoryManager.create(updateReactable)).thenReturn(handler2);
        reactorHandlerRegistry.update(updateReactable);

        Assert.assertEquals(2, reactorHandlerRegistry.getEntrypoints().size());
    }

    @Test
    public void shouldHaveOneEntrypoint_updateSameReactable() {
        DummyReactable reactable = createReactable("reactable1","/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable1","api.gravitee.io", "/new-path");
        ReactorHandler handler2 = createReactorHandler(updateReactable);
        when(reactorHandlerFactoryManager.create(updateReactable)).thenReturn(handler2);
        reactorHandlerRegistry.update(updateReactable);

        Assert.assertEquals(1, reactorHandlerRegistry.getEntrypoints().size());
        Assert.assertEquals("/new-path/", reactorHandlerRegistry.getEntrypoints().get(0).path());
    }

    @Test
    public void shouldHaveOneEntrypoint_removeSameReactable() {
        DummyReactable reactable = createReactable("reactable1","/");
        ReactorHandler handler = createReactorHandler(reactable);
        when(reactorHandlerFactoryManager.create(reactable)).thenReturn(handler);
        reactorHandlerRegistry.create(reactable);

        DummyReactable updateReactable = createReactable("reactable1","/");
        reactorHandlerRegistry.remove(updateReactable);

        Assert.assertEquals(0, reactorHandlerRegistry.getEntrypoints().size());
    }

    private DummyReactable createReactable(String id, VirtualHost ... virtualHosts) {
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
        private final List<Entrypoint> entrypoints;

        public DummyReactable(String id, List<Entrypoint> entrypoints) {
            this.id = id;
            this.entrypoints = entrypoints;
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
        public List<Entrypoint> entrypoints() {
            return entrypoints;
        }
    }
}
