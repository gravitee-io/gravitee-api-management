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
package io.gravitee.apim.core.api_key.use_case;

import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;

/**
 * Revoke an Application's API Key.
 */
public class RevokeApplicationApiKeyUseCase {

    private final ApplicationCrudService applicationCrudService;
    private final ApiKeyQueryService apiKeyQueryService;
    private final RevokeApiKeyDomainService revokeApiKeyDomainService;

    public RevokeApplicationApiKeyUseCase(
        ApplicationCrudService applicationCrudService,
        ApiKeyQueryService apiKeyQueryService,
        RevokeApiKeyDomainService revokeApiKeyDomainService
    ) {
        this.applicationCrudService = applicationCrudService;
        this.apiKeyQueryService = apiKeyQueryService;
        this.revokeApiKeyDomainService = revokeApiKeyDomainService;
    }

    public Output execute(Input input) {
        var application = applicationCrudService.findById(
            new ExecutionContext(input.auditInfo().organizationId(), input.auditInfo().environmentId()),
            input.applicationId()
        );

        var apiKey = apiKeyQueryService.findById(input.apiKeyId());
        if (apiKey.isEmpty()) {
            throw new ApiKeyNotFoundException();
        }

        if (!apiKey.get().getApplicationId().equals(application.getId())) {
            throw new ApiKeyNotFoundException();
        }

        if (!application.hasApiKeySharedMode()) {
            throw new InvalidApplicationApiKeyModeException(
                String.format(
                    "Invalid operation for API Key mode [%s] of application [%s]",
                    application.getApiKeyMode(),
                    application.getId()
                )
            );
        }

        return new Output(revokeApiKeyDomainService.revoke(apiKey.get(), input.auditInfo()));
    }

    public record Input(String apiKeyId, String applicationId, AuditInfo auditInfo) {}

    public record Output(ApiKeyEntity apiKey) {}
}
