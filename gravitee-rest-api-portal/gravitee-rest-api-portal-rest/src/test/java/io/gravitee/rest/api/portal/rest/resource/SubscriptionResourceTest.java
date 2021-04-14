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

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "subscriptions/";
    }

    private static final String SUBSCRIPTION = "my-subscription";
    private static final String UNKNOWN_SUBSCRIPTION = "unknown-subscription";
    private static final String API = "my-api";
    private static final String APPLICATION = "my-application";
    private SubscriptionEntity subscriptionEntity;

    @Before
    public void init() {
        resetAllMocks();

        subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION);
        subscriptionEntity.setApi(API);
        subscriptionEntity.setApplication(APPLICATION);

        doReturn(subscriptionEntity).when(subscriptionService).findById(SUBSCRIPTION);
        doThrow(SubscriptionNotFoundException.class).when(subscriptionService).findById(UNKNOWN_SUBSCRIPTION);

        doReturn(Arrays.asList(new ApiKeyEntity())).when(apiKeyService).findBySubscription(SUBSCRIPTION);

        doReturn(new Subscription()).when(subscriptionMapper).convert(any());
        doReturn(new Key()).when(keyMapper).convert(any());
        doReturn(true).when(permissionService).hasPermission(any(), any(), any());
    }

    @Test
    public void shouldGetSubscription() {
        Response response = target(SUBSCRIPTION).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(subscriptionMapper).convert(subscriptionEntity);

        Subscription subscription = response.readEntity(Subscription.class);
        assertNotNull(subscription);
        assertNull(subscription.getKeys());
    }

    @Test
    public void testPermissionsForGettingASubscription() {
        reset(permissionService);

        doReturn(true).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.READ);
        doReturn(true)
            .when(permissionService)
            .hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.READ);
        assertEquals(HttpStatusCode.OK_200, target(SUBSCRIPTION).request().get().getStatus());

        doReturn(true).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.READ);
        doReturn(false)
            .when(permissionService)
            .hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.READ);
        assertEquals(HttpStatusCode.OK_200, target(SUBSCRIPTION).request().get().getStatus());

        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.READ);
        doReturn(true)
            .when(permissionService)
            .hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.READ);
        assertEquals(HttpStatusCode.OK_200, target(SUBSCRIPTION).request().get().getStatus());

        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.READ);
        doReturn(false)
            .when(permissionService)
            .hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.READ);
        assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).request().get().getStatus());
    }

    @Test
    public void shouldGetSubscriptionWithKeys() {
        doReturn(true).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION, API, RolePermissionAction.READ);
        doReturn(true)
            .when(permissionService)
            .hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.READ);

        final Response response = target(SUBSCRIPTION).queryParam("include", "keys").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        Mockito.verify(subscriptionMapper).convert(subscriptionEntity);

        Subscription subscription = response.readEntity(Subscription.class);
        assertNotNull(subscription);
        assertNotNull(subscription.getKeys());
        assertFalse(subscription.getKeys().isEmpty());
    }

    @Test
    public void shouldNotGetSubscription() {
        final Response response = target(UNKNOWN_SUBSCRIPTION).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());

        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
    }

    @Test
    public void shouldCloseSubscription() {
        final Response response = target(SUBSCRIPTION).path("_close").request().post(null);
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());

        assertFalse(response.hasEntity());
    }

    @Test
    public void testPermissionsForClosingASubscription() {
        reset(permissionService);
        doReturn(true)
            .when(permissionService)
            .hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.DELETE);
        assertEquals(HttpStatusCode.NO_CONTENT_204, target(SUBSCRIPTION).path("_close").request().post(null).getStatus());

        doReturn(false)
            .when(permissionService)
            .hasPermission(RolePermission.APPLICATION_SUBSCRIPTION, APPLICATION, RolePermissionAction.DELETE);
        assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).path("_close").request().post(null).getStatus());
    }
}
