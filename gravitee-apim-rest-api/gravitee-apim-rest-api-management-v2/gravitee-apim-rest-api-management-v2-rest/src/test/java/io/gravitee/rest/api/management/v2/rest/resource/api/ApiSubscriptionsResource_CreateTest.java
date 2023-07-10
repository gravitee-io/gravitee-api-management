/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.SubscriptionFixtures;
import io.gravitee.rest.api.management.v2.rest.model.CreateSubscription;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.Subscription;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.ProcessSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

public class ApiSubscriptionsResource_CreateTest extends ApiSubscriptionsResourceTest {

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions";
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.CREATE)
            )
        )
            .thenReturn(false);

        final Response response = rootTarget().request().post(Entity.json(SubscriptionFixtures.aCreateSubscription()));
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_create_subscription() {
        final CreateSubscription createSubscription = SubscriptionFixtures
            .aCreateSubscription()
            .toBuilder()
            .applicationId(APPLICATION)
            .planId(PLAN)
            .customApiKey(null)
            .build();
        final SubscriptionEntity subscriptionEntity = SubscriptionFixtures
            .aSubscriptionEntity()
            .toBuilder()
            .id(SUBSCRIPTION)
            .application(APPLICATION)
            .plan(PLAN)
            .build();

        when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any()))
            .thenReturn(subscriptionEntity);

        final Response response = rootTarget().request().post(Entity.json(createSubscription));
        assertEquals(CREATED_201, response.getStatus());

        final Subscription subscription = response.readEntity(Subscription.class);
        assertEquals(SUBSCRIPTION, subscription.getId());
        assertEquals(PLAN, subscription.getPlan().getId());
        assertEquals(APPLICATION, subscription.getApplication().getId());

        verify(subscriptionService)
            .create(
                eq(GraviteeContext.getExecutionContext()),
                argThat(newSubscriptionEntity -> {
                    assertEquals(PLAN, newSubscriptionEntity.getPlan());
                    assertEquals(APPLICATION, newSubscriptionEntity.getApplication());

                    return true;
                }),
                isNull()
            );
    }

    @Test
    public void should_return_400_if_custom_api_key_not_enabled() {
        final CreateSubscription createSubscription = SubscriptionFixtures
            .aCreateSubscription()
            .toBuilder()
            .applicationId(APPLICATION)
            .planId(PLAN)
            .customApiKey("custom")
            .build();

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(false);

        final Response response = rootTarget().request().post(Entity.json(createSubscription));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
        assertEquals("You are not allowed to provide a custom API Key", error.getMessage());
    }

    @Test
    public void should_create_subscription_with_custom_api_key() {
        final CreateSubscription createSubscription = SubscriptionFixtures
            .aCreateSubscription()
            .toBuilder()
            .applicationId(APPLICATION)
            .planId(PLAN)
            .customApiKey("custom")
            .build();
        final SubscriptionEntity subscriptionEntity = SubscriptionFixtures
            .aSubscriptionEntity()
            .toBuilder()
            .id(SUBSCRIPTION)
            .application(APPLICATION)
            .plan(PLAN)
            .status(SubscriptionStatus.ACCEPTED)
            .build();

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);
        when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any()))
            .thenReturn(subscriptionEntity);

        final Response response = rootTarget().request().post(Entity.json(createSubscription));
        assertEquals(CREATED_201, response.getStatus());

        final Subscription subscription = response.readEntity(Subscription.class);
        assertEquals(SUBSCRIPTION, subscription.getId());
    }

    @Test
    public void should_create_subscription_with_custom_api_key_and_auto_process_it_if_pending() {
        final CreateSubscription createSubscription = SubscriptionFixtures
            .aCreateSubscription()
            .toBuilder()
            .applicationId(APPLICATION)
            .planId(PLAN)
            .customApiKey("custom")
            .build();
        final SubscriptionEntity subscriptionEntity = SubscriptionFixtures
            .aSubscriptionEntity()
            .toBuilder()
            .id(SUBSCRIPTION)
            .application(APPLICATION)
            .plan(PLAN)
            .status(SubscriptionStatus.PENDING)
            .build();

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);
        when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any()))
            .thenReturn(subscriptionEntity);
        when(subscriptionService.process(eq(GraviteeContext.getExecutionContext()), any(ProcessSubscriptionEntity.class), any()))
            .thenReturn(subscriptionEntity.toBuilder().status(SubscriptionStatus.ACCEPTED).build());

        final Response response = rootTarget().request().post(Entity.json(createSubscription));
        assertEquals(CREATED_201, response.getStatus());

        final Subscription subscription = response.readEntity(Subscription.class);
        assertEquals(SUBSCRIPTION, subscription.getId());

        verify(subscriptionService)
            .process(
                eq(GraviteeContext.getExecutionContext()),
                argThat(processSubscription -> {
                    assertEquals(SUBSCRIPTION, processSubscription.getId());
                    assertEquals("custom", processSubscription.getCustomApiKey());
                    assertTrue(processSubscription.isAccepted());
                    assertNotNull(processSubscription.getStartingAt());

                    return true;
                }),
                eq(USER_NAME)
            );
    }
}
