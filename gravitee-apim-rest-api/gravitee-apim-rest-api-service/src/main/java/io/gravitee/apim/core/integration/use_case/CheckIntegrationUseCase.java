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
package io.gravitee.apim.core.integration.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import lombok.RequiredArgsConstructor;

/**
 * Ensure that the integration exists.
 *
 * <p>
 *     This allows us to ensure that the auth token provided by the Agent is allowed to manage the integration.
 * </p>
 */
@RequiredArgsConstructor
@UseCase
public class CheckIntegrationUseCase {

    private final IntegrationCrudService integrationCrudService;
    private final EnvironmentCrudService environmentCrudService;
    private final PermissionDomainService permissionDomainService;

    public Output execute(Input input) {
        return integrationCrudService
            .findById(input.integrationId)
            .filter(integration -> {
                var environment = environmentCrudService.get(integration.getEnvironmentId());
                return environment.getOrganizationId().equals(input.organizationId);
            })
            .filter(integration -> {
                var hasEnvironmentIntegrationPermission = permissionDomainService.hasExactPermissions(
                    input.organizationId,
                    input.userId,
                    io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_INTEGRATION,
                    integration.getEnvironmentId(),
                    RolePermissionAction.CREATE,
                    RolePermissionAction.READ,
                    RolePermissionAction.UPDATE,
                    RolePermissionAction.DELETE
                );

                // Check same permissions used to preview & ingest APIs
                var hasIntegrationPermission = permissionDomainService.hasPermission(
                    input.organizationId,
                    input.userId,
                    RolePermission.INTEGRATION_DEFINITION,
                    integration.getId(),
                    RolePermissionAction.CREATE
                );
                return hasEnvironmentIntegrationPermission || hasIntegrationPermission;
            })
            .map(integration -> {
                if (!integration.getProvider().equals(input.provider)) {
                    return new Output(
                        false,
                        String.format(
                            "Integration [id=%s] does not match. Expected provider [provider=%s]",
                            integration.getId(),
                            integration.getProvider()
                        )
                    );
                }
                return new Output(true);
            })
            .orElse(new Output(false, String.format("Integration [id=%s] not found", input.integrationId)));
    }

    public record Input(String organizationId, String userId, String integrationId, String provider) {
        public Input(String integrationId) {
            this(null, null, integrationId, null);
        }
    }

    public record Output(boolean success, String message) {
        public Output(boolean success) {
            this(success, null);
        }
    }
}
