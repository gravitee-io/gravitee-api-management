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
package io.gravitee.apim.core.analytics.use_case;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import lombok.RequiredArgsConstructor;

/**
 * Shared validation logic for unified analytics use cases.
 * Ensures the target API is V4, not TCP, and accessible within the environment.
 */
@RequiredArgsConstructor
@DomainService
public class AnalyticsApiValidator {

    private final ApiCrudService apiCrudService;

    /**
     * Validates that the API exists, is V4, is not a TCP proxy, and belongs to the given environment.
     *
     * @param apiId the API identifier
     * @param environmentId the environment identifier for multi-tenancy check
     * @throws ApiNotFoundException if the API does not exist or doesn't belong to the environment
     * @throws ApiInvalidDefinitionVersionException if the API is not V4
     * @throws IllegalArgumentException if the API is a TCP proxy
     */
    public void validate(String apiId, String environmentId) {
        final Api api = apiCrudService.get(apiId);
        validateApiDefinitionVersion(api.getDefinitionVersion(), apiId);
        validateApiIsNotTcp(api.getApiDefinitionHttpV4());
        validateApiMultiTenancyAccess(api, environmentId);
    }

    private static void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    private static void validateApiDefinitionVersion(DefinitionVersion definitionVersion, String apiId) {
        if (!DefinitionVersion.V4.equals(definitionVersion)) {
            throw new ApiInvalidDefinitionVersionException(apiId);
        }
    }

    private static void validateApiIsNotTcp(io.gravitee.definition.model.v4.Api apiDefinitionV4) {
        if (apiDefinitionV4 != null && apiDefinitionV4.isTcpProxy()) {
            throw new IllegalArgumentException("Analytics are not supported for TCP Proxy APIs");
        }
    }
}
