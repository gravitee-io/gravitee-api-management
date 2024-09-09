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
import io.gravitee.apim.core.api.domain_service.ApiMetadataDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiMetadataDomainService;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.api.model.ApiMetadata;
import io.gravitee.apim.core.api.model.NewApiMetadata;
import io.gravitee.apim.core.audit.model.AuditInfo;

@UseCase
public class CreateApiMetadataUseCase {

    private final ValidateApiMetadataDomainService validateApiMetadataDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;
    private final ApiCrudService apiCrudService;

    public CreateApiMetadataUseCase(
        ValidateApiMetadataDomainService validateApiMetadataDomainService,
        ApiMetadataDomainService apiMetadataDomainService,
        ApiCrudService apiCrudService
    ) {
        this.validateApiMetadataDomainService = validateApiMetadataDomainService;
        this.apiMetadataDomainService = apiMetadataDomainService;
        this.apiCrudService = apiCrudService;
    }

    public Output execute(Input input) {
        var apiId = input.newApiMetadata.getApiId();

        // Check that api exists with env id
        var api = apiCrudService.get(apiId);

        if (!api.getEnvironmentId().equals(input.auditInfo().environmentId())) {
            throw new ApiNotFoundException(apiId);
        }

        this.validateApiMetadataDomainService.validateUniqueKey(apiId, input.newApiMetadata.getKey());
        this.validateApiMetadataDomainService.validateUniqueName(input.auditInfo.environmentId(), apiId, input.newApiMetadata.getName());
        this.validateApiMetadataDomainService.validateValueByFormat(
                api,
                input.auditInfo().organizationId(),
                input.newApiMetadata.getValue(),
                input.newApiMetadata.getFormat()
            );

        return new Output(this.apiMetadataDomainService.create(input.newApiMetadata(), input.auditInfo()));
    }

    public record Input(NewApiMetadata newApiMetadata, AuditInfo auditInfo) {}

    public record Output(ApiMetadata created) {}
}
