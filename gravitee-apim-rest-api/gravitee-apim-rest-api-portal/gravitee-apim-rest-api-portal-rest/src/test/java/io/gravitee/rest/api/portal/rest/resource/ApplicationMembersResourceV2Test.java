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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import inmemory.MemberQueryServiceInMemory;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.MemberV2;
import io.gravitee.rest.api.portal.rest.model.MembersV2Response;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApplicationMembersResourceV2Test extends AbstractResourceTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String OTHER_APPLICATION_ID = "other-application-id";
    private static final String EMPTY_APPLICATION_ID = "empty-application-id";

    @Autowired
    private MemberQueryServiceInMemory memberQueryService;

    @Override
    protected String contextPath() {
        return "applications/";
    }

    @BeforeEach
    void init() {
        resetAllMocks();
        memberQueryService.reset();

        doReturn(new ApplicationEntity()).when(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);
        doReturn(new ApplicationEntity()).when(applicationService).findById(GraviteeContext.getExecutionContext(), OTHER_APPLICATION_ID);
        doReturn(new ApplicationEntity()).when(applicationService).findById(GraviteeContext.getExecutionContext(), EMPTY_APPLICATION_ID);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);

        memberQueryService.initWith(
            List.of(
                aMember("member-2", "John Doe", "john@gravitee.io", APPLICATION_ID, "USER"),
                aMember("member-1", "Jane Doe", "jane@gravitee.io", APPLICATION_ID, "OWNER"),
                aMember("member-3", "Alice Smith", "alice@example.org", OTHER_APPLICATION_ID, "USER")
            )
        );
    }

    @AfterEach
    void cleanup() {
        memberQueryService.reset();
    }

    @Test
    void should_get_application_members_v2() {
        final var response = target(APPLICATION_ID).path("membersV2").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final var membersResponse = response.readEntity(MembersV2Response.class);
        assertNotNull(membersResponse);
        assertEquals(2, membersResponse.getData().size());
        assertEquals("member-1", membersResponse.getData().get(0).getUser().getId());
        assertEquals("member-2", membersResponse.getData().get(1).getUser().getId());
        assertEquals(MemberV2.StatusEnum.ACTIVE, membersResponse.getData().get(0).getStatus());
        assertEquals(MemberV2.TypeEnum.USER, membersResponse.getData().get(0).getType());

        Links links = membersResponse.getLinks();
        assertNotNull(links);
    }

    @Test
    void should_get_empty_members_list_v2() {
        final var response = target(EMPTY_APPLICATION_ID).path("membersV2").request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final var membersResponse = response.readEntity(MembersV2Response.class);
        assertNotNull(membersResponse);
        assertEquals(0, membersResponse.getData().size());
    }

    @Test
    void should_return_403_when_missing_permission() {
        doReturn(false).when(permissionService).hasPermission(any(), any(), any(), any());

        final var response = target(APPLICATION_ID).path("membersV2").request().get();

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    void should_paginate_members_v2() {
        final var response = target(APPLICATION_ID).path("membersV2").queryParam("page", 2).queryParam("size", 1).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        final var membersResponse = response.readEntity(MembersV2Response.class);
        assertNotNull(membersResponse);
        assertEquals(1, membersResponse.getData().size());
        assertEquals("member-2", membersResponse.getData().get(0).getUser().getId());
    }

    private static Member aMember(String id, String displayName, String email, String applicationId, String role) {
        return Member.builder()
            .id(id)
            .displayName(displayName)
            .email(email)
            .type(MembershipMemberType.USER)
            .referenceType(MembershipReferenceType.APPLICATION)
            .referenceId(applicationId)
            .createdAt(new Date())
            .updatedAt(new Date())
            .roles(List.of(Member.Role.builder().scope(RoleScope.APPLICATION).name(role).build()))
            .build();
    }
}
