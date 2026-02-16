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
package io.gravitee.rest.api.portal.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import fixtures.core.model.ApiKeyFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionKeysResourceTest extends AbstractResourceTest {

    private static final String SUBSCRIPTION = "my-subscription";
    private static final String ANOTHER_SUBSCRIPTION = "my-other-ubscription";
    private static final String KEY = "my-key";
    private static final String API = "my-api";
    private static final String APPLICATION = "my-application";
    private ApiKeyEntity apiKeyEntity;

    @Autowired
    private ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudServiceInMemory;

    @Autowired
    private SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "subscriptions/";
    }

    @BeforeEach
    public void init() {
        resetAllMocks();

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION);

        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey(KEY);
        apiKeyEntity.setSubscriptions(Set.of(subscription));
        apiKeyEntity.setCreatedAt(new Date());

        doReturn(apiKeyEntity).when(apiKeyService).renew(GraviteeContext.getExecutionContext(), subscription);
        doReturn(apiKeyEntity).when(apiKeyService).findByKeyAndApi(GraviteeContext.getExecutionContext(), KEY, API);

        doReturn(new Key().key(KEY)).when(keyMapper).convert(apiKeyEntity);

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setApi(API);
        subscriptionEntity.setReferenceId(API);
        subscriptionEntity.setReferenceType("API");
        subscriptionEntity.setApplication(APPLICATION);
        doReturn(subscriptionEntity).when(subscriptionService).findById(eq(SUBSCRIPTION));
        doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());
    }

    @Test
    public void shouldRenewSubscription() {
        when(apiKeyService.renew(eq(GraviteeContext.getExecutionContext()), any(SubscriptionEntity.class))).thenReturn(apiKeyEntity);
        when(keyMapper.convert(any(ApiKeyEntity.class))).thenCallRealMethod();

        final Response response = target(SUBSCRIPTION).path("keys/_renew").request().post(null);
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertNull(response.getHeaders().getFirst(HttpHeaders.LOCATION));

        Mockito.verify(apiKeyService).renew(eq(GraviteeContext.getExecutionContext()), any(SubscriptionEntity.class));

        Key key = response.readEntity(Key.class);
        assertNotNull(key);
        assertEquals(KEY, key.getKey());
    }

    @Test
    public void testPermissionsForRenewingSubscription() {
        reset(permissionService);

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            );
        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.UPDATE)
            );
        assertEquals(HttpStatusCode.CREATED_201, target(SUBSCRIPTION).path("keys/_renew").request().post(null).getStatus());

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            );
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.UPDATE)
            );
        assertEquals(HttpStatusCode.CREATED_201, target(SUBSCRIPTION).path("keys/_renew").request().post(null).getStatus());

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            );
        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.UPDATE)
            );
        assertEquals(HttpStatusCode.CREATED_201, target(SUBSCRIPTION).path("keys/_renew").request().post(null).getStatus());

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            );
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.UPDATE)
            );
        assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).path("keys/_renew").request().post(null).getStatus());
    }

    @Test
    public void should_revoke_key() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(
                fixtures.core.model.SubscriptionFixtures.aSubscription()
                    .toBuilder()
                    .id(SUBSCRIPTION)
                    .apiId(API)
                    .referenceId(API)
                    .referenceType(io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API)
                    .applicationId(APPLICATION)
                    .build()
            )
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().id(APPLICATION).apiKeyMode(ApiKeyMode.EXCLUSIVE).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(ApiKeyFixtures.anApiKey().toBuilder().key(KEY).applicationId(APPLICATION).subscriptions(List.of(SUBSCRIPTION)).build())
        );

        final Response response = target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null);

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
    }

    @Test
    public void should_return_forbidden_error_when_not_enough_permissions() {
        reset(permissionService);

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            );
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.UPDATE)
            );
        assertThat(target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null).getStatus()).isEqualTo(
            HttpStatusCode.FORBIDDEN_403
        );
    }

    @Test
    public void should_revoke_api_key_when_only_allowed_to_update_api_subscription() {
        reset(permissionService);

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            );
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.UPDATE)
            );

        subscriptionCrudServiceInMemory.initWith(
            List.of(
                fixtures.core.model.SubscriptionFixtures.aSubscription()
                    .toBuilder()
                    .id(SUBSCRIPTION)
                    .apiId(API)
                    .referenceId(API)
                    .referenceType(io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API)
                    .applicationId(APPLICATION)
                    .build()
            )
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().id(APPLICATION).apiKeyMode(ApiKeyMode.EXCLUSIVE).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(ApiKeyFixtures.anApiKey().toBuilder().key(KEY).applicationId(APPLICATION).subscriptions(List.of(SUBSCRIPTION)).build())
        );

        assertThat(target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null).getStatus()).isEqualTo(
            HttpStatusCode.NO_CONTENT_204
        );
    }

    @Test
    public void should_revoke_api_key_when_only_allowed_to_update_application_subscription() {
        reset(permissionService);

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.UPDATE)
            );
        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.UPDATE)
            );

        subscriptionCrudServiceInMemory.initWith(
            List.of(
                fixtures.core.model.SubscriptionFixtures.aSubscription()
                    .toBuilder()
                    .id(SUBSCRIPTION)
                    .apiId(API)
                    .referenceId(API)
                    .referenceType(io.gravitee.apim.core.subscription.model.SubscriptionReferenceType.API)
                    .applicationId(APPLICATION)
                    .build()
            )
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().id(APPLICATION).apiKeyMode(ApiKeyMode.EXCLUSIVE).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(ApiKeyFixtures.anApiKey().toBuilder().key(KEY).applicationId(APPLICATION).subscriptions(List.of(SUBSCRIPTION)).build())
        );

        assertThat(target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null).getStatus()).isEqualTo(
            HttpStatusCode.NO_CONTENT_204
        );
    }
}
