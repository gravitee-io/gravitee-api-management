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
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiKeyFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.rest.api.management.v2.rest.model.ApiKey;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApiSubscriptionsResource_RevokeApiKeyTest extends AbstractApiSubscriptionsResourceTest {

    private static final String API_KEY_ID = "my-api-key";

    @Autowired
    private ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudServiceInMemory;

    @Autowired
    private SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/apis/" + API + "/subscriptions/" + SUBSCRIPTION + "/api-keys/" + API_KEY_ID + "/_revoke";
    }

    @BeforeEach
    public void setUp() {
        super.setUp();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        when(environmentService.findById(ENVIRONMENT)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().id(APPLICATION).apiKeyMode(ApiKeyMode.EXCLUSIVE).build())
        );
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        Stream
            .of(apiKeyCrudServiceInMemory, applicationCrudServiceInMemory, subscriptionCrudServiceInMemory)
            .forEach(InMemoryAlternative::reset);

        GraviteeContext.cleanContext();
    }

    @Test
    public void should_return_404_if_subscription_not_found() {
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey().toBuilder().id(API_KEY_ID).applicationId(APPLICATION).subscriptions(List.of(SUBSCRIPTION)).build()
            )
        );

        final Response response = rootTarget().request().post(Entity.json(null));

        assertThat(response).hasStatus(NOT_FOUND_404).asError().hasMessage("Subscription [" + SUBSCRIPTION + "] cannot be found.");
    }

    @Test
    public void should_return_404_if_subscription_associated_to_another_api() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(fixtures.core.model.SubscriptionFixtures.aSubscription().toBuilder().id(SUBSCRIPTION).apiId("another-api").build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey().toBuilder().id(API_KEY_ID).applicationId(APPLICATION).subscriptions(List.of(SUBSCRIPTION)).build()
            )
        );

        final Response response = rootTarget().request().post(Entity.json(null));

        assertThat(response).hasStatus(NOT_FOUND_404).asError().hasMessage("Subscription [" + SUBSCRIPTION + "] cannot be found.");
    }

    @Test
    public void should_return_404_if_api_key_not_found() {
        final Response response = rootTarget().request().post(Entity.json(null));

        assertThat(response).hasStatus(NOT_FOUND_404).asError().hasMessage("No API Key can be found.");
    }

    @Test
    public void should_return_404_if_api_key_associated_to_another_subscription() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(fixtures.core.model.SubscriptionFixtures.aSubscription().toBuilder().id(SUBSCRIPTION).apiId(API).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures
                    .anApiKey()
                    .toBuilder()
                    .id(API_KEY_ID)
                    .applicationId(APPLICATION)
                    .subscriptions(List.of("another-subscription"))
                    .build()
            )
        );

        final Response response = rootTarget().request().post(Entity.json(null));

        assertThat(response).hasStatus(NOT_FOUND_404).asError().hasMessage("No API Key can be found.");
    }

    @Test
    public void should_return_400_if_application_is_in_shared_api_key_mode() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(
                fixtures.core.model.SubscriptionFixtures
                    .aSubscription()
                    .toBuilder()
                    .id(SUBSCRIPTION)
                    .apiId(API)
                    .applicationId(APPLICATION)
                    .build()
            )
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().id(APPLICATION).apiKeyMode(ApiKeyMode.SHARED).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey().toBuilder().id(API_KEY_ID).applicationId(APPLICATION).subscriptions(List.of(SUBSCRIPTION)).build()
            )
        );

        final Response response = rootTarget().request().post(Entity.json(null));

        assertThat(response)
            .hasStatus(BAD_REQUEST_400)
            .asError()
            .hasMessage("Invalid operation for API Key mode [SHARED] of application [my-application]");
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

        final Response response = rootTarget().request().post(Entity.json(null));

        assertThat(response).hasStatus(FORBIDDEN_403).asError().hasMessage("You do not have sufficient rights to access this resource");
    }

    @Test
    public void should_revoke_api_key() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(
                fixtures.core.model.SubscriptionFixtures
                    .aSubscription()
                    .toBuilder()
                    .id(SUBSCRIPTION)
                    .apiId(API)
                    .applicationId(APPLICATION)
                    .build()
            )
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().id(APPLICATION).apiKeyMode(ApiKeyMode.EXCLUSIVE).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey().toBuilder().id(API_KEY_ID).applicationId(APPLICATION).subscriptions(List.of(SUBSCRIPTION)).build()
            )
        );

        final Response response = rootTarget().request().post(Entity.json(null));

        assertThat(response)
            .hasStatus(OK_200)
            .asEntity(ApiKey.class)
            .extracting(ApiKey::getId, ApiKey::getRevoked)
            .containsExactly(API_KEY_ID, true);
    }
}
