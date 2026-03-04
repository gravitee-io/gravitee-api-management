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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.exception.ApiInvalidDefinitionVersionException;
import io.gravitee.apim.core.api.exception.ApiInvalidTypeException;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.TcpProxyNotSupportedException;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class SearchApiAnalyticsUseCase {

    private final ApiCrudService apiCrudService;

    public Output execute(ExecutionContext executionContext, Input input) {
        validateApiRequirements(input.apiId(), input.environmentId());

        return switch (input.type()) {
            case COUNT -> new Output(Type.COUNT);
            case STATS -> new Output(Type.STATS);
            case GROUP_BY -> new Output(Type.GROUP_BY);
            case DATE_HISTO -> new Output(Type.DATE_HISTO);
        };
    }

    private void validateApiRequirements(String apiId, String environmentId) {
        final Api api = apiCrudService.get(apiId);
        validateApiDefinitionVersion(api.getDefinitionVersion(), apiId);
        validateApiType(api);
        validateApiMultiTenancyAccess(api, environmentId);
        validateApiIsNotTcp(api);
    }

    private static void validateApiDefinitionVersion(DefinitionVersion definitionVersion, String apiId) {
        if (!DefinitionVersion.V4.equals(definitionVersion)) {
            throw new ApiInvalidDefinitionVersionException(apiId);
        }
    }

    private static void validateApiType(Api api) {
        if (!ApiType.PROXY.equals(api.getType())) {
            throw new ApiInvalidTypeException(api.getId(), ApiType.PROXY);
        }
    }

    private static void validateApiMultiTenancyAccess(Api api, String environmentId) {
        if (!api.belongsToEnvironment(environmentId)) {
            throw new ApiNotFoundException(api.getId());
        }
    }

    private static void validateApiIsNotTcp(Api api) {
        if (api.getApiDefinitionHttpV4().isTcpProxy()) {
            throw new TcpProxyNotSupportedException(api.getId());
        }
    }

    public record Input(String apiId, String environmentId, Type type) {}

    public record Output(Type type) {}

    public enum Type {
        COUNT,
        STATS,
        GROUP_BY,
        DATE_HISTO,
    }
}
