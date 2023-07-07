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

import static io.gravitee.common.http.HttpStatusCode.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.SubscriptionFixtures;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.Subscription;
import io.gravitee.rest.api.model.ProcessSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.Test;
import org.mockito.Mockito;

public class ApiSubscriptionsResource_RejectTest extends ApiSubscriptionsResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions/" + SUBSCRIPTION + "/_reject";
    }

    @Test
    public void should_return_404_if_not_found() {
        when(subscriptionService.findById(SUBSCRIPTION)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION));

        final Response response = rootTarget().request().post(Entity.json(SubscriptionFixtures.aRejectSubscription()));
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Subscription [" + SUBSCRIPTION + "] cannot be found.", error.getMessage());
    }

    @Test
    public void should_return_404_if_subscription_associated_to_another_api() {
        final SubscriptionEntity subscriptionEntity = SubscriptionFixtures
            .aSubscriptionEntity()
            .toBuilder()
            .id(SUBSCRIPTION)
            .api("ANOTHER-API")
            .build();

        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(subscriptionEntity);

        final Response response = rootTarget().request().post(Entity.json(SubscriptionFixtures.aRejectSubscription()));
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Subscription [" + SUBSCRIPTION + "] cannot be found.", error.getMessage());
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            )
        )
            .thenReturn(false);

        final Response response = rootTarget().request().post(Entity.json(SubscriptionFixtures.aRejectSubscription()));
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_reject_subscription() {
        final SubscriptionEntity subscriptionEntity = SubscriptionFixtures
            .aSubscriptionEntity()
            .toBuilder()
            .id(SUBSCRIPTION)
            .api(API)
            .plan(PLAN)
            .status(SubscriptionStatus.PENDING)
            .build();
        final var rejectSubscription = SubscriptionFixtures.aRejectSubscription();

        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(subscriptionEntity);
        when(subscriptionService.process(eq(GraviteeContext.getExecutionContext()), any(ProcessSubscriptionEntity.class), eq(USER_NAME)))
            .thenReturn(subscriptionEntity);

        final Response response = rootTarget().request().post(Entity.json(rejectSubscription));
        assertEquals(OK_200, response.getStatus());

        final Subscription subscription = response.readEntity(Subscription.class);
        assertEquals(SUBSCRIPTION, subscription.getId());

        verify(subscriptionService)
            .process(
                eq(GraviteeContext.getExecutionContext()),
                Mockito.argThat(processSubscriptionEntity -> {
                    assertEquals(SUBSCRIPTION, processSubscriptionEntity.getId());
                    assertFalse(processSubscriptionEntity.isAccepted());
                    return true;
                }),
                eq(USER_NAME)
            );
    }
}
