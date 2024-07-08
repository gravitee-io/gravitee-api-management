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
package io.gravitee.apim.core.plugin.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import inmemory.ResourcePluginQueryServiceInMemory;
import io.gravitee.apim.core.plugin.domain_service.PluginFilterByLicenseDomainService;
import io.gravitee.apim.core.plugin.model.ResourcePlugin;
import io.gravitee.node.api.license.LicenseManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetResourcePluginsUseCaseTest {

    private final ResourcePluginQueryServiceInMemory resourcePluginQueryServiceInMemory = new ResourcePluginQueryServiceInMemory();
    private LicenseManager licenseManager;

    private GetResourcePluginsUseCase getResourcePluginsUseCase;

    @BeforeEach
    void setup() {
        licenseManager = mock(LicenseManager.class);
        getResourcePluginsUseCase =
            new GetResourcePluginsUseCase(resourcePluginQueryServiceInMemory, new PluginFilterByLicenseDomainService(licenseManager));
    }

    @Test
    void getResourcesByOrganization() {
        resourcePluginQueryServiceInMemory.initWith(
            List.of(
                ResourcePlugin.builder().id("resource-1").name("Resource 1").feature("feature-resource-1").deployed(true).build(),
                ResourcePlugin.builder().id("resource-2").name("Resource 2").feature("feature-resource-2").deployed(true).build(),
                ResourcePlugin.builder().id("resource-3").name("Resource 3").feature("feature-resource-3").deployed(false).build()
            )
        );
        var license = mock(io.gravitee.node.api.license.License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("org-id")).thenReturn(license);

        when(license.isFeatureEnabled("feature-resource-1")).thenReturn(false);
        when(license.isFeatureEnabled("feature-resource-2")).thenReturn(true);
        when(license.isFeatureEnabled("feature-resource-3")).thenReturn(true);

        assertThat(getResourcePluginsUseCase.getResourcesByOrganization(new GetResourcePluginsUseCase.Input("org-id")).plugins())
            .containsExactly(
                ResourcePlugin.builder().id("resource-1").name("Resource 1").feature("feature-resource-1").deployed(false).build(),
                ResourcePlugin.builder().id("resource-2").name("Resource 2").feature("feature-resource-2").deployed(true).build(),
                ResourcePlugin.builder().id("resource-3").name("Resource 3").feature("feature-resource-3").deployed(false).build()
            );
    }
}
