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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static io.gravitee.rest.api.model.MembershipMemberType.USER;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A member whose role only reads the API (the default USER role) is refused the API's subscriptions, as they are refused
 * the API itself, although their role grants {@code API_SUBSCRIPTION[READ]}. A member who manages the API still reads them.
 */
public class ApiSubscriptionsResourceNotAdminTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String SUBSCRIPTION = "my-subscription";
    private static final String API_GROUP = "api-group";
    private static final String READ_ONLY_ROLE_ID = "read-only-role-id";
    private static final String MANAGER_ROLE_ID = "manager-role-id";
    private static final List<String> SUBSCRIPTION_READS = List.of(
        "subscriptions",
        "subscriptions/" + SUBSCRIPTION,
        "subscriptions/" + SUBSCRIPTION + "/apikeys",
        "subscribers",
        "subscribers/_paged"
    );

    @Override
    protected String contextPath() {
        return "apis/" + API;
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @Before
    public void resetSharedMocks() {
        // The resource mocks are Spring singletons, shared with the test classes run before this one.
        reset(
            membershipService,
            roleService,
            apiAuthorizationServiceV4,
            apiSearchServiceV4,
            subscriptionService,
            apiKeyService,
            applicationService
        );
    }

    @After
    public void cleanUp() {
        reset(
            membershipService,
            roleService,
            apiAuthorizationServiceV4,
            apiSearchServiceV4,
            subscriptionService,
            apiKeyService,
            applicationService
        );
    }

    @Test
    public void should_refuse_a_member_whose_role_only_reads_the_api() {
        givenAnApiOwnedByAGroup();
        givenADirectMembership(READ_ONLY_ROLE_ID, false);

        for (String path : SUBSCRIPTION_READS) {
            Response response = envTarget().path(path).request().get();

            assertEquals("GET " + path, FORBIDDEN_403, response.getStatus());
        }
        verifyNoInteractions(subscriptionService, apiKeyService, applicationService);
    }

    @Test
    public void should_refuse_checking_a_custom_api_key_to_a_member_whose_role_only_reads_the_api() {
        givenAnApiOwnedByAGroup();
        givenADirectMembership(READ_ONLY_ROLE_ID, false);

        Response response = envTarget()
            .path("subscriptions/_canCreate")
            .queryParam("key", "a-custom-key")
            .queryParam("application", "my-application")
            .request()
            .get();

        assertEquals(FORBIDDEN_403, response.getStatus());
        verifyNoInteractions(apiKeyService);
    }

    @Test
    public void should_list_the_subscriptions_to_a_member_who_manages_the_api_through_its_group() {
        givenAnApiOwnedByAGroup();
        givenADirectMembership(READ_ONLY_ROLE_ID, false);
        givenAGroupMembership(MANAGER_ROLE_ID);
        when(subscriptionService.search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(false), eq(false))).thenReturn(
            new Page<>(List.of(), 0, 0, 0)
        );
        when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(new Metadata());

        Response response = envTarget().path("subscriptions").request().get();

        assertEquals(OK_200, response.getStatus());
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
        when(apiAuthorizationServiceV4.canManageApi(role)).thenReturn(managesApi);
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
