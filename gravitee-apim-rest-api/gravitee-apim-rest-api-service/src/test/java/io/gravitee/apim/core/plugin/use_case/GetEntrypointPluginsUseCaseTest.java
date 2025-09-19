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

import inmemory.EntrypointPluginQueryServiceInMemory;
import io.gravitee.apim.core.plugin.domain_service.PluginFilterByLicenseDomainService;
import io.gravitee.apim.core.plugin.model.PlatformPlugin;
import io.gravitee.node.api.license.LicenseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetEntrypointPluginsUseCaseTest {

    private final EntrypointPluginQueryServiceInMemory entrypointPluginQueryServiceInMemory = new EntrypointPluginQueryServiceInMemory();
    private LicenseManager licenseManager;
    private GetEntrypointPluginsUseCase getEntrypointPluginsUseCase;

    @BeforeEach
    void setup() {
        licenseManager = mock(LicenseManager.class);
        getEntrypointPluginsUseCase = new GetEntrypointPluginsUseCase(
            entrypointPluginQueryServiceInMemory,
            new PluginFilterByLicenseDomainService(licenseManager)
        );

        entrypointPluginQueryServiceInMemory.reset();
    }

    @Test
    void should_set_deployed_status_depending_on_license() {
        var license = mock(io.gravitee.node.api.license.License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("org-id")).thenReturn(license);

        when(license.isFeatureEnabled("apim-proxy")).thenReturn(true);
        when(license.isFeatureEnabled("apim-en-connector-mock")).thenReturn(true);
        when(license.isFeatureEnabled("apim-en-connector-sse")).thenReturn(false);

        var res = getEntrypointPluginsUseCase.getEntrypointPluginsByOrganization(new GetEntrypointPluginsUseCase.Input("org-id"));
        assertThat(res.plugins()).hasSize(3);
        assertThat(
            res
                .plugins()
                .stream()
                .filter(p -> p.getId().equals("sse"))
                .findFirst()
                .map(PlatformPlugin::isDeployed)
        ).contains(false);
        assertThat(
            res
                .plugins()
                .stream()
                .filter(p -> p.getId().equals("mock"))
                .findFirst()
                .map(PlatformPlugin::isDeployed)
        ).contains(false);
        assertThat(
            res
                .plugins()
                .stream()
                .filter(p -> p.getId().equals("http-proxy"))
                .findFirst()
                .map(PlatformPlugin::isDeployed)
        ).contains(true);
    }
}
