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
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.CreateObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.core.observability.dashboard.use_case.ListObservabilityDashboardUseCase;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.DashboardDto;
import io.gravitee.gamma.rest.resources.observability.dashboards.dto.SaveDashboardRequestDto;
import io.gravitee.gamma.rest.resources.tracing.dto.PaginatedResponseDto;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import lombok.CustomLog;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Saved-dashboard endpoints, mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/observability/dashboards} by
 * {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /?page=&perPage=} — dashboards saved in the context environment, paginated
 *       (1-based, {@code perPage} capped server-side at 100). See {@link ListObservabilityDashboardUseCase}
 *       for why pagination is applied by slicing rather than a native repository query.</li>
 *   <li>{@code POST /} — creates a dashboard from a client-supplied id (AGENTS.md §9), returns
 *       {@code 201} with a {@code Location} header.</li>
 *   <li>{@code GET /{dashboardId}}, {@code PUT}, {@code DELETE} — see {@link ObservabilityDashboardResource}.</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * Declarative {@code ENVIRONMENT_DASHBOARD} with per-verb ACLs — a CRUD resource, unlike the
 * deliberately lax {@code ENVIRONMENT_DASHBOARD:READ || ENVIRONMENT_API:READ} check used by
 * {@code LogsResource} / {@code ObservabilityFiltersResource} for metadata discovery. POST and PUT
 * stay separate verbs (no PUT upsert): {@code @Permissions} is per method and CREATE / UPDATE are
 * distinct ACLs, so collapsing them would silently grant creation to anyone holding update rights.
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
@CustomLog
public class ObservabilityDashboardsResource {

    private static final String UNKNOWN_USER = "unknown";

    @Inject
    private ListObservabilityDashboardUseCase listDashboardUseCase;

    @Inject
    private CreateObservabilityDashboardUseCase createDashboardUseCase;

    @Context
    private ResourceContext resourceContext;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.READ }) })
    public PaginatedResponseDto<DashboardDto> list(@QueryParam("page") Integer page, @QueryParam("perPage") Integer perPage) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = listDashboardUseCase.execute(new ListObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), page, perPage));
        List<DashboardDto> data = output.dashboards().stream().map(DashboardDto::from).toList();
        return PaginatedResponseDto.of(data, output.totalCount(), output.page(), output.perPage());
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_DASHBOARD, acls = { RolePermissionAction.CREATE }) })
    public Response create(@Context UriInfo uriInfo, SaveDashboardRequestDto request) {
        if (request == null) {
            throw new InvalidDashboardException("Request body is required");
        }
        var ctx = GraviteeContext.getExecutionContext();
        var output = createDashboardUseCase.execute(
            new CreateObservabilityDashboardUseCase.Input(ctx.getEnvironmentId(), currentUserId(), request.id(), request.toContent())
        );
        DashboardDto dto = DashboardDto.from(output.dashboard());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(dto.id()).build()).entity(dto).build();
    }

    /**
     * Id of the authenticated caller, for {@code createdBy}.
     *
     * <p>Kept local on purpose: the observability adapters derive the same id when they build their
     * {@code AuditInfo}, but they live under {@code infra} and the onion rules forbid
     * {@code resources} and {@code infra} from seeing each other, so the two copies have no shared
     * home short of a new top-level package or a core port — a call worth making on its own rather
     * than inside this feature.
     */
    private static String currentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails user) {
            return user.getUsername();
        }
        if (authentication != null && authentication.getPrincipal() != null) {
            log.debug("Authenticated principal is not a UserDetails, falling back to its string form for createdBy");
            return authentication.getPrincipal().toString();
        }
        log.warn("No authenticated principal while creating a dashboard, recording createdBy as '{}'", UNKNOWN_USER);
        return UNKNOWN_USER;
    }

    @Path("/{dashboardId}")
    public ObservabilityDashboardResource getDashboardResource() {
        return resourceContext.getResource(ObservabilityDashboardResource.class);
    }
}
