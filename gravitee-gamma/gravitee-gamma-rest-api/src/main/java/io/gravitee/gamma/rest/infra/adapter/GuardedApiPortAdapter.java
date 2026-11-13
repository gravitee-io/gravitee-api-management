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
package io.gravitee.gamma.rest.infra.adapter;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.gamma.rest.core.guardian.model.GuardedApi;
import io.gravitee.gamma.rest.core.guardian.port.service_provider.GuardedApiPort;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * {@link GuardedApiPort} over the APIM API store.
 *
 * @author GraviteeSource Team
 */
@RequiredArgsConstructor
public class GuardedApiPortAdapter implements GuardedApiPort {

    private final ApiCrudService apiCrudService;

    @Override
    public Optional<GuardedApi> findById(String environmentId, String apiId) {
        return apiCrudService
            .findById(apiId)
            .filter(api -> Objects.equals(api.getEnvironmentId(), environmentId))
            .map(GuardedApiPortAdapter::toGuardedApi);
    }

    private static GuardedApi toGuardedApi(Api api) {
        var builder = GuardedApi.builder().id(api.getId()).environmentId(api.getEnvironmentId()).name(api.getName()).type(api.getType());
        if (api.getApiDefinitionValue() instanceof io.gravitee.definition.model.v4.Api definition) {
            builder.supportsGuardians(true).internalFlow(definition.getInternalFlow());
        }
        return builder.build();
    }
}
