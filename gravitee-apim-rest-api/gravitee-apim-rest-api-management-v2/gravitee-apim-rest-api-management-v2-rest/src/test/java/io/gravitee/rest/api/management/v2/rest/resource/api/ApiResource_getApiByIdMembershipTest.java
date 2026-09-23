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
package io.gravitee.rest.api.management.v2.rest.resource.api;

import static assertions.MAPIAssertions.assertThat;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.rest.api.model.MembershipMemberType.USER;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.management.v2.rest.model.Api;
import io.gravitee.rest.api.management.v2.rest.model.ApiFederated;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiResource_getApiByIdMembershipTest extends AbstractNonAdminApiResourceTest {

    private static final String API_NAME = "my-federated-api";
    private static final String API_ROLE_ID = "api-role-id";
    private static final String API_GROUP = "api-group";
    private static final String API_GROUP_ROLE_ID = "api-group-role-id";
    private static final String OTHER_API = "other-api-id";
    private static final String OTHER_API_ROLE_ID = "other-api-role-id";
    private static final String OTHER_GROUP = "other-group";
    private static final String OTHER_GROUP_ROLE_ID = "other-group-role-id";

    @Autowired
    private ApiAuthorizationService apiAuthorizationService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Test
    void should_return_403_when_user_is_neither_admin_nor_direct_member_nor_group_member() {
        givenAFederatedApiOwnedByAGroup();
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.API)).thenReturn(Set.of());
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.GROUP)).thenReturn(Set.of());

        final Response response = rootTarget(API).request().get();

        assertThat(response).hasStatus(FORBIDDEN_403).asError().hasHttpStatus(FORBIDDEN_403);
    }

    @Test
    void should_return_403_when_user_is_member_of_another_api() {
        givenAFederatedApiOwnedByAGroup();
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.API)).thenReturn(
            Set.of(
                MembershipEntity.builder()
                    .memberId(USER_NAME)
                    .memberType(USER)
                    .referenceId(OTHER_API)
                    .referenceType(MembershipReferenceType.API)
                    .roleId(OTHER_API_ROLE_ID)
                    .build()
            )
        );
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.GROUP)).thenReturn(Set.of());
        var otherApiRole = RoleEntity.builder().id(OTHER_API_ROLE_ID).build();
        when(roleService.findById(OTHER_API_ROLE_ID)).thenReturn(otherApiRole);
        when(apiAuthorizationService.canManageApi(otherApiRole)).thenReturn(true);

        final Response response = rootTarget(API).request().get();

        assertThat(response).hasStatus(FORBIDDEN_403).asError().hasHttpStatus(FORBIDDEN_403);
    }

    @Test
    void should_return_403_when_user_is_member_of_a_group_not_attached_to_the_api() {
        givenAFederatedApiOwnedByAGroup();
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.API)).thenReturn(Set.of());
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.GROUP)).thenReturn(
            Set.of(
                MembershipEntity.builder()
                    .memberId(USER_NAME)
                    .memberType(USER)
                    .referenceId(OTHER_GROUP)
                    .referenceType(MembershipReferenceType.GROUP)
                    .roleId(OTHER_GROUP_ROLE_ID)
                    .build()
            )
        );
        var otherGroupRole = RoleEntity.builder().id(OTHER_GROUP_ROLE_ID).build();
        when(roleService.findById(OTHER_GROUP_ROLE_ID)).thenReturn(otherGroupRole);
        when(apiAuthorizationService.canManageApi(otherGroupRole)).thenReturn(true);

        final Response response = rootTarget(API).request().get();

        assertThat(response).hasStatus(FORBIDDEN_403).asError().hasHttpStatus(FORBIDDEN_403);
    }

    @Test
    void should_return_the_api_when_user_is_a_member_through_the_api_group() {
        givenAFederatedApiOwnedByAGroup();
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.API)).thenReturn(Set.of());
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.GROUP)).thenReturn(
            Set.of(
                MembershipEntity.builder()
                    .memberId(USER_NAME)
                    .memberType(USER)
                    .referenceId(API_GROUP)
                    .referenceType(MembershipReferenceType.GROUP)
                    .roleId(API_GROUP_ROLE_ID)
                    .build()
            )
        );
        var apiGroupRole = RoleEntity.builder().id(API_GROUP_ROLE_ID).build();
        when(roleService.findById(API_GROUP_ROLE_ID)).thenReturn(apiGroupRole);
        when(apiAuthorizationService.canManageApi(apiGroupRole)).thenReturn(true);

        final Response response = rootTarget(API).request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(Api.class)
            .extracting(Api::getApiFederated)
            .extracting(ApiFederated::getId, ApiFederated::getName)
            .containsExactly(API, API_NAME);
    }

    @Test
    void should_return_the_api_when_user_is_a_direct_member() {
        givenAFederatedApiOwnedByAGroup();
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.API)).thenReturn(
            Set.of(
                MembershipEntity.builder()
                    .memberId(USER_NAME)
                    .memberType(USER)
                    .referenceId(API)
                    .referenceType(MembershipReferenceType.API)
                    .roleId(API_ROLE_ID)
                    .build()
            )
        );
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.GROUP)).thenReturn(Set.of());
        var apiRole = RoleEntity.builder().id(API_ROLE_ID).build();
        when(roleService.findById(API_ROLE_ID)).thenReturn(apiRole);
        when(apiAuthorizationService.canManageApi(apiRole)).thenReturn(true);

        final Response response = rootTarget(API).request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(Api.class)
            .extracting(Api::getApiFederated)
            .extracting(ApiFederated::getId, ApiFederated::getName)
            .containsExactly(API, "my-federated-api");
    }

    private void givenAFederatedApiOwnedByAGroup() {
        var federatedApi = FederatedApiEntity.builder()
            .id(API)
            .name(API_NAME)
            .groups(Set.of(API_GROUP))
            .originContext(new OriginContext.Integration("integration-id"))
            .updatedAt(new Date())
            .build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, true, true, true)).thenReturn(federatedApi);
    }
}
