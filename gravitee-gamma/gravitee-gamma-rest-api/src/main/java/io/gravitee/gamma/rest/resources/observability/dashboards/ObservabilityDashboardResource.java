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
package io.gravitee.gamma.rest.resources.observability.dashboards;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.observability.dashboard.exception.InvalidDashboardException;
import io.gravitee.gamma.rest.core.observability.dashboard.model.VersionPrecondition;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.DeleteObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.GetObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.UpdateObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.DashboardDto;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.SaveDashboardRequestDto;
import io.gravitee.rest.api.management.rest.model.ErrorEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Single saved-dashboard endpoints (GET / PUT / DELETE), reached via
 * {@link ObservabilityDashboardsResource}'s {@code {dashboardId}} locator.
 *
 * <p>Environment isolation: a dashboard id from another environment resolves to 404, not 403, on
 * every verb (see {@code DashboardRepository#findByIdAndEnvironmentId}), so cross-environment
 * existence cannot be probed.
 *
 * <p>On PUT the id comes from the path — a different id in the request body is ignored, like every
 * other server-owned field (see {@link SaveDashboardRequestDto}).
 *
 * <h2>Concurrency (OBS-17)</h2>
 * The revision an edit is based on travels as an {@code ETag} / {@code If-Match} pair rather than a
 * body field: it is the mechanism HTTP already has for this exact problem, so clients, proxies and
 * generated SDKs recognise it without being taught a Gamma-specific convention, and a future
 * versioned resource inherits the same vocabulary instead of inventing its own.
 *
 * <p>Every response carrying a single dashboard sets {@code ETag} to its version, and PUT requires
 * that value back in {@code If-Match}. The precondition is enforced, never inferred:
 *
 * <ul>
 *   <li>no {@code If-Match} → {@code 428 Precondition Required} — the request is refused rather than
 *       treated as an overwrite, since an implicit force is exactly what this feature exists to
 *       prevent;</li>
 *   <li>a stale {@code If-Match} → {@code 412 Precondition Failed}, carrying the current dashboard;</li>
 *   <li>{@code If-Match: *} → the write is applied over whatever revision is current. This is HTTP's
 *       own spelling of a deliberate overwrite, which is what makes it safe to offer: it cannot be
 *       reached by forgetting the header, only by choosing a different one. It is the second step of
 *       the conflict flow — a client that got a 412 and whose user chose "overwrite" can apply the
 *       edit in one request instead of re-reading and re-submitting, which would race again.
 *       Existence is still required, so an overwrite cannot resurrect a deleted dashboard.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class ObservabilityDashboardResource {

    private static final int PRECONDITION_REQUIRED_428 = 428;
    private static final String WILDCARD_VALIDATOR = "*";

    @Inject
    private GetObservabilityDashboardUseCase getDashboardUseCase;

    @Inject
    private UpdateObservabilityDashboardUseCase updateDashboardUseCase;

    @Inject
    private DeleteObservabilityDashboardUseCase deleteDashboardUseCase;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.READ }) })
    public Response get(@PathParam("dashboardId") String dashboardId) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = getDashboardUseCase.execute(new GetObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), dashboardId));
        return DashboardEntityTag.withETag(Response.ok(), output.dashboard()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.UPDATE }) })
    public Response update(@PathParam("dashboardId") String dashboardId, @Context HttpHeaders headers, SaveDashboardRequestDto request) {
        if (request == null) {
            throw new InvalidDashboardException("Request body is required");
        }
        VersionPrecondition precondition = requiredVersionPrecondition(headers.getRequestHeader(HttpHeaders.IF_MATCH));
        var ctx = GraviteeContext.getExecutionContext();
        var output = updateDashboardUseCase.execute(
            new UpdateObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), dashboardId, precondition, request.toContent())
        );
        return DashboardEntityTag.withETag(Response.ok(), output.dashboard()).build();
    }

    @DELETE
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.DELETE }) })
    public Response delete(@PathParam("dashboardId") String dashboardId) {
        var ctx = GraviteeContext.getExecutionContext();
        deleteDashboardUseCase.execute(new DeleteObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), dashboardId));
        return Response.noContent().build();
    }

    /**
     * Turns {@code If-Match} into the precondition the write is subject to.
     *
     * <p>Takes the header's every value rather than one string, because HTTP lets a client spell the same list two
     * ways — {@code If-Match: "1", "3"} or the field repeated once per validator — and reading only the first would
     * refuse a request whose precondition holds. Both forms flatten to the same set of validators here.
     *
     * <p>Weak forms ({@code W/"4"}) are accepted as equivalent: the validator is a revision counter, so a proxy
     * rewriting the form does not change which revision it names. A validator that is not a version this API issued
     * is refused rather than guessed at.
     */
    private static VersionPrecondition requiredVersionPrecondition(List<String> ifMatch) {
        List<String> validators = ifMatch == null
            ? List.of()
            : ifMatch
                .stream()
                .filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split(",")))
                .map(String::trim)
                .filter(validator -> !validator.isEmpty())
                .toList();

        if (validators.isEmpty()) {
            throw new WebApplicationException(
                Response.status(PRECONDITION_REQUIRED_428)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(
                        new ErrorEntity(
                            "If-Match is required on a dashboard update: send back the ETag you received, or `*` to overwrite whatever revision is current",
                            PRECONDITION_REQUIRED_428
                        )
                    )
                    .build()
            );
        }
        if (validators.contains(WILDCARD_VALIDATOR)) {
            if (validators.size() > 1) {
                // `*` already means "any revision", so pairing it with a specific one states two different intents.
                // Guessing which was meant could silently overwrite; ask instead.
                throw new InvalidDashboardException("If-Match must be either `*` or a list of ETags, not both");
            }
            return VersionPrecondition.anyVersion();
        }
        return VersionPrecondition.oneOf(validators.stream().map(ObservabilityDashboardResource::parseVersion).collect(Collectors.toSet()));
    }

    private static int parseVersion(String validator) {
        String value = validator.startsWith("W/") ? validator.substring(2) : validator;
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidDashboardException("If-Match '%s' is not a dashboard ETag issued by this API".formatted(validator));
        }
    }
}
