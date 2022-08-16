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
package io.gravitee.plugin.entrypoint.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointConnectorFactory;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointConnectorPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class EntrypointConnectorPluginHandlerTest {

    @Mock
    private EntrypointConnectorPluginManager mockEntrypointConnectorPluginManager;

    @InjectMocks
    private EntrypointConnectorPluginHandler entrypointConnectorPluginHandler = new EntrypointConnectorPluginHandler();

    @Test
    public void shouldBeEntrypointType() {
        assertThat(entrypointConnectorPluginHandler.type()).isEqualTo(EntrypointConnectorPlugin.PLUGIN_TYPE);
    }

    @Test
    public void shouldHandleEntrypoint() {
        boolean canHandle = entrypointConnectorPluginHandler.canHandle(new FakeEntrypointConnectorPlugin());
        assertThat(canHandle).isTrue();
    }

    @Test
    public void shouldNotHandleEntrypoint() {
        Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.type()).thenReturn("bad");
        boolean canHandle = entrypointConnectorPluginHandler.canHandle(mockPlugin);
        assertThat(canHandle).isFalse();
    }

    @Test
    public void shouldHandleNewPlugin() {
        FakeEntrypointConnectorPlugin plugin = new FakeEntrypointConnectorPlugin();
        entrypointConnectorPluginHandler.handle(plugin, FakeEntrypointConnectorFactory.class);
        verify(mockEntrypointConnectorPluginManager).register(any(EntrypointConnectorPlugin.class));
    }
}
