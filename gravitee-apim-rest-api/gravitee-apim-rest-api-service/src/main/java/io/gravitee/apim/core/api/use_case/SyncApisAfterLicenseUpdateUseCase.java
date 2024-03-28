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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.api.domain_service.StopApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiQueryService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Plugin;
import io.gravitee.node.api.license.ForbiddenFeatureException;
import io.gravitee.node.api.license.InvalidLicenseException;
import io.gravitee.node.api.license.LicenseManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SyncApisAfterLicenseUpdateUseCase {

    private final ApiQueryService apiQueryService;
    private final LicenseManager licenseManager;

    private final StopApiDomainService stopApiDomainService;

    public SyncApisAfterLicenseUpdateUseCase(
        ApiQueryService apiQueryService,
        LicenseManager licenseManager,
        StopApiDomainService stopApiDomainService
    ) {
        this.apiQueryService = apiQueryService;
        this.licenseManager = licenseManager;
        this.stopApiDomainService = stopApiDomainService;
    }

    public Output execute(Input input) {
        // get all deployed APIs
        var apisToCheck = apiQueryService.findAllStartedApisByOrganization(input.organizationId);

        var stoppedApis = new ArrayList<Api>();
        // check features
        apisToCheck.forEach(api -> {
            List<Plugin> plugins;
            if (api.getDefinitionVersion() == DefinitionVersion.V4) {
                plugins = api.getApiDefinitionV4().getPlugins();
            } else {
                plugins = api.getApiDefinition().getPlugins();
            }
            try {
                licenseManager.validatePluginFeatures(
                    input.organizationId,
                    plugins.stream().map(p -> new LicenseManager.Plugin(p.type(), p.id())).collect(Collectors.toSet())
                );
            } catch (InvalidLicenseException | ForbiddenFeatureException e) {
                // if not compatible or invalid license ==> stop API
                stopApiDomainService.stop(api, AuditInfo.builder().organizationId(input.organizationId).build()); // FIXME: audit info actor should be SYSTEM
                stoppedApis.add(api);
            }
        });
        return new Output(stoppedApis);
    }

    public record Input(String organizationId) {}

    public record Output(List<Api> stoppedApis) {}
}
