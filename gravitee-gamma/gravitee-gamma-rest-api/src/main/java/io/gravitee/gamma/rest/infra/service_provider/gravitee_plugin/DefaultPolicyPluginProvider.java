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

import static java.util.stream.Collectors.toMap;

import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.GuardrailProtocol;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.gravitee_plugin.port.service_provider.PolicyPluginProvider;
import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailPolicy;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.policy.PolicyPlugin;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DefaultPolicyPluginProvider extends AbstractPluginProvider<PolicyPlugin<?>> implements PolicyPluginProvider {

    public DefaultPolicyPluginProvider(ConfigurablePluginManager<PolicyPlugin<?>> pluginManager, LicenseManager licenseManager) {
        super(pluginManager, licenseManager);
    }

    @Override
    public Map<String, PolicyCapabilities> findAllCapabilities() {
        return pluginManager
            .findAll(true)
            .stream()
            .collect(
                toMap(PolicyPlugin::id, plugin ->
                    new PolicyCapabilities(
                        GuardrailPolicy.class.isAssignableFrom(plugin.policy()),
                        supportedApiTypes(plugin.manifest().properties())
                    )
                )
            );
    }

    /** Reads the manifest's flow phases of each API type a guardrail can be scoped to ({@code llm_proxy=REQUEST,RESPONSE}). */
    private static Set<ApiType> supportedApiTypes(Map<String, String> properties) {
        if (properties == null) {
            return Set.of();
        }
        return Arrays.stream(GuardrailProtocol.values())
            .filter(protocol -> {
                var phases = properties.get(protocol.manifestKey());
                return phases != null && !phases.isBlank();
            })
            .map(GuardrailProtocol::apiType)
            .collect(Collectors.toSet());
    }
}
