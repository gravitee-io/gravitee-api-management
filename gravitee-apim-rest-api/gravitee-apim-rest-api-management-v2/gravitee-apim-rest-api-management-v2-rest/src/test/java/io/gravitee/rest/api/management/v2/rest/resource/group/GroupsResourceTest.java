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
package io.gravitee.rest.api.management.v2.rest.resource.group;

import static assertions.MAPIAssertions.assertThat;
import static fixtures.MemberModelFixtures.aGroupMember;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.mapper.GroupMapper;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.model.GroupsResponse;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.GroupEventRuleEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class GroupsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";

    WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/groups";
    }

    @BeforeEach
    public void init() throws TechnicalException {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(membershipService, groupService);
        GraviteeContext.cleanContext();
    }

    @Nested
    class ListGroups {

        @Test
        public void should_control_permissions() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_GROUP),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            final Response response = target.queryParam("perPage", 10).queryParam("page", 1).request().get();
            assertThat(response).hasStatus(FORBIDDEN_403);
        }

        @Test
        public void should_return_empty_list_if_no_results() {
            when(groupService.findAll(GraviteeContext.getExecutionContext())).thenReturn(List.of());

            var paginatedTarget = target.queryParam("perPage", 10).queryParam("page", 1);
            final Response response = paginatedTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(GroupsResponse.class)
                .isEqualTo(
                    GroupsResponse
                        .builder()
                        .data(List.of())
                        .pagination(Pagination.builder().build())
                        .links(Links.builder().self(paginatedTarget.getUri().toString()).build())
                        .build()
                );
        }

        @Test
        public void should_return_groups() {
            GroupEntity group1 = GroupEntity
                .builder()
                .id("group-1")
                .name("group-1")
                .eventRules(List.of(GroupEventRuleEntity.builder().event("API_CREATE").build()))
                .roles(ofEntries(entry(RoleScope.API, "READ_ONLY"), entry(RoleScope.APPLICATION, "USER")))
                .apiPrimaryOwner("api-owner-id")
                .createdAt(Date.from(Instant.parse("2020-01-01T00:00:00.00Z")))
                .updatedAt(Date.from(Instant.parse("2020-01-01T00:00:00.00Z")))
                .disableMembershipNotifications(true)
                .emailInvitation(true)
                .primaryOwner(true)
                .systemInvitation(true)
                .lockApiRole(true)
                .lockApplicationRole(true)
                .manageable(true)
                .maxInvitation(100)
                .build();

            GroupEntity group2 = GroupEntity
                .builder()
                .id("group-2")
                .name("group-2")
                .eventRules(List.of())
                .roles(Map.of())
                .apiPrimaryOwner(null)
                .createdAt(Date.from(Instant.parse("2020-01-02T00:00:00.00Z")))
                .updatedAt(Date.from(Instant.parse("2020-01-02T00:00:00.00Z")))
                .disableMembershipNotifications(false)
                .emailInvitation(false)
                .primaryOwner(false)
                .systemInvitation(false)
                .lockApiRole(false)
                .lockApplicationRole(false)
                .manageable(false)
                .maxInvitation(null)
                .build();

            when(groupService.findAll(GraviteeContext.getExecutionContext())).thenReturn(List.of(group1, group2));

            target = target.queryParam("perPage", 10).queryParam("page", 1);
            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(GroupsResponse.class)
                .isEqualTo(
                    GroupsResponse
                        .builder()
                        .data(Stream.of(group1, group2).map(GroupMapper.INSTANCE::map).toList())
                        .pagination(Pagination.builder().page(1).perPage(10).pageCount(1).pageItemsCount(2).totalCount(2L).build())
                        .links(Links.builder().self(target.getUri().toString()).build())
                        .build()
                );
        }
    }

    @Nested
    class ListGroupMembers {

        private static final String GROUP_ID = "my-group";
        private static final String GROUP_NAME = "group-name";

        @BeforeEach
        void setUp() {
            target = rootTarget("/" + GROUP_ID + "/members");

            when(groupService.findById(any(), eq(GROUP_ID))).thenReturn(GroupEntity.builder().id(GROUP_ID).name(GROUP_NAME).build());
        }

        @Test
        public void should_get_error_if_user_does_not_have_correct_permissions() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.GROUP_MEMBER,
                    GROUP_ID,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_GROUP,
                    ENVIRONMENT,
                    RolePermissionAction.READ
                )
            )
                .thenReturn(false);

            final Response response = target.request().get();

            assertThat(response).hasStatus(FORBIDDEN_403);
        }

        @Test
        public void should_get_error_when_group_not_found() {
            when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID)).thenThrow(new GroupNotFoundException(GROUP_ID));

            final Response response = target.request().get();

            assertThat(response).hasStatus(NOT_FOUND_404);
        }

        @Test
        public void should_get_error_when_technical_management_exception_thrown() {
            when(groupService.findById(GraviteeContext.getExecutionContext(), GROUP_ID))
                .thenThrow(new TechnicalManagementException("Oops"));

            final Response response = target.request().get();

            assertThat(response).hasStatus(INTERNAL_SERVER_ERROR_500);
        }

        @Test
        public void should_return_empty_page_when_no_results() {
            when(membershipService.getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.GROUP, GROUP_ID))
                .thenReturn(Set.of());

            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(MembersResponse.class)
                .isEqualTo(
                    MembersResponse
                        .builder()
                        .pagination(Pagination.builder().build())
                        .data(List.of())
                        .metadata(Map.of("groupName", GROUP_NAME))
                        .links(Links.builder().self(target.getUri().toString()).build())
                        .build()
                );
        }

        @Test
        public void should_return_results() {
            MemberEntity member1 = aGroupMember(GROUP_ID).withId("member-1");
            MemberEntity member2 = aGroupMember(GROUP_ID).withId("member-2");

            when(membershipService.getMembersByReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.GROUP, GROUP_ID))
                .thenReturn(Set.of(member1, member2));

            final Response response = target.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(MembersResponse.class)
                .isEqualTo(
                    MembersResponse
                        .builder()
                        .pagination(Pagination.builder().page(1).pageCount(1).perPage(10).pageItemsCount(2).totalCount(2L).build())
                        .data(Stream.of(member1, member2).map(MemberMapper.INSTANCE::map).toList())
                        .metadata(Map.of("groupName", GROUP_NAME))
                        .links(Links.builder().self(target.getUri().toString()).build())
                        .build()
                );
        }
    }
}
