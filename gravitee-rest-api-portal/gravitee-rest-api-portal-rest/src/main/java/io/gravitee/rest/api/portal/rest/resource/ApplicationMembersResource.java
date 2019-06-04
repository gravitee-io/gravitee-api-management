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

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.portal.rest.mapper.MemberMapper;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.model.MemberInput;
import io.gravitee.rest.api.portal.rest.model.MembersResponse;
import io.gravitee.rest.api.portal.rest.model.RoleEnum;
import io.gravitee.rest.api.portal.rest.model.TransferOwnershipInput;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMembersResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Context
    private UriInfo uriInfo;
    
    @Inject
    private ApplicationService applicationService;
    
    @Inject
    private UserService userService;
    
    @Inject
    private MemberMapper memberMapper;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMembersByApplicationId(@PathParam("applicationId") String applicationId, @DefaultValue(PAGE_QUERY_PARAM_DEFAULT)@QueryParam("page") Integer page, @DefaultValue(SIZE_QUERY_PARAM_DEFAULT)@QueryParam("size") Integer size) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //Does current user have sufficient credentials on this application ?
        // APIPortal: user rights on application ?
        
        List<Member> membersList = membershipService.getMembers(MembershipReferenceType.APPLICATION, applicationId, RoleScope.APPLICATION).stream()
                .map(memberMapper::convert)
                .collect(Collectors.toList());
        
        int totalItems = membersList.size();
        
        membersList = this.paginateResultList(membersList, page, size);

        MembersResponse membersResponse = new MembersResponse()
                .data(membersList)
                .links(this.computePaginatedLinks(uriInfo, page, size, totalItems))
                ;
        
        return Response
                .ok(membersResponse)
                .build();
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApplicationMember(@PathParam("applicationId") String applicationId, @Valid MemberInput memberInput) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //Does user exist ?
        userService.findById(memberInput.getUser());
        
        //Does current user have sufficient credentials on this application ?
        // APIPortal: user rights on application ?
        
        //There can be only one
        if (RoleEnum.PRIMARY_OWNER == memberInput.getRole()) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }
        
        //APIPortal: what is a user 'reference' ?
        MemberEntity membership = membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, applicationId),
                new MembershipService.MembershipUser(memberInput.getUser(), null),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, memberInput.getRole().getValue()));

        return Response
                .ok(memberMapper.convert(membership))
                .build();
    }

    @GET
    @Path("/{memberId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApplicationMemberByApplicationIdAndMemberId(@PathParam("applicationId") String applicationId, @PathParam("memberId") String memberId) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //Does user exist ?
        userService.findById(memberId);
        
        //Does current user have sufficient credentials on this application ?
        // APIPortal: user rights on application ?
        
        
        MemberEntity memberEntity = membershipService.getMember(MembershipReferenceType.APPLICATION, applicationId, memberId, RoleScope.APPLICATION);
        if(memberEntity != null) {
            return Response
                    .ok(memberMapper.convert(memberEntity))
                    .build();
        }
        throw new NotFoundException();
    }

    @DELETE
    @Path("/{memberId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteApplicationMember(@PathParam("applicationId") String applicationId, @PathParam("memberId") String memberId) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //Does user exist ?
        userService.findById(memberId);
        
        //Does current user have sufficient credentials on this application ?
        // APIPortal: user rights on application ?

        membershipService.deleteMember(MembershipReferenceType.APPLICATION, applicationId, memberId);
        return Response.noContent().build();
    }
    
    @PUT
    @Path("/{memberId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateApplicationMemberByApplicationIdAndMemberId(@PathParam("applicationId") String applicationId, @PathParam("memberId") String memberId, @Valid MemberInput memberInput) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //Does user exist ?
        userService.findById(memberId);
        
        if(memberInput.getUser() != null && !memberId.equals(memberInput.getUser())) {
            throw new BadRequestException("'memberInput.user' should the same as 'memberId'");
        }
        
        //There can be only one
        if (RoleEnum.PRIMARY_OWNER == memberInput.getRole()) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }
        
        //Does current user have sufficient credentials on this application ?
        // APIPortal: user rights on application ?
        
        //APIPortal: what is a user 'reference' ?
        MemberEntity membership = membershipService.addOrUpdateMember(
                new MembershipService.MembershipReference(MembershipReferenceType.APPLICATION, applicationId),
                new MembershipService.MembershipUser(memberId, null),
                new MembershipService.MembershipRole(RoleScope.APPLICATION, memberInput.getRole().getValue()));

        return Response
                .ok(memberMapper.convert(membership))
                .build();
    }

    @POST
    @Path("/_transfer_ownership")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transferMemberOwnership(@PathParam("applicationId") String applicationId, TransferOwnershipInput transferOwnershipInput) {
        //Does application exist ?
        applicationService.findById(applicationId);
        
        //Does user exist ?
        userService.findById(transferOwnershipInput.getNewPrimaryOwner());
        
        //There can be only one
        if (RoleEnum.PRIMARY_OWNER == transferOwnershipInput.getPrimaryOwnerNewrole()) {
            throw new SinglePrimaryOwnerException(RoleScope.APPLICATION);
        }
        
        RoleEntity newPORole = null;

        try {
            newPORole = roleService.findById(RoleScope.APPLICATION, transferOwnershipInput.getPrimaryOwnerNewrole().getValue());
        } catch (RoleNotFoundException re) {
            //APIPortal : it doesn't matter... really ?
        }
        
        membershipService.transferApplicationOwnership(applicationId, 
                new MembershipService.MembershipUser( transferOwnershipInput.getNewPrimaryOwner(), null), newPORole);
        return Response
                .noContent()
                .build();
    }
    
}
