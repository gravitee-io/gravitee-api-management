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
package io.gravitee.gamma.rest.resources.guardian;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.guardian.use_case.CreateGuardianUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.SearchGuardianUseCase;
import io.gravitee.gamma.rest.resources.CurrentAuditInfo;
import io.gravitee.gamma.rest.resources.guardian.dto.CreateGuardianRequestDto;
import io.gravitee.gamma.rest.resources.guardian.dto.GuardianDto;
import io.gravitee.gamma.rest.resources.tracing.dto.PaginatedResponseDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
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

/**
 * Guardian collection endpoints, mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/guardians} by {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /?q=&page=&perPage=&sortBy=} — the Guardians of the context environment,
 *       filtered by name when {@code q} is given, paginated (1-based). {@code sortBy} is a field
 *       name optionally prefixed with {@code -}; an unknown one answers 400.</li>
 *   <li>{@code POST /} — creates a Guardian from a whole definition and answers {@code 201} with a
 *       {@code Location} header. The new Guardian is {@code UNDEPLOYED}: this stores a definition, it
 *       does not deploy it.</li>
 *   <li>{@code GET|PUT|DELETE /{guardianId}} — see {@link GuardianResource}.</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * Declarative {@code ENVIRONMENT_GUARDIAN} with per-verb ACLs. CREATE and READ stay distinct so
 * holding read rights never grants creation.
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class GuardiansResource {

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private SearchGuardianUseCase searchGuardianUseCase;

    @Inject
    private CreateGuardianUseCase createGuardianUseCase;

    @GET
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.READ }) })
    public PaginatedResponseDto<GuardianDto> list(
        @QueryParam("q") String q,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage,
        @QueryParam("sortBy") String sortBy
    ) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = searchGuardianUseCase.execute(
            SearchGuardianUseCase.Input.builder()
                .environmentId(ctx.getEnvironmentId())
                .q(q)
                .pageNumber(page == null ? 0 : page)
                .pageSize(perPage == null ? 0 : perPage)
                .sortBy(sortBy)
                .build()
        );

        var guardians = output.guardians();
        return PaginatedResponseDto.of(
            guardians.getContent().stream().map(GuardianDto::from).toList(),
            guardians.getTotalElements(),
            (int) guardians.getPageNumber(),
            output.perPage()
        );
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.CREATE }) })
    public Response create(@NotNull CreateGuardianRequestDto request) {
        var output = createGuardianUseCase.execute(
            CreateGuardianUseCase.Input.builder().auditInfo(CurrentAuditInfo.of()).guardianToCreate(request.toCore()).build()
        );

        var created = GuardianDto.from(output.guardian());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(created.id()).build()).entity(created).build();
    }

    @Path("/{guardianId}")
    public GuardianResource getGuardianResource() {
        return resourceContext.getResource(GuardianResource.class);
    }
}
