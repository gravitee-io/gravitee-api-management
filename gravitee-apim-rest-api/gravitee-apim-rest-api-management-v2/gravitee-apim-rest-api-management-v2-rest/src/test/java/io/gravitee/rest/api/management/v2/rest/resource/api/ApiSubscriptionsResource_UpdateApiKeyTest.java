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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.ApplicationFixtures;
import fixtures.SubscriptionFixtures;
import io.gravitee.rest.api.management.v2.rest.model.ApiKey;
import io.gravitee.rest.api.management.v2.rest.model.Error;
import io.gravitee.rest.api.management.v2.rest.model.UpdateApiKey;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ApiKeyNotFoundException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ApiSubscriptionsResource_UpdateApiKeyTest extends AbstractApiSubscriptionsResourceTest {

    private static final String API_KEY_ID = "my-api-key";

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions/" + SUBSCRIPTION + "/api-keys/" + API_KEY_ID;
    }

    @Test
    public void should_return_404_if_subscription_not_found() {
        when(subscriptionService.findById(SUBSCRIPTION)).thenThrow(new SubscriptionNotFoundException(SUBSCRIPTION));

        final Response response = rootTarget().request().put(Entity.json(SubscriptionFixtures.anUpdateApiKey()));
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

        final Response response = rootTarget().request().put(Entity.json(SubscriptionFixtures.anUpdateApiKey()));
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("Subscription [" + SUBSCRIPTION + "] cannot be found.", error.getMessage());
    }

    @Test
    public void should_return_404_if_api_key_not_found() {
        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(SubscriptionFixtures.aSubscriptionEntity());
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION)).thenReturn(
            ApplicationFixtures.anApplicationEntity().toBuilder().id(APPLICATION).build()
        );
        when(apiKeyService.findById(GraviteeContext.getExecutionContext(), API_KEY_ID)).thenThrow(new ApiKeyNotFoundException());

        final Response response = rootTarget().request().put(Entity.json(SubscriptionFixtures.anUpdateApiKey()));
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("No API Key can be found.", error.getMessage());
    }

    @Test
    public void should_return_404_if_api_key_associated_to_another_subscription() {
        final ApiKeyEntity apiKeyEntity = SubscriptionFixtures.anApiKeyEntity()
            .toBuilder()
            .id(API_KEY_ID)
            .subscriptions(Set.of(SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("ANOTHER-SUBSCRIPTION").build()))
            .build();

        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(SubscriptionFixtures.aSubscriptionEntity());
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION)).thenReturn(
            ApplicationFixtures.anApplicationEntity().toBuilder().id(APPLICATION).build()
        );
        when(apiKeyService.findById(GraviteeContext.getExecutionContext(), API_KEY_ID)).thenReturn(apiKeyEntity);

        final Response response = rootTarget().request().put(Entity.json(SubscriptionFixtures.anUpdateApiKey()));
        assertEquals(NOT_FOUND_404, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(NOT_FOUND_404, (int) error.getHttpStatus());
        assertEquals("No API Key can be found.", error.getMessage());
    }

    @Test
    public void should_return_400_if_application_is_in_shared_api_key_mode() {
        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(SubscriptionFixtures.aSubscriptionEntity());
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION)).thenReturn(
            ApplicationFixtures.anApplicationEntity().toBuilder().id(APPLICATION).apiKeyMode(ApiKeyMode.SHARED).build()
        );

        final Response response = rootTarget().request().put(Entity.json(SubscriptionFixtures.anUpdateApiKey()));
        assertEquals(BAD_REQUEST_400, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(BAD_REQUEST_400, (int) error.getHttpStatus());
        assertEquals("Invalid operation for API Key mode [SHARED] of application [my-application].", error.getMessage());
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
        ).thenReturn(false);

        final Response response = rootTarget().request().put(Entity.json(SubscriptionFixtures.anUpdateApiKey()));
        assertEquals(FORBIDDEN_403, response.getStatus());

        var error = response.readEntity(Error.class);
        assertEquals(FORBIDDEN_403, (int) error.getHttpStatus());
        assertEquals("You do not have sufficient rights to access this resource", error.getMessage());
    }

    @Test
    public void should_update_api_key() {
        final ApiKeyEntity apiKeyEntity = SubscriptionFixtures.anApiKeyEntity()
            .toBuilder()
            .id(API_KEY_ID)
            .subscriptions(
                Set.of(
                    SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("ANOTHER-SUBSCRIPTION").build(),
                    SubscriptionFixtures.aSubscriptionEntity().toBuilder().id(SUBSCRIPTION).build()
                )
            )
            .build();

        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(SubscriptionFixtures.aSubscriptionEntity());
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION)).thenReturn(
            ApplicationFixtures.anApplicationEntity().toBuilder().id(APPLICATION).build()
        );
        when(apiKeyService.findById(GraviteeContext.getExecutionContext(), API_KEY_ID)).thenReturn(apiKeyEntity);
        when(apiKeyService.update(any(), any())).thenAnswer(i -> i.getArgument(1));

        final UpdateApiKey updateApiKey = SubscriptionFixtures.anUpdateApiKey();
        final Response response = rootTarget().request().put(Entity.json(updateApiKey));

        assertEquals(OK_200, response.getStatus());

        var apiKey = response.readEntity(ApiKey.class);
        assertEquals(API_KEY_ID, apiKey.getId());
        assertEquals(updateApiKey.getExpireAt().toInstant().toEpochMilli(), apiKey.getExpireAt().toInstant().toEpochMilli());
    }

    @Test
    void should_remove_expiration_date_of_api_key() {
        final ApiKeyEntity apiKeyEntity = SubscriptionFixtures.anApiKeyEntity()
            .toBuilder()
            .id(API_KEY_ID)
            .subscriptions(
                Set.of(
                    SubscriptionFixtures.aSubscriptionEntity().toBuilder().id("ANOTHER-SUBSCRIPTION").build(),
                    SubscriptionFixtures.aSubscriptionEntity().toBuilder().id(SUBSCRIPTION).build()
                )
            )
            .build();

        when(subscriptionService.findById(SUBSCRIPTION)).thenReturn(SubscriptionFixtures.aSubscriptionEntity());
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION)).thenReturn(
            ApplicationFixtures.anApplicationEntity().toBuilder().id(APPLICATION).build()
        );
        when(apiKeyService.findById(GraviteeContext.getExecutionContext(), API_KEY_ID)).thenReturn(apiKeyEntity);
        when(apiKeyService.update(any(), any())).thenAnswer(i -> i.getArgument(1));

        final UpdateApiKey updateApiKey = new UpdateApiKey().expireAt(null);
        final Response response = rootTarget().request().put(Entity.json(updateApiKey));

        assertEquals(OK_200, response.getStatus());

        var apiKey = response.readEntity(ApiKey.class);
        assertEquals(API_KEY_ID, apiKey.getId());
        assertNull(apiKey.getExpireAt());
    }
}
