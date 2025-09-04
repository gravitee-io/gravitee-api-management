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
package io.gravitee.rest.api.management.v2.rest.resource.cluster;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.cluster.use_case.members.AddClusterMemberUseCase;
import io.gravitee.apim.core.cluster.use_case.members.DeleteClusterMemberUseCase;
import io.gravitee.apim.core.cluster.use_case.members.GetClusterMembersUseCase;
import io.gravitee.apim.core.cluster.use_case.members.GetClusterPermissionsUseCase;
import io.gravitee.apim.core.cluster.use_case.members.TransferClusterOwnershipUseCase;
import io.gravitee.apim.core.cluster.use_case.members.UpdateClusterMemberUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MembershipMapper;
import io.gravitee.rest.api.management.v2.rest.model.AddMember;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.model.TransferOwnership;
import io.gravitee.rest.api.management.v2.rest.model.UpdateMember;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

public class ClusterMembersResource extends AbstractResource {

    @PathParam("clusterId")
    String clusterId;

    @Inject
    private GetClusterMembersUseCase getClusterMembersUseCase;

    @Inject
    private GetClusterPermissionsUseCase getClusterPermissionsUseCase;

    @Inject
    private AddClusterMemberUseCase addClusterMemberUseCase;

    @Inject
    private UpdateClusterMemberUseCase updateClusterMemberUseCase;

    @Inject
    private DeleteClusterMemberUseCase deleteClusterMemberUseCase;

    @Inject
    private TransferClusterOwnershipUseCase transferClusterOwnershipUseCase;

    @GET
    @Path("permissions")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get cluster members permissions",
        description = "User must have the CLUSTER_MEMBER permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Cluster member's permissions",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON,
            array = @ArraySchema(schema = @Schema(implementation = MemberEntity.class))
        )
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.CLUSTER_MEMBER, acls = { RolePermissionAction.READ }) })
    public Response getClusterMembersPermissions() {
        var output = getClusterPermissionsUseCase.execute(
            new GetClusterPermissionsUseCase.Input(isAuthenticated(), isAdmin(), getAuthenticatedUser(), clusterId)
        );
        return Response.ok(output.permissions()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.CLUSTER_MEMBER, acls = { RolePermissionAction.READ }) })
    public MembersResponse getClusterMembers(@BeanParam @Valid PaginationParam paginationParam) {
        var output = getClusterMembersUseCase.execute(new GetClusterMembersUseCase.Input(clusterId));
        var membersSubList = computePaginationData(output.members(), paginationParam);
        return new MembersResponse()
            .data(MemberMapper.INSTANCE.map(membersSubList))
            .pagination(PaginationInfo.computePaginationInfo(output.members().size(), membersSubList.size(), paginationParam))
            .links(computePaginationLinks(output.members().size(), paginationParam));
    }

    @POST
    @Operation(summary = "Add a cluster member", description = "User must have the CLUSTER_MEMBER permission to use this service")
    @ApiResponse(responseCode = "201", description = "Member has been added successfully")
    @ApiResponse(responseCode = "400", description = "Membership parameter is not valid")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.CLUSTER_MEMBER, acls = RolePermissionAction.CREATE) })
    public Response addClusterMember(@Valid @NotNull AddMember addMember) {
        var executionContext = GraviteeContext.getExecutionContext();
        var userDetails = getAuthenticatedUserDetails();

        AuditInfo audit = AuditInfo
            .builder()
            .organizationId(executionContext.getOrganizationId())
            .environmentId(executionContext.getEnvironmentId())
            .actor(
                AuditActor
                    .builder()
                    .userId(userDetails.getUsername())
                    .userSource(userDetails.getSource())
                    .userSourceId(userDetails.getSourceId())
                    .build()
            )
            .build();

        addClusterMemberUseCase.execute(new AddClusterMemberUseCase.Input(audit, MemberMapper.INSTANCE.map(addMember), clusterId));

        return Response.status(Response.Status.CREATED).build();
    }

    @PUT
    @Path("/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.CLUSTER_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response updateClusterMember(@PathParam("memberId") String memberId, @Valid @NotNull UpdateMember updateMember) {
        var output = updateClusterMemberUseCase.execute(
            new UpdateClusterMemberUseCase.Input(updateMember.getRoleName(), memberId, clusterId)
        );
        return Response.ok().entity(output.updatedMember()).build();
    }

    @Path("/{memberId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.CLUSTER_MEMBER, acls = RolePermissionAction.DELETE) })
    public Response deleteClusterMember(@PathParam("memberId") String memberId) {
        deleteClusterMemberUseCase.execute(new DeleteClusterMemberUseCase.Input(clusterId, memberId));
        return Response.noContent().build();
    }

    @POST
    @Path("/_transfer-ownership")
    @Permissions({ @Permission(value = RolePermission.CLUSTER_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response transferClusterOwnership(TransferOwnership transferOwnership) {
        transferClusterOwnershipUseCase.execute(
            new TransferClusterOwnershipUseCase.Input(MembershipMapper.INSTANCE.map(transferOwnership), clusterId)
        );
        return Response.noContent().build();
    }
}
