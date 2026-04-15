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
package io.gravitee.rest.api.management.v2.rest.resource.api_product;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.api_product.exception.ApiProductNotFoundException;
import io.gravitee.apim.core.api_product.use_case.TransferApiProductOwnershipUseCase;
import io.gravitee.apim.core.api_product.use_case.VerifyApiProductExistsUseCase;
import io.gravitee.apim.core.api_product.use_case.members.AddApiProductMemberUseCase;
import io.gravitee.apim.core.api_product.use_case.members.DeleteApiProductMemberUseCase;
import io.gravitee.apim.core.api_product.use_case.members.GetApiProductMembersUseCase;
import io.gravitee.apim.core.api_product.use_case.members.UpdateApiProductMemberUseCase;
import io.gravitee.rest.api.management.v2.rest.mapper.MemberMapper;
import io.gravitee.rest.api.management.v2.rest.model.AddMember;
import io.gravitee.rest.api.management.v2.rest.model.ApiProductTransferOwnership;
import io.gravitee.rest.api.management.v2.rest.model.Links;
import io.gravitee.rest.api.management.v2.rest.model.Member;
import io.gravitee.rest.api.management.v2.rest.model.MembersResponse;
import io.gravitee.rest.api.management.v2.rest.model.Pagination;
import io.gravitee.rest.api.management.v2.rest.model.Role;
import io.gravitee.rest.api.management.v2.rest.model.UpdateMember;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.permissions.ApiProductPermission;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ApiProductMembersResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String API_PRODUCT_ID = "c45b8e66-4d2a-47ad-9b8e-664d2a97ad88";

    @Inject
    private VerifyApiProductExistsUseCase verifyApiProductExistsUseCase;

    @Inject
    private GetApiProductMembersUseCase getApiProductMembersUseCase;

    @Inject
    private AddApiProductMemberUseCase addApiProductMemberUseCase;

    @Inject
    private UpdateApiProductMemberUseCase updateApiProductMemberUseCase;

    @Inject
    private DeleteApiProductMemberUseCase deleteApiProductMemberUseCase;

    @Inject
    private TransferApiProductOwnershipUseCase transferApiProductOwnershipUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/api-products/" + API_PRODUCT_ID + "/members";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(
            membershipService,
            verifyApiProductExistsUseCase,
            getApiProductMembersUseCase,
            addApiProductMemberUseCase,
            updateApiProductMemberUseCase,
            deleteApiProductMemberUseCase,
            transferApiProductOwnershipUseCase
        );
    }

    private void givenApiProductExists() {
        doNothing().when(verifyApiProductExistsUseCase).execute(any());
    }

    private void givenApiProductMissing() {
        doThrow(new ApiProductNotFoundException(API_PRODUCT_ID)).when(verifyApiProductExistsUseCase).execute(any());
    }

    @Nested
    class GetApiProductMemberPermissionsTest {

        @BeforeEach
        void setUp() {
            givenApiProductExists();
        }

        /**
         * JerseySpringTest.AuthenticationFilter always returns {@code true} for {@code isUserInRole},
         * so {@code isAdmin()} is {@code true} in all tests — only the admin branch of
         * {@link io.gravitee.rest.api.management.v2.rest.resource.api_product.ApiProductMembersResource#getApiProductMemberPermissions()}
         * can be exercised here. The non-admin branch (which delegates to
         * {@code membershipService.getUserMemberPermissions}) cannot be reached through this framework
         * without replacing the underlying JAX-RS security filter.
         */
        @Test
        void should_return_404_when_api_product_not_found() {
            givenApiProductMissing();

            Response response = rootTarget("permissions").request().get();

            assertThat(response).hasStatus(NOT_FOUND_404);
        }

        @Test
        void should_return_200_with_all_api_product_permissions_for_admin() {
            Response response = rootTarget("permissions").request().get();

            Assertions.assertThat(response.getStatus()).isEqualTo(OK_200);

            @SuppressWarnings("unchecked")
            Map<String, String> permissions = response.readEntity(Map.class);

            Assertions.assertThat(permissions).isNotNull().hasSize(ApiProductPermission.values().length);

            // Each permission key maps to a string serialised from char[] — verify CRUD chars present
            final String expectedRights = new String(
                new char[] {
                    RolePermissionAction.CREATE.getId(),
                    RolePermissionAction.READ.getId(),
                    RolePermissionAction.UPDATE.getId(),
                    RolePermissionAction.DELETE.getId(),
                }
            );

            Arrays.stream(ApiProductPermission.values()).forEach(perm ->
                Assertions.assertThat(permissions)
                    .as("Permission map should contain '%s' with full CRUD rights", perm.getName())
                    .containsEntry(perm.getName(), expectedRights)
            );
        }
    }

    @Nested
    class ListApiProductMembers {

        @BeforeEach
        void setUp() {
            givenApiProductExists();
        }

        @Test
        void should_return_404_when_api_product_not_found() {
            givenApiProductMissing();

            Response response = rootTarget().request().get();

            assertThat(response).hasStatus(NOT_FOUND_404);
        }

        @Test
        void should_return_403_when_user_not_permitted_to_list_members() {
            shouldReturn403(RolePermission.API_PRODUCT_MEMBER, API_PRODUCT_ID, RolePermissionAction.READ, () ->
                rootTarget().request().get()
            );
        }

        @Test
        void should_list_one_member() {
            var member = io.gravitee.apim.core.member.model.Member.builder()
                .id("memberId")
                .displayName("John Doe")
                .roles(List.of(io.gravitee.apim.core.member.model.Member.Role.builder().name("API_PRODUCT_USER").build()))
                .type(MembershipMemberType.USER)
                .build();

            when(getApiProductMembersUseCase.execute(any())).thenReturn(new GetApiProductMembersUseCase.Output(List.of(member)));

            Response response = rootTarget().request().get();

            assertThat(response).hasStatus(OK_200);
            Assertions.assertThat(response.readEntity(MembersResponse.class)).isEqualTo(
                new MembersResponse()
                    .pagination(new Pagination().page(1).pageCount(1).perPage(10).totalCount(1L).pageItemsCount(1))
                    .data(Stream.of(member).map(MemberMapper.INSTANCE::map).toList())
                    .links(new Links().self(rootTarget().getUri().toString()))
            );
        }
    }

    @Nested
    class CreateApiProductMember {

        @BeforeEach
        void setUp() {
            givenApiProductExists();
            when(addApiProductMemberUseCase.execute(any())).thenAnswer(invocation -> {
                var input = invocation.getArgument(0, AddApiProductMemberUseCase.Input.class);
                if ("PRIMARY_OWNER".equals(input.addMember().getRoleName())) {
                    throw new SinglePrimaryOwnerException(RoleScope.API_PRODUCT);
                }
                return new AddApiProductMemberUseCase.Output(
                    MemberEntity.builder()
                        .id(input.addMember().getUserId())
                        .referenceId(input.apiProductId())
                        .displayName("John Doe")
                        .roles(List.of(RoleEntity.builder().name(input.addMember().getRoleName()).build()))
                        .build()
                );
            });
        }

        @Test
        void should_return_404_when_api_product_not_found() {
            givenApiProductMissing();

            Response response = rootTarget().request().post(Entity.json(new AddMember().roleName("API_PRODUCT_USER").userId("userId")));

            assertThat(response).hasStatus(NOT_FOUND_404);
        }

        @Test
        void should_return_403_when_user_not_permitted_to_create_members() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.API_PRODUCT_MEMBER),
                    eq(API_PRODUCT_ID),
                    eq(RolePermissionAction.CREATE)
                )
            ).thenReturn(false);

            Response response = rootTarget().request().post(Entity.json(new AddMember()));

            assertThat(response).hasStatus(FORBIDDEN_403);
        }

        @Test
        void should_return_400_when_role_is_primary_owner() {
            var body = new AddMember().roleName("PRIMARY_OWNER").userId("userId");
            Response response = rootTarget().request().post(Entity.json(body));
            assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasMessage("An API_PRODUCT must always have only one PRIMARY_OWNER !");
        }

        @Test
        void should_create_api_product_member() {
            var body = new AddMember().roleName("API_PRODUCT_USER").userId("userId");
            Response response = rootTarget().request().post(Entity.json(body));

            assertThat(response)
                .hasStatus(CREATED_201)
                .asEntity(Member.class)
                .isEqualTo(new Member().id("userId").displayName("John Doe").roles(List.of(new Role().name("API_PRODUCT_USER"))));

            verify(addApiProductMemberUseCase).execute(
                argThat(
                    input ->
                        "userId".equals(input.addMember().getUserId()) &&
                        "API_PRODUCT_USER".equals(input.addMember().getRoleName()) &&
                        API_PRODUCT_ID.equals(input.apiProductId())
                )
            );
        }
    }

    @Nested
    class UpdateApiProductMember {

        private static final String MEMBER_ID = "memberId";

        @BeforeEach
        void setUp() {
            givenApiProductExists();
            when(updateApiProductMemberUseCase.execute(any())).thenAnswer(invocation -> {
                var input = invocation.getArgument(0, UpdateApiProductMemberUseCase.Input.class);
                if ("PRIMARY_OWNER".equals(input.newRole())) {
                    throw new SinglePrimaryOwnerException(RoleScope.API_PRODUCT);
                }
                return new UpdateApiProductMemberUseCase.Output(
                    io.gravitee.apim.core.member.model.Member.builder()
                        .id(input.memberId())
                        .referenceId(input.apiProductId())
                        .referenceType(MembershipReferenceType.API_PRODUCT)
                        .type(MembershipMemberType.USER)
                        .displayName("John Doe")
                        .roles(
                            List.of(
                                io.gravitee.apim.core.member.model.Member.Role.builder()
                                    .name(input.newRole())
                                    .scope(RoleScope.API_PRODUCT)
                                    .build()
                            )
                        )
                        .build()
                );
            });
        }

        @Test
        void should_return_404_when_api_product_not_found() {
            givenApiProductMissing();

            Response response = rootTarget(MEMBER_ID).request().put(Entity.json(new UpdateMember().roleName("API_PRODUCT_USER")));

            assertThat(response).hasStatus(NOT_FOUND_404);
        }

        @Test
        void should_return_403_when_user_not_permitted_to_edit_members() {
            shouldReturn403(RolePermission.API_PRODUCT_MEMBER, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                rootTarget(MEMBER_ID).request().put(Entity.json(new UpdateMember()))
            );
        }

        @Test
        void should_return_400_when_editing_with_role_primary_owner() {
            var update = new UpdateMember().roleName("PRIMARY_OWNER");
            Response response = rootTarget(MEMBER_ID).request().put(Entity.json(update));

            assertThat(response)
                .hasStatus(BAD_REQUEST_400)
                .asError()
                .hasMessage("An API_PRODUCT must always have only one PRIMARY_OWNER !");
        }

        @Test
        void should_edit_a_membership() {
            var update = new UpdateMember().roleName("API_PRODUCT_USER");
            Response response = rootTarget(MEMBER_ID).request().put(Entity.json(update));

            assertThat(response)
                .hasStatus(OK_200)
                .asEntity(Member.class)
                .isEqualTo(
                    new Member()
                        .id(MEMBER_ID)
                        .displayName("John Doe")
                        .roles(
                            List.of(
                                new Role()
                                    .name("API_PRODUCT_USER")
                                    .scope(io.gravitee.rest.api.management.v2.rest.model.RoleScope.API_PRODUCT)
                            )
                        )
                );
        }
    }

    @Nested
    class DeleteApiProductMember {

        private static final String MEMBER_ID = "memberId";

        @BeforeEach
        void setUp() {
            givenApiProductExists();
        }

        @Test
        void should_return_404_when_api_product_not_found() {
            givenApiProductMissing();

            Response response = rootTarget(MEMBER_ID).request().delete();

            assertThat(response).hasStatus(NOT_FOUND_404);
        }

        @Test
        void should_return_403_when_user_not_permitted_to_delete_members() {
            shouldReturn403(RolePermission.API_PRODUCT_MEMBER, API_PRODUCT_ID, RolePermissionAction.DELETE, () ->
                rootTarget(MEMBER_ID).request().delete()
            );
        }

        @Test
        void should_delete_a_membership() {
            Response response = rootTarget(MEMBER_ID).request().delete();

            assertThat(response).hasStatus(NO_CONTENT_204);
            verify(deleteApiProductMemberUseCase).execute(
                argThat(input -> API_PRODUCT_ID.equals(input.apiProductId()) && MEMBER_ID.equals(input.memberId()))
            );
        }
    }

    @Nested
    class TransferOwnershipTest {

        private WebTarget target;

        @BeforeEach
        void setup() {
            target = rootTarget("_transfer-ownership");
            givenApiProductExists();
        }

        @Test
        void should_return_204_on_successful_transfer() {
            when(transferApiProductOwnershipUseCase.execute(any())).thenReturn(new TransferApiProductOwnershipUseCase.Output());

            Response response = target.request().post(json(new ApiProductTransferOwnership()));

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
        }

        @Test
        void should_return_204_on_successful_group_transfer() {
            when(transferApiProductOwnershipUseCase.execute(any())).thenReturn(new TransferApiProductOwnershipUseCase.Output());

            // Send userType=GROUP via raw map — the generated model gains this field after regeneration
            var body = Map.of("newPrimaryOwnerId", "group-1", "userType", "GROUP", "currentPrimaryOwnerNewRole", "USER");

            Response response = target.request().post(json(body));

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);

            var captor = ArgumentCaptor.forClass(TransferApiProductOwnershipUseCase.Input.class);
            verify(transferApiProductOwnershipUseCase).execute(captor.capture());
            assertThat(captor.getValue().transferOwnership().getMemberType()).isEqualTo(
                io.gravitee.apim.core.member.model.MembershipMemberType.GROUP
            );
        }

        @Test
        void should_return_404_when_api_product_not_found() {
            givenApiProductMissing();

            Response response = target.request().post(json(new ApiProductTransferOwnership()));

            assertThat(response).hasStatus(NOT_FOUND_404);
        }

        @Test
        void should_return_403_if_incorrect_permissions() {
            shouldReturn403(RolePermission.API_PRODUCT_MEMBER, API_PRODUCT_ID, RolePermissionAction.UPDATE, () ->
                target.request().post(json(new ApiProductTransferOwnership()))
            );
        }
    }
}
