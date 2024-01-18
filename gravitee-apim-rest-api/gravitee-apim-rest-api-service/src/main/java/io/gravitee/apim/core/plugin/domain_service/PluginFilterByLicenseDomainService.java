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

import io.gravitee.apim.core.plugin.model.PlatformPlugin;
import io.gravitee.node.api.license.LicenseManager;
import java.util.Set;
import java.util.stream.Collectors;

public class PluginFilterByLicenseDomainService {

    private final LicenseManager licenseManager;

    public PluginFilterByLicenseDomainService(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    public <T extends PlatformPlugin> Set<T> setPluginDeployedStatusDependingOnLicense(Set<T> pluginSet, String organizationId) {
        var license = licenseManager.getOrganizationLicenseOrPlatform(organizationId);
        return pluginSet
            .stream()
            .peek(plugin -> plugin.setDeployed(plugin.isDeployed() && license.isFeatureEnabled(plugin.getFeature())))
            .collect(Collectors.toSet());
    }
}
