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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiExposedEntrypointDomainService;
import io.gravitee.apim.core.api.model.ExposedEntrypoint;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@UseCase
public class GetExposedEntrypointsUseCase {

    private final ApiCrudService apiCrudService;
    private final ApiExposedEntrypointDomainService apiExposedEntrypointDomainService;

    public Output execute(Input input) {
        var api = apiCrudService.get(input.apiId());

        return new Output(apiExposedEntrypointDomainService.get(input.organizationId, input.environmentId, api));
    }

    public record Input(String organizationId, String environmentId, String apiId) {}

    public record Output(List<ExposedEntrypoint> exposedEntrypoints) {}
}
