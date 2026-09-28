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
package io.gravitee.gamma.rest.infra.service_provider.gravitee_plugin;

import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.AgentProviderPluginProvider;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.agentprovider.AgentProviderPlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;

public class DefaultAgentProviderPluginProvider extends AbstractPluginProvider<AgentProviderPlugin> implements AgentProviderPluginProvider {

    public DefaultAgentProviderPluginProvider(ConfigurablePluginManager<AgentProviderPlugin> pluginManager, LicenseManager licenseManager) {
        super(pluginManager, licenseManager);
    }
}
