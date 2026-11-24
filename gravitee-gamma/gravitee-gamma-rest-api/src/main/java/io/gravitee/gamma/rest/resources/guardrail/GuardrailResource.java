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
import io.gravitee.gamma.rest.core.guardrail.use_case.DeleteGuardrailUseCase;
import io.gravitee.gamma.rest.core.guardrail.use_case.DeployGuardrailUseCase;
import io.gravitee.gamma.rest.core.guardrail.use_case.GetGuardrailDeploymentUseCase;
import io.gravitee.gamma.rest.core.guardrail.use_case.GetGuardrailUseCase;
import io.gravitee.gamma.rest.core.guardrail.use_case.ListGuardrailApisUseCase;
import io.gravitee.gamma.rest.core.guardrail.use_case.ListGuardrailDeploymentsUseCase;
import io.gravitee.gamma.rest.core.guardrail.use_case.RollbackGuardrailUseCase;
import io.gravitee.gamma.rest.core.guardrail.use_case.UndeployGuardrailUseCase;
import io.gravitee.gamma.rest.core.guardrail.use_case.UpdateGuardrailUseCase;
import io.gravitee.gamma.rest.resources.CurrentAuditInfo;
import io.gravitee.gamma.rest.resources.dto.PaginatedResponseDto;
import io.gravitee.gamma.rest.resources.guardrail.dto.ApiReferenceDto;
import io.gravitee.gamma.rest.resources.guardrail.dto.DeployGuardrailRequestDto;
import io.gravitee.gamma.rest.resources.guardrail.dto.GuardrailDeploymentDetailDto;
import io.gravitee.gamma.rest.resources.guardrail.dto.GuardrailDeploymentDto;
import io.gravitee.gamma.rest.resources.guardrail.dto.UpdateGuardrailRequestDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.GraviteeLicenseFeature;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

/**
 * One Guardrail, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/guardrails/{guardrailId}}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET} — the Guardrail, or {@code 404} when the id is unknown <em>or belongs to another
 *       environment</em>: the answer is the same either way, so a 404 never confirms that an id
 *       exists elsewhere.</li>
 *   <li>{@code PUT} — replaces the whole definition; {@code 409} when it would move a Guardrail enabled on an API to
 *       another phase or to other API types. A field the payload leaves out is left out of
 *       the stored definition too; only the identity and the deployment stamp are kept back.</li>
 *   <li>{@code POST /_deploy} — publishes the current draft with an optional deployment label.</li>
 *   <li>{@code POST /_undeploy} — withdraws the publication while preserving the draft; {@code 409} while the
 *       Guardrail is enabled on an API.</li>
 *   <li>{@code GET /deployments?page=&perPage=} — the deployed revisions, newest first, paginated
 *       (1-based); withdrawals are not revisions and are left out.</li>
 *   <li>{@code GET /deployments/{deploymentId}} — one revision with the definition it deployed, or {@code 404}
 *       when the id is not a deployment of this Guardrail.</li>
 *   <li>{@code POST /deployments/{deploymentId}/_rollback} — replaces the draft with the definition of that
 *       revision, keeping the description, without deploying it; the checks of a {@code PUT} apply.</li>
 *   <li>{@code GET /apis} — the APIs of the environment running the Guardrail, by name, limited to those the
 *       caller may read.</li>
 *   <li>{@code DELETE} — {@code 204}, or {@code 409} while the Guardrail has an active publication:
 *       {@code _undeploy} withdraws it first. {@code 409} as well while an API runs it, counting them.</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class GuardrailResource {

    @Inject
    private GetGuardrailUseCase getGuardrailUseCase;

    @Inject
    private UpdateGuardrailUseCase updateGuardrailUseCase;

    @Inject
    private DeleteGuardrailUseCase deleteGuardrailUseCase;

    @Inject
    private DeployGuardrailUseCase deployGuardrailUseCase;

    @Inject
    private UndeployGuardrailUseCase undeployGuardrailUseCase;

    @Inject
    private ListGuardrailDeploymentsUseCase listGuardrailDeploymentsUseCase;

    @Inject
    private GetGuardrailDeploymentUseCase getGuardrailDeploymentUseCase;

    @Inject
    private RollbackGuardrailUseCase rollbackGuardrailUseCase;

    @Inject
    private ListGuardrailApisUseCase listGuardrailApisUseCase;

    @POST
    @Path("/_undeploy")
    @GraviteeLicenseFeature(GuardrailsResource.GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.UPDATE }) })
    public Response undeploy(@PathParam("guardrailId") String guardrailId) {
        var output = undeployGuardrailUseCase.execute(new UndeployGuardrailUseCase.Input(CurrentAuditInfo.of(), guardrailId));
        return GuardrailEntityTag.withETag(Response.ok(), output.guardrail()).build();
    }

    @POST
    @Path("/_deploy")
    @Consumes(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature(GuardrailsResource.GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.UPDATE }) })
    public Response deploy(@PathParam("guardrailId") String guardrailId, @Valid DeployGuardrailRequestDto request) {
        var output = deployGuardrailUseCase.execute(
            new DeployGuardrailUseCase.Input(CurrentAuditInfo.of(), guardrailId, request == null ? null : request.deploymentLabel())
        );
        return GuardrailEntityTag.withETag(Response.ok(), output.guardrail()).build();
    }

    @GET
    @Path("/apis")
    @GraviteeLicenseFeature(GuardrailsResource.GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.READ }) })
    public PaginatedResponseDto<ApiReferenceDto> listApis(
        @PathParam("guardrailId") String guardrailId,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage
    ) {
        var output = listGuardrailApisUseCase.execute(
            new ListGuardrailApisUseCase.Input(CurrentAuditInfo.of(), guardrailId, page == null ? 0 : page, perPage == null ? 0 : perPage)
        );
        var apis = output.apis();
        return PaginatedResponseDto.of(
            apis.getContent().stream().map(ApiReferenceDto::from).toList(),
            apis.getTotalElements(),
            (int) apis.getPageNumber(),
            output.perPage()
        );
    }

    @GET
    @Path("/deployments")
    @GraviteeLicenseFeature(GuardrailsResource.GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.READ }) })
    public PaginatedResponseDto<GuardrailDeploymentDto> listDeployments(
        @PathParam("guardrailId") String guardrailId,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage
    ) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = listGuardrailDeploymentsUseCase.execute(
            new ListGuardrailDeploymentsUseCase.Input(
                ctx.getEnvironmentId(),
                guardrailId,
                page == null ? 0 : page,
                perPage == null ? 0 : perPage
            )
        );
        var deployments = output.deployments();
        return PaginatedResponseDto.of(
            deployments.getContent().stream().map(GuardrailDeploymentDto::from).toList(),
            deployments.getTotalElements(),
            (int) deployments.getPageNumber(),
            output.perPage()
        );
    }

    @GET
    @Path("/deployments/{deploymentId}")
    @GraviteeLicenseFeature(GuardrailsResource.GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.READ }) })
    public GuardrailDeploymentDetailDto getDeployment(
        @PathParam("guardrailId") String guardrailId,
        @PathParam("deploymentId") String deploymentId
    ) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = getGuardrailDeploymentUseCase.execute(
            new GetGuardrailDeploymentUseCase.Input(ctx.getEnvironmentId(), guardrailId, deploymentId)
        );
        return GuardrailDeploymentDetailDto.from(output.deployment());
    }

    @POST
    @Path("/deployments/{deploymentId}/_rollback")
    @GraviteeLicenseFeature(GuardrailsResource.GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.UPDATE }) })
    public Response rollback(@PathParam("guardrailId") String guardrailId, @PathParam("deploymentId") String deploymentId) {
        var output = rollbackGuardrailUseCase.execute(new RollbackGuardrailUseCase.Input(CurrentAuditInfo.of(), guardrailId, deploymentId));
        return GuardrailEntityTag.withETag(Response.ok(), output.guardrail()).build();
    }

    @GET
    @GraviteeLicenseFeature(GuardrailsResource.GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.READ }) })
    public Response get(@PathParam("guardrailId") String guardrailId) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = getGuardrailUseCase.execute(
            GetGuardrailUseCase.Input.builder().environmentId(ctx.getEnvironmentId()).guardrailId(guardrailId).build()
        );
        return GuardrailEntityTag.withETag(Response.ok(), output.guardrail()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature(GuardrailsResource.GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.UPDATE }) })
    public Response update(
        @PathParam("guardrailId") String guardrailId,
        @Context HttpHeaders headers,
        @NotNull UpdateGuardrailRequestDto request
    ) {
        var precondition = GuardrailEntityTag.requiredPrecondition(headers.getRequestHeader(HttpHeaders.IF_MATCH));
        var output = updateGuardrailUseCase.execute(
            UpdateGuardrailUseCase.Input.builder()
                .auditInfo(CurrentAuditInfo.of())
                .guardrailId(guardrailId)
                .guardrailToUpdate(request.toCore())
                .precondition(precondition)
                .build()
        );
        return GuardrailEntityTag.withETag(Response.ok(), output.guardrail()).build();
    }

    @DELETE
    @GraviteeLicenseFeature(GuardrailsResource.GUARDRAILS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDRAIL, acls = { RolePermissionAction.DELETE }) })
    public Response delete(@PathParam("guardrailId") String guardrailId) {
        deleteGuardrailUseCase.execute(
            DeleteGuardrailUseCase.Input.builder().auditInfo(CurrentAuditInfo.of()).guardrailId(guardrailId).build()
        );
        return Response.noContent().build();
    }
}
