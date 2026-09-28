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
package io.gravitee.gamma.infra.gravitee_plugin.service_provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.core.domain.gravitee_plugin.exception.PluginNotFoundException;
import io.gravitee.plugin.agentprovider.AgentProviderPlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultAgentProviderPluginProviderTest {

    @SuppressWarnings("unchecked")
    private final ConfigurablePluginManager<AgentProviderPlugin> pluginManager = mock(ConfigurablePluginManager.class);

    private final DefaultAgentProviderPluginProvider provider = new DefaultAgentProviderPluginProvider(pluginManager);

    @Test
    void should_throw_not_found_when_plugin_is_not_loaded() {
        when(pluginManager.get("unknown")).thenReturn(null);

        assertThatThrownBy(() -> provider.findById("unknown"))
            .isInstanceOf(PluginNotFoundException.class)
            .hasMessageContaining("unknown");
    }
}
