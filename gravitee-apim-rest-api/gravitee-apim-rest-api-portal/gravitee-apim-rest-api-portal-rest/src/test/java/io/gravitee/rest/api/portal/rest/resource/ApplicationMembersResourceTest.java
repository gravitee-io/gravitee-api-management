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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.UserNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.collections.Sets;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationMembersResourceTest extends AbstractResourceTest {

    private static final String APPLICATION = "my-application";
    private static final String UNKNOWN_APPLICATION = "unknown-application";
    private static final String MEMBER_1 = "my-member";
    private static final String MEMBER_2 = "my-member-2";
    private static final String UNKNOWN_MEMBER = "unknown-member";

    @Override
    protected String contextPath() {
        return "applications/";
    }

    @Before
    public void init() {
        resetAllMocks();

        MemberEntity memberEntity1 = new MemberEntity();
        memberEntity1.setId(MEMBER_1);

        MemberEntity memberEntity2 = new MemberEntity();
        memberEntity2.setId(MEMBER_2);
        doReturn(new Member().id(MEMBER_2)).when(memberMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(memberEntity2), any());
        doReturn(new Member().id(MEMBER_1)).when(memberMapper).convert(eq(GraviteeContext.getExecutionContext()), eq(memberEntity1), any());
        doReturn(Sets.newSet(memberEntity1, memberEntity2))
            .when(membershipService)
            .getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.APPLICATION, APPLICATION);
        doReturn(memberEntity1)
            .when(membershipService)
            .getUserMember(GraviteeContext.getExecutionContext(), MembershipReferenceType.APPLICATION, APPLICATION, MEMBER_1);
        doReturn(null)
            .when(membershipService)
            .getUserMember(GraviteeContext.getExecutionContext(), MembershipReferenceType.APPLICATION, APPLICATION, MEMBER_2);

        doThrow(ApplicationNotFoundException.class)
            .when(applicationService)
            .findById(GraviteeContext.getExecutionContext(), UNKNOWN_APPLICATION);
        doThrow(UserNotFoundException.class).when(userService).findById(GraviteeContext.getExecutionContext(), UNKNOWN_MEMBER);
    }

    @Test
    public void shouldGetMembers() {
        final Response response = target(APPLICATION).path("members").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        MembersResponse membersResponse = response.readEntity(MembersResponse.class);
        assertEquals(2, membersResponse.getData().size());
        assertTrue(
            (MEMBER_1.equals(membersResponse.getData().get(0).getId()) && MEMBER_2.equals(membersResponse.getData().get(1).getId())) ||
            (MEMBER_1.equals(membersResponse.getData().get(1).getId()) && MEMBER_2.equals(membersResponse.getData().get(0).getId()))
        );

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
        assertEquals("errors.pagination.invalid", error.getCode());
        assertEquals("400", error.getStatus());
        assertEquals("Pagination is not valid", error.getMessage());
    }

    @Test
    public void shouldGetNoMemberAndNoLink() {
        doReturn(new HashSet<>()).when(membershipService).getMembersByReference(eq(GraviteeContext.getExecutionContext()), any(), any());

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

        Mockito
            .verify(membershipService)
            .deleteReferenceMember(
                GraviteeContext.getExecutionContext(),
                MembershipReferenceType.APPLICATION,
                APPLICATION,
                MembershipMemberType.USER,
                MEMBER_1
            );
    }

    @Test
    public void shouldCreateMember() {
        MemberInput memberInput = new MemberInput().role("USER").user(MEMBER_1);
        MemberEntity returnedMemberEntity = mock(MemberEntity.class);
        doReturn(MEMBER_1).when(returnedMemberEntity).getId();
        doReturn(returnedMemberEntity)
            .when(membershipService)
            .addRoleToMemberOnReference(eq(GraviteeContext.getExecutionContext()), any(), any(), any());

        final Response response = target(APPLICATION).path("members").request().post(Entity.json(memberInput));
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(
            target(APPLICATION).path("members").path(MEMBER_1).getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );

        ArgumentCaptor<MembershipService.MembershipReference> memberShipRefCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipReference.class
        );
        ArgumentCaptor<MembershipService.MembershipRole> memberShipRoleCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipRole.class
        );
        ArgumentCaptor<MembershipService.MembershipMember> memberShipUserCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipMember.class
        );

        Mockito
            .verify(membershipService)
            .addRoleToMemberOnReference(
                eq(GraviteeContext.getExecutionContext()),
                memberShipRefCaptor.capture(),
                memberShipUserCaptor.capture(),
                memberShipRoleCaptor.capture()
            );
        assertEquals(APPLICATION, memberShipRefCaptor.getValue().getId());
        assertEquals("USER", memberShipRoleCaptor.getValue().getName());
        assertEquals(MEMBER_1, memberShipUserCaptor.getValue().getMemberId());
    }

    @Test
    public void shouldUpdateMember() {
        MemberInput memberInput = new MemberInput().role("USER");
        final Response response = target(APPLICATION).path("members").path(MEMBER_2).request().put(Entity.json(memberInput));
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        ArgumentCaptor<MembershipService.MembershipReference> memberShipRefCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipReference.class
        );
        ArgumentCaptor<MembershipService.MembershipRole> memberShipRoleCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipRole.class
        );
        ArgumentCaptor<MembershipService.MembershipMember> memberShipUserCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipMember.class
        );

        Mockito
            .verify(membershipService)
            .updateRoleToMemberOnReference(
                eq(GraviteeContext.getExecutionContext()),
                memberShipRefCaptor.capture(),
                memberShipUserCaptor.capture(),
                memberShipRoleCaptor.capture()
            );
        assertEquals(APPLICATION, memberShipRefCaptor.getValue().getId());
        assertEquals("USER", memberShipRoleCaptor.getValue().getName());
        assertEquals(MEMBER_2, memberShipUserCaptor.getValue().getMemberId());
    }

    @Test
    public void shouldTransferOwnerShip() {
        RoleEntity mockRoleEntity = new RoleEntity();
        TransferOwnershipInput input = new TransferOwnershipInput().newPrimaryOwnerId(MEMBER_1).primaryOwnerNewrole("OWNER");
        doReturn(Optional.of(mockRoleEntity)).when(roleService).findByScopeAndName(any(), any(), any());
        final Response response = target(APPLICATION).path("members").path("_transfer_ownership").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        ArgumentCaptor<String> applicationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<RoleEntity>> roleCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<MembershipService.MembershipMember> memberShipUserCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipMember.class
        );

        Mockito
            .verify(membershipService)
            .transferApplicationOwnership(
                eq(GraviteeContext.getExecutionContext()),
                applicationCaptor.capture(),
                memberShipUserCaptor.capture(),
                roleCaptor.capture()
            );
        assertEquals(APPLICATION, applicationCaptor.getValue());
        assertEquals(mockRoleEntity, roleCaptor.getValue().get(0));
        assertEquals(MEMBER_1, memberShipUserCaptor.getValue().getMemberId());
    }

    @Test
    public void shouldTransferOwnerShipWithWrongRole() {
        doReturn(Optional.empty()).when(roleService).findByScopeAndName(any(), any(), any());
        TransferOwnershipInput input = new TransferOwnershipInput().newPrimaryOwnerId(MEMBER_1).primaryOwnerNewrole("OWNER");
        final Response response = target(APPLICATION).path("members").path("_transfer_ownership").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        ArgumentCaptor<String> applicationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List<RoleEntity>> roleCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<MembershipService.MembershipMember> memberShipUserCaptor = ArgumentCaptor.forClass(
            MembershipService.MembershipMember.class
        );

        Mockito
            .verify(membershipService)
            .transferApplicationOwnership(
                eq(GraviteeContext.getExecutionContext()),
                applicationCaptor.capture(),
                memberShipUserCaptor.capture(),
                roleCaptor.capture()
            );
        assertEquals(APPLICATION, applicationCaptor.getValue());
        assertNotNull(roleCaptor.getValue());
        assertTrue(roleCaptor.getValue().isEmpty());
        assertEquals(MEMBER_1, memberShipUserCaptor.getValue().getMemberId());
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
        assertEquals("An APPLICATION must always have only one PRIMARY_OWNER !", error.getMessage());
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
        assertEquals("An APPLICATION must always have only one PRIMARY_OWNER !", error.getMessage());
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
        TransferOwnershipInput input = new TransferOwnershipInput().newPrimaryOwnerId(MEMBER_1);
        final Response response = target(UNKNOWN_APPLICATION)
            .path("members")
            .path("_transfer_ownership")
            .request()
            .post(Entity.json(input));
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    //400 POST /members/_transfer_ownership
    @Test
    public void shouldHaveBadRequestWhileTransferingOwnerShipToPrimaryOwner() {
        TransferOwnershipInput input = new TransferOwnershipInput().newPrimaryOwnerId(MEMBER_1).primaryOwnerNewrole("PRIMARY_OWNER");
        final Response response = target(APPLICATION).path("members").path("_transfer_ownership").request().post(Entity.json(input));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());

        Error error = errors.get(0);
        assertEquals("An APPLICATION must always have only one PRIMARY_OWNER !", error.getMessage());
    }
}
