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
package io.gravitee.apim.core.analytics.utils;

import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AnalyticsUtils {

    public static void validateHttpV4Api(Api api, String environmentId) {
        validateApiDefinitionVersion(api);
        validateApiMultiTenancyAccess(api, environmentId);
        validateApiIsNotTcp(api);
    }

    private void validateApiIsNotTcp(Api api) {
        if (api.getApiDefinitionV4().isTcpProxy()) {
            throw new TcpProxyNotSupportedException(api.getId());
        }
    }

    private static void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    private static void validateApiDefinitionVersion(Api api) {
        if (!DefinitionVersion.V4.equals(api.getDefinitionVersion())) {
            throw new ApiInvalidDefinitionVersionException(api.getId());
        }
    }
}
