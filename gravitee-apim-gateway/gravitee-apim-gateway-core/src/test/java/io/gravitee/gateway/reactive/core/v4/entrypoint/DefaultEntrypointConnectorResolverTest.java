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
package io.gravitee.gateway.reactive.core.v4.entrypoint;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_LISTENER_TYPE;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.connector.entrypoint.BaseEntrypointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.HttpEntrypointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.EntrypointAsyncConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.HttpEntrypointAsyncConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.HttpEntrypointAsyncConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.plugin.entrypoint.internal.DefaultEntrypointConnectorPluginManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockSettings;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class DefaultEntrypointConnectorResolverTest {

    protected static final String ENTRYPOINT_TYPE = "test";
    protected static final String ENTRYPOINT_CONFIG = "{ \"config\": \"something\"}";
    protected static final String MOCK_EXCEPTION = "Mock exception";

    @Mock
    private ExecutionContext ctx;

    @Mock
    private DefaultEntrypointConnectorPluginManager pluginManager;

    @Mock
    private DeploymentContext deploymentContext;

    @Mock
    private EntrypointConnectorFactory connectorFactory;

    @BeforeEach
    void init() {
        lenient().when(pluginManager.getFactoryById(ENTRYPOINT_TYPE)).thenReturn(connectorFactory);
    }

    @Test
    void shouldResolveEntrypointConnector() {
        final Api api = buildApi();
        final HttpEntrypointConnector entrypointConnector = mock(HttpEntrypointConnector.class);

        when(entrypointConnector.supportedListenerType()).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.HTTP);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE)).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.HTTP);
        when(connectorFactory.createConnector(deploymentContext, ENTRYPOINT_CONFIG)).thenReturn(entrypointConnector);
        when(entrypointConnector.matches(ctx)).thenReturn(true);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        final HttpEntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertSame(entrypointConnector, resolvedEntrypointConnector);
    }

    @Test
    void shouldResolveAsyncEntrypointConnectorWithQos() {
        final Api api = buildApi();
        final HttpEntrypointAsyncConnector entrypointAsyncConnector = mock(HttpEntrypointAsyncConnector.class);
        final HttpEntrypointAsyncConnectorFactory<HttpEntrypointAsyncConnector> asyncConnectorFactory = mock(
            HttpEntrypointAsyncConnectorFactory.class
        );
        when(asyncConnectorFactory.supportedApi()).thenReturn(ApiType.MESSAGE);
        when(pluginManager.getFactoryById(ENTRYPOINT_TYPE)).thenAnswer(invocation -> asyncConnectorFactory);

        when(entrypointAsyncConnector.supportedListenerType()).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.HTTP);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE)).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.HTTP);
        when(asyncConnectorFactory.createConnector(deploymentContext, Qos.AUTO, ENTRYPOINT_CONFIG)).thenReturn(entrypointAsyncConnector);
        when(entrypointAsyncConnector.matches((HttpExecutionContext) ctx)).thenReturn(true);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        final BaseEntrypointConnector<ExecutionContext> resolvedEntrypointConnector = cut.resolve(ctx);

        assertSame(entrypointAsyncConnector, resolvedEntrypointConnector);
    }

    @Test
    void shouldNotResolveEntrypointConnectorWhenEntryPointFactoryNotFound() {
        when(pluginManager.getFactoryById(ENTRYPOINT_TYPE)).thenReturn(null);
        final Api api = buildApi();
        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        final EntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertNull(resolvedEntrypointConnector);
    }

    @Test
    void shouldResolveFirstEntrypointConnectorWhenMultipleEntrypoints() {
        final Api api = buildApi();
        final Entrypoint entrypoint2 = buildEntrypoint();
        final Entrypoint entrypoint3 = buildEntrypoint();

        api.getListeners().get(0).getEntrypoints().add(entrypoint2);
        api.getListeners().get(0).getEntrypoints().add(entrypoint3);

        final HttpEntrypointConnector entrypointConnector = mock(HttpEntrypointConnector.class);

        when(entrypointConnector.supportedListenerType()).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.HTTP);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE)).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.HTTP);
        when(connectorFactory.createConnector(deploymentContext, ENTRYPOINT_CONFIG))
            .thenReturn(entrypointConnector)
            .thenReturn(mock(EntrypointConnector.class))
            .thenReturn(mock(EntrypointConnector.class));

        when(entrypointConnector.matches(ctx)).thenReturn(true);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        final HttpEntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertSame(entrypointConnector, resolvedEntrypointConnector);

        // 3 entrypoints defined on HTTP listener + 1 SUBSCRIPTION listener -> 4 connectors instantiated.
        verify(connectorFactory, times(4)).createConnector(deploymentContext, ENTRYPOINT_CONFIG);
    }

    @Test
    void shouldNotResolveWhenNotHttpListener() {
        final Api api = buildApi();
        api.getListeners().get(0).setType(ListenerType.TCP);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        final EntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertNull(resolvedEntrypointConnector);
    }

    @Test
    void shouldNotResolveWhenNotSameListenerType() {
        final Api api = buildApi();
        final EntrypointConnector entrypointConnector = mock(EntrypointConnector.class);

        when(entrypointConnector.supportedListenerType()).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.SUBSCRIPTION);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE)).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.HTTP);
        when(connectorFactory.createConnector(deploymentContext, ENTRYPOINT_CONFIG)).thenReturn(entrypointConnector);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        final EntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertNull(resolvedEntrypointConnector);
    }

    @Test
    void shouldNotResolveWhenNotMatching() {
        final Api api = buildApi();
        final HttpEntrypointConnector entrypointConnector = mock(HttpEntrypointConnector.class);

        when(entrypointConnector.supportedListenerType()).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.HTTP);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE)).thenReturn(io.gravitee.gateway.reactive.api.ListenerType.HTTP);
        when(connectorFactory.createConnector(deploymentContext, ENTRYPOINT_CONFIG)).thenReturn(entrypointConnector);
        when(entrypointConnector.matches(ctx)).thenReturn(false);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        final HttpEntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertNull(resolvedEntrypointConnector);
    }

    @Test
    void shouldPreStopEntrypointConnectors() throws Exception {
        final Api api = buildApi();
        api.getListeners().get(0).getEntrypoints().add(buildEntrypoint());

        final EntrypointConnector entrypointConnector1 = mock(EntrypointConnector.class);
        final EntrypointConnector entrypointConnector2 = mock(EntrypointConnector.class);
        final EntrypointConnector entrypointConnector3 = mock(EntrypointConnector.class);

        when(connectorFactory.createConnector(deploymentContext, ENTRYPOINT_CONFIG))
            .thenReturn(entrypointConnector1)
            .thenReturn(entrypointConnector2)
            .thenReturn(entrypointConnector3);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        cut.preStop();

        verify(entrypointConnector1).preStop();
        verify(entrypointConnector2).preStop();
        verify(entrypointConnector3).preStop();
    }

    @Test
    void shouldIgnoreErrorWhenPreStopEntrypointConnectors() throws Exception {
        final Api api = buildApi();
        api.getListeners().get(0).getEntrypoints().add(buildEntrypoint());

        final EntrypointConnector entrypointConnector1 = mock(EntrypointConnector.class);
        final EntrypointConnector entrypointConnector2 = mock(EntrypointConnector.class);
        final EntrypointConnector entrypointConnector3 = mock(EntrypointConnector.class);

        when(entrypointConnector2.preStop()).thenThrow(new Exception(MOCK_EXCEPTION));

        when(connectorFactory.createConnector(deploymentContext, ENTRYPOINT_CONFIG))
            .thenReturn(entrypointConnector1)
            .thenReturn(entrypointConnector2)
            .thenReturn(entrypointConnector3);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        cut.preStop();

        verify(entrypointConnector1).preStop();
        verify(entrypointConnector2).preStop();
        verify(entrypointConnector3).preStop();
    }

    @Test
    void shouldStopEntrypointConnectors() throws Exception {
        final Api api = buildApi();
        api.getListeners().get(0).getEntrypoints().add(buildEntrypoint());

        final EntrypointConnector entrypointConnector1 = mock(EntrypointConnector.class);
        final EntrypointConnector entrypointConnector2 = mock(EntrypointConnector.class);
        final EntrypointConnector entrypointConnector3 = mock(EntrypointConnector.class);

        when(connectorFactory.createConnector(deploymentContext, ENTRYPOINT_CONFIG))
            .thenReturn(entrypointConnector1)
            .thenReturn(entrypointConnector2)
            .thenReturn(entrypointConnector3);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        cut.stop();

        verify(entrypointConnector1).stop();
        verify(entrypointConnector2).stop();
        verify(entrypointConnector3).stop();
    }

    @Test
    void shouldIgnoreErrorWhenStopEntrypointConnectors() throws Exception {
        final Api api = buildApi();
        api.getListeners().get(0).getEntrypoints().add(buildEntrypoint());

        final EntrypointConnector entrypointConnector1 = mock(EntrypointConnector.class);
        final EntrypointConnector entrypointConnector2 = mock(EntrypointConnector.class);
        final EntrypointConnector entrypointConnector3 = mock(EntrypointConnector.class);

        when(entrypointConnector2.stop()).thenThrow(new Exception(MOCK_EXCEPTION));

        when(connectorFactory.createConnector(deploymentContext, ENTRYPOINT_CONFIG))
            .thenReturn(entrypointConnector1)
            .thenReturn(entrypointConnector2)
            .thenReturn(entrypointConnector3);

        final DefaultEntrypointConnectorResolver cut = new DefaultEntrypointConnectorResolver(api, deploymentContext, pluginManager);
        cut.stop();

        verify(entrypointConnector1).stop();
        verify(entrypointConnector2).stop();
        verify(entrypointConnector3).stop();
    }

    private Api buildApi() {
        final Api api = new Api();
        final List<Listener> listeners = new ArrayList<>();
        listeners.add(buildListener(ListenerType.HTTP));
        listeners.add(buildListener(ListenerType.SUBSCRIPTION));
        api.setListeners(listeners);
        return api;
    }

    private Listener buildListener(ListenerType listenerType) {
        Listener listener;
        switch (listenerType) {
            case HTTP:
                listener = new HttpListener();
                break;
            case SUBSCRIPTION:
                listener = new SubscriptionListener();
                break;
            default:
            case TCP:
                throw new UnsupportedOperationException(String.format("Listener type '%s' not yet supported", listenerType));
        }
        final List<Entrypoint> entrypoints = new ArrayList<>();
        listener.setEntrypoints(entrypoints);
        entrypoints.add(buildEntrypoint());
        return listener;
    }

    private Entrypoint buildEntrypoint() {
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType(ENTRYPOINT_TYPE);
        entrypoint.setConfiguration(ENTRYPOINT_CONFIG);
        return entrypoint;
    }
}
