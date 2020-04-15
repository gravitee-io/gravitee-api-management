/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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
import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;

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
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.READ)
    })
    public Response getMembersByApplicationId(@PathParam("applicationId") String applicationId, @BeanParam PaginationParam paginationParam) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        List<Member> membersList = membershipService.getMembersByReference(MembershipReferenceType.APPLICATION, applicationId).stream()
                .map(membership -> memberMapper.convert(membership, uriInfo))
                .collect(Collectors.toList());
        
        return createListResponse(membersList, paginationParam);
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.CREATE)
    })
    public Response createApplicationMember(@PathParam("applicationId") String applicationId, @Valid @NotNull(message="Input must not be null.") MemberInput memberInput) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //There can be only one
        if (SystemRole.PRIMARY_OWNER.name().equals(memberInput.getRole())) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }
        
        MemberEntity membership = membershipService.addRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, applicationId),
                new MembershipService.MembershipMember(memberInput.getUser(), memberInput.getReference(), MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, memberInput.getRole()));

        return Response
                .status(Status.CREATED)
                .entity(memberMapper.convert(membership, uriInfo))
                .build();
    }

    @GET
    @Path("/{memberId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.READ)
    })
    public Response getApplicationMemberByApplicationIdAndMemberId(@PathParam("applicationId") String applicationId, @PathParam("memberId") String memberId) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //Does user exist ?
        userService.findById(memberId);
        
        
        MemberEntity memberEntity = membershipService.getUserMember(MembershipReferenceType.APPLICATION, applicationId, memberId);
        if(memberEntity != null) {
            return Response
                    .ok(memberMapper.convert(memberEntity, uriInfo))
                    .build();
        }
        throw new NotFoundException();
    }

    @DELETE
    @Path("/{memberId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.DELETE)
    })
    public Response deleteApplicationMember(@PathParam("applicationId") String applicationId, @PathParam("memberId") String memberId) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //Does user exist ?
        userService.findById(memberId);
        
        membershipService.deleteReferenceMember(MembershipReferenceType.APPLICATION, applicationId, MembershipMemberType.USER, memberId);
        return Response.noContent().build();
    }
    
    @PUT
    @Path("/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE)
    })
    public Response updateApplicationMemberByApplicationIdAndMemberId(@PathParam("applicationId") String applicationId, @PathParam("memberId") String memberId, @Valid @NotNull(message="Input must not be null.") MemberInput memberInput) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //Does user exist ?
        userService.findById(memberId);
        
        if(memberInput.getUser() != null && !memberId.equals(memberInput.getUser())) {
            throw new BadRequestException("'memberInput.user' should the same as 'memberId'");
        }
        
        //There can be only one
        if (SystemRole.PRIMARY_OWNER.name().equals(memberInput.getRole())) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }
        
        MemberEntity membership = membershipService.updateRoleToMemberOnReference(
                new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, applicationId),
                new MembershipService.MembershipMember(memberId, memberInput.getReference(), MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, memberInput.getRole()));

        return Response
                .ok(memberMapper.convert(membership, uriInfo))
                .build();
    }

    @POST
    @Path("/_transfer_ownership")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
        @Permission(value = RolePermission.APPLICATION_MEMBER, acls = RolePermissionAction.UPDATE)
    })
    public Response transferMemberOwnership(@PathParam("applicationId") String applicationId, @NotNull(message="Input must not be null.") TransferOwnershipInput transferOwnershipInput) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //There can be only one
        if (SystemRole.PRIMARY_OWNER.name().equals(transferOwnershipInput.getPrimaryOwnerNewrole())) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }
        
        List<RoleEntity> newRoles = new ArrayList<>();

        Optional<RoleEntity> optionalRole = roleService.findByScopeAndName(RoleScope.APPLICATION, transferOwnershipInput.getPrimaryOwnerNewrole());
        if (optionalRole.isPresent()) {
            newRoles.add(optionalRole.get());
        }
        // else condition doesn't matter because default role will be applied on former PrimaryOwner
        
        membershipService.transferApplicationOwnership(applicationId, 
                new MembershipService.MembershipMember( transferOwnershipInput.getNewPrimaryOwnerId(), transferOwnershipInput.getNewPrimaryOwnerReference(), MembershipMemberType.USER), newRoles);
        return Response
                .noContent()
                .build();
    }
    
}
