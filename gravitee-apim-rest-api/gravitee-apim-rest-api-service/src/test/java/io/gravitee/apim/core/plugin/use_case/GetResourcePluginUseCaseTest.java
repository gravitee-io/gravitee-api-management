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

import inmemory.ResourcePluginCrudServiceInMemory;
import inmemory.ResourcePluginQueryServiceInMemory;
import io.gravitee.apim.core.plugin.domain_service.PluginFilterByLicenseDomainService;
import io.gravitee.apim.core.plugin.model.ResourcePlugin;
import io.gravitee.node.api.license.LicenseManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetResourcePluginUseCaseTest {

    private final ResourcePluginQueryServiceInMemory resourcePluginQueryServiceInMemory = new ResourcePluginQueryServiceInMemory();
    private final ResourcePluginCrudServiceInMemory resourcePluginCrudServiceInMemory = new ResourcePluginCrudServiceInMemory();

    private LicenseManager licenseManager;

    private GetResourcePluginUseCase getResourcePluginUseCase;

    @BeforeEach
    void setup() {
        licenseManager = mock(LicenseManager.class);
        getResourcePluginUseCase =
            new GetResourcePluginUseCase(
                resourcePluginCrudServiceInMemory,
                resourcePluginQueryServiceInMemory,
                new PluginFilterByLicenseDomainService(licenseManager)
            );
    }

    @Test
    void should_return_plugin() {
        resourcePluginCrudServiceInMemory.initWith(
            List.of(ResourcePlugin.builder().id("resource-1").name("Resource 1").feature("feature-resource-1").deployed(true).build())
        );

        var license = mock(io.gravitee.node.api.license.License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("org-id")).thenReturn(license);
        when(license.isFeatureEnabled("feature-resource-1")).thenReturn(true);

        var output = getResourcePluginUseCase.execute(new GetResourcePluginUseCase.Input("org-id", "resource-1"));
        assertThat(output.plugin())
            .extracting(ResourcePlugin::getId, ResourcePlugin::getName, ResourcePlugin::getFeature, ResourcePlugin::isDeployed)
            .containsExactly("resource-1", "Resource 1", "feature-resource-1", true);
    }

    @Test
    void should_return_plugin_with_schema() {
        resourcePluginCrudServiceInMemory.initWith(
            List.of(ResourcePlugin.builder().id("resource-1").name("Resource 1").feature("feature-resource-1").deployed(true).build())
        );

        var license = mock(io.gravitee.node.api.license.License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("org-id")).thenReturn(license);
        when(license.isFeatureEnabled("feature-resource-1")).thenReturn(true);

        var output = getResourcePluginUseCase.execute(new GetResourcePluginUseCase.Input("org-id", "resource-1", true));
        assertThat(output.plugin())
            .extracting(ResourcePlugin::getId, ResourcePlugin::getName, ResourcePlugin::getFeature, ResourcePlugin::isDeployed)
            .containsExactly("resource-1", "Resource 1", "feature-resource-1", true);
        assertThat(output.schema()).isExactlyInstanceOf(String.class);
    }
}
