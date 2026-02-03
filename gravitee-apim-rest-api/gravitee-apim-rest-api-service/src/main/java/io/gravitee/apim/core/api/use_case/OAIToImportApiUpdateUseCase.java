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
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.domain_service.import_definition.ImportDefinitionUpdateDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import lombok.Builder;

@UseCase
public class OAIToImportApiUpdateUseCase {

    @Builder
    public record Input(
            String apiId,
            ImportSwaggerDescriptorEntity importSwaggerDescriptor,
            boolean withDocumentation,
            boolean withOASValidationPolicy,
            AuditInfo auditInfo) {
    }

    public record Output(Api updatedApi) {
    }

    private final ApiCrudService apiCrudService;
    private final OAIDomainService oaiDomainService;
    private final ImportDefinitionUpdateDomainService importDefinitionUpdateDomainService;

    public OAIToImportApiUpdateUseCase(
            ApiCrudService apiCrudService,
            OAIDomainService oaiDomainService,
            ImportDefinitionUpdateDomainService importDefinitionUpdateDomainService) {
        this.apiCrudService = apiCrudService;
        this.oaiDomainService = oaiDomainService;
        this.importDefinitionUpdateDomainService = importDefinitionUpdateDomainService;
    }

    public Output execute(Input input) {
        Api existingApi = apiCrudService.findById(input.apiId())
                .orElseThrow(() -> new ApiNotFoundException(input.apiId()));

        var organizationId = input.auditInfo.organizationId();
        var environmentId = input.auditInfo.environmentId();
        var importDefinition = oaiDomainService.convert(
                organizationId,
                environmentId,
                input.importSwaggerDescriptor,
                input.withDocumentation(),
                input.withOASValidationPolicy());

        if (importDefinition != null) {
            final Api updatedApi = importDefinitionUpdateDomainService.update(importDefinition, existingApi,
                    input.auditInfo);
            return new Output(updatedApi);
        }

        return null;
    }
}
