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

import inmemory.ApplicationMemberUserQueryServiceInMemory;
import inmemory.MemberQueryServiceInMemory;
import inmemory.MembershipDomainServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.application_member.model.ApplicationMemberSearchUser;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.MemberV2;
import io.gravitee.rest.api.portal.rest.model.MemberV2Input;
import io.gravitee.rest.api.portal.rest.model.MembersV2Response;
import io.gravitee.rest.api.portal.rest.model.SearchUsersV2Response;
import io.gravitee.rest.api.portal.rest.model.TransferOwnershipV2Input;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApplicationMembersResourceV2Test extends AbstractResourceTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String OTHER_APPLICATION_ID = "other-application-id";
    private static final String EMPTY_APPLICATION_ID = "empty-application-id";

    @Autowired
    private MemberQueryServiceInMemory memberQueryService;

    @Autowired
    private RoleQueryServiceInMemory roleQueryService;

    @Autowired
    private ApplicationMemberUserQueryServiceInMemory applicationMemberUserQueryService;

    @Autowired
    private MembershipDomainServiceInMemory membershipDomainService;

    @Override
    protected String contextPath() {
        return "applications/";
    }

    @BeforeEach
    void init() {
        resetAllMocks();
        memberQueryService.reset();
        roleQueryService.reset();
        applicationMemberUserQueryService.reset();
        membershipDomainService.reset();

        doReturn(new ApplicationEntity()).when(applicationService).findById(GraviteeContext.getExecutionContext(), APPLICATION_ID);
        doReturn(new ApplicationEntity()).when(applicationService).findById(GraviteeContext.getExecutionContext(), OTHER_APPLICATION_ID);
        doReturn(new ApplicationEntity()).when(applicationService).findById(GraviteeContext.getExecutionContext(), EMPTY_APPLICATION_ID);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
        when(membershipService.createNewMembership(any(), any(), any(), any(), any(), any())).thenAnswer(invocation ->
            MemberEntity.builder()
                .referenceType(invocation.getArgument(1))
                .referenceId(invocation.getArgument(2))
                .id(invocation.getArgument(3))
                .type(MembershipMemberType.USER)
                .roles(
                    List.of(
                        RoleEntity.builder()
                            .id("role-" + invocation.<String>getArgument(5).toLowerCase())
                            .name(invocation.getArgument(5))
                            .scope(RoleScope.APPLICATION)
                            .build()
                    )
                )
                .build()
        );

        memberQueryService.initWith(
            List.of(
                aMember("member-2", "John Doe", "john@gravitee.io", APPLICATION_ID, "USER"),
                aMember("member-1", "Jane Doe", "jane@gravitee.io", APPLICATION_ID, "OWNER"),
                aMember("member-3", "Alice Smith", "alice@example.org", OTHER_APPLICATION_ID, "USER")
            )
        );
        roleQueryService.initWith(
            List.of(aRole("USER", GraviteeContext.getCurrentOrganization()), aRole("ADMIN", GraviteeContext.getCurrentOrganization()))
        );
        applicationMemberUserQueryService.initWith(
            List.of(
                aSearchUser("member-1", "ref-1", "Jane", "Doe", "Jane Doe", "jane@gravitee.io"),
                aSearchUser("member-4", "ref-4", "Bob", "Smith", "Bob Smith", "bob@gravitee.io"),
                aSearchUser("member-5", "ref-5", "Alice", "Cooper", "Alice Cooper", "alice@gravitee.io")
            )
        );
    }

    @AfterEach
    void cleanup() {
        memberQueryService.reset();
        roleQueryService.reset();
        applicationMemberUserQueryService.reset();
        membershipDomainService.reset();
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

    @Nested
    class UpdateMemberTest {

        @Test
        void should_update_application_member_role_v2() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .path("member-1")
                .request()
                .put(Entity.json(new MemberV2Input().user("member-1").role("ADMIN")));

            assertEquals(HttpStatusCode.OK_200, response.getStatus());
            final var member = response.readEntity(MemberV2.class);
            assertNotNull(member);
            assertEquals("member-1", member.getUser().getId());
            assertEquals("ADMIN", member.getRole());
        }

        @Test
        void should_return_400_when_role_does_not_exist() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .path("member-1")
                .request()
                .put(Entity.json(new MemberV2Input().user("member-1").role("UNKNOWN")));

            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        }

        @Test
        void should_return_403_when_missing_update_permission() {
            doReturn(false).when(permissionService).hasPermission(any(), any(), any(), any());

            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .path("member-1")
                .request()
                .put(Entity.json(new MemberV2Input().user("member-1").role("ADMIN")));

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        }
    }

    @Nested
    class AddMemberTest {

        @Test
        void should_create_application_member_v2() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .request()
                .post(Entity.json(new MemberV2Input().user("member-4").reference("ref-4").role("ADMIN")));

            assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
            final var member = response.readEntity(MemberV2.class);
            assertNotNull(member);
            assertEquals("member-4", member.getUser().getId());
            assertEquals("ADMIN", member.getRole());
        }

        @Test
        void should_create_application_members_v2_from_batch_payload() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .request()
                .post(Entity.json("{\"members\":[{\"userId\":\"member-4\",\"role\":\"USER\"}],\"notify\":false}"));

            assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
            final var members = response.readEntity(MemberV2[].class);
            assertNotNull(members);
            assertEquals(1, members.length);
            assertEquals("member-4", members[0].getUser().getId());
            assertEquals("USER", members[0].getRole());
        }

        @Test
        void should_return_400_when_adding_existing_member() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .request()
                .post(Entity.json(new MemberV2Input().user("member-1").reference("ref-1").role("USER")));

            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        }

        @Test
        void should_return_400_when_adding_primary_owner() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .request()
                .post(Entity.json(new MemberV2Input().user("member-4").reference("ref-4").role("PRIMARY_OWNER")));

            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        }

        @Test
        void should_return_400_when_role_does_not_exist_on_add_member() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .request()
                .post(Entity.json(new MemberV2Input().user("member-4").reference("ref-4").role("UNKNOWN")));

            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        }

        @Test
        void should_return_403_when_missing_create_permission_on_add_member() {
            doReturn(false).when(permissionService).hasPermission(any(), any(), any(), any());

            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .request()
                .post(Entity.json(new MemberV2Input().user("member-4").reference("ref-4").role("ADMIN")));

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        }
    }

    @Nested
    class DeleteMemberTest {

        @Test
        void should_delete_application_member_v2() {
            final var response = target(APPLICATION_ID).path("membersV2").path("member-2").request().delete();

            assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
            assertEquals(2, memberQueryService.storage().size());
        }

        @Test
        void should_return_400_when_deleting_primary_owner() {
            memberQueryService.initWith(
                List.of(aMember("member-primary-owner", "John Primary", "primary@gravitee.io", APPLICATION_ID, "PRIMARY_OWNER"))
            );

            final var response = target(APPLICATION_ID).path("membersV2").path("member-primary-owner").request().delete();

            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        }

        @Test
        void should_return_403_when_missing_delete_permission() {
            doReturn(false).when(permissionService).hasPermission(any(), any(), any(), any());

            final var response = target(APPLICATION_ID).path("membersV2").path("member-2").request().delete();

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        }
    }

    @Nested
    class SearchUsersTest {

        @Test
        void should_search_users_for_application_member_v2() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .path("_search-users")
                .queryParam("q", "smith")
                .request()
                .post(null);

            assertEquals(HttpStatusCode.OK_200, response.getStatus());
            final var searchUsersResponse = response.readEntity(SearchUsersV2Response.class);
            assertNotNull(searchUsersResponse);
            assertEquals(1, searchUsersResponse.getData().size());
            assertEquals("member-4", searchUsersResponse.getData().get(0).getId());
        }

        @Test
        void should_exclude_existing_members_from_search_result() {
            final var response = target(APPLICATION_ID).path("membersV2").path("_search-users").queryParam("q", "").request().post(null);

            assertEquals(HttpStatusCode.OK_200, response.getStatus());
            final var searchUsersResponse = response.readEntity(SearchUsersV2Response.class);
            assertNotNull(searchUsersResponse);
            assertEquals(2, searchUsersResponse.getData().size());
        }

        @Test
        void should_return_403_when_missing_create_permission_on_search_users() {
            doReturn(false).when(permissionService).hasPermission(any(), any(), any(), any());

            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .path("_search-users")
                .queryParam("q", "smith")
                .request()
                .post(null);

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        }
    }

    @Nested
    class TransferOwnershipTest {

        @Test
        void should_transfer_application_ownership_v2() {
            membershipDomainService.initWith(
                List.of(
                    MemberEntity.builder()
                        .id("member-primary-owner")
                        .referenceType(MembershipReferenceType.APPLICATION)
                        .referenceId(APPLICATION_ID)
                        .roles(List.of(RoleEntity.builder().name("PRIMARY_OWNER").scope(RoleScope.APPLICATION).build()))
                        .build(),
                    MemberEntity.builder()
                        .id("member-4")
                        .referenceType(MembershipReferenceType.APPLICATION)
                        .referenceId(APPLICATION_ID)
                        .roles(List.of(RoleEntity.builder().name("USER").scope(RoleScope.APPLICATION).build()))
                        .build()
                )
            );

            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .path("_transfer-ownership")
                .request()
                .post(
                    Entity.json(
                        new TransferOwnershipV2Input()
                            .newPrimaryOwnerId("member-4")
                            .newPrimaryOwnerReference("ref-4")
                            .primaryOwnerNewrole("USER")
                    )
                );

            assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
            assertEquals(
                "USER",
                membershipDomainService
                    .storage()
                    .stream()
                    .filter(member -> "member-primary-owner".equals(member.getId()))
                    .findFirst()
                    .orElseThrow()
                    .getRoles()
                    .get(0)
                    .getName()
            );
            assertEquals(
                "PRIMARY_OWNER",
                membershipDomainService
                    .storage()
                    .stream()
                    .filter(member -> "member-4".equals(member.getId()))
                    .findFirst()
                    .orElseThrow()
                    .getRoles()
                    .get(0)
                    .getName()
            );
        }

        @Test
        void should_return_400_when_previous_owner_new_role_does_not_exist() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .path("_transfer-ownership")
                .request()
                .post(
                    Entity.json(
                        new TransferOwnershipV2Input()
                            .newPrimaryOwnerId("member-4")
                            .newPrimaryOwnerReference("ref-4")
                            .primaryOwnerNewrole("UNKNOWN")
                    )
                );

            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        }

        @Test
        void should_return_400_when_previous_owner_new_role_is_primary_owner() {
            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .path("_transfer-ownership")
                .request()
                .post(
                    Entity.json(
                        new TransferOwnershipV2Input()
                            .newPrimaryOwnerId("member-4")
                            .newPrimaryOwnerReference("ref-4")
                            .primaryOwnerNewrole("PRIMARY_OWNER")
                    )
                );

            assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        }

        @Test
        void should_return_403_when_missing_update_permission_on_transfer_ownership() {
            doReturn(false).when(permissionService).hasPermission(any(), any(), any(), any());

            final var response = target(APPLICATION_ID)
                .path("membersV2")
                .path("_transfer-ownership")
                .request()
                .post(
                    Entity.json(
                        new TransferOwnershipV2Input()
                            .newPrimaryOwnerId("member-4")
                            .newPrimaryOwnerReference("ref-4")
                            .primaryOwnerNewrole("USER")
                    )
                );

            assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
        }
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

    private static Role aRole(String roleName, String organizationId) {
        return Role.builder()
            .id("role-" + roleName.toLowerCase())
            .name(roleName)
            .scope(Role.Scope.APPLICATION)
            .referenceType(Role.ReferenceType.ORGANIZATION)
            .referenceId(organizationId)
            .build();
    }

    private static ApplicationMemberSearchUser aSearchUser(
        String id,
        String reference,
        String firstName,
        String lastName,
        String displayName,
        String email
    ) {
        return new ApplicationMemberSearchUser(id, reference, firstName, lastName, displayName, email);
    }
}
