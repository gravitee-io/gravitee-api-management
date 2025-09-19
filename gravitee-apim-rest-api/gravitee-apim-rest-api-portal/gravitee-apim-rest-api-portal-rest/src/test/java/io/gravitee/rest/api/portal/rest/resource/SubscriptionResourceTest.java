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

import static jakarta.ws.rs.client.Entity.json;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.*;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
    private static final String PLAN = "my-plan";
    private SubscriptionEntity subscriptionEntity;

    @Before
    public void init() {
        resetAllMocks();

        subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION);
        subscriptionEntity.setApi(API);
        subscriptionEntity.setApplication(APPLICATION);
        subscriptionEntity.setPlan(PLAN);

        doReturn(subscriptionEntity).when(subscriptionService).findById(SUBSCRIPTION);
        doThrow(SubscriptionNotFoundException.class).when(subscriptionService).findById(UNKNOWN_SUBSCRIPTION);

        doReturn(Arrays.asList(new ApiKeyEntity()))
            .when(apiKeyService)
            .findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION);

        doReturn(new Subscription()).when(subscriptionMapper).convert(any());
        doReturn(new Key()).when(keyMapper).convert(any(ApiKeyEntity.class));
        doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());
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

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            );
        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.READ)
            );
        assertEquals(HttpStatusCode.OK_200, target(SUBSCRIPTION).request().get().getStatus());

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            );
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.READ)
            );
        assertEquals(HttpStatusCode.OK_200, target(SUBSCRIPTION).request().get().getStatus());

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            );
        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.READ)
            );
        assertEquals(HttpStatusCode.OK_200, target(SUBSCRIPTION).request().get().getStatus());

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            );
        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.READ)
            );
        assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).request().get().getStatus());
    }

    @Test
    public void shouldGetSubscriptionWithKeys() {
        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.API_SUBSCRIPTION),
                eq(API),
                eq(RolePermissionAction.READ)
            );
        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.READ)
            );

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
    public void testPermissionsForClosingASubscription() {
        reset(permissionService);

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.DELETE)
            );
        assertEquals(HttpStatusCode.FORBIDDEN_403, target(SUBSCRIPTION).path("_close").request().post(null).getStatus());
    }

    @Test
    public void shouldNotUpdateSubscriptionConfigurationCauseNotFound() {
        SubscriptionConfigurationInput subscriptionConfigurationInput = new SubscriptionConfigurationInput();
        Response response = target(UNKNOWN_SUBSCRIPTION).request().put(json(subscriptionConfigurationInput));

        assertEquals(404, response.getStatus());
        verify(subscriptionService).findById(UNKNOWN_SUBSCRIPTION);
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    public void shouldNotUpdateSubscriptionConfigurationCauseInsufficientPermissions() {
        doReturn(false).when(permissionService).hasPermission(any(), any(), any(), any());

        SubscriptionConfigurationInput subscriptionConfigurationInput = new SubscriptionConfigurationInput();
        Response response = target(SUBSCRIPTION).request().put(json(subscriptionConfigurationInput));

        assertEquals(403, response.getStatus());
        verify(subscriptionService).findById(SUBSCRIPTION);
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    public void shouldUpdateSubscriptionConfiguration() {
        UpdateSubscriptionInput updateSubscriptionInput = new UpdateSubscriptionInput();
        SubscriptionConfigurationInput subscriptionConfigurationInput = new SubscriptionConfigurationInput();
        subscriptionConfigurationInput.setEntrypointConfiguration("{\"url\":\"my-url\"}");
        updateSubscriptionInput.setConfiguration(subscriptionConfigurationInput);

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        SubscriptionConfigurationEntity subscriptionConfigurationEntity = new SubscriptionConfigurationEntity();
        subscriptionConfigurationEntity.setEntrypointConfiguration("{\"url\":\"my-url\"}");
        subscriptionEntity.setConfiguration(subscriptionConfigurationEntity);
        when(
            subscriptionService.update(eq(GraviteeContext.getExecutionContext()), any(UpdateSubscriptionConfigurationEntity.class))
        ).thenReturn(subscriptionEntity);

        Response response = target(SUBSCRIPTION).request().put(json(updateSubscriptionInput));

        assertEquals(200, response.getStatus());

        SubscriptionEntity subscriptionEntityResponse = response.readEntity(SubscriptionEntity.class);
        assertEquals("{\"url\":\"my-url\"}", subscriptionEntityResponse.getConfiguration().getEntrypointConfiguration());

        ArgumentCaptor<UpdateSubscriptionConfigurationEntity> subscriptionCaptor = ArgumentCaptor.forClass(
            UpdateSubscriptionConfigurationEntity.class
        );
        verify(subscriptionService).update(eq(GraviteeContext.getExecutionContext()), subscriptionCaptor.capture());
        assertEquals(SUBSCRIPTION, subscriptionCaptor.getValue().getSubscriptionId());
    }

    @Test
    public void shouldPauseSubscriptionByConsumer() {
        Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "STOPPED").request().post(null);

        assertEquals(200, response.getStatus());
        verify(subscriptionService, times(1)).pauseConsumer(eq(GraviteeContext.getExecutionContext()), eq(subscriptionEntity.getId()));
    }

    @Test
    public void shouldResumeSubscriptionByConsumer() {
        Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "STARTED").request().post(null);

        assertEquals(200, response.getStatus());
        verify(subscriptionService, times(1)).resumeConsumer(eq(GraviteeContext.getExecutionContext()), eq(subscriptionEntity.getId()));
    }

    @Test
    public void shouldHaveBadRequestIfTryingAWrongConsumerStatus() {
        Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "INVALID").request().post(null);

        assertEquals(400, response.getStatus());
        verify(subscriptionService, times(0)).pauseConsumer(eq(GraviteeContext.getExecutionContext()), eq(subscriptionEntity.getId()));
        verify(subscriptionService, times(0)).resumeConsumer(eq(GraviteeContext.getExecutionContext()), eq(subscriptionEntity.getId()));
    }

    @Test
    public void shouldBeForbiddenWhenUpdatingConsumerStatus() {
        reset(permissionService);

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.UPDATE)
            );
        Response response = target(SUBSCRIPTION).path("_changeConsumerStatus").queryParam("status", "STOPPED").request().post(null);

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldReturnEmptyWhenNoAppsFound() {
        when(applicationService.findByUser(any(), any())).thenReturn(Collections.emptySet());

        final Response response = target().queryParam("apiId", API).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldThrowForbiddenWhenNoPermission() {
        doReturn(false).when(permissionService).hasPermission(any(), any(), any(), any());

        final Response response = target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get();

        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void shouldReturnEmptyWhenNoSubscriptions() {
        doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());
        doReturn(Collections.emptyList()).when(subscriptionService).search(any(), any());

        final Response response = target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldReturnMappedSubscriptions() {
        doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId("sub-id");
        doReturn(Collections.singletonList(subscription)).when(subscriptionService).search(any(), any());
        doReturn(new Subscription()).when(subscriptionMapper).convert(any());
        doReturn(new Metadata()).when(subscriptionService).getMetadata(any(), any());

        final Response response = target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldReturnAllSubscriptionsWhenPaginationIsDisabled() {
        doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId("sub-id");
        doReturn(Collections.singletonList(subscription)).when(subscriptionService).search(any(), any());
        doReturn(new Subscription()).when(subscriptionMapper).convert(any());
        doReturn(new Metadata()).when(subscriptionService).getMetadata(any(), any());

        final Response response = target()
            .queryParam("apiId", API)
            .queryParam("applicationId", APPLICATION)
            .queryParam("size", -1)
            .request()
            .get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Getter
    @AllArgsConstructor
    private class SubscriptionConfiguration {

        private String url;
    }
}
