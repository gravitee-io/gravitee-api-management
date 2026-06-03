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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.ApplicationCrudServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.invitation.crud_service.InvitationCrudService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.InvitationId;
import io.gravitee.apim.core.invitation.model.SearchApplicationInvitationsCriteria;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.portal.rest.model.ApplicationInvitationsSearchFilters;
import io.gravitee.rest.api.portal.rest.model.ApplicationInvitationsSearchInput;
import io.gravitee.rest.api.portal.rest.model.InvitationCreateInput;
import io.gravitee.rest.api.portal.rest.model.InvitationRecipientInput;
import io.gravitee.rest.api.portal.rest.model.InvitationUpdateInput;
import io.gravitee.rest.api.portal.rest.model.InvitationsResponse;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApplicationInvitationsResourceTest extends AbstractResourceTest {

    private static final String APPLICATION = "my-application";
    private static final String UNKNOWN_APPLICATION = "unknown-application";
    private static final String OTHER_APPLICATION = "other-application";
    private static final String INVITATION_ID_1 = "00000000-0000-0000-0000-000000000001";
    private static final String INVITATION_ID_2 = "00000000-0000-0000-0000-000000000002";
    private static final String ROLE_NAME = "USER";
    private static final String UPDATED_ROLE_NAME = "OWNER";

    @Autowired
    private InvitationQueryService invitationQueryService;

    @Autowired
    private InvitationCrudService invitationCrudService;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudService;

    @Autowired
    private RoleQueryServiceInMemory roleQueryService;

    @Override
    protected String contextPath() {
        return "applications/";
    }

    @BeforeEach
    void init() {
        resetAllMocks();
        reset(invitationQueryService);
        reset(invitationCrudService);
        applicationCrudService.reset();
        roleQueryService.reset();
        applicationCrudService.initWith(List.of(BaseApplicationEntity.builder().id(APPLICATION).environmentId("DEFAULT").build()));
        roleQueryService.initWith(List.of(applicationRole(ROLE_NAME), applicationRole(UPDATED_ROLE_NAME)));
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    void should_create_invitations() {
        when(invitationQueryService.findByReference(any())).thenReturn(List.of());
        when(invitationCrudService.create(any(ApplicationInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = target(APPLICATION)
            .path("invitations")
            .request()
            .post(
                Entity.json(
                    new InvitationCreateInput()
                        .recipients(
                            List.of(
                                new InvitationRecipientInput().email(" Alice@example.com "),
                                new InvitationRecipientInput().email("BOB@example.com")
                            )
                        )
                        .role(ROLE_NAME)
                        .notify(false)
                )
            );

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CREATED_201);
        var invitationsResponse = response.readEntity(InvitationsResponse.class);
        assertThat(invitationsResponse.getData()).hasSize(2);
        assertThat(invitationsResponse.getData())
            .extracting(io.gravitee.rest.api.portal.rest.model.Invitation::getEmail)
            .containsExactly("alice@example.com", "bob@example.com");
        assertThat(invitationsResponse.getData())
            .extracting(io.gravitee.rest.api.portal.rest.model.Invitation::getRole)
            .containsOnly(ROLE_NAME);
        verify(invitationCrudService).create(
            argThat(invitation -> invitation.applicationId().equals(APPLICATION) && invitation.email().equals("alice@example.com"))
        );
        verify(invitationCrudService).create(
            argThat(invitation -> invitation.applicationId().equals(APPLICATION) && invitation.email().equals("bob@example.com"))
        );
    }

    @Test
    void should_return_bad_request_when_create_input_is_invalid() {
        var response = target(APPLICATION)
            .path("invitations")
            .request()
            .post(
                Entity.json(
                    new InvitationCreateInput()
                        .recipients(List.of(new InvitationRecipientInput().email("not-an-email")))
                        .role(ROLE_NAME)
                        .notify(false)
                )
            );

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        verify(invitationCrudService, never()).create(any());
    }

    @Test
    void should_return_not_found_when_creating_invitation_for_unknown_application() {
        var response = target(UNKNOWN_APPLICATION).path("invitations").request().post(Entity.json(createInput(false)));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(invitationCrudService, never()).create(any());
    }

    @Test
    void should_return_conflict_when_pending_invitation_already_exists() {
        when(invitationQueryService.findByReference(any())).thenReturn(List.of(anInvitation(INVITATION_ID_1, "alice@example.com")));

        var response = target(APPLICATION).path("invitations").request().post(Entity.json(createInput(false)));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.CONFLICT_409);
        verify(invitationCrudService, never()).create(any());
    }

    @Test
    void should_return_internal_server_error_when_notify_is_not_supported_yet() {
        var response = target(APPLICATION).path("invitations").request().post(Entity.json(createInput(true)));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        verify(invitationCrudService, never()).create(any());
    }

    @Test
    void should_search_invitations() {
        when(
            invitationQueryService.findByApplicationId(
                eq(APPLICATION),
                eq(new SearchApplicationInvitationsCriteria(Optional.empty())),
                eq(new PageableImpl(1, 10))
            )
        ).thenReturn(
            new Page<>(
                List.of(anInvitation(INVITATION_ID_1, "alice@example.com"), anInvitation(INVITATION_ID_2, "bob@example.com")),
                1,
                2,
                2
            )
        );

        var response = target(APPLICATION)
            .path("invitations")
            .path("_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(new ApplicationInvitationsSearchInput()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var invitationsResponse = response.readEntity(InvitationsResponse.class);
        assertThat(invitationsResponse.getData()).hasSize(2);
        assertThat(invitationsResponse.getData())
            .extracting(io.gravitee.rest.api.portal.rest.model.Invitation::getEmail)
            .containsExactly("alice@example.com", "bob@example.com");
        assertThat(invitationsResponse.getMetadata().get("paginateMetaData")).containsEntry("totalElements", 2);
        assertThat(invitationsResponse.getMetadata().get("pagination")).containsEntry("total", 2);
        verify(invitationQueryService).findByApplicationId(
            APPLICATION,
            new SearchApplicationInvitationsCriteria(Optional.empty()),
            new PageableImpl(1, 10)
        );
    }

    @Test
    void should_search_invitations_by_email() {
        when(
            invitationQueryService.findByApplicationId(
                eq(APPLICATION),
                eq(new SearchApplicationInvitationsCriteria(Optional.of("bob"))),
                eq(new PageableImpl(1, 10))
            )
        ).thenReturn(new Page<>(List.of(anInvitation(INVITATION_ID_2, "bob@example.com")), 1, 1, 1));

        var input = new ApplicationInvitationsSearchInput().filters(new ApplicationInvitationsSearchFilters().email("bob"));
        var response = target(APPLICATION)
            .path("invitations")
            .path("_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(input));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var invitationsResponse = response.readEntity(InvitationsResponse.class);
        assertThat(invitationsResponse.getData()).hasSize(1);
        assertThat(invitationsResponse.getData().get(0).getEmail()).isEqualTo("bob@example.com");
        assertThat(invitationsResponse.getMetadata().get("paginateMetaData")).containsEntry("totalElements", 1);
    }

    @Test
    void should_return_not_found_when_application_does_not_exist() {
        var response = target(UNKNOWN_APPLICATION)
            .path("invitations")
            .path("_search")
            .queryParam("page", 1)
            .queryParam("size", 10)
            .request()
            .post(Entity.json(new ApplicationInvitationsSearchInput()));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(invitationQueryService, never()).findByApplicationId(anyString(), any(), any());
    }

    @Test
    void should_delete_invitation() {
        var invitationId = InvitationId.of(INVITATION_ID_1);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(
            Optional.of(anInvitation(INVITATION_ID_1, "alice@example.com"))
        );

        var response = target(APPLICATION).path("invitations").path(INVITATION_ID_1).request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
        verify(invitationCrudService).delete(invitationId);
    }

    @Test
    void should_return_not_found_when_deleting_invitation_for_unknown_application() {
        var response = target(UNKNOWN_APPLICATION).path("invitations").path(INVITATION_ID_1).request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(invitationCrudService, never()).findApplicationInvitationById(any());
        verify(invitationCrudService, never()).delete(any());
    }

    @Test
    void should_return_not_found_when_deleting_unknown_invitation() {
        var invitationId = InvitationId.of(INVITATION_ID_1);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(Optional.empty());

        var response = target(APPLICATION).path("invitations").path(INVITATION_ID_1).request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(invitationCrudService, never()).delete(any());
    }

    @Test
    void should_return_not_found_when_deleting_invitation_from_another_application() {
        var invitationId = InvitationId.of(INVITATION_ID_1);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(
            Optional.of(anInvitation(INVITATION_ID_1, OTHER_APPLICATION, "alice@example.com"))
        );

        var response = target(APPLICATION).path("invitations").path(INVITATION_ID_1).request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(invitationCrudService, never()).delete(any());
    }

    @Test
    void should_update_invitation_role() {
        var invitationId = InvitationId.of(INVITATION_ID_1);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(
            Optional.of(anInvitation(INVITATION_ID_1, "alice@example.com"))
        );
        when(invitationCrudService.update(any(ApplicationInvitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = target(APPLICATION)
            .path("invitations")
            .path(INVITATION_ID_1)
            .request()
            .put(Entity.json(new InvitationUpdateInput().role(" " + UPDATED_ROLE_NAME + " ")));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var invitation = response.readEntity(io.gravitee.rest.api.portal.rest.model.Invitation.class);
        assertThat(invitation.getId()).isEqualTo(INVITATION_ID_1);
        assertThat(invitation.getEmail()).isEqualTo("alice@example.com");
        assertThat(invitation.getRole()).isEqualTo(UPDATED_ROLE_NAME);
        verify(invitationCrudService).update(
            argThat(
                updatedInvitation ->
                    updatedInvitation.id().equals(invitationId) &&
                    updatedInvitation.applicationId().equals(APPLICATION) &&
                    updatedInvitation.email().equals("alice@example.com") &&
                    updatedInvitation.roleName().equals(UPDATED_ROLE_NAME)
            )
        );
    }

    @Test
    void should_return_not_found_when_updating_invitation_for_unknown_application() {
        var response = target(UNKNOWN_APPLICATION)
            .path("invitations")
            .path(INVITATION_ID_1)
            .request()
            .put(Entity.json(new InvitationUpdateInput().role(UPDATED_ROLE_NAME)));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(invitationCrudService, never()).findApplicationInvitationById(any());
        verify(invitationCrudService, never()).update(any());
    }

    @Test
    void should_return_not_found_when_updating_unknown_invitation() {
        var invitationId = InvitationId.of(INVITATION_ID_1);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(Optional.empty());

        var response = target(APPLICATION)
            .path("invitations")
            .path(INVITATION_ID_1)
            .request()
            .put(Entity.json(new InvitationUpdateInput().role(UPDATED_ROLE_NAME)));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(invitationCrudService, never()).update(any());
    }

    @Test
    void should_return_not_found_when_updating_invitation_from_another_application() {
        var invitationId = InvitationId.of(INVITATION_ID_1);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(
            Optional.of(anInvitation(INVITATION_ID_1, OTHER_APPLICATION, "alice@example.com"))
        );

        var response = target(APPLICATION)
            .path("invitations")
            .path(INVITATION_ID_1)
            .request()
            .put(Entity.json(new InvitationUpdateInput().role(UPDATED_ROLE_NAME)));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        verify(invitationCrudService, never()).update(any());
    }

    @Test
    void should_return_bad_request_when_updating_invitation_with_blank_role() {
        var invitationId = InvitationId.of(INVITATION_ID_1);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(
            Optional.of(anInvitation(INVITATION_ID_1, "alice@example.com"))
        );

        var response = target(APPLICATION)
            .path("invitations")
            .path(INVITATION_ID_1)
            .request()
            .put(Entity.json(new InvitationUpdateInput().role(" ")));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        verify(invitationCrudService, never()).update(any());
    }

    @Test
    void should_return_bad_request_when_updating_invitation_with_unknown_role() {
        var invitationId = InvitationId.of(INVITATION_ID_1);
        when(invitationCrudService.findApplicationInvitationById(invitationId)).thenReturn(
            Optional.of(anInvitation(INVITATION_ID_1, "alice@example.com"))
        );

        var response = target(APPLICATION)
            .path("invitations")
            .path(INVITATION_ID_1)
            .request()
            .put(Entity.json(new InvitationUpdateInput().role("UNKNOWN")));

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.BAD_REQUEST_400);
        verify(invitationCrudService, never()).update(any());
    }

    private ApplicationInvitation anInvitation(String id, String email) {
        return anInvitation(id, APPLICATION, email);
    }

    private ApplicationInvitation anInvitation(String id, String applicationId, String email) {
        return new ApplicationInvitation(
            InvitationId.of(id),
            applicationId,
            email,
            "USER",
            ZonedDateTime.parse("2026-04-23T09:30:00Z"),
            ZonedDateTime.parse("2026-04-23T09:45:00Z")
        );
    }

    private InvitationCreateInput createInput(boolean notify) {
        return new InvitationCreateInput()
            .recipients(List.of(new InvitationRecipientInput().email("alice@example.com")))
            .role(ROLE_NAME)
            .notify(notify);
    }

    private Role applicationRole(String roleName) {
        return Role.builder()
            .id("application-%s-role".formatted(roleName.toLowerCase()))
            .name(roleName)
            .referenceId("DEFAULT")
            .referenceType(Role.ReferenceType.ORGANIZATION)
            .scope(Role.Scope.APPLICATION)
            .build();
    }
}
