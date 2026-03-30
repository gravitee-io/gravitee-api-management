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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;

/**
 * Shared checks for per-API V4 analytics use cases (definition version, tenancy, TCP, analytics toggle).
 */
final class ApiAnalyticsV4ApiValidation {

    private ApiAnalyticsV4ApiValidation() {}

    static Api validateAndGetApi(ApiCrudService apiCrudService, String apiId, String environmentId) {
        final Api api = apiCrudService.get(apiId);
        validateApiDefinitionVersion(api.getDefinitionVersion(), apiId);
        validateApiIsNotTcp(api.getApiDefinitionHttpV4());
        validateApiMultiTenancyAccess(api, environmentId);
        return api;
    }

    /**
     * When analytics is disabled in the API definition, unified analytics use cases return empty data without querying the repository.
     * Missing {@code analytics} block is treated as enabled (same default as the definition model).
     */
    static boolean isAnalyticsEnabled(Api api) {
        var def = api.getApiDefinitionHttpV4();
        if (def == null || def.getAnalytics() == null) {
            return true;
        }
        return def.getAnalytics().isEnabled();
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
        if (apiDefinitionV4.isTcpProxy()) {
            throw new IllegalArgumentException("Analytics are not supported for TCP Proxy APIs");
        }
    }
}
