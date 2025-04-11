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
package io.gravitee.apim.infra.domain_service.api;

import io.gravitee.apim.core.api.domain_service.ApiExposedEntrypointDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ExposedEntrypoint;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ApiExposedEntrypointDomainServiceLegacyWrapper implements ApiExposedEntrypointDomainService {

    private final ApiEntrypointService apiEntrypointService;

    @Override
    public List<ExposedEntrypoint> get(String organizationId, String environmentId, Api api) {
        GenericApiEntity genericApiEntity = api.getType() == ApiType.NATIVE
            ? ApiAdapter.INSTANCE.toNativeApiEntity(api)
            : ApiAdapter.INSTANCE.toApiEntity(api);

        return apiEntrypointService
            .getApiEntrypoints(new ExecutionContext(organizationId, environmentId), genericApiEntity)
            .stream()
            .map(entrypoint -> new ExposedEntrypoint(entrypoint.getTarget()))
            .toList();
    }
}
