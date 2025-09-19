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
package io.gravitee.apim.core.plugin.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.plugin.model.ConnectorPlugin;
import io.gravitee.node.api.license.LicenseManager;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginFilterByLicenseDomainServiceTest {

    LicenseManager licenseManager;
    PluginFilterByLicenseDomainService service;

    @BeforeEach
    void setup() {
        licenseManager = mock(LicenseManager.class);
        service = new PluginFilterByLicenseDomainService(licenseManager);
    }

    @Test
    void setPluginDeployedStatusDependingOnLicense() {
        var input = Set.of(
            ConnectorPlugin.builder().id("plugin-1").feature("feature-1").deployed(true).build(),
            ConnectorPlugin.builder().id("plugin-1").feature("feature-2").deployed(true).build(),
            ConnectorPlugin.builder().id("plugin-1").feature("feature-3").deployed(false).build()
        );

        var license = mock(io.gravitee.node.api.license.License.class);
        when(licenseManager.getOrganizationLicenseOrPlatform("org-id")).thenReturn(license);
        when(license.isFeatureEnabled("feature-1")).thenReturn(false);
        when(license.isFeatureEnabled("feature-2")).thenReturn(true);
        when(license.isFeatureEnabled("feature-3")).thenReturn(true);

        var res = service.setPluginDeployedStatusDependingOnLicense(input, "org-id");

        assertThat(res).containsExactly(
            ConnectorPlugin.builder().id("plugin-1").feature("feature-1").deployed(false).build(),
            ConnectorPlugin.builder().id("plugin-1").feature("feature-2").deployed(true).build(),
            ConnectorPlugin.builder().id("plugin-1").feature("feature-3").deployed(false).build()
        );
    }
}
