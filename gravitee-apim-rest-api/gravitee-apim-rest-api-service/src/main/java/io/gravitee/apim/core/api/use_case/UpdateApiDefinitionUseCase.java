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
import io.gravitee.apim.core.api.domain_service.import_definition.ImportDefinitionUpdateDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.exception.InvalidApiDefinitionException;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import lombok.CustomLog;

@UseCase
@CustomLog
public class UpdateApiDefinitionUseCase {

    public record Input(String apiId, ImportDefinition importDefinition, AuditInfo auditInfo) {}

    public record Output(ApiWithFlows apiWithFlows) {}

    private final ApiCrudService apiCrudService;
    private final ImportDefinitionUpdateDomainService importDefinitionUpdateDomainService;
    private final FlowCrudService flowCrudService;

    public UpdateApiDefinitionUseCase(
        ApiCrudService apiCrudService,
        ImportDefinitionUpdateDomainService importDefinitionUpdateDomainService,
        FlowCrudService flowCrudService
    ) {
        this.apiCrudService = apiCrudService;
        this.importDefinitionUpdateDomainService = importDefinitionUpdateDomainService;
        this.flowCrudService = flowCrudService;
    }

    public Output execute(Input input) {
        ensureIsV4Api(input.importDefinition().getApiExport());

        var existingApi = apiCrudService.findById(input.apiId()).orElseThrow(() -> new ApiNotFoundException(input.apiId()));

        var updatedApi = importDefinitionUpdateDomainService.update(input.importDefinition(), existingApi, input.auditInfo());

        var apiId = updatedApi.getId();
        var flows = ApiType.NATIVE.equals(updatedApi.getType())
            ? flowCrudService.getNativeApiFlows(apiId)
            : flowCrudService.getApiV4Flows(apiId);

        return new Output(new ApiWithFlows(updatedApi, flows));
    }

    private void ensureIsV4Api(ApiExport api) {
        if (api == null) {
            throw new InvalidApiDefinitionException("The API definition is required");
        }
        if (api.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new ApiDefinitionVersionNotSupportedException(api.getDefinitionVersion().getLabel());
        }
    }
}
