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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.ApplicationCrudServiceInMemory;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.ApplicationInvitationId;
import io.gravitee.apim.core.invitation.model.SearchApplicationInvitationsCriteria;
import io.gravitee.apim.core.invitation.query_service.InvitationQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.portal.rest.model.ApplicationInvitationsSearchFilters;
import io.gravitee.rest.api.portal.rest.model.ApplicationInvitationsSearchInput;
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
    private static final String INVITATION_ID_1 = "00000000-0000-0000-0000-000000000001";
    private static final String INVITATION_ID_2 = "00000000-0000-0000-0000-000000000002";

    @Autowired
    private InvitationQueryService invitationQueryService;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudService;

    @Override
    protected String contextPath() {
        return "applications/";
    }

    @BeforeEach
    void init() {
        resetAllMocks();
        reset(invitationQueryService);
        applicationCrudService.reset();
        applicationCrudService.initWith(List.of(BaseApplicationEntity.builder().id(APPLICATION).environmentId("DEFAULT").build()));
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
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

    private ApplicationInvitation anInvitation(String id, String email) {
        return new ApplicationInvitation(
            ApplicationInvitationId.of(id),
            APPLICATION,
            email,
            "USER",
            ZonedDateTime.parse("2026-04-23T09:30:00Z"),
            ZonedDateTime.parse("2026-04-23T09:45:00Z")
        );
    }
}
