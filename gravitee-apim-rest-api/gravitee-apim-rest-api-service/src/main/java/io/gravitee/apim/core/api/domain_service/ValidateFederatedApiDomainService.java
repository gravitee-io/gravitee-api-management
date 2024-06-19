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
package io.gravitee.apim.core.api.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.definition.model.DefinitionVersion;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ValidateFederatedApiDomainService {

    private final GroupValidationService groupValidationService;

    public Api validateAndSanitizeForCreation(final Api api) {
        if (api.getDefinitionVersion() != DefinitionVersion.FEDERATED) {
            throw new ValidationDomainException("Definition version is unsupported, should be FEDERATED");
        }

        // Reset lifecycle state as Federated API are not deployed on Gateway
        api.setLifecycleState(null);

        return api;
    }

    public Api validateAndSanitizeForUpdate(final Api updateApi, final Api existingApi, PrimaryOwnerEntity primaryOwnerEntity) {
        var groupIds = groupValidationService.validateAndSanitize(
            updateApi.getGroups(),
            existingApi.getEnvironmentId(),
            primaryOwnerEntity
        );
        updateApi.setGroups(groupIds);

        var lifecycleState = ValidateApiLifecycleService.validateFederatedApiLifecycleState(
            existingApi.getApiLifecycleState(),
            updateApi.getApiLifecycleState()
        );
        updateApi.setApiLifecycleState(lifecycleState);

        return updateApi;
    }
}
