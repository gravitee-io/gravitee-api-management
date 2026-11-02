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
package io.gravitee.gamma.rest.resources.runtimeoverlay;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.runtime_overlay.model.RuntimeOverlay;
import io.gravitee.gamma.rest.core.runtime_overlay.use_case.ListRuntimeOverlaysUseCase;
import io.gravitee.gamma.rest.core.runtime_overlay.use_case.StartRuntimeOverlayUseCase;
import io.gravitee.gamma.rest.core.runtime_overlay.use_case.StopRuntimeOverlayUseCase;
import io.gravitee.gamma.rest.resources.runtimeoverlay.dto.RuntimeOverlayDto;
import io.gravitee.gamma.rest.resources.runtimeoverlay.dto.StartRuntimeOverlayRequest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;

/**
 * Runtime overlays, mounted under {@code /organizations/{orgId}/environments/{envId}/runtime-overlays}.
 * A runtime overlay changes how a deployed API behaves, so starting or stopping one requires the permission of the
 * redeploy it replaces.
 */
@CustomLog
public class RuntimeOverlaysResource {

    @Inject
    private StartRuntimeOverlayUseCase startRuntimeOverlayUseCase;

    @Inject
    private StopRuntimeOverlayUseCase stopRuntimeOverlayUseCase;

    @Inject
    private ListRuntimeOverlaysUseCase listRuntimeOverlaysUseCase;

    @Inject
    private PermissionService permissionService;

    @Context
    private SecurityContext securityContext;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<RuntimeOverlayDto> list() {
        final long now = System.currentTimeMillis();
        return listRuntimeOverlaysUseCase
            .execute(new ListRuntimeOverlaysUseCase.Input(GraviteeContext.getCurrentEnvironment(), now))
            .runtimeOverlays()
            .stream()
            .filter(overlay -> canUpdateApi(overlay.apiId()))
            .map(overlay -> toDto(overlay, now))
            .toList();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> start(final StartRuntimeOverlayRequest request) {
        if (request == null || request.apiId() == null || request.kind() == null) {
            throw new BadRequestException("apiId and kind are required");
        }
        checkApiUpdatePermission(request.apiId());
        final var ctx = GraviteeContext.getExecutionContext();
        final var output = startRuntimeOverlayUseCase.execute(
            new StartRuntimeOverlayUseCase.Input(
                ctx.getOrganizationId(),
                ctx.getEnvironmentId(),
                userId(),
                request.apiId(),
                request.kind(),
                request.durationSeconds(),
                request.payload(),
                System.currentTimeMillis()
            )
        );
        final Map<String, Object> runtimeOverlay = output.runtimeOverlay();
        log.info(
            "📣 GENERIC_EVENT [{}] created (kind runtime-overlay, action apply): overlay [{}] of kind [{}] on API [{}] for {} s (until {}), payload {}",
            output.eventId(),
            runtimeOverlay.get("id"),
            request.kind(),
            request.apiId(),
            output.durationSeconds(),
            Instant.ofEpochMilli((Long) runtimeOverlay.get("endAt")),
            runtimeOverlay.get("payload")
        );
        return runtimeOverlay;
    }

    @DELETE
    @Path("/{runtimeOverlayId}")
    public Response stop(@PathParam("runtimeOverlayId") final String runtimeOverlayId, @QueryParam("apiId") final String apiId) {
        if (apiId == null) {
            throw new BadRequestException("apiId is required");
        }
        checkApiUpdatePermission(apiId);
        final var ctx = GraviteeContext.getExecutionContext();
        final var output = stopRuntimeOverlayUseCase.execute(
            new StopRuntimeOverlayUseCase.Input(ctx.getOrganizationId(), ctx.getEnvironmentId(), userId(), apiId, runtimeOverlayId)
        );
        log.info(
            "🛑 GENERIC_EVENT [{}] created (kind runtime-overlay, action remove): overlay [{}] on API [{}]",
            output.eventId(),
            runtimeOverlayId,
            apiId
        );
        return Response.noContent().build();
    }

    private static RuntimeOverlayDto toDto(final RuntimeOverlay overlay, final long now) {
        return new RuntimeOverlayDto(
            overlay.id(),
            overlay.kind(),
            overlay.apiId(),
            overlay.startAt(),
            overlay.endAt(),
            overlay.statusAt(now).name(),
            overlay.payload()
        );
    }

    private String userId() {
        final var principal = securityContext.getUserPrincipal();
        return principal != null ? principal.getName() : null;
    }

    private boolean canUpdateApi(final String apiId) {
        return permissionService.hasPermission(
            GraviteeContext.getExecutionContext(),
            RolePermission.API_DEFINITION,
            apiId,
            RolePermissionAction.UPDATE
        );
    }

    private void checkApiUpdatePermission(final String apiId) {
        if (!canUpdateApi(apiId)) {
            throw new ForbiddenAccessException();
        }
    }
}
