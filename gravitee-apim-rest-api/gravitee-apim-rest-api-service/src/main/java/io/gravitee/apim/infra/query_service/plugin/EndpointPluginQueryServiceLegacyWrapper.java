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
package io.gravitee.apim.infra.query_service.plugin;

import io.gravitee.apim.core.plugin.model.ConnectorPlugin;
import io.gravitee.apim.core.plugin.query_service.EndpointPluginQueryService;
import io.gravitee.apim.infra.adapter.ConnectorPluginAdapter;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.service.v4.EndpointConnectorPluginService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class EndpointPluginQueryServiceLegacyWrapper implements EndpointPluginQueryService {

    private final EndpointConnectorPluginService endpointPluginQueryService;

    private final LicenseManager licenseManager;

    public EndpointPluginQueryServiceLegacyWrapper(
        EndpointConnectorPluginService endpointPluginQueryService,
        LicenseManager licenseManager
    ) {
        this.endpointPluginQueryService = endpointPluginQueryService;
        this.licenseManager = licenseManager;
    }

    @Override
    public Set<ConnectorPlugin> findByOrganization(String organizationId) {
        var license = licenseManager.getOrganizationLicense(organizationId);
        return endpointPluginQueryService
            .findAll()
            .stream()
            .map(plugin -> {
                var resultPlugin = ConnectorPluginAdapter.INSTANCE.map(plugin);
                if (license != null) {
                    resultPlugin.setDeployed(plugin.isDeployed() && license.isFeatureEnabled(plugin.getFeature()));
                }
                return resultPlugin;
            })
            .collect(Collectors.toSet());
    }
}
