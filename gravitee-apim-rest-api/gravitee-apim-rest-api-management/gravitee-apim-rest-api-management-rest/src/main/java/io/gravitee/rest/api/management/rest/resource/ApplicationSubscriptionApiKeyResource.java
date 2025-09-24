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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.apim.core.api_key.use_case.RevokeApplicationSubscriptionApiKeyUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author GraviteeSource Team
 */
@Tag(name = "API Keys")
public class ApplicationSubscriptionApiKeyResource extends AbstractApiKeyResource {

    @Inject
    private RevokeApplicationSubscriptionApiKeyUseCase revokeApplicationSubscriptionApiKeyUsecase;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @Parameter(name = "application", hidden = true)
    private String application;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("subscription")
    @Parameter(name = "subscription", hidden = true)
    private String subscription;

    @PathParam("apikey")
    @Parameter(name = "apikey")
    private String apikey;

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke an API Key", description = "User must have the MANAGE_API_KEYS permission to use this service")
    @ApiResponse(responseCode = "204", description = "API Key successfully revoked")
    @ApiResponse(responseCode = "400", description = "API Key does not correspond to the subscription")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response revokeApiKeyForApplicationSubscription() {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        final var user = getAuthenticatedUserDetails();

        revokeApplicationSubscriptionApiKeyUsecase.execute(
            new RevokeApplicationSubscriptionApiKeyUseCase.Input(
                apikey,
                application,
                subscription,
                AuditInfo.builder()
                    .organizationId(executionContext.getOrganizationId())
                    .environmentId(executionContext.getEnvironmentId())
                    .actor(
                        AuditActor.builder()
                            .userId(user.getUsername())
                            .userSource(user.getSource())
                            .userSourceId(user.getSourceId())
                            .build()
                    )
                    .build()
            )
        );
        return Response.status(Response.Status.NO_CONTENT).build();
    }
}
