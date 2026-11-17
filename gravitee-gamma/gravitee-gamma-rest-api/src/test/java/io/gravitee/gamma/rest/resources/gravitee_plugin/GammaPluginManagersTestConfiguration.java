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
package io.gravitee.gamma.rest.resources.gravitee_plugin;

import static org.mockito.Mockito.mock;

import io.gravitee.gamma.rest.infra.config.GammaPluginsConfiguration;
import io.gravitee.plugin.agentprovider.AgentProviderPlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.gravitee.plugin.resource.ResourcePlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The plugins configuration over mocked plugin managers, one per plugin kind it declares a provider for.
 *
 * <p>Shared by the tests of every plugin catalog: the configuration under test needs all the managers
 * whichever catalog a test is about, so a new plugin kind is added here once rather than to each of them.
 *
 * @author GraviteeSource Team
 */
@Configuration
@Import(GammaPluginsConfiguration.class)
class GammaPluginManagersTestConfiguration {

    @Bean
    ConfigurablePluginManager<PolicyPlugin<?>> policyPluginManager() {
        return mock(ConfigurablePluginManager.class);
    }

    @Bean
    ConfigurablePluginManager<ResourcePlugin<?>> resourcePluginManager() {
        return mock(ConfigurablePluginManager.class);
    }

    @Bean
    ConfigurablePluginManager<AgentProviderPlugin> agentProviderPluginManager() {
        return mock(ConfigurablePluginManager.class);
    }
}
