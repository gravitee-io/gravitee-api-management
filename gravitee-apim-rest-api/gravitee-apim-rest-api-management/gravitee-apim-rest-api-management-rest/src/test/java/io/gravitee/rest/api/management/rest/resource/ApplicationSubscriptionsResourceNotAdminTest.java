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

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationSubscriptionsResourceNotAdminTest extends AbstractResourceTest {

    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";
    private static final String API = "my-api";
    private static final String SUBSCRIPTION = "my-subscription";

    @Override
    protected String contextPath() {
        return "applications";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAdminAuthenticationFilter.class);
    }

    @BeforeEach
    void init() {
        reset(apiSearchServiceV4, membershipService, accessControlService, planSearchService, subscriptionService, userService);

        PlanEntity plan = new PlanEntity();
        plan.setId(PLAN);
        plan.setReferenceId(API);
        plan.setReferenceType(GenericPlanEntity.ReferenceType.API);
        PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType("api-key");
        plan.setSecurity(planSecurity);
        when(planSearchService.findById(GraviteeContext.getExecutionContext(), PLAN)).thenReturn(plan);

        ApiEntity api = new ApiEntity();
        api.setId(API);
        api.setPrimaryOwner(mock(PrimaryOwnerEntity.class));
        when(
            apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API), eq(false), eq(false), eq(false))
        ).thenReturn(api);
    }

    @Test
    void should_return_404_and_not_subscribe_when_user_is_not_a_member_and_cannot_consume_api_from_portal() {
        when(accessControlService.canConsumeApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(false);

        final Response response = subscribe();

        assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        verify(subscriptionService, never()).create(any(), any());
    }

    @Test
    void should_subscribe_when_user_can_consume_api_from_portal() {
        when(accessControlService.canConsumeApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);
        givenSubscriptionIsCreated();

        final Response response = subscribe();

        assertThat(response.getStatus()).isEqualTo(CREATED_201);
    }

    @Test
    void should_subscribe_when_user_is_a_member_of_api_not_consumable_from_portal() {
        when(membershipService.getUserMemberPermissions(any(), eq(MembershipReferenceType.API), eq(API), eq(USER_NAME))).thenReturn(
            Map.of("DEFINITION", new char[] { 'R' })
        );
        when(accessControlService.canConsumeApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(false);
        givenSubscriptionIsCreated();

        final Response response = subscribe();

        assertThat(response.getStatus()).isEqualTo(CREATED_201);
    }

    private void givenSubscriptionIsCreated() {
        SubscriptionEntity createdSubscription = new SubscriptionEntity();
        createdSubscription.setId(SUBSCRIPTION);
        createdSubscription.setPlan(PLAN);
        when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(createdSubscription);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any(), anyBoolean())).thenReturn(mock(UserEntity.class));
    }

    private Response subscribe() {
        return envTarget()
            .path(APPLICATION)
            .path("subscriptions")
            .queryParam("plan", PLAN)
            .request()
            .post(Entity.json(new NewSubscriptionEntity()));
    }
}
