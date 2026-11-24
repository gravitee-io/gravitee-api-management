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
package io.gravitee.gamma.rest.resources.guardrail;

import io.gravitee.common.http.MediaType;
import io.gravitee.gamma.rest.core.guardrail.use_case.CreateGuardrailUseCase;
import io.gravitee.gamma.rest.core.guardrail.use_case.SearchGuardrailUseCase;
import io.gravitee.gamma.rest.resources.CurrentAuditInfo;
import io.gravitee.gamma.rest.resources.dto.PaginatedResponseDto;
import io.gravitee.gamma.rest.resources.guardrail.dto.CreateGuardrailRequestDto;
import io.gravitee.gamma.rest.resources.guardrail.dto.GuardrailDto;
import io.gravitee.gamma.rest.resources.guardrail.dto.GuardrailSummaryDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.GraviteeLicenseFeature;
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
 * Guardrail collection endpoints, mounted under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/guardrails} by {@code GammaRootResource}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET /?q=&page=&perPage=&sortBy=} — the Guardrails of the context environment,
 *       filtered by name when {@code q} is given, paginated (1-based). {@code sortBy} is a field
 *       name optionally prefixed with {@code -}; an unknown one answers 400.</li>
 *   <li>{@code POST /} — creates a Guardrail from a whole definition and answers {@code 201} with a
 *       {@code Location} header. The new Guardrail is {@code UNDEPLOYED}: this stores a definition, it
 *       does not deploy it.</li>
 *   <li>{@code GET|PUT|DELETE /{guardrailId}} — see {@link GuardrailResource}.</li>
 * </ul>
 *
 * <h2>Authorization</h2>
 * Declarative {@code ENVIRONMENT_GUARDRAIL} with per-verb ACLs. CREATE and READ stay distinct so
 * holding read rights never grants creation. The Guardrail endpoints also require the
 * {@code apim-guardians} license feature and answer 403 without it.
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class GuardrailsResource {

    // TODO(AIAM-1913): the license feature keeps its Guardian name until gravitee-node renames it
    public static final String GUARDRAILS_LICENSE_FEATURE = "apim-guardians";

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;

    @Inject
    private SearchGuardrailUseCase searchGuardrailUseCase;

    @Inject
    private CreateGuardrailUseCase createGuardrailUseCase;

    @GET
    @GraviteeLicenseFeature(GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.READ }) })
    public PaginatedResponseDto<GuardrailSummaryDto> list(
        @QueryParam("q") String q,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage,
        @QueryParam("sortBy") String sortBy,
        @QueryParam("deployedFor") String deployedFor
    ) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = searchGuardrailUseCase.execute(
            SearchGuardrailUseCase.Input.builder()
                .environmentId(ctx.getEnvironmentId())
                .q(q)
                .pageNumber(page == null ? 0 : page)
                .pageSize(perPage == null ? 0 : perPage)
                .sortBy(sortBy)
                .deployedFor(deployedFor)
                .build()
        );

        var guardrails = output.guardrails();
        return PaginatedResponseDto.of(
            guardrails.getContent().stream().map(GuardrailSummaryDto::from).toList(),
            guardrails.getTotalElements(),
            (int) guardrails.getPageNumber(),
            output.perPage()
        );
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature(GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.CREATE }) })
    public Response create(@NotNull CreateGuardrailRequestDto request) {
        var output = createGuardrailUseCase.execute(
            CreateGuardrailUseCase.Input.builder().auditInfo(CurrentAuditInfo.of()).guardrailToCreate(request.toCore()).build()
        );

        var created = output.guardrail();
        return GuardrailEntityTag.withETag(
            Response.created(uriInfo.getAbsolutePathBuilder().path(created.getId()).build()),
            created
        ).build();
    }

    @Path("/{guardrailId}")
    public GuardrailResource getGuardrailResource() {
        return resourceContext.getResource(GuardrailResource.class);
    }
}
