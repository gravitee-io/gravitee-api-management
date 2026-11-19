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
import io.gravitee.gamma.rest.core.guardian.use_case.DeleteGuardianUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.DeployGuardianUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.GetGuardianDeploymentUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.GetGuardianUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.ListGuardianDeploymentsUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.RollbackGuardianUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.UndeployGuardianUseCase;
import io.gravitee.gamma.rest.core.guardian.use_case.UpdateGuardianUseCase;
import io.gravitee.gamma.rest.resources.CurrentAuditInfo;
import io.gravitee.gamma.rest.resources.dto.PaginatedResponseDto;
import io.gravitee.gamma.rest.resources.guardian.dto.DeployGuardianRequestDto;
import io.gravitee.gamma.rest.resources.guardian.dto.GuardianDeploymentDetailDto;
import io.gravitee.gamma.rest.resources.guardian.dto.GuardianDeploymentDto;
import io.gravitee.gamma.rest.resources.guardian.dto.UpdateGuardianRequestDto;
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
 * One Guardian, under
 * {@code /gamma/organizations/{orgId}/environments/{envId}/guardians/{guardianId}}.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>{@code GET} — the Guardian, or {@code 404} when the id is unknown <em>or belongs to another
 *       environment</em>: the answer is the same either way, so a 404 never confirms that an id
 *       exists elsewhere.</li>
 *   <li>{@code PUT} — replaces the whole definition. A field the payload leaves out is left out of
 *       the stored definition too; only the identity and the deployment stamp are kept back.</li>
 *   <li>{@code POST /_deploy} — publishes the current draft with an optional deployment label.</li>
 *   <li>{@code POST /_undeploy} — withdraws the publication while preserving the draft.</li>
 *   <li>{@code GET /deployments?page=&perPage=} — the deployed revisions, newest first, paginated
 *       (1-based); withdrawals are not revisions and are left out.</li>
 *   <li>{@code GET /deployments/{deploymentId}} — one revision with the definition it deployed, or {@code 404}
 *       when the id is not a deployment of this Guardian.</li>
 *   <li>{@code POST /deployments/{deploymentId}/_rollback} — replaces the draft with the definition of that
 *       revision, keeping the description, without deploying it; the checks of a {@code PUT} apply.</li>
 *   <li>{@code DELETE} — {@code 204}, or {@code 409} while the Guardian has an active publication:
 *       {@code _undeploy} withdraws it first. The PRD's refusal to delete a Guardian enabled on an
 *       API waits on the API-Guardian association (AIAM-1533).</li>
 * </ul>
 *
 * @author GraviteeSource Team
 */
@Produces(MediaType.APPLICATION_JSON)
public class GuardianResource {

    @Inject
    private GetGuardianUseCase getGuardianUseCase;

    @Inject
    private UpdateGuardianUseCase updateGuardianUseCase;

    @Inject
    private DeleteGuardianUseCase deleteGuardianUseCase;

    @Inject
    private DeployGuardianUseCase deployGuardianUseCase;

    @Inject
    private UndeployGuardianUseCase undeployGuardianUseCase;

    @Inject
    private ListGuardianDeploymentsUseCase listGuardianDeploymentsUseCase;

    @Inject
    private GetGuardianDeploymentUseCase getGuardianDeploymentUseCase;

    @Inject
    private RollbackGuardianUseCase rollbackGuardianUseCase;

    @POST
    @Path("/_undeploy")
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.UPDATE }) })
    public Response undeploy(@PathParam("guardianId") String guardianId) {
        var output = undeployGuardianUseCase.execute(new UndeployGuardianUseCase.Input(CurrentAuditInfo.of(), guardianId));
        return GuardianEntityTag.withETag(Response.ok(), output.guardian()).build();
    }

    @POST
    @Path("/_deploy")
    @Consumes(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.UPDATE }) })
    public Response deploy(@PathParam("guardianId") String guardianId, @Valid DeployGuardianRequestDto request) {
        var output = deployGuardianUseCase.execute(
            new DeployGuardianUseCase.Input(CurrentAuditInfo.of(), guardianId, request == null ? null : request.deploymentLabel())
        );
        return GuardianEntityTag.withETag(Response.ok(), output.guardian()).build();
    }

    @GET
    @Path("/deployments")
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.READ }) })
    public PaginatedResponseDto<GuardianDeploymentDto> listDeployments(
        @PathParam("guardianId") String guardianId,
        @QueryParam("page") Integer page,
        @QueryParam("perPage") Integer perPage
    ) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = listGuardianDeploymentsUseCase.execute(
            new ListGuardianDeploymentsUseCase.Input(
                ctx.getEnvironmentId(),
                guardianId,
                page == null ? 0 : page,
                perPage == null ? 0 : perPage
            )
        );
        var deployments = output.deployments();
        return PaginatedResponseDto.of(
            deployments.getContent().stream().map(GuardianDeploymentDto::from).toList(),
            deployments.getTotalElements(),
            (int) deployments.getPageNumber(),
            output.perPage()
        );
    }

    @GET
    @Path("/deployments/{deploymentId}")
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.READ }) })
    public GuardianDeploymentDetailDto getDeployment(
        @PathParam("guardianId") String guardianId,
        @PathParam("deploymentId") String deploymentId
    ) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = getGuardianDeploymentUseCase.execute(
            new GetGuardianDeploymentUseCase.Input(ctx.getEnvironmentId(), guardianId, deploymentId)
        );
        return GuardianDeploymentDetailDto.from(output.deployment());
    }

    @POST
    @Path("/deployments/{deploymentId}/_rollback")
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.UPDATE }) })
    public Response rollback(@PathParam("guardianId") String guardianId, @PathParam("deploymentId") String deploymentId) {
        var output = rollbackGuardianUseCase.execute(new RollbackGuardianUseCase.Input(CurrentAuditInfo.of(), guardianId, deploymentId));
        return GuardianEntityTag.withETag(Response.ok(), output.guardian()).build();
    }

    @GET
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.READ }) })
    public Response get(@PathParam("guardianId") String guardianId) {
        var ctx = GraviteeContext.getExecutionContext();
        var output = getGuardianUseCase.execute(
            GetGuardianUseCase.Input.builder().environmentId(ctx.getEnvironmentId()).guardianId(guardianId).build()
        );
        return GuardianEntityTag.withETag(Response.ok(), output.guardian()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.UPDATE }) })
    public Response update(
        @PathParam("guardianId") String guardianId,
        @Context HttpHeaders headers,
        @NotNull UpdateGuardianRequestDto request
    ) {
        var precondition = GuardianEntityTag.requiredPrecondition(headers.getRequestHeader(HttpHeaders.IF_MATCH));
        var output = updateGuardianUseCase.execute(
            UpdateGuardianUseCase.Input.builder()
                .auditInfo(CurrentAuditInfo.of())
                .guardianId(guardianId)
                .guardianToUpdate(request.toCore())
                .precondition(precondition)
                .build()
        );
        return GuardianEntityTag.withETag(Response.ok(), output.guardian()).build();
    }

    @DELETE
    @GraviteeLicenseFeature(GuardiansResource.GUARDIANS_LICENSE_FEATURE)
    @Permissions({ @Permission(value = RolePermission.ENVIRONMENT_GUARDIAN, acls = { RolePermissionAction.DELETE }) })
    public Response delete(@PathParam("guardianId") String guardianId) {
        deleteGuardianUseCase.execute(
            DeleteGuardianUseCase.Input.builder().auditInfo(CurrentAuditInfo.of()).guardianId(guardianId).build()
        );
        return Response.noContent().build();
    }
}
