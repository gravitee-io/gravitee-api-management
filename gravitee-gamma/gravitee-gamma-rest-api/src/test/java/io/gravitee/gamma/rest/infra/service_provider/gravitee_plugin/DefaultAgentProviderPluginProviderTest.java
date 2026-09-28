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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.gamma.rest.core.gravitee_plugin.exception.PluginNotFoundException;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.agentprovider.AgentProviderPlugin;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultAgentProviderPluginProviderTest {

    private static final String ORGANIZATION = "org-id";
    private static final String PLUGIN_ID = "azure-foundry";
    private static final String FEATURE = "apim-agent-provider";

    @SuppressWarnings("unchecked")
    private final ConfigurablePluginManager<AgentProviderPlugin> pluginManager = mock(ConfigurablePluginManager.class);

    private final LicenseManager licenseManager = mock(LicenseManager.class);
    private final License license = mock(License.class);

    private final DefaultAgentProviderPluginProvider provider = new DefaultAgentProviderPluginProvider(pluginManager, licenseManager);

    @BeforeEach
    void setUp() throws IOException {
        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(license);
        when(license.isFeatureEnabled(FEATURE)).thenReturn(true);
        when(pluginManager.getIcon(any(), anyBoolean())).thenReturn("icon");
    }

    @Nested
    class FindAll {

        @Test
        void should_list_deployed_and_undeployed_plugins() {
            var deployed = aPlugin(PLUGIN_ID, true);
            var undeployed = aPlugin("other", false);
            when(pluginManager.findAll(true)).thenReturn(List.of(deployed, undeployed));

            assertThat(provider.findAll(ORGANIZATION))
                .extracting(PlatformPlugin::id, PlatformPlugin::deployed)
                .containsExactlyInAnyOrder(tuple(PLUGIN_ID, true), tuple("other", false));
        }

        @Test
        void should_report_a_plugin_as_not_deployed_when_the_organization_license_does_not_enable_it() {
            when(license.isFeatureEnabled(FEATURE)).thenReturn(false);
            var plugin = aPlugin(PLUGIN_ID, true);
            when(pluginManager.findAll(true)).thenReturn(List.of(plugin));

            assertThat(provider.findAll(ORGANIZATION)).extracting(PlatformPlugin::deployed).containsExactly(false);
        }

        @Test
        void should_map_the_plugin_manifest() {
            var plugin = aPlugin(PLUGIN_ID, true);
            when(pluginManager.findAll(true)).thenReturn(List.of(plugin));

            assertThat(provider.findAll(ORGANIZATION)).containsExactly(
                new PlatformPlugin(PLUGIN_ID, "Azure Foundry", "Foundry agents", "agent-provider", "1.0.0", "icon", FEATURE, true)
            );
        }
    }

    @Nested
    class FindById {

        @Test
        void should_find_an_undeployed_plugin() {
            var plugin = aPlugin(PLUGIN_ID, false);
            when(pluginManager.get(PLUGIN_ID, true)).thenReturn(plugin);

            assertThat(provider.findById(ORGANIZATION, PLUGIN_ID))
                .extracting(PlatformPlugin::id, PlatformPlugin::deployed)
                .containsExactly(PLUGIN_ID, false);
        }

        @Test
        void should_throw_not_found_when_plugin_is_not_loaded() {
            assertThatThrownBy(() -> provider.findById(ORGANIZATION, "unknown"))
                .isInstanceOf(PluginNotFoundException.class)
                .hasMessageContaining("unknown");
        }
    }

    @Nested
    class GetSchema {

        @Test
        void should_return_the_schema() throws IOException {
            var plugin = aPlugin(PLUGIN_ID, true);
            when(pluginManager.get(PLUGIN_ID, true)).thenReturn(plugin);
            when(pluginManager.getSchema(PLUGIN_ID, true)).thenReturn("{}");

            assertThat(provider.getSchema(PLUGIN_ID)).isEqualTo("{}");
        }

        @Test
        void should_throw_not_found_when_plugin_is_not_loaded() {
            assertThatThrownBy(() -> provider.getSchema("unknown")).isInstanceOf(PluginNotFoundException.class);
        }

        @Test
        void should_throw_a_technical_error_when_the_schema_cannot_be_read() throws IOException {
            var plugin = aPlugin(PLUGIN_ID, true);
            when(pluginManager.get(PLUGIN_ID, true)).thenReturn(plugin);
            when(pluginManager.getSchema(PLUGIN_ID, true)).thenThrow(new IOException("boom"));

            assertThatThrownBy(() -> provider.getSchema(PLUGIN_ID))
                .isInstanceOf(TechnicalDomainException.class)
                .hasCauseInstanceOf(IOException.class);
        }
    }

    @Nested
    class GetMoreInformation {

        @Test
        void should_throw_not_found_when_plugin_is_not_loaded() {
            assertThatThrownBy(() -> provider.getMoreInformation("unknown")).isInstanceOf(PluginNotFoundException.class);
        }
    }

    private static AgentProviderPlugin aPlugin(String id, boolean deployed) {
        var plugin = mock(AgentProviderPlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.id()).thenReturn(id);
        when(plugin.deployed()).thenReturn(deployed);
        when(plugin.manifest().name()).thenReturn("Azure Foundry");
        when(plugin.manifest().description()).thenReturn("Foundry agents");
        when(plugin.manifest().category()).thenReturn("agent-provider");
        when(plugin.manifest().version()).thenReturn("1.0.0");
        when(plugin.manifest().feature()).thenReturn(FEATURE);
        return plugin;
    }
}
