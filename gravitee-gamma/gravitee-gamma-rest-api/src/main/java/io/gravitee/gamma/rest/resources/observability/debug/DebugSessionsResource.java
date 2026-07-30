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
package io.gravitee.gamma.rest.resources.observability.debug;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.observability.debug.use_case.ManageDebugSessionUseCase;
import io.gravitee.gamma.rest.resources.observability.debug.dto.DebugSessionDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Debug sessions, mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/debug-sessions}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code POST /{apiId}?ttlSeconds=&sampling=} — raises the analytics detail of a deployed API
 *       (verbose tracing and payload capture) for a bounded window. Returns the granted window,
 *       which may be shorter than the one asked for.</li>
 *   <li>{@code DELETE /{apiId}} — ends the session early. Sessions expire on their own, so this
 *       only spares the remaining window.</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * {@code API_DEFINITION[UPDATE]} on the API. A session changes how a deployed API behaves, so it
 * asks for the same permission as the redeployment it replaces — switching mechanism must not lower
 * the bar.
 *
 * @author GraviteeSource Team
 */
public class DebugSessionsResource {

    @Inject
    private ManageDebugSessionUseCase manageDebugSessionUseCase;

    @Inject
    private PermissionService permissionService;

    @Context
    private SecurityContext securityContext;

    @POST
    @Path("/{apiId}")
    @Produces(MediaType.APPLICATION_JSON)
    public DebugSessionDto openSession(
        @PathParam("apiId") String apiId,
        @QueryParam("ttlSeconds") Integer ttlSeconds,
        @QueryParam("sampling") Integer sampling
    ) {
        checkApiUpdatePermission(apiId);

        var output = manageDebugSessionUseCase.open(
            input(apiId),
            ttlSeconds != null ? ttlSeconds : ManageDebugSessionUseCase.DEFAULT_TTL_SECONDS,
            sampling != null ? sampling : ManageDebugSessionUseCase.DEFAULT_SAMPLING_PERCENT
        );

        return new DebugSessionDto(output.apiId(), output.expiresAt(), output.samplingPercent());
    }

    @DELETE
    @Path("/{apiId}")
    public Response closeSession(@PathParam("apiId") String apiId) {
        checkApiUpdatePermission(apiId);
        manageDebugSessionUseCase.close(input(apiId));
        return Response.noContent().build();
    }

    private ManageDebugSessionUseCase.Input input(final String apiId) {
        var ctx = GraviteeContext.getExecutionContext();
        var principal = securityContext.getUserPrincipal();
        return new ManageDebugSessionUseCase.Input(
            ctx.getOrganizationId(),
            ctx.getEnvironmentId(),
            apiId,
            principal != null ? principal.getName() : null
        );
    }

    private void checkApiUpdatePermission(final String apiId) {
        var ctx = GraviteeContext.getExecutionContext();
        if (!permissionService.hasPermission(ctx, RolePermission.API_DEFINITION, apiId, RolePermissionAction.UPDATE)) {
            throw new ForbiddenAccessException();
        }
    }
}
