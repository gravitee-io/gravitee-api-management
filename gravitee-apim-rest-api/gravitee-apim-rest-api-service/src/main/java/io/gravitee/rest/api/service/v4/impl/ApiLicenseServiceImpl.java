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
package io.gravitee.rest.api.service.v4.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Plugin;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenFeatureException;
import io.gravitee.rest.api.service.exceptions.InvalidLicenseException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.v4.ApiLicenseService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiLicenseServiceImpl implements ApiLicenseService {

    private final LicenseManager licenseManager;
    private final ApiSearchService apiSearchService;
    private final ObjectMapper objectMapper;

    public ApiLicenseServiceImpl(LicenseManager licenseManager, ApiSearchService apiSearchService, ObjectMapper objectMapper) {
        this.licenseManager = licenseManager;
        this.apiSearchService = apiSearchService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void checkLicense(ExecutionContext executionContext, String apiId) {
        var repositoryApi = apiSearchService.findRepositoryApiById(executionContext, apiId);
        List<Plugin> plugins = getPlugins(repositoryApi);

        try {
            var licensePlugins = plugins
                .stream()
                .map(p -> new LicenseManager.Plugin(p.type(), p.id()))
                .toList();
            licenseManager.validatePluginFeatures(executionContext.getOrganizationId(), licensePlugins);
        } catch (io.gravitee.node.api.license.ForbiddenFeatureException ffe) {
            throw new ForbiddenFeatureException(ffe.getFeatures().stream().map(LicenseManager.ForbiddenFeature::plugin).toList());
        } catch (io.gravitee.node.api.license.InvalidLicenseException e) {
            throw new InvalidLicenseException(e.getMessage());
        }
    }

    private List<Plugin> getPlugins(Api repositoryApi) {
        try {
            return switch (repositoryApi.getDefinitionVersion()) {
                case V4 -> switch (repositoryApi.getType()) {
                    case NATIVE -> objectMapper
                        .readValue(repositoryApi.getDefinition(), io.gravitee.definition.model.v4.nativeapi.NativeApi.class)
                        .getPlugins();
                    case LLM_PROXY, PROXY, MESSAGE -> objectMapper
                        .readValue(repositoryApi.getDefinition(), io.gravitee.definition.model.v4.Api.class)
                        .getPlugins();
                };
                case V1, V2 -> objectMapper.readValue(repositoryApi.getDefinition(), io.gravitee.definition.model.Api.class).getPlugins();
                case FEDERATED, FEDERATED_AGENT -> List.of();
                case null -> objectMapper.readValue(repositoryApi.getDefinition(), io.gravitee.definition.model.Api.class).getPlugins();
            };
        } catch (Exception e) {
            throw new TechnicalManagementException(e.getMessage());
        }
    }
}
