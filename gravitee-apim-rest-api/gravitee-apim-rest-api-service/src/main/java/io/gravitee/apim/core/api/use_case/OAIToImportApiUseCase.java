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
import io.gravitee.apim.core.api.domain_service.ImportDefinitionCreateDomainService;
import io.gravitee.apim.core.api.domain_service.OAIDomainService;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import lombok.Builder;

@UseCase
public class OAIToImportApiUseCase {

    protected static final String DEFAULT_IMPORT_PAGE_NAME = "Swagger";

    @Builder
    public record Input(
        ImportSwaggerDescriptorEntity importSwaggerDescriptor,
        boolean withDocumentation,
        boolean withOASValidationPolicy,
        AuditInfo auditInfo
    ) {
        Input(ImportSwaggerDescriptorEntity importSwaggerDescriptor, AuditInfo auditInfo) {
            this(importSwaggerDescriptor, false, false, auditInfo);
        }
    }

    public record Output(ApiWithFlows apiWithFlows) {}

    private final OAIDomainService oaiDomainService;
    private final ImportDefinitionCreateDomainService importDefinitionCreateDomainService;

    public OAIToImportApiUseCase(
        OAIDomainService oaiDomainService,
        ImportDefinitionCreateDomainService importDefinitionCreateDomainService
    ) {
        this.oaiDomainService = oaiDomainService;
        this.importDefinitionCreateDomainService = importDefinitionCreateDomainService;
    }

    public Output execute(Input input) {
        var organizationId = input.auditInfo.organizationId();
        var environmentId = input.auditInfo.environmentId();
        var importDefinition = oaiDomainService.convert(
            organizationId,
            environmentId,
            input.importSwaggerDescriptor,
            input.withDocumentation(),
            input.withOASValidationPolicy()
        );

        if (importDefinition != null) {
            final ApiWithFlows apiWithFlows = importDefinitionCreateDomainService.create(input.auditInfo, importDefinition);
            return new Output(apiWithFlows);
        }

        return null;
    }
}
