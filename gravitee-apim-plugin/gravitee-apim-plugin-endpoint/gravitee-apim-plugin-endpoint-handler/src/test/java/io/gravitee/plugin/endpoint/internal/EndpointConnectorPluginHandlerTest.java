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
package io.gravitee.plugin.endpoint.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPlugin;
import io.gravitee.plugin.endpoint.EndpointConnectorPluginManager;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnectorFactory;
import io.gravitee.plugin.endpoint.internal.fake.FakeEndpointConnectorPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class EndpointConnectorPluginHandlerTest {

    @Mock
    private EndpointConnectorPluginManager mockEndpointConnectorPluginManager;

    @InjectMocks
    private EndpointConnectorPluginHandler endpointPluginHandler = new EndpointConnectorPluginHandler();

    @Test
    public void shouldBeEndpointType() {
        assertThat(endpointPluginHandler.type()).isEqualTo(EndpointConnectorPlugin.PLUGIN_TYPE);
    }

    @Test
    public void shouldHandleEndpoint() {
        boolean canHandle = endpointPluginHandler.canHandle(new FakeEndpointConnectorPlugin());
        assertThat(canHandle).isTrue();
    }

    @Test
    public void shouldNotHandleEndpoint() {
        Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.type()).thenReturn("bad");
        boolean canHandle = endpointPluginHandler.canHandle(mockPlugin);
        assertThat(canHandle).isFalse();
    }

    @Test
    public void shouldHandleNewPlugin() {
        FakeEndpointConnectorPlugin plugin = new FakeEndpointConnectorPlugin();
        endpointPluginHandler.handle(plugin, FakeEndpointConnectorFactory.class);
        verify(mockEndpointConnectorPluginManager).register(any(EndpointConnectorPlugin.class));
    }
}
