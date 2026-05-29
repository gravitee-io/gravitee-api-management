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
package io.gravitee.gamma.authorization.rest.resource;

import io.gravitee.apim.plugin.gamma.api.identity.AmConnection;
import io.gravitee.apim.plugin.gamma.api.identity.AmConnectionRepository;
import io.gravitee.apim.plugin.gamma.api.identity.AmNotConfiguredException;
import io.gravitee.gamma.authorization.am.AmSyncConflictException;
import io.gravitee.gamma.authorization.am.AmSyncJobManager;
import io.gravitee.gamma.authorization.am.AmSyncJobState;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.rest.dto.AmSyncManualRequest;
import io.gravitee.gamma.authorization.rest.dto.AmSyncStartResponse;
import io.gravitee.gamma.authorization.rest.dto.AmSyncStatusResponse;
import io.gravitee.gamma.authorization.rest.exception.ErrorBody;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Objects;

/**
 * Triggers and reports an asynchronous sync of AM users into FGA PRINCIPAL entities, scoped to the
 * organization's configured AM connection. POST starts a job (202) and GET returns its status.
 */
@Path("/users/sync")
@Produces(MediaType.APPLICATION_JSON)
public class AuthzAmUserSyncResource {

    private final AmConnectionRepository amConnectionRepository;
    private final AmSyncJobManager jobManager;

    @Context
    private SecurityContext securityContext;

    @Inject
    public AuthzAmUserSyncResource(AmConnectionRepository amConnectionRepository, AmSyncJobManager jobManager) {
        this.amConnectionRepository = Objects.requireNonNull(amConnectionRepository, "amConnectionRepository must not be null");
        this.jobManager = Objects.requireNonNull(jobManager, "jobManager must not be null");
    }

    @POST
    @Permissions(
        {
            @Permission(
                value = RolePermission.ENVIRONMENT_AUTHORIZATION,
                acls = { RolePermissionAction.CREATE, RolePermissionAction.UPDATE }
            ),
        }
    )
    public Response sync() {
        // Resolve the caller and connection on the request thread — the worker thread has no
        // GraviteeContext thread-locals.
        AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext);
        try {
            AmConnection connection = amConnectionRepository.requireByOrg(caller.organizationId());
            AmSyncJobState state = jobManager.start(caller, connection);
            return Response.status(Response.Status.ACCEPTED).entity(AmSyncStartResponse.from(state)).build();
        } catch (AmNotConfiguredException e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorBody(AmNotConfiguredException.CODE, e.getMessage()))
                .build();
        } catch (AmSyncConflictException e) {
            return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorBody("SyncAlreadyRunning", e.getMessage()))
                .build();
        }
    }

    // Dev/smoke-test trigger: starts an async sync against connection details supplied inline,
    // bypassing the stored AmConnection. Returns 202 with the job id; poll GET /users/sync (keyed
    // by the caller's organization) for progress.
    @POST
    @Path("/run")
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions(
        {
            @Permission(
                value = RolePermission.ENVIRONMENT_AUTHORIZATION,
                acls = { RolePermissionAction.CREATE, RolePermissionAction.UPDATE }
            ),
        }
    )
    public Response run(@Valid AmSyncManualRequest request) {
        AuthzCallerContext caller = AuthzCallerContext.ofUser(
            request.organizationId(),
            GraviteeContext.getCurrentEnvironment(),
            securityContext.getUserPrincipal().getName()
        );
        AmConnection connection = new AmConnection(request.amUrl(), request.serviceToken(), request.domainId(), null, null);
        try {
            AmSyncJobState state = jobManager.start(caller, connection);
            return Response.status(Response.Status.ACCEPTED).entity(AmSyncStartResponse.from(state)).build();
        } catch (AmSyncConflictException e) {
            return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorBody("SyncAlreadyRunning", e.getMessage()))
                .build();
        }
    }

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public Response status() {
        return jobManager
            .getStatus(GraviteeContext.getCurrentOrganization())
            .map(state -> Response.ok(AmSyncStatusResponse.from(state)).build())
            .orElseGet(() ->
                Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(new ErrorBody("SyncNotFound", "No user sync has run for this organization"))
                    .build()
            );
    }
}
