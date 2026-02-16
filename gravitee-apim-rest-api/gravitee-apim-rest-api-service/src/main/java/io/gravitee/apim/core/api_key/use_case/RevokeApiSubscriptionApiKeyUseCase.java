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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.api_key.model.ApiKeyEntity;
import io.gravitee.apim.core.api_key.query_service.ApiKeyQueryService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.rest.api.service.exceptions.InvalidApplicationApiKeyModeException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;

/**
 * Revoke an API Key of an API's subscription.
 */
@UseCase
public class RevokeApiSubscriptionApiKeyUseCase {

    private final SubscriptionCrudService subscriptionCrudService;
    private final ApplicationCrudService applicationCrudService;
    private final ApiKeyQueryService apiKeyQueryService;
    private final RevokeApiKeyDomainService revokeApiKeyDomainService;

    public RevokeApiSubscriptionApiKeyUseCase(
        SubscriptionCrudService subscriptionCrudService,
        ApplicationCrudService applicationCrudService,
        ApiKeyQueryService apiKeyQueryService,
        RevokeApiKeyDomainService revokeApiKeyDomainService
    ) {
        this.subscriptionCrudService = subscriptionCrudService;
        this.applicationCrudService = applicationCrudService;
        this.apiKeyQueryService = apiKeyQueryService;
        this.revokeApiKeyDomainService = revokeApiKeyDomainService;
    }

    public Output execute(Input input) {
        var apiKey = apiKeyQueryService.findById(input.apiKeyId());
        if (apiKey.isEmpty()) {
            throw new ApiKeyNotFoundException();
        }
        if (!apiKey.get().hasSubscription(input.subscriptionId)) {
            throw new ApiKeyNotFoundException();
        }

        var subscription = subscriptionCrudService.get(input.subscriptionId);
        if (
            !subscription.getReferenceId().equals(input.referenceId) || !subscription.getReferenceType().name().equals(input.referenceType)
        ) {
            throw new SubscriptionNotFoundException(input.subscriptionId);
        }

        var application = applicationCrudService.findById(
            new ExecutionContext(input.auditInfo().organizationId(), input.auditInfo().environmentId()),
            apiKey.get().getApplicationId()
        );
        if (application.hasApiKeySharedMode()) {
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

    public record Input(String apiKeyId, String referenceId, String referenceType, String subscriptionId, AuditInfo auditInfo) {}

    public record Output(ApiKeyEntity apiKey) {}
}
