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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A member whose role only reads the API (the default USER role) is refused the API's subscriptions, as they are refused
 * the API itself, although their role grants {@code API_SUBSCRIPTION[READ]}. A member who manages the API still reads them.
 */
class ApiSubscriptionsResource_ReadMembershipTest extends AbstractNonAdminApiResourceTest {

    private static final String SUBSCRIPTION = "my-subscription";
    private static final String API_GROUP = "api-group";
    private static final String READ_ONLY_ROLE_ID = "read-only-role-id";
    private static final String MANAGER_ROLE_ID = "manager-role-id";

    @Autowired
    private ApiAuthorizationService apiAuthorizationService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis";
    }

    @BeforeEach
    void resetSharedMocks() {
        // The resource mocks are Spring singletons, shared with the test classes run before this one.
        reset(membershipService, apiAuthorizationService, subscriptionService);
    }

    @AfterEach
    @Override
    public void tearDown() {
        super.tearDown();
        reset(membershipService, apiAuthorizationService, subscriptionService);
    }

    @ParameterizedTest(name = "GET {0}")
    @ValueSource(
        strings = {
            "subscriptions",
            "subscriptions/_export",
            "subscriptions/" + SUBSCRIPTION,
            "subscriptions/" + SUBSCRIPTION + "/api-keys",
            "subscribers",
        }
    )
    void should_refuse_a_member_whose_role_only_reads_the_api(String path) {
        givenAnApiOwnedByAGroup();
        givenADirectMembership(READ_ONLY_ROLE_ID, false);

        var response = rootTarget(API + "/" + path).request().get();

        assertThat(response).hasStatus(FORBIDDEN_403).asError().hasHttpStatus(FORBIDDEN_403);
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void should_list_the_subscriptions_to_a_member_who_manages_the_api_through_its_group() {
        givenAnApiOwnedByAGroup();
        givenADirectMembership(READ_ONLY_ROLE_ID, false);
        givenAGroupMembership(MANAGER_ROLE_ID);
        when(subscriptionService.search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(false), eq(false))).thenReturn(
            new Page<>(List.of(), 0, 0, 0)
        );

        var response = rootTarget(API + "/subscriptions").request().get();

        assertThat(response).hasStatus(OK_200);
    }

    private void givenAnApiOwnedByAGroup() {
        var api = ApiEntity.builder().id(API).groups(Set.of(API_GROUP)).build();
        when(apiSearchServiceV4.findGenericById(GraviteeContext.getExecutionContext(), API, false, false, false)).thenReturn(api);
    }

    private void givenADirectMembership(String roleId, boolean roleManagesApi) {
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.API)).thenReturn(
            Set.of(membership(API, MembershipReferenceType.API, roleId))
        );
        givenRole(roleId, roleManagesApi);
    }

    private void givenAGroupMembership(String roleId) {
        when(membershipService.getMembershipsByMemberAndReference(USER, USER_NAME, MembershipReferenceType.GROUP)).thenReturn(
            Set.of(membership(API_GROUP, MembershipReferenceType.GROUP, roleId))
        );
        givenRole(roleId, true);
    }

    private void givenRole(String roleId, boolean managesApi) {
        var role = RoleEntity.builder().id(roleId).build();
        when(roleService.findById(roleId)).thenReturn(role);
        when(apiAuthorizationService.canManageApi(role)).thenReturn(managesApi);
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
