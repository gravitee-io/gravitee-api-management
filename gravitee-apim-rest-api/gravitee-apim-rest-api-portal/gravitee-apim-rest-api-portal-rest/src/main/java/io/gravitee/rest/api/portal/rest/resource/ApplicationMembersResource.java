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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.portal.rest.mapper.MemberMapper;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.model.MemberInput;
import io.gravitee.rest.api.portal.rest.model.TransferOwnershipInput;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMembersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private UserService userService;

    @Inject
    private MemberMapper memberMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.READ) })
    public Response getMembersByApplicationId(
        @PathParam("applicationId") String applicationId,
        @BeanParam PaginationParam paginationParam
    ) {
        //Does application exist ?
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        applicationService.findById(executionContext, applicationId);

        List<Member> membersList = membershipService
            .getMembersByReference(executionContext, MembershipReferenceType.APPLICATION, applicationId)
            .stream()
            .map(membership -> memberMapper.convert(executionContext, membership, uriInfo))
            .collect(Collectors.toList());

        return createListResponse(executionContext, membersList, paginationParam);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.CREATE) })
    public Response createApplicationMember(
        @PathParam("applicationId") String applicationId,
        @Valid @NotNull(message = "Input must not be null.") MemberInput memberInput
    ) {
        //Does application exist ?
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);

        //There can be only one
        if (SystemRole.PRIMARY_OWNER.name().equals(memberInput.getRole())) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }

        MemberEntity membership = membershipService.addRoleToMemberOnReference(
            GraviteeContext.getExecutionContext(),
            new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, applicationId),
            new MembershipService.MembershipMember(memberInput.getUser(), memberInput.getReference(), MembershipMemberType.USER),
            new MembershipService.MembershipRole(RoleScope.APPLICATION, memberInput.getRole())
        );

        return Response.created(this.getLocationHeader(membership.getId()))
            .entity(memberMapper.convert(GraviteeContext.getExecutionContext(), membership, uriInfo))
            .build();
    }

    @GET
    @Path("/{memberId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.READ) })
    public Response getApplicationMemberByApplicationIdAndMemberId(
        @PathParam("applicationId") String applicationId,
        @PathParam("memberId") String memberId
    ) {
        //Does application exist ?
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);

        //Does user exist ?
        userService.findById(GraviteeContext.getExecutionContext(), memberId);

        MemberEntity memberEntity = membershipService.getUserMember(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.APPLICATION,
            applicationId,
            memberId
        );
        if (memberEntity != null) {
            return Response.ok(memberMapper.convert(GraviteeContext.getExecutionContext(), memberEntity, uriInfo)).build();
        }
        throw new NotFoundException();
    }

    @DELETE
    @Path("/{memberId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.DELETE) })
    public Response deleteApplicationMember(@PathParam("applicationId") String applicationId, @PathParam("memberId") String memberId) {
        //Does application exist ?
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);

        //Does user exist ?
        userService.findById(GraviteeContext.getExecutionContext(), memberId);

        membershipService.deleteReferenceMember(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceType.APPLICATION,
            applicationId,
            MembershipMemberType.USER,
            memberId
        );
        return Response.noContent().build();
    }

    @PUT
    @Path("/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response updateApplicationMemberByApplicationIdAndMemberId(
        @PathParam("applicationId") String applicationId,
        @PathParam("memberId") String memberId,
        @Valid @NotNull(message = "Input must not be null.") MemberInput memberInput
    ) {
        //Does application exist ?
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);

        //Does user exist ?
        userService.findById(GraviteeContext.getExecutionContext(), memberId);

        if (memberInput.getUser() != null && !memberId.equals(memberInput.getUser())) {
            throw new BadRequestException("'memberInput.user' should the same as 'memberId'");
        }

        //There can be only one
        if (SystemRole.PRIMARY_OWNER.name().equals(memberInput.getRole())) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }

        MemberEntity membership = membershipService.updateRoleToMemberOnReference(
            GraviteeContext.getExecutionContext(),
            new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, applicationId),
            new MembershipService.MembershipMember(memberId, memberInput.getReference(), MembershipMemberType.USER),
            new MembershipService.MembershipRole(RoleScope.APPLICATION, memberInput.getRole())
        );

        return Response.ok(memberMapper.convert(GraviteeContext.getExecutionContext(), membership, uriInfo)).build();
    }

    @POST
    @Path("/_transfer_ownership")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({ @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE) })
    public Response transferMemberOwnership(
        @PathParam("applicationId") String applicationId,
        @NotNull(message = "Input must not be null.") TransferOwnershipInput transferOwnershipInput
    ) {
        //Does application exist ?
        applicationService.findById(GraviteeContext.getExecutionContext(), applicationId);

        //There can be only one
        if (SystemRole.PRIMARY_OWNER.name().equals(transferOwnershipInput.getPrimaryOwnerNewrole())) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }

        List<RoleEntity> newRoles = new ArrayList<>();

        Optional<RoleEntity> optionalRole = roleService.findByScopeAndName(
            RoleScope.APPLICATION,
            transferOwnershipInput.getPrimaryOwnerNewrole(),
            GraviteeContext.getCurrentOrganization()
        );
        if (optionalRole.isPresent()) {
            newRoles.add(optionalRole.get());
        }
        // else condition doesn't matter because default role will be applied on former PrimaryOwner

        membershipService.transferApplicationOwnership(
            GraviteeContext.getExecutionContext(),
            applicationId,
            new MembershipService.MembershipMember(
                transferOwnershipInput.getNewPrimaryOwnerId(),
                transferOwnershipInput.getNewPrimaryOwnerReference(),
                MembershipMemberType.USER
            ),
            newRoles
        );
        return Response.noContent().build();
    }
}
