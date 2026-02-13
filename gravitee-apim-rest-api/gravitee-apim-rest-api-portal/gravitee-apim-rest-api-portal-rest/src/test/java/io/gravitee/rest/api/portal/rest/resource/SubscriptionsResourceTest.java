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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.ApiKeyModeEnum;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionConfigurationInput;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.portal.rest.model.SubscriptionsResponse;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.v4.exception.SubscriptionMetadataInvalidException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsResourceTest extends AbstractResourceTest {

    private static final String SUBSCRIPTION = "my-subscription";
    private static final String ANOTHER_SUBSCRIPTION = "my-other-subscription";
    private static final String API = "my-api";
    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";

    @Override
    protected String contextPath() {
        return "subscriptions";
    }

    @BeforeEach
    public void init() {
        resetAllMocks();

        SubscriptionEntity subscriptionEntity1 = new SubscriptionEntity();
        subscriptionEntity1.setId(SUBSCRIPTION);
        subscriptionEntity1.setStatus(SubscriptionStatus.ACCEPTED);
        SubscriptionEntity subscriptionEntity2 = new SubscriptionEntity();
        subscriptionEntity2.setId(ANOTHER_SUBSCRIPTION);
        subscriptionEntity2.setStatus(SubscriptionStatus.ACCEPTED);
        final Page<SubscriptionEntity> subscriptionPage = new Page<>(asList(subscriptionEntity1, subscriptionEntity2), 0, 1, 2);
        doReturn(subscriptionPage.getContent()).when(subscriptionService).search(eq(GraviteeContext.getExecutionContext()), any());
        doReturn(subscriptionPage).when(subscriptionService).search(any(), any(), any());

        SubscriptionEntity createdSubscription = new SubscriptionEntity();
        createdSubscription.setId(SUBSCRIPTION);
        createdSubscription.setStatus(SubscriptionStatus.ACCEPTED);
        doReturn(createdSubscription).when(subscriptionService).create(eq(GraviteeContext.getExecutionContext()), any());

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setApi(API);
        subscriptionEntity.setApplication(APPLICATION);
        doReturn(subscriptionEntity).when(subscriptionService).findById(eq(SUBSCRIPTION));
        doReturn(true).when(permissionService).hasPermission(any(), any(), any(), any());

        PlanEntity planEntity = new PlanEntity();
        planEntity.setApi(API);
        doReturn(planEntity).when(planSearchService).findById(GraviteeContext.getExecutionContext(), PLAN);
    }

    @Test
    public void shouldGetSubscriptionsForApi() {
        final ApplicationListItem application = new ApplicationListItem();
        application.setId(APPLICATION);

        doReturn(newSet(application)).when(applicationService).findByUser(eq(GraviteeContext.getExecutionContext()), any());

        Metadata metadata = new Metadata();
        metadata.put("api-id", "name", "My api");
        doReturn(metadata).when(subscriptionService).getMetadata(eq(GraviteeContext.getExecutionContext()), any());

        final Response response = target().queryParam("apiId", API).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        SubscriptionsResponse subscriptionResponse = response.readEntity(SubscriptionsResponse.class);
        assertEquals(2, subscriptionResponse.getData().size());
    }

    @Test
    public void shouldGetNoSubscription() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        SubscriptionsResponse subscriptionResponse = response.readEntity(SubscriptionsResponse.class);
        assertEquals(0, subscriptionResponse.getData().size());
    }

    @Test
    public void shouldGetNoPublishedApiAndNoLink() {
        final Page<SubscriptionEntity> subscriptionPage = new Page<>(emptyList(), 0, 1, 2);
        doReturn(subscriptionPage).when(subscriptionService).search(any(), any(), any());

        //Test with default limit
        final Response response = target().queryParam("apiId", API).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());

        SubscriptionsResponse subscriptionResponse = response.readEntity(SubscriptionsResponse.class);
        assertEquals(0, subscriptionResponse.getData().size());

        Links links = subscriptionResponse.getLinks();
        assertNull(links);

        //Test with small limit
        final Response anotherResponse = target().queryParam("apiId", API).queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());

        subscriptionResponse = anotherResponse.readEntity(SubscriptionsResponse.class);
        assertEquals(0, subscriptionResponse.getData().size());

        links = subscriptionResponse.getLinks();
        assertNull(links);
    }

    @Test
    public void shouldCreateSubscription() {
        SubscriptionConfigurationInput configuration = new SubscriptionConfigurationInput();
        configuration.setEntrypointConfiguration(new SubscriptionConfiguration("my-url"));
        SubscriptionInput subscriptionInput = new SubscriptionInput()
            .application(APPLICATION)
            .plan(PLAN)
            .metadata(Map.of("my-metadata", "my-value"))
            ._configuration(configuration)
            .request("request")
            .apiKeyMode(ApiKeyModeEnum.EXCLUSIVE);

        final ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
        final Key key = new Key();
        when(apiKeyService.findBySubscription(GraviteeContext.getExecutionContext(), SUBSCRIPTION)).thenReturn(
            Collections.singletonList(apiKeyEntity)
        );
        when(keyMapper.convert(apiKeyEntity)).thenReturn(key);

        final Response response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<NewSubscriptionEntity> argument = ArgumentCaptor.forClass(NewSubscriptionEntity.class);
        Mockito.verify(subscriptionService).create(eq(GraviteeContext.getExecutionContext()), argument.capture());
        NewSubscriptionEntity newSubscriptionEntity = argument.getValue();
        assertEquals(APPLICATION, newSubscriptionEntity.getApplication());
        assertEquals(PLAN, newSubscriptionEntity.getPlan());
        assertEquals("request", newSubscriptionEntity.getRequest());
        assertEquals(Map.of("my-metadata", "my-value"), newSubscriptionEntity.getMetadata());
        assertEquals("{\"url\":\"my-url\"}", newSubscriptionEntity.getConfiguration().getEntrypointConfiguration());
        assertEquals(ApiKeyMode.EXCLUSIVE, argument.getValue().getApiKeyMode());

        final Subscription subscriptionResponse = response.readEntity(Subscription.class);
        assertNotNull(subscriptionResponse);
        assertEquals(SUBSCRIPTION, subscriptionResponse.getId());
        assertNotNull(subscriptionResponse.getKeys());
        assertEquals(1, subscriptionResponse.getKeys().size());
        assertEquals(key, subscriptionResponse.getKeys().get(0));
    }

    @Test
    public void shouldReturnBadRequestWhenMetadataKeyIsInvalid() {
        doThrow(new SubscriptionMetadataInvalidException("Invalid metadata key."))
            .when(subscriptionService)
            .create(
                eq(GraviteeContext.getExecutionContext()),
                argThat(e -> e.getMetadata() != null && e.getMetadata().containsKey("bad key"))
            );

        SubscriptionInput subscriptionInput = new SubscriptionInput()
            .application(APPLICATION)
            .plan(PLAN)
            .metadata(Map.of("bad key", "value"));

        final Response response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        Mockito.verify(subscriptionService, times(1)).create(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldHaveBadRequestWhileCreatingSubscription() {
        final Response response = target().request().post(Entity.json(null));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void testPermissionsForCreation() {
        reset(permissionService);

        SubscriptionInput subscriptionInput = new SubscriptionInput()
            .application(APPLICATION)
            .plan(PLAN)
            .request("request")
            .apiKeyMode(ApiKeyModeEnum.EXCLUSIVE);

        doReturn(true)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.CREATE)
            );
        Response response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(OK_200, response.getStatus());

        doReturn(false)
            .when(permissionService)
            .hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.APPLICATION_SUBSCRIPTION),
                eq(APPLICATION),
                eq(RolePermissionAction.CREATE)
            );
        response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(FORBIDDEN_403, response.getStatus());
    }

    @Test
    public void testPermissionsForListing() {
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

        Metadata metadata = new Metadata();
        metadata.put("api-id", "name", "My api");
        doReturn(metadata).when(subscriptionService).getMetadata(eq(GraviteeContext.getExecutionContext()), any());

        assertEquals(OK_200, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());

        //-----

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

        assertEquals(FORBIDDEN_403, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());

        assertEquals(FORBIDDEN_403, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());

        //----

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

        assertEquals(OK_200, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());

        //----

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

        assertEquals(FORBIDDEN_403, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(FORBIDDEN_403, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());
    }

    @Getter
    @AllArgsConstructor
    private class SubscriptionConfiguration {

        private String url;
    }
}
