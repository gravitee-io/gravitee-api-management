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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.ApplicationFixtures;
import fixtures.PlanFixtures;
import fixtures.SubscriptionFixtures;
import fixtures.UserFixtures;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.Subscription;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ApiSubscriptionsResource_GetTest extends AbstractApiSubscriptionsResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions/" + SUBSCRIPTION;
    }

    @Test
    public void should_return_404_if_not_found() {
        when(subscriptionService.findById(SUBSCRIPTION)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION));

        final Response response = rootTarget().request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Subscription [" + SUBSCRIPTION + "] cannot be found.", error.getMessage());
    }

    @Test
    public void should_return_404_if_subscription_associated_to_another_api() {
        final SubscriptionEntity subscriptionEntity = SubscriptionFixtures.aSubscriptionEntity()
            .toBuilder()
            .id(SUBSCRIPTION)
            .referenceId("ANOTHER-API")
            .referenceType("API")
            .build();

        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(subscriptionEntity);

        final Response response = rootTarget().request().get();
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
                eq(RolePermissionAction.READ)
            )
        ).thenReturn(false);

        final Response response = rootTarget().request().get();
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_return_subscription() {
        final SubscriptionEntity subscriptionEntity = SubscriptionFixtures.aSubscriptionEntity()
            .toBuilder()
            .id(SUBSCRIPTION)
            .api(API)
            .plan(PLAN)
            .application(APPLICATION)
            .build();

        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(subscriptionEntity);

        final Response response = rootTarget().request().get();
        assertEquals(OK_200, response.getStatus());

        var subscription = response.readEntity(Subscription.class);
        assertEquals(subscriptionEntity.getId(), subscription.getId());
    }

    @Test
    public void should_return_subscription_with_expands() {
        final SubscriptionEntity subscriptionEntity = SubscriptionFixtures.aSubscriptionEntity()
            .toBuilder()
            .id(SUBSCRIPTION)
            .api(API)
            .plan(PLAN)
            .application(APPLICATION)
            .subscribedBy(SUBSCRIBED_BY)
            .build();

        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(subscriptionEntity);

        var planEntity = PlanFixtures.aPlanEntityV4().toBuilder().id(PLAN).build();
        when(planSearchService.findByIdIn(GraviteeContext.getExecutionContext(), Set.of(PLAN))).thenReturn(Set.of(planEntity));

        when(
            applicationService.findByIds(
                eq(GraviteeContext.getExecutionContext()),
                argThat(argument -> List.of(APPLICATION).containsAll(argument))
            )
        ).thenReturn(Set.of(ApplicationFixtures.anApplicationListItem().toBuilder().id(APPLICATION).build()));

        when(
            userService.findByIds(
                eq(GraviteeContext.getExecutionContext()),
                argThat(argument -> List.of(SUBSCRIBED_BY).containsAll(argument))
            )
        ).thenReturn(
            Set.of(
                UserFixtures.aUserEntity().toBuilder().id("first-user").build(),
                UserFixtures.aUserEntity().toBuilder().id("second-user").build()
            )
        );

        final Response response = rootTarget().queryParam("expands", "plan,application,subscribedBy").request().get();

        assertEquals(OK_200, response.getStatus());

        var subscription = response.readEntity(Subscription.class);
        assertEquals(subscriptionEntity.getId(), subscription.getId());
        assertEquals(subscriptionEntity.getPlan(), subscription.getPlan().getId());
        assertNotNull(subscription.getPlan().getName());
        assertNotNull(subscription.getPlan().getDescription());
        assertEquals(subscription.getPlan().getMode().name(), planEntity.getMode().name());
        assertEquals(subscriptionEntity.getApplication(), subscription.getApplication().getId());
        assertNotNull(subscription.getApplication().getName());
        assertNotNull(subscription.getApplication().getDescription());

        verify(userService, times(1)).findByIds(any(), any());
    }
}
