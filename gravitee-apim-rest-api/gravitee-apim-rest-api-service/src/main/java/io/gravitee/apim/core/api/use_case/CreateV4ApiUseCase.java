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

import static io.gravitee.apim.core.api.domain_service.ApiIndexerDomainService.oneShotIndexation;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.domain_service.CreateApiDomainService;
import io.gravitee.apim.core.api.domain_service.ValidateApiDomainService;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.NewApi;
import io.gravitee.apim.core.api.model.factory.ApiModelFactory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;

@UseCase
public class CreateV4ApiUseCase {

    private final ValidateApiDomainService validateApiDomainService;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final CreateApiDomainService createApiDomainService;

    public CreateV4ApiUseCase(
        ValidateApiDomainService validateApiDomainService,
        ApiPrimaryOwnerFactory apiPrimaryOwnerFactory,
        CreateApiDomainService createApiDomainService
    ) {
        this.validateApiDomainService = validateApiDomainService;
        this.apiPrimaryOwnerFactory = apiPrimaryOwnerFactory;
        this.createApiDomainService = createApiDomainService;
    }

    public record Input(NewApi newApi, AuditInfo auditInfo) {}

    public record Output(ApiWithFlows api) {}

    public Output execute(Input input) {
        var auditInfo = input.auditInfo;

        var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            auditInfo.actor().userId()
        );

        var created = createApiDomainService.create(
            ApiModelFactory.fromNewApi(input.newApi, auditInfo.environmentId()),
            primaryOwner,
            auditInfo,
            api ->
                validateApiDomainService.validateAndSanitizeForCreation(
                    api,
                    primaryOwner,
                    auditInfo.environmentId(),
                    auditInfo.organizationId()
                ),
            oneShotIndexation(auditInfo)
        );

        return new Output(created);
    }
}
