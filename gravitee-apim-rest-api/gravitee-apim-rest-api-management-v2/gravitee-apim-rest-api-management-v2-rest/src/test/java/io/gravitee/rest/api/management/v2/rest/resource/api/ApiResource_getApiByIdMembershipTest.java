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
import static org.mockito.Mockito.reset;
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
import jakarta.ws.rs.container.ContainerRequestFilter;
import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
    private static final String ORGANIZATION_ADMIN_HEADER = "X-Test-Organization-Admin";

    @Autowired
    private ApiAuthorizationService apiAuthorizationService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        super.decorate(resourceConfig);
        resourceConfig.register(
            (ContainerRequestFilter) requestContext -> {
                if (requestContext.getHeaderString(ORGANIZATION_ADMIN_HEADER) != null) {
                    // Leaves the non-admin principal in SecurityContextHolder on this very thread before switching to the admin context.
                    requestContext.getSecurityContext().getUserPrincipal();
                    new AuthenticationFilter().filter(requestContext);
                }
            },
            6
        );
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        reset(membershipService, apiAuthorizationService);
    }

    static Stream<Arguments> nonMemberCases() {
        return Stream.of(
            Arguments.of("neither admin nor direct member nor group member", Set.of(), Set.of()),
            Arguments.of("member of another api", Set.of(apiMembership(OTHER_API, OTHER_API_ROLE_ID)), Set.of()),
            Arguments.of("member of a group not attached to the api", Set.of(), Set.of(groupMembership(OTHER_GROUP, OTHER_GROUP_ROLE_ID)))
        );
    }

    static Stream<Arguments> memberCases() {
        return Stream.of(
            Arguments.of("member through the api group", Set.of(), Set.of(groupMembership(API_GROUP, API_GROUP_ROLE_ID))),
            Arguments.of("direct member", Set.of(apiMembership(API, API_ROLE_ID)), Set.of())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nonMemberCases")
    void should_return_403_when_user_is_not_a_member_of_the_api(
        String userCase,
        Set<MembershipEntity> apiMemberships,
        Set<MembershipEntity> groupMemberships
    ) {
        givenAFederatedApiOwnedByAGroup();
        givenUserMemberships(apiMemberships, groupMemberships);

        var response = rootTarget(API).request().get();

        assertThat(response).hasStatus(FORBIDDEN_403).asError().hasHttpStatus(FORBIDDEN_403);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("memberCases")
    void should_return_the_api_when_user_is_a_member_of_the_api(
        String userCase,
        Set<MembershipEntity> apiMemberships,
        Set<MembershipEntity> groupMemberships
    ) {
        givenAFederatedApiOwnedByAGroup();
        givenUserMemberships(apiMemberships, groupMemberships);

        var response = rootTarget(API).request().get();

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(Api.class)
            .extracting(Api::getApiFederated)
            .extracting(ApiFederated::getId, ApiFederated::getName)
            .containsExactly(API, API_NAME);
    }

    @Test
    void should_return_the_api_to_an_admin_after_a_non_admin_request() {
        givenAFederatedApiOwnedByAGroup();
        givenUserMemberships(Set.of(), Set.of());

        var nonAdminResponse = rootTarget(API).request().get();
        var adminResponse = rootTarget(API).request().header(ORGANIZATION_ADMIN_HEADER, true).get();

        assertThat(nonAdminResponse).hasStatus(FORBIDDEN_403);
        assertThat(adminResponse)
            .hasStatus(OK_200)
            .asEntity(Api.class)
            .extracting(Api::getApiFederated)
            .extracting(ApiFederated::getId, ApiFederated::getName)
            .containsExactly(API, API_NAME);
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

    private void givenUserMemberships(Set<MembershipEntity> apiMemberships, Set<MembershipEntity> groupMemberships) {
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.API)).thenReturn(apiMemberships);
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.GROUP)).thenReturn(
            groupMemberships
        );
        Stream.concat(apiMemberships.stream(), groupMemberships.stream()).forEach(membership -> {
            var role = RoleEntity.builder().id(membership.getRoleId()).build();
            when(roleService.findById(membership.getRoleId())).thenReturn(role);
            when(apiAuthorizationService.canManageApi(role)).thenReturn(true);
        });
    }

    private static MembershipEntity apiMembership(String apiId, String roleId) {
        return membership(apiId, MembershipReferenceType.API, roleId);
    }

    private static MembershipEntity groupMembership(String groupId, String roleId) {
        return membership(groupId, MembershipReferenceType.GROUP, roleId);
    }

    private static MembershipEntity membership(String referenceId, MembershipReferenceType referenceType, String roleId) {
        return MembershipEntity.builder()
            .memberId(USER_NAME)
            .memberType(USER)
            .referenceId(referenceId)
            .referenceType(referenceType)
            .roleId(roleId)
            .build();
    }
}
