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
package io.gravitee.rest.api.management.v2.rest.resource.integration;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.LicenseFixtures;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.node.api.license.LicenseManager;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.model.AddIntegrationMember;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.Role;
import io.gravitee.rest.api.management.v2.rest.model.UpdateIntegrationMember;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class IntegrationMembersResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT = "my-env";
    static final String INTEGRATION_ID = "integration-id";

    WebTarget target;

    @Autowired
    LicenseManager licenseManager;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/integrations/" + INTEGRATION_ID + "/members";
    }

    @BeforeEach
    public void init() {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        roleQueryService.resetSystemRoles(ORGANIZATION);
        enableApiPrimaryOwnerMode(ENVIRONMENT, ApiPrimaryOwnerMode.USER);
        givenExistingUsers(
            List.of(BaseUserEntity.builder().id(USER_NAME).firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
        );

        when(licenseManager.getOrganizationLicenseOrPlatform(ORGANIZATION)).thenReturn(LicenseFixtures.anEnterpriseLicense());
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        reset(membershipService);
        GraviteeContext.cleanContext();
    }

    @Nested
    class Create {

        @BeforeEach
        void setUp() {
            when(membershipService.createNewMembershipForIntegration(any(), any(), any(), any(), any()))
                .thenAnswer(invocation ->
                    MemberEntity
                        .builder()
                        .id(invocation.getArgument(2))
                        .referenceId(invocation.getArgument(1))
                        .displayName("John Doe")
                        .roles(List.of(RoleEntity.builder().name(invocation.getArgument(4)).build()))
                        .build()
                );
        }

        @Test
        public void should_return_403_when_user_not_permitted_to_create_members() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.INTEGRATION_MEMBER),
                    eq(INTEGRATION_ID),
                    eq(RolePermissionAction.CREATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().post(Entity.json(new AddIntegrationMember()));
            assertThat(response).hasStatus(FORBIDDEN_403);
        }

        @Test
        public void should_return_400_when_role_is_primary_owner() {
            var newMembership = AddIntegrationMember.builder().roleName("PRIMARY_OWNER").build();
            final Response response = target.request().post(Entity.json(newMembership));
            assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasMessage("An INTEGRATION must always have only one PRIMARY_OWNER !");
        }

        @Test
        public void should_return_400_when_user_id_and_external_reference_is_empty() {
            var newMembership = AddIntegrationMember.builder().roleName("OWNER").build();
            final Response response = target.request().post(Entity.json(newMembership));
            assertThat(response).hasStatus(BAD_REQUEST_400).asError().hasMessage("Request must specify either userId or externalReference");
        }

        @Test
        public void should_create_an_integration_member() {
            var newMembership = AddIntegrationMember.builder().roleName("OWNER").userId("userId").build();
            final Response response = target.request().post(Entity.json(newMembership));
            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(Member.class)
                .isEqualTo(
                    Member.builder().id("userId").displayName("John Doe").roles(List.of(Role.builder().name("OWNER").build())).build()
                );
        }
    }

    @Nested
    class ListMembers {

        @Test
        public void should_return_403_when_user_not_permitted_to_list_members() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.INTEGRATION_MEMBER),
                    eq(INTEGRATION_ID),
                    any()
                )
            )
                .thenReturn(false);

            final Response response = target.request().get();
            assertThat(response).hasStatus(FORBIDDEN_403);
        }

        @Test
        public void should_list_one_integration_member() {
            var member = MemberEntity
                .builder()
                .id("memberId")
                .displayName("John Doe")
                .roles(List.of(RoleEntity.builder().name("OWNER").build()))
                .type(MembershipMemberType.USER)
                .build();

            when(
                membershipService.getMembersByReference(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(MembershipReferenceType.INTEGRATION),
                    eq(INTEGRATION_ID)
                )
            )
                .thenReturn(Set.of(member));

            final Response response = target.request().get();
            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(MembersResponse.class)
                .isEqualTo(
                    MembersResponse
                        .builder()
                        .pagination(Pagination.builder().page(1).pageCount(1).perPage(10).totalCount(1L).pageItemsCount(1).build())
                        .data(Stream.of(member).map(MemberMapper.INSTANCE::map).toList())
                        .links(Links.builder().self(target.getUri().toString()).build())
                        .build()
                );
        }

        @Test
        public void should_list_multiple_members_with_pagination() {
            var owner = new RoleEntity();
            owner.setName("OWNER");

            var roleOwner = RoleEntity.builder().name("OWNER").build();
            var roleUser = RoleEntity.builder().name("USER").build();

            var member1 = MemberEntity
                .builder()
                .id("member1")
                .displayName("John Doe")
                .roles(List.of(roleOwner))
                .type(MembershipMemberType.USER)
                .build();

            var member2 = MemberEntity
                .builder()
                .id("member2")
                .displayName("Jane Doe")
                .roles(List.of(roleUser))
                .type(MembershipMemberType.USER)
                .build();

            var member3 = MemberEntity
                .builder()
                .id("member3")
                .displayName("Richard Roe")
                .roles(List.of(roleUser))
                .type(MembershipMemberType.USER)
                .build();

            var member4 = MemberEntity
                .builder()
                .id("member4")
                .displayName("Baby Doe")
                .roles(List.of(roleUser))
                .type(MembershipMemberType.USER)
                .build();

            when(
                membershipService.getMembersByReference(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(MembershipReferenceType.INTEGRATION),
                    eq(INTEGRATION_ID)
                )
            )
                .thenReturn(Set.of(member1, member2, member3, member4));

            var paginatedTarget = target
                .queryParam(PaginationParam.PAGE_QUERY_PARAM_NAME, 1)
                .queryParam(PaginationParam.PER_PAGE_QUERY_PARAM_NAME, 2);
            final Response response = paginatedTarget.request().get();

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(MembersResponse.class)
                .isEqualTo(
                    MembersResponse
                        .builder()
                        .pagination(Pagination.builder().page(1).pageCount(2).perPage(2).totalCount(4L).pageItemsCount(2).build())
                        .data(Stream.of(member1, member2).map(MemberMapper.INSTANCE::map).toList())
                        .links(
                            Links
                                .builder()
                                .self(paginatedTarget.getUri().toString())
                                .first(paginatedTarget.getUri().toString())
                                .last(target.queryParam("page", 2).queryParam("perPage", 2).getUri().toString())
                                .next(target.queryParam("page", 2).queryParam("perPage", 2).getUri().toString())
                                .build()
                        )
                        .build()
                );
        }
    }

    @Nested
    class Delete {

        private static final String MEMBER_ID = "memberId";

        @BeforeEach
        void setUp() {
            target = rootTarget(MEMBER_ID);
        }

        @Test
        public void should_return_403_when_user_not_permitted_to_delete_members() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.INTEGRATION_MEMBER),
                    eq(INTEGRATION_ID),
                    eq(RolePermissionAction.DELETE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().delete();
            assertThat(response).hasStatus(FORBIDDEN_403);
        }

        @Test
        public void should_delete_a_membership() {
            final Response response = target.request().delete();
            assertThat(response).hasStatus(NO_CONTENT_204);

            verify(membershipService)
                .deleteMemberForIntegration(eq(GraviteeContext.getExecutionContext()), eq(INTEGRATION_ID), eq(MEMBER_ID));
        }
    }

    @Nested
    class Update {

        private static final String MEMBER_ID = "memberId";

        @BeforeEach
        void setUp() {
            target = rootTarget(MEMBER_ID);

            when(membershipService.updateMembershipForIntegration(any(), any(), any(), any()))
                .thenAnswer(invocation ->
                    MemberEntity
                        .builder()
                        .id(invocation.getArgument(2))
                        .referenceId(invocation.getArgument(1))
                        .displayName("John Doe")
                        .roles(List.of(RoleEntity.builder().name(invocation.getArgument(3)).build()))
                        .build()
                );
        }

        @Test
        public void should_return_403_when_user_not_permitted_to_edit_members() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.INTEGRATION_MEMBER),
                    eq(INTEGRATION_ID),
                    eq(RolePermissionAction.UPDATE)
                )
            )
                .thenReturn(false);

            final Response response = target.request().put(Entity.json(new UpdateIntegrationMember()));
            assertThat(response).hasStatus(FORBIDDEN_403);
        }

        @Test
        public void should_return_400_when_editing_with_role_primary_owner() {
            var newMembership = UpdateIntegrationMember.builder().roleName("PRIMARY_OWNER").build();

            final Response response = target.request().put(Entity.json(newMembership));

            assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasMessage("An INTEGRATION must always have only one PRIMARY_OWNER !");
        }

        @Test
        public void should_edit_a_membership() {
            var newMembership = UpdateIntegrationMember.builder().roleName("OWNER").build();

            final Response response = target.request().put(Entity.json(newMembership));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Member.class)
                .isEqualTo(
                    Member.builder().id(MEMBER_ID).displayName("John Doe").roles(List.of(Role.builder().name("OWNER").build())).build()
                );
        }
    }
}
