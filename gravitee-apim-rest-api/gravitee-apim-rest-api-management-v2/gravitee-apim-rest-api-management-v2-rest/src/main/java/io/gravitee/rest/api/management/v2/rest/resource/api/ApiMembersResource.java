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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static io.gravitee.rest.api.model.permissions.SystemRole.PRIMARY_OWNER;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.model.AddMember;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.model.UpdateMember;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ApiMembersResource extends AbstractResource {

    @PathParam("apiId")
    private String apiId;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_MEMBER, acls = { RolePermissionAction.READ }) })
    public MembersResponse getApiMembers(@BeanParam @Valid PaginationParam paginationParam) {
        var members = membershipService
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, apiId)
            .stream()
            .filter(memberEntity -> memberEntity.getType() == MembershipMemberType.USER)
            .map(MemberMapper.INSTANCE::map)
            .sorted(Comparator.comparing(Member::getId))
            .collect(Collectors.toList());

        List<Member> membersSubList = computePaginationData(members, paginationParam);

        return new MembersResponse()
            .data(membersSubList)
            .pagination(PaginationInfo.computePaginationInfo((long) members.size(), membersSubList.size(), paginationParam))
            .links(computePaginationLinks(members.size(), paginationParam));
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.CREATE) })
    public Response createApiMembership(AddMember apiMembership) {
        checkRoleIsNotPrimaryOwner(apiMembership.getRoleName());

        if (apiMembership.getUserId() == null && apiMembership.getExternalReference() == null) {
            throw new InvalidDataException("Request must specify either userId or externalReference");
        }

        var createdMember = membershipService.createNewMembershipForApi(
            GraviteeContext.getExecutionContext(),
            apiId,
            apiMembership.getUserId(),
            apiMembership.getExternalReference(),
            apiMembership.getRoleName()
        );
        return Response.status(Response.Status.CREATED).entity(MemberMapper.INSTANCE.map(createdMember)).build();
    }

    @PUT
    @Path("/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response updateApiMembership(@PathParam("memberId") String memberId, UpdateMember apiMembership) {
        checkRoleIsNotPrimaryOwner(apiMembership.getRoleName());

        var updatedMembership = membershipService.updateMembershipForApi(
            GraviteeContext.getExecutionContext(),
            apiId,
            memberId,
            apiMembership.getRoleName()
        );
        return Response.ok().entity(MemberMapper.INSTANCE.map(updatedMembership)).build();
    }

    @Path("/{memberId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.API_MEMBER, acls = RolePermissionAction.DELETE) })
    public Response deleteApiMembership(@PathParam("memberId") String memberId) {
        membershipService.deleteMemberForApi(GraviteeContext.getExecutionContext(), apiId, memberId);
        return Response.noContent().build();
    }

    private void checkRoleIsNotPrimaryOwner(String roleId) {
        if (PRIMARY_OWNER.name().equals(roleId)) {
            throw new SinglePrimaryOwnerException(RoleScope.API);
        }
    }
}
