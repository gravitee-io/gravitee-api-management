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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import io.gravitee.apim.core.api_product.use_case.TransferApiProductOwnershipUseCase;
import io.gravitee.apim.core.api_product.use_case.VerifyApiProductExistsUseCase;
import io.gravitee.apim.core.api_product.use_case.members.AddApiProductMemberUseCase;
import io.gravitee.apim.core.api_product.use_case.members.DeleteApiProductMemberUseCase;
import io.gravitee.apim.core.api_product.use_case.members.GetApiProductMembersUseCase;
import io.gravitee.apim.core.api_product.use_case.members.UpdateApiProductMemberUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MembershipMapper;
import io.gravitee.rest.api.management.v2.rest.model.AddMember;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductTransferOwnership;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdateMember;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.ApiProductPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;

@CustomLog
public class ApiProductMembersResource extends AbstractResource {

    @PathParam("apiProductId")
    private String apiProductId;

    @Inject
    private VerifyApiProductExistsUseCase verifyApiProductExistsUseCase;

    @Inject
    private GetApiProductMembersUseCase getApiProductMembersUseCase;

    @Inject
    private AddApiProductMemberUseCase addApiProductMemberUseCase;

    @Inject
    private UpdateApiProductMemberUseCase updateApiProductMemberUseCase;

    @Inject
    private DeleteApiProductMemberUseCase deleteApiProductMemberUseCase;

    @Inject
    private TransferApiProductOwnershipUseCase transferApiProductOwnershipUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_MEMBER, acls = { RolePermissionAction.READ }) })
    public Response getApiProductMembers(@BeanParam @Valid PaginationParam paginationParam) {
        verifyApiProductInCurrentEnvironment();
        var output = getApiProductMembersUseCase.execute(new GetApiProductMembersUseCase.Input(apiProductId));
        var members = output.members().stream().map(MemberMapper.INSTANCE::map).toList();

        List<Member> membersSubList = computePaginationData(members, paginationParam);

        return Response.ok(
            new MembersResponse()
                .data(membersSubList)
                .pagination(PaginationInfo.computePaginationInfo(members.size(), membersSubList.size(), paginationParam))
                .links(computePaginationLinks(members.size(), paginationParam))
        ).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_MEMBER, acls = RolePermissionAction.CREATE) })
    public Response createApiProductMembership(@Valid @NotNull AddMember apiProductMembership) {
        verifyApiProductInCurrentEnvironment();

        var output = addApiProductMemberUseCase.execute(
            new AddApiProductMemberUseCase.Input(MemberMapper.INSTANCE.map(apiProductMembership), apiProductId)
        );
        return Response.status(Response.Status.CREATED).entity(MemberMapper.INSTANCE.map(output.createdMember())).build();
    }

    @PUT
    @Path("/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response updateApiProductMembership(@PathParam("memberId") String memberId, @Valid @NotNull UpdateMember updateMember) {
        verifyApiProductInCurrentEnvironment();

        var output = updateApiProductMemberUseCase.execute(
            new UpdateApiProductMemberUseCase.Input(updateMember.getRoleName(), memberId, apiProductId)
        );
        return Response.ok().entity(MemberMapper.INSTANCE.map(output.updatedMember())).build();
    }

    @Path("/{memberId}")
    @DELETE
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_MEMBER, acls = RolePermissionAction.DELETE) })
    public Response deleteApiProductMembership(@PathParam("memberId") String memberId) {
        verifyApiProductInCurrentEnvironment();
        deleteApiProductMemberUseCase.execute(new DeleteApiProductMemberUseCase.Input(apiProductId, memberId));
        return Response.noContent().build();
    }

    @GET
    @Path("/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiProductMemberPermissions() {
        verifyApiProductInCurrentEnvironment();
        Map<String, char[]> permissions = new HashMap<>();
        if (isAuthenticated()) {
            final String userId = getAuthenticatedUser();
            if (isAdmin()) {
                final char[] rights = new char[] {
                    RolePermissionAction.CREATE.getId(),
                    RolePermissionAction.READ.getId(),
                    RolePermissionAction.UPDATE.getId(),
                    RolePermissionAction.DELETE.getId(),
                };
                for (ApiProductPermission perm : ApiProductPermission.values()) {
                    permissions.put(perm.getName(), rights);
                }
            } else {
                permissions = membershipService.getUserMemberPermissions(
                    GraviteeContext.getExecutionContext(),
                    MembershipReferenceType.API_PRODUCT,
                    apiProductId,
                    userId
                );
            }
        }
        return Response.ok(permissions).build();
    }

    @POST
    @Path("/_transfer-ownership")
    @Consumes(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response transferApiProductOwnership(@Valid @NotNull ApiProductTransferOwnership transferOwnership) {
        verifyApiProductInCurrentEnvironment();
        transferApiProductOwnershipUseCase.execute(
            new TransferApiProductOwnershipUseCase.Input(MembershipMapper.INSTANCE.map(transferOwnership), apiProductId)
        );
        return Response.noContent().build();
    }

    private void verifyApiProductInCurrentEnvironment() {
        var ctx = GraviteeContext.getExecutionContext();
        verifyApiProductExistsUseCase.execute(new VerifyApiProductExistsUseCase.Input(ctx.getEnvironmentId(), apiProductId));
    }
}
