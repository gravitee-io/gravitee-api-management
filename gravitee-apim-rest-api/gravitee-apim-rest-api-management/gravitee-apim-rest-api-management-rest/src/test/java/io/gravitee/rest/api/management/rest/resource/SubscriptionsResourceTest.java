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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionsResourceTest extends AbstractResourceTest {

    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";

    @Override
    protected String contextPath() {
        return "subscriptions";
    }

    @BeforeEach
    void init() {
        reset(subscriptionService, applicationService, planService);
    }

    @Test
    void should_return_403_when_user_cannot_read_subscriptions_of_requested_application() {
        when(
            permissionService.hasPermission(
                any(ExecutionContext.class),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                any(RolePermissionAction[].class)
            )
        ).thenReturn(false);

        final Response response = envTarget().queryParam("application", APPLICATION).request().get();

        assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
        verify(subscriptionService, never()).findByApplicationAndPlan(any(), any(), any());
    }

    @Test
    void should_only_return_subscriptions_of_current_environment() {
        when(subscriptionService.findByApplicationAndPlan(any(ExecutionContext.class), isNull(), isNull())).thenReturn(
            List.of(aSubscription("current-env-subscription", "DEFAULT"), aSubscription("other-env-subscription", "OTHER"))
        );
        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION);
        application.setPrimaryOwner(new PrimaryOwnerEntity());
        when(applicationService.findById(any(ExecutionContext.class), eq(APPLICATION))).thenReturn(application);
        PlanEntity plan = new PlanEntity();
        plan.setId(PLAN);
        when(planService.findById(any(ExecutionContext.class), eq(PLAN))).thenReturn(plan);

        final Response response = envTarget().request().get();

        assertThat(response.getStatus()).isEqualTo(OK_200);
        List<String> subscriptionIds = new ArrayList<>();
        response.readEntity(JsonNode.class).forEach(subscription -> subscriptionIds.add(subscription.get("id").asText()));
        assertThat(subscriptionIds).containsExactly("current-env-subscription");
    }

    private static SubscriptionEntity aSubscription(String id, String environmentId) {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(id);
        subscription.setEnvironmentId(environmentId);
        subscription.setApplication(APPLICATION);
        subscription.setPlan(PLAN);
        return subscription;
    }
}
