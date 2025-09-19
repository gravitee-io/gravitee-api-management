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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.apim.core.api_key.use_case.RevokeApplicationApiKeyUseCase;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.mapper.KeyMapper;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApiKeyService;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationKeysResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ApiKeyService apiKeyService;

    @Inject
    private RevokeApplicationApiKeyUseCase revokeApplicationApiKeyUsecase;

    @Inject
    private KeyMapper keyMapper;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("applicationId")
    private String applicationId;

    @POST
    @Path("/_renew")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response renewSharedKey() {
        ApplicationEntity applicationEntity = applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);
        final Key createdKey = keyMapper.convert(apiKeyService.renew(GraviteeContext.getExecutionContext(), applicationEntity));
        return Response.status(Response.Status.CREATED).entity(createdKey).build();
    }

    @POST
    @Path("/{apiKey}/_revoke")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.UPDATE) })
    public Response revokeKeySubscription(@PathParam("apiKey") String apiKey) {
        final var executionContext = GraviteeContext.getExecutionContext();
        final var user = getAuthenticatedUserDetails();

        revokeApplicationApiKeyUsecase.execute(
            new RevokeApplicationApiKeyUseCase.Input(
                apiKey,
                applicationId,
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

        return Response.noContent().build();
    }
}
