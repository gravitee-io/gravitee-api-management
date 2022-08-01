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
import io.gravitee.plugin.entrypoint.EntrypointPlugin;
import io.gravitee.plugin.entrypoint.EntrypointPluginManager;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointFactory;
import io.gravitee.plugin.entrypoint.internal.fake.FakeEntrypointPlugin;
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
class EntrypointPluginHandlerTest {

    @Mock
    private EntrypointPluginManager mockEntrypointPluginManager;

    @InjectMocks
    private EntrypointPluginHandler entrypointPluginHandler = new EntrypointPluginHandler();

    @Test
    public void shouldBeEntrypointType() {
        assertThat(entrypointPluginHandler.type()).isEqualTo(EntrypointPlugin.PLUGIN_TYPE);
    }

    @Test
    public void shouldHandleEntrypoint() {
        boolean canHandle = entrypointPluginHandler.canHandle(new FakeEntrypointPlugin());
        assertThat(canHandle).isTrue();
    }

    @Test
    public void shouldNotHandleEntrypoint() {
        Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.type()).thenReturn("bad");
        boolean canHandle = entrypointPluginHandler.canHandle(mockPlugin);
        assertThat(canHandle).isFalse();
    }

    @Test
    public void shouldHandleNewPlugin() {
        FakeEntrypointPlugin plugin = new FakeEntrypointPlugin();
        entrypointPluginHandler.handle(plugin, FakeEntrypointFactory.class);
        verify(mockEntrypointPluginManager).register(any(EntrypointPlugin.class));
    }
}
