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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.portal.rest.model.MembersResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.Member;
import io.gravitee.rest.api.portal.rest.model.MemberInput;
import io.gravitee.rest.api.portal.rest.model.TransferOwnershipInput;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.RoleNotFoundException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMembersResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications/";
    }
    
    private static final String APPLICATION = "my-application";
    private static final String UNKNOWN_APPLICATION = "unknown-application";
    private static final String MEMBER_1 = "my-member";
    private static final String MEMBER_2 = "my-member-2";
    private static final String UNKNOWN_MEMBER = "unknown-member";

    @Before
    public void init() {
        resetAllMocks();
        
        MemberEntity memberEntity1 = new MemberEntity();
        memberEntity1.setId(MEMBER_1);

        MemberEntity memberEntity2 = new MemberEntity();
        memberEntity2.setId(MEMBER_2);
        
        doReturn(new HashSet<>(Arrays.asList(memberEntity1, memberEntity2))).when(membershipService).getMembers(MembershipReferenceType.APPLICATION, APPLICATION, RoleScope.APPLICATION);
        doReturn(memberEntity1).when(membershipService).getMember(MembershipReferenceType.APPLICATION, APPLICATION, MEMBER_1, RoleScope.APPLICATION);
        doReturn(null).when(membershipService).getMember(MembershipReferenceType.APPLICATION, APPLICATION, MEMBER_2, RoleScope.APPLICATION);
        doReturn(new Member().id(MEMBER_1)).when(memberMapper).convert(memberEntity1);
        doReturn(new Member().id(MEMBER_2)).when(memberMapper).convert(memberEntity2);

        doThrow(ApplicationNotFoundException.class).when(applicationService).findById(UNKNOWN_APPLICATION);
        doThrow(UserNotFoundException.class).when(userService).findById(UNKNOWN_MEMBER);

    }
    
    @Test
    public void shouldGetMembers() {
        final Response response = target(APPLICATION).path("members").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        MembersResponse membersResponse = response.readEntity(MembersResponse.class);
        assertEquals(2, membersResponse.getData().size());
        assertEquals(MEMBER_1, membersResponse.getData().get(0).getId());
        assertEquals(MEMBER_2, membersResponse.getData().get(1).getId());
        
        Links links = membersResponse.getLinks();
        assertNotNull(links);
    }
    
    @Test
    public void shouldGetMembersWithPaginatedLink() {
        final Response response = target(APPLICATION).path("members").queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        MembersResponse membersResponse = response.readEntity(MembersResponse.class);
        assertEquals(1, membersResponse.getData().size());
        assertEquals(MEMBER_2, membersResponse.getData().get(0).getId());
    
        Links links = membersResponse.getLinks();
        assertNotNull(links);
        
    }
    
    @Test
    public void shouldNotGetMember() {
        final Response response = target(APPLICATION).path("members").queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        
        Error error = errors.get(0);
        assertEquals("400", error.getCode());
        assertEquals("javax.ws.rs.BadRequestException", error.getTitle());
        assertEquals("page is not valid", error.getDetail());
    }
    
    @Test
    public void shouldGetNoMemberAndNoLink() {

        doReturn(new HashSet<>()).when(membershipService).getMembers(any(), any(), any());
        
        //Test with default limit
        final Response response = target(APPLICATION).path("members").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        MembersResponse membersResponse = response.readEntity(MembersResponse.class);
        assertEquals(0, membersResponse.getData().size());
        
        Links links = membersResponse.getLinks();
        assertNull(links);
        
        //Test with small limit
        final Response anotherResponse = target(APPLICATION).path("members").queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());
        
        membersResponse = anotherResponse.readEntity(MembersResponse.class);
        assertEquals(0, membersResponse.getData().size());
        
        links = membersResponse.getLinks();
        assertNull(links);

    }
    
    @Test
    public void shouldGetMember() {
        final Response response = target(APPLICATION).path("members").path(MEMBER_1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Member responseMember = response.readEntity(Member.class);
        assertNotNull(responseMember);
        assertEquals(MEMBER_1, responseMember.getId());
    }
    
    @Test
    public void shouldDeleteMember() {
        final Response response = target(APPLICATION).path("members").path(MEMBER_1).request().delete();
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        
        Mockito.verify(membershipService).deleteMember(MembershipReferenceType.APPLICATION, APPLICATION, MEMBER_1);
    }
    
    @Test
    public void shouldCreateMember() {
        MemberInput memberInput = new MemberInput().role("USER").user(MEMBER_1);
        final Response response = target(APPLICATION).path("members").request().post(Entity.json(memberInput));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        
        ArgumentCaptor<MembershipService.MembershipReference> memberShipRefCaptor = ArgumentCaptor.forClass(MembershipService.MembershipReference.class);
        ArgumentCaptor<MembershipService.MembershipRole> memberShipRoleCaptor = ArgumentCaptor.forClass(MembershipService.MembershipRole.class);
        ArgumentCaptor<MembershipService.MembershipUser> memberShipUserCaptor = ArgumentCaptor.forClass(MembershipService.MembershipUser.class);

        Mockito.verify(membershipService).addOrUpdateMember(memberShipRefCaptor.capture(), memberShipUserCaptor.capture(), memberShipRoleCaptor.capture());
        assertEquals(APPLICATION, memberShipRefCaptor.getValue().getId());
        assertEquals("USER", memberShipRoleCaptor.getValue().getName());
        assertEquals(MEMBER_1, memberShipUserCaptor.getValue().getId());
    }
    
    @Test
    public void shouldUpdateMember() {
        MemberInput memberInput = new MemberInput().role("USER");
        final Response response = target(APPLICATION).path("members").path(MEMBER_2).request().put(Entity.json(memberInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        ArgumentCaptor<MembershipService.MembershipReference> memberShipRefCaptor = ArgumentCaptor.forClass(MembershipService.MembershipReference.class);
        ArgumentCaptor<MembershipService.MembershipRole> memberShipRoleCaptor = ArgumentCaptor.forClass(MembershipService.MembershipRole.class);
        ArgumentCaptor<MembershipService.MembershipUser> memberShipUserCaptor = ArgumentCaptor.forClass(MembershipService.MembershipUser.class);

        Mockito.verify(membershipService).addOrUpdateMember(memberShipRefCaptor.capture(), memberShipUserCaptor.capture(), memberShipRoleCaptor.capture());
        assertEquals(APPLICATION, memberShipRefCaptor.getValue().getId());
        assertEquals("USER", memberShipRoleCaptor.getValue().getName());
        assertEquals(MEMBER_2, memberShipUserCaptor.getValue().getId());
    }
    
    @Test
    public void shouldTransferOwnerShip() {
        RoleEntity mockRoleEntity = new RoleEntity();
        doReturn(mockRoleEntity).when(roleService).findById(any(), any());
        
        TransferOwnershipInput input = new TransferOwnershipInput().newPrimaryOwner(MEMBER_1).primaryOwnerNewrole("OWNER");
        final Response response = target(APPLICATION).path("members").path("_transfer_ownership").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        
        ArgumentCaptor<String> applicationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RoleEntity> roleCaptor = ArgumentCaptor.forClass(RoleEntity.class);
        ArgumentCaptor<MembershipService.MembershipUser> memberShipUserCaptor = ArgumentCaptor.forClass(MembershipService.MembershipUser.class);

        Mockito.verify(membershipService).transferApplicationOwnership(applicationCaptor.capture(), memberShipUserCaptor.capture(), roleCaptor.capture());
        assertEquals(APPLICATION, applicationCaptor.getValue());
        assertEquals(mockRoleEntity, roleCaptor.getValue());
        assertEquals(MEMBER_1, memberShipUserCaptor.getValue().getId());
    }
    
    @Test
    public void shouldTransferOwnerShipWithWrongRole() {
        doThrow(new RoleNotFoundException(RoleScope.APPLICATION, "OWNER")).when(roleService).findById(any(), any());
        
        TransferOwnershipInput input = new TransferOwnershipInput().newPrimaryOwner(MEMBER_1).primaryOwnerNewrole("OWNER");
        final Response response = target(APPLICATION).path("members").path("_transfer_ownership").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        
        ArgumentCaptor<String> applicationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RoleEntity> roleCaptor = ArgumentCaptor.forClass(RoleEntity.class);
        ArgumentCaptor<MembershipService.MembershipUser> memberShipUserCaptor = ArgumentCaptor.forClass(MembershipService.MembershipUser.class);

        Mockito.verify(membershipService).transferApplicationOwnership(applicationCaptor.capture(), memberShipUserCaptor.capture(), roleCaptor.capture());
        assertEquals(APPLICATION, applicationCaptor.getValue());
        assertEquals(null, roleCaptor.getValue());
        assertEquals(MEMBER_1, memberShipUserCaptor.getValue().getId());
    }
    
    //404 GET /members
    @Test
    public void shouldHaveNotFoundWhileGettingMembers() {
        final Response response = target(UNKNOWN_APPLICATION).path("members").request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    //404 POST /members
    @Test
    public void shouldHaveNotFoundWhileCreatingNewMemberUnknonwnApplication() {
        MemberInput memberInput = new MemberInput().role("USER").user(MEMBER_1);
        final Response response = target(UNKNOWN_APPLICATION).path("members").request().post(Entity.json(memberInput));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    @Test
    public void shouldHaveNotFoundWhileCreatingNewMemberUnknownMember() {
        MemberInput memberInput = new MemberInput().role("USER").user(UNKNOWN_MEMBER);
        final Response response = target(APPLICATION).path("members").request().post(Entity.json(memberInput));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
  
    //400 POST /members
    @Test
    public void shouldHaveBadRequestWhileCreatingNewMemberAsPrimaryOwner() {
        MemberInput memberInput = new MemberInput().role("PRIMARY_OWNER").user(MEMBER_1);
        final Response response = target(APPLICATION).path("members").request().post(Entity.json(memberInput));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        
        Error error = errors.get(0);
        assertEquals("An APPLICATION must always have only one PRIMARY_OWNER !", error.getDetail());
    }
    
    //404 PUT /members/{memberId}
    @Test
    public void shouldHaveNotFoundWhileUpdatingNewMemberUnknonwnApplication() {
        MemberInput memberInput = new MemberInput().role("USER").user(MEMBER_1);
        final Response response = target(UNKNOWN_APPLICATION).path("members").path(MEMBER_1).request().put(Entity.json(memberInput));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    @Test
    public void shouldHaveNotFoundWhileUpdatingNewMemberUnknownMember() {
        MemberInput memberInput = new MemberInput().role("USER").user(UNKNOWN_MEMBER);
        final Response response = target(APPLICATION).path("members").path(UNKNOWN_MEMBER).request().put(Entity.json(memberInput));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    //400 PUT /members/{memberId}
    @Test
    public void shouldHaveBadRequestWhileUpdatingWrongMember() {
        MemberInput memberInput = new MemberInput().role("USER").user(MEMBER_1);
        final Response response = target(APPLICATION).path("members").path(MEMBER_2).request().put(Entity.json(memberInput));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
    @Test
    public void shouldHaveBadRequestWhileUpdatingMemberToPrimaryOwner() {
        MemberInput memberInput = new MemberInput().role("PRIMARY_OWNER").user(MEMBER_1);
        final Response response = target(APPLICATION).path("members").path(MEMBER_1).request().put(Entity.json(memberInput));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        
        Error error = errors.get(0);
        assertEquals("An APPLICATION must always have only one PRIMARY_OWNER !", error.getDetail());
    }
    
    //404 DELETE /members/{memberId}
    @Test
    public void shouldHaveNotFoundWhileDeletingMemberUnknonwnApplication() {
        final Response response = target(UNKNOWN_APPLICATION).path("members").path(MEMBER_1).request().delete();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    @Test
    public void shouldHaveNotFoundWhileDeletingMemberUnknownMember() {
        final Response response = target(APPLICATION).path("members").path(UNKNOWN_MEMBER).request().delete();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    //404 GET /members/{memberId}
    @Test
    public void shouldHaveNotFoundWhileGettingMemberUnknownApplication() {
        final Response response = target(UNKNOWN_APPLICATION).path("members").path(MEMBER_1).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    @Test
    public void shouldHaveNotFoundWhileGettingMemberUnknownMember() {
        final Response response = target(APPLICATION).path("members").path(UNKNOWN_MEMBER).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    @Test
    public void shouldHaveNotFoundWhileGettingMemberWithExistingUserAndNullMembership() {
        final Response response = target(APPLICATION).path("members").path(MEMBER_2).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    
    //404 POST /members/_transfer_ownership
    @Test
    public void shouldHaveNotFoundWhileTransferingOwnerShipUnknownApplication() {
        TransferOwnershipInput input = new TransferOwnershipInput().newPrimaryOwner(MEMBER_1);
        final Response response = target(UNKNOWN_APPLICATION).path("members").path("_transfer_ownership").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    @Test
    public void shouldHaveNotFoundWhileTransferingOwnerShipUnknownMember() {
        TransferOwnershipInput input = new TransferOwnershipInput().newPrimaryOwner(UNKNOWN_MEMBER);
        final Response response = target(APPLICATION).path("members").path("_transfer_ownership").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
    //400 POST /members/_transfer_ownership
    @Test
    public void shouldHaveBadRequestWhileTransferingOwnerShipToPrimaryOwner() {
        TransferOwnershipInput input = new TransferOwnershipInput().newPrimaryOwner(MEMBER_1).primaryOwnerNewrole("PRIMARY_OWNER");
        final Response response = target(APPLICATION).path("members").path("_transfer_ownership").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        
        Error error = errors.get(0);
        assertEquals("An APPLICATION must always have only one PRIMARY_OWNER !", error.getDetail());
    }
}
