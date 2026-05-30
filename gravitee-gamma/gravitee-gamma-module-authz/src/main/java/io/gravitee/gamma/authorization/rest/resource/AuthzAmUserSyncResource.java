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

import io.gravitee.apim.core.async_job.model.AsyncJob;
import io.gravitee.apim.plugin.gamma.api.identity.AmNotConfiguredException;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import io.gravitee.gamma.authorization.core.am.exception.AmSyncConflictException;
import io.gravitee.gamma.authorization.core.am.use_case.GetAmUserSyncStatusUseCase;
import io.gravitee.gamma.authorization.core.am.use_case.StartAmUserSyncUseCase;
import io.gravitee.gamma.authorization.rest.dto.AmSyncStartResponse;
import io.gravitee.gamma.authorization.rest.dto.AmSyncStatusResponse;
import io.gravitee.gamma.authorization.rest.exception.ErrorBody;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.Objects;
import lombok.CustomLog;

/**
 * Triggers and reports an asynchronous sync of AM users into FGA PRINCIPAL entities, scoped to the
 * organization's configured AM connection. POST starts a job (202) and GET returns its status.
 */
@Path("/users/sync")
@Produces(MediaType.APPLICATION_JSON)
@CustomLog
public class AuthzAmUserSyncResource {

    private final StartAmUserSyncUseCase startAmUserSyncUseCase;
    private final GetAmUserSyncStatusUseCase getAmUserSyncStatusUseCase;

    @Context
    private SecurityContext securityContext;

    @Inject
    public AuthzAmUserSyncResource(
        StartAmUserSyncUseCase startAmUserSyncUseCase,
        GetAmUserSyncStatusUseCase getAmUserSyncStatusUseCase
    ) {
        this.startAmUserSyncUseCase = Objects.requireNonNull(startAmUserSyncUseCase, "startAmUserSyncUseCase must not be null");
        this.getAmUserSyncStatusUseCase = Objects.requireNonNull(getAmUserSyncStatusUseCase, "getAmUserSyncStatusUseCase must not be null");
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
        try {
            // Resolve the caller on the request thread — the worker thread has no GraviteeContext
            // thread-locals. The use case resolves the AM connection (synchronously) from there.
            AuthzCallerContext caller = AuthzCallerResolver.resolve(securityContext);
            AsyncJob job = startAmUserSyncUseCase.execute(new StartAmUserSyncUseCase.Input(caller)).job();
            return Response.status(Response.Status.ACCEPTED).entity(AmSyncStartResponse.from(job)).build();
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
        } catch (RuntimeException e) {
            log.error("Unexpected error starting AM user sync", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorBody("SyncStartFailed", "Could not start the AM user sync"))
                .build();
        }
    }

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_AUTHORIZATION, acls = { RolePermissionAction.READ }) })
    public Response status() {
        try {
            var input = new GetAmUserSyncStatusUseCase.Input(
                GraviteeContext.getCurrentOrganization(),
                GraviteeContext.getCurrentEnvironment()
            );
            return getAmUserSyncStatusUseCase
                .execute(input)
                .job()
                .map(job -> Response.ok(AmSyncStatusResponse.from(job)).build())
                .orElseGet(() ->
                    Response.status(Response.Status.NOT_FOUND)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(new ErrorBody("SyncNotFound", "No user sync has run for this organization"))
                        .build()
                );
        } catch (RuntimeException e) {
            log.error("Unexpected error retrieving AM user sync status", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorBody("SyncStatusFailed", "Could not retrieve the AM user sync status"))
                .build();
        }
    }
}
