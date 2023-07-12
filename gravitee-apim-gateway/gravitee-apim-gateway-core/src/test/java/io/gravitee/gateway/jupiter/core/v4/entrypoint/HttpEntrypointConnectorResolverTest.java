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
package io.gravitee.gateway.jupiter.core.v4.entrypoint;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.AbstractConnectorFactory;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.GenericExecutionContext;
import io.gravitee.plugin.entrypoint.internal.DefaultEntrypointConnectorPluginManager;
import java.util.ArrayList;
import java.util.Set;
import javax.validation.constraints.NotNull;
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
class HttpEntrypointConnectorResolverTest {

    protected static final String ENTRYPOINT_TYPE = "test";
    protected static final String ENTRYPOINT_CONFIG = "{ \"config\": \"something\"}";
    protected static final Set<ConnectorMode> SUPPORTED_MODES = Set.of(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    protected static final ApiType SUPPORTED_API_TYPE = ApiType.ASYNC;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private DefaultEntrypointConnectorPluginManager pluginManager;

    @Mock
    private AbstractConnectorFactory<EntrypointConnector> connectorFactory;

    @BeforeEach
    void init() {
        lenient().when(pluginManager.getFactoryById(ENTRYPOINT_TYPE)).thenReturn(connectorFactory);
    }

    @Test
    void shouldResolveEntrypointConnector() {
        final Api api = buildApi();
        final EntrypointConnector entrypointConnector = mock(EntrypointConnector.class);

        when(connectorFactory.createConnector(ENTRYPOINT_CONFIG)).thenReturn(entrypointConnector);
        when(entrypointConnector.supportedListenerType()).thenReturn(io.gravitee.gateway.jupiter.api.ListenerType.HTTP);
        when(entrypointConnector.matches(ctx)).thenReturn(true);

        final HttpEntrypointConnectorResolver cut = new HttpEntrypointConnectorResolver(api, pluginManager);
        final EntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertSame(entrypointConnector, resolvedEntrypointConnector);
    }

    @Test
    void shouldResolveFirstEntrypointConnectorWhenMultipleEntrypoints() {
        final Api api = buildApi();
        final Entrypoint entrypoint2 = buildEntrypoint();
        final Entrypoint entrypoint3 = buildEntrypoint();

        ((HttpListener) api.getListeners().get(0)).getEntrypoints().add(entrypoint2);
        ((HttpListener) api.getListeners().get(0)).getEntrypoints().add(entrypoint3);

        final EntrypointConnector entrypointConnector = mock(EntrypointConnector.class);

        when(connectorFactory.createConnector(ENTRYPOINT_CONFIG))
            .thenReturn(entrypointConnector)
            .thenReturn(mock(EntrypointConnector.class))
            .thenReturn(mock(EntrypointConnector.class));

        when(entrypointConnector.matches(ctx)).thenReturn(true);
        when(entrypointConnector.supportedListenerType()).thenReturn(io.gravitee.gateway.jupiter.api.ListenerType.HTTP);

        final HttpEntrypointConnectorResolver cut = new HttpEntrypointConnectorResolver(api, pluginManager);
        final EntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertSame(entrypointConnector, resolvedEntrypointConnector);
        verify(connectorFactory, times(3)).createConnector(ENTRYPOINT_CONFIG);
    }

    @Test
    void shouldNotResolveWhenNotHttpListener() {
        final Api api = buildApi();
        api.getListeners().get(0).setType(ListenerType.TCP);

        final HttpEntrypointConnectorResolver cut = new HttpEntrypointConnectorResolver(api, pluginManager);
        final EntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertNull(resolvedEntrypointConnector);
    }

    @Test
    void shouldNotResolveWhenNotMatching() {
        final Api api = buildApi();
        final EntrypointConnector entrypointConnector = mock(EntrypointConnector.class);

        when(connectorFactory.createConnector(ENTRYPOINT_CONFIG)).thenReturn(entrypointConnector);
        when(entrypointConnector.supportedListenerType()).thenReturn(io.gravitee.gateway.jupiter.api.ListenerType.HTTP);
        when(entrypointConnector.matches(ctx)).thenReturn(false);

        final HttpEntrypointConnectorResolver cut = new HttpEntrypointConnectorResolver(api, pluginManager);
        final EntrypointConnector resolvedEntrypointConnector = cut.resolve(ctx);

        assertNull(resolvedEntrypointConnector);
    }

    private Api buildApi() {
        final Api api = new Api();
        final ArrayList<@NotNull Listener> listeners = new ArrayList<>();
        api.setListeners(listeners);

        listeners.add(buildListener());
        return api;
    }

    private Listener buildListener() {
        final HttpListener httpListener = new HttpListener();
        final ArrayList<Entrypoint> entrypoints = new ArrayList<>();

        httpListener.setEntrypoints(entrypoints);
        entrypoints.add(buildEntrypoint());
        return httpListener;
    }

    private Entrypoint buildEntrypoint() {
        final Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType(ENTRYPOINT_TYPE);
        entrypoint.setConfiguration(ENTRYPOINT_CONFIG);

        return entrypoint;
    }
}
