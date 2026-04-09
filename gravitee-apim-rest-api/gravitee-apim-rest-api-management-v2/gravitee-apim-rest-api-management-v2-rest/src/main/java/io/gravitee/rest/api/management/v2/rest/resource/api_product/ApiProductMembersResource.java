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

import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

import io.gravitee.apim.core.api_product.use_case.VerifyApiProductExistsUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.model.AddMember;
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
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.CustomLog;

@CustomLog
public class ApiProductMembersResource extends AbstractResource {

    @PathParam("apiProductId")
    private String apiProductId;

    @Inject
    private VerifyApiProductExistsUseCase verifyApiProductExistsUseCase;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_MEMBER, acls = { RolePermissionAction.READ }) })
    public Response getApiProductMembers(@BeanParam @Valid PaginationParam paginationParam) {
        verifyApiProductInCurrentEnvironment();
        var members = membershipService
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API_PRODUCT, apiProductId)
            .stream()
            .map(MemberMapper.INSTANCE::map)
            .sorted(Comparator.comparing(Member::getId, Comparator.nullsLast(String::compareTo)))
            .collect(Collectors.toList());

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
    public Response createApiProductMembership(AddMember apiProductMembership) {
        verifyApiProductInCurrentEnvironment();
        checkRoleIsNotPrimaryOwner(apiProductMembership.getRoleName());

        if (apiProductMembership.getUserId() == null && apiProductMembership.getExternalReference() == null) {
            throw new InvalidDataException("Request must specify either userId or externalReference");
        }

        var createdMember = membershipService.createNewMembership(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.API_PRODUCT,
            apiProductId,
            apiProductMembership.getUserId(),
            apiProductMembership.getExternalReference(),
            apiProductMembership.getRoleName()
        );
        return Response.status(Response.Status.CREATED).entity(MemberMapper.INSTANCE.map(createdMember)).build();
    }

    @PUT
    @Path("/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response updateApiProductMembership(@PathParam("memberId") String memberId, UpdateMember updateMember) {
        verifyApiProductInCurrentEnvironment();
        checkRoleIsNotPrimaryOwner(updateMember.getRoleName());

        var updatedMembership = membershipService.updateMembershipForApiProduct(
            GraviteeContext.getExecutionContext(),
            apiProductId,
            memberId,
            updateMember.getRoleName()
        );
        return Response.ok().entity(MemberMapper.INSTANCE.map(updatedMembership)).build();
    }

    @Path("/{memberId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_PRODUCT_MEMBER, acls = RolePermissionAction.DELETE) })
    public Response deleteApiProductMembership(@PathParam("memberId") String memberId) {
        verifyApiProductInCurrentEnvironment();
        membershipService.deleteMemberForApiProduct(GraviteeContext.getExecutionContext(), apiProductId, memberId);
        return Response.noContent().build();
    }

    @GET
    @Path("/permissions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiProductMemberPermissions() {
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

    private void verifyApiProductInCurrentEnvironment() {
        var ctx = GraviteeContext.getExecutionContext();
        verifyApiProductExistsUseCase.execute(new VerifyApiProductExistsUseCase.Input(ctx.getEnvironmentId(), apiProductId));
    }

    private void checkRoleIsNotPrimaryOwner(String roleId) {
        if (PRIMARY_OWNER.name().equals(roleId)) {
            throw new SinglePrimaryOwnerException(RoleScope.API_PRODUCT);
        }
    }
}
