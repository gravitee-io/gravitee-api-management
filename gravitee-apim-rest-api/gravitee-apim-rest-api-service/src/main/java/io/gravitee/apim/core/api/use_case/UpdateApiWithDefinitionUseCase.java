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
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.import_definition.ApiExport;
import io.gravitee.apim.core.api.model.import_definition.ImportDefinition;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import lombok.extern.slf4j.Slf4j;

@UseCase
@Slf4j
public class UpdateApiWithDefinitionUseCase {

    public record Input(String apiId, ImportDefinition importDefinition, AuditInfo auditInfo) {}

    public record Output(Api updatedApi) {}

    private final ApiCrudService apiCrudService;
    private final ImportDefinitionUpdateDomainService importDefinitionUpdateDomainService;

    public UpdateApiWithDefinitionUseCase(
        ApiCrudService apiCrudService,
        ImportDefinitionUpdateDomainService importDefinitionUpdateDomainService
    ) {
        this.apiCrudService = apiCrudService;
        this.importDefinitionUpdateDomainService = importDefinitionUpdateDomainService;
    }

    public Output execute(Input input) {
        var existingApi = apiCrudService
            .findById(input.apiId())
            .orElseThrow(() -> new ApiNotFoundException(input.apiId()));

        var apiExport = input.importDefinition().getApiExport();

        // Ensure the import is compatible with the existing API
        ensureIsV4ProxyApi(apiExport);
        ensureDefinitionVersionMatch(apiExport, existingApi);

        // Force the imported definition to use the existing API's ID
        apiExport.setId(existingApi.getId());

        var updatedApi = importDefinitionUpdateDomainService.update(input.importDefinition(), existingApi, input.auditInfo());
        return new Output(updatedApi);
    }

    private void ensureDefinitionVersionMatch(ApiExport apiExport, Api existingApi) {
        if (apiExport.getDefinitionVersion() != existingApi.getDefinitionVersion()) {
            throw new IllegalArgumentException(
                String.format(
                    "Cannot update API: definition versions don't match (imported: %s, existing: %s)",
                    apiExport.getDefinitionVersion(),
                    existingApi.getDefinitionVersion()
                )
            );
        }
    }

    private void ensureIsV4ProxyApi(ApiExport api) {
        if (api.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new ApiDefinitionVersionNotSupportedException(api.getDefinitionVersion().getLabel());
        }
        if (api.getType() != ApiType.PROXY) {
            throw new IllegalArgumentException("Only V4 PROXY APIs can be updated via definition import");
        }
    }
}

