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
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.Key;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

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

    @Override
    protected String contextPath() {
        return "subscriptions/";
    }

    @Before
    public void init() {
        resetAllMocks();

        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey(KEY);
        apiKeyEntity.setSubscription(SUBSCRIPTION);

        doReturn(apiKeyEntity).when(apiKeyService).renew(SUBSCRIPTION);
        doReturn(apiKeyEntity).when(apiKeyService).findByKey(KEY);

        doReturn(new Key().id(KEY)).when(keyMapper).convert(apiKeyEntity);

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setApi(API);
        subscriptionEntity.setApplication(APPLICATION);
        doReturn(subscriptionEntity).when(subscriptionService).findById(eq(SUBSCRIPTION));
        doReturn(true).when(permissionService).hasPermission(any(), any(), any());
    }

    @Test
    public void shouldRenewSubscription() {
        final Response response = target(SUBSCRIPTION).path("keys/_renew").request().post(null);
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertNull(response.getHeaders().getFirst(HttpHeaders.LOCATION));

        Mockito.verify(apiKeyService).renew(SUBSCRIPTION);

        Key key = response.readEntity(Key.class);
        assertNotNull(key);
        assertEquals(KEY, key.getId());
    }

    @Test
    public void testPermissionsForRenewingSubscription() {
        reset(permissionService);

        doReturn(true).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.UPDATE);
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.UPDATE);
        assertEquals(HttpStatusCode.CREATED_201, target(SUBSCRIPTION).path("keys/_renew").request().post(null).getStatus());

        doReturn(true).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.UPDATE);
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.UPDATE);
        assertEquals(HttpStatusCode.CREATED_201, target(SUBSCRIPTION).path("keys/_renew").request().post(null).getStatus());

        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.UPDATE);
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.UPDATE);
        assertEquals(HttpStatusCode.CREATED_201, target(SUBSCRIPTION).path("keys/_renew").request().post(null).getStatus());

        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.UPDATE);
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.UPDATE);
        assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).path("keys/_renew").request().post(null).getStatus());
    }

    @Test
    public void shouldRevokeKey() {
        final Response response = target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null);
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        Mockito.verify(apiKeyService).revoke(KEY, true);

        assertFalse(response.hasEntity());
    }

    @Test
    public void shouldRevokeKeyNoSubscription() {
        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey(KEY);

        doReturn(apiKeyEntity).when(apiKeyService).findByKey(KEY);

        final Response response = target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null);
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        Mockito.verify(apiKeyService).revoke(KEY, true);

        assertFalse(response.hasEntity());
    }

    @Test
    public void shouldNotRevokeKey() {
        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey(KEY);
        apiKeyEntity.setSubscription(ANOTHER_SUBSCRIPTION);

        doReturn(apiKeyEntity).when(apiKeyService).findByKey(KEY);

        final Response response = target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());

        assertEquals("'keyId' parameter does not correspond to the subscription", response.readEntity(String.class));
    }

    @Test
    public void testPermissionsForRevokingKeys() {
        reset(permissionService);

        doReturn(true).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.UPDATE);
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.UPDATE);
        assertEquals(HttpStatusCode.NO_CONTENT_204, target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null).getStatus());

        doReturn(true).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.UPDATE);
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.UPDATE);
        assertEquals(HttpStatusCode.NO_CONTENT_204, target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null).getStatus());

        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.UPDATE);
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.UPDATE);
        assertEquals(HttpStatusCode.NO_CONTENT_204, target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null).getStatus());

        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.UPDATE);
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.UPDATE);
        assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).path("keys/" + KEY + "/_revoke").request().post(null).getStatus());
    }
}
