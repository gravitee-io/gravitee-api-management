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
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.rest.core.gravitee_plugin.model.PlatformPlugin;
import io.gravitee.node.api.license.License;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.plugin.core.api.ConfigurablePluginManager;
import io.gravitee.plugin.resource.ResourcePlugin;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DefaultResourcePluginProviderTest {

    private static final String ORGANIZATION = "org-id";
    private static final String FEATURE = "apim-ai-resource";

    @SuppressWarnings("unchecked")
    private final ConfigurablePluginManager<ResourcePlugin<?>> pluginManager = mock(ConfigurablePluginManager.class);

    private final LicenseManager licenseManager = mock(LicenseManager.class);

    private final DefaultResourcePluginProvider provider = new DefaultResourcePluginProvider(pluginManager, licenseManager);

    @Nested
    class FindAll {

        /** What the license holds back is listed, and said to be so: the screen and the validation both read it here. */
        @Test
        void should_report_a_plugin_the_organization_license_does_not_enable_as_not_deployed() {
            var license = mock(License.class);
            when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(license);
            when(license.isFeatureEnabled(FEATURE)).thenReturn(false);
            givenInstalled(aPlugin("ai-model-token-classification", true));

            assertThat(provider.findAll(ORGANIZATION))
                .extracting(PlatformPlugin::id, PlatformPlugin::deployed)
                .containsExactly(tuple("ai-model-token-classification", false));
        }
    }

    private void givenInstalled(ResourcePlugin<?>... plugins) {
        when(pluginManager.findAll(true)).thenReturn(List.of(plugins));
    }

    private static ResourcePlugin<?> aPlugin(String id, boolean deployed) {
        ResourcePlugin<?> plugin = mock(ResourcePlugin.class, RETURNS_DEEP_STUBS);
        when(plugin.id()).thenReturn(id);
        when(plugin.deployed()).thenReturn(deployed);
        when(plugin.manifest().feature()).thenReturn(FEATURE);
        return plugin;
    }
}
