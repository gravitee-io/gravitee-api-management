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

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.portal.rest.model.SubscriptionsResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.internal.util.collections.Sets.newSet;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "subscriptions";
    }
    private static final String SUBSCRIPTION = "my-subscription";
    private static final String ANOTHER_SUBSCRIPTION = "my-other-subscription";
    private static final String API = "my-api";
    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";

    @Before
    public void init() {
        resetAllMocks();

        SubscriptionEntity subscriptionEntity1 = new SubscriptionEntity();
        subscriptionEntity1.setId(SUBSCRIPTION);
        SubscriptionEntity subscriptionEntity2 = new SubscriptionEntity();
        subscriptionEntity2.setId(ANOTHER_SUBSCRIPTION);
        final Page<SubscriptionEntity> subscriptionPage =
                new Page<>(asList(subscriptionEntity1, subscriptionEntity2), 0, 1, 2);
        doReturn(subscriptionPage.getContent()).when(subscriptionService).search(any());
        doReturn(subscriptionPage).when(subscriptionService).search(any(), any());

        doReturn(new Subscription().id(SUBSCRIPTION)).when(subscriptionMapper).convert(subscriptionEntity1);
        doReturn(new Subscription().id(ANOTHER_SUBSCRIPTION)).when(subscriptionMapper).convert(subscriptionEntity2);

        SubscriptionEntity createdSubscription = new SubscriptionEntity();
        createdSubscription.setId(SUBSCRIPTION);
        doReturn(createdSubscription).when(subscriptionService).create(any());

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setApi(API);
        subscriptionEntity.setApplication(APPLICATION);
        doReturn(subscriptionEntity).when(subscriptionService).findById(eq(SUBSCRIPTION));
        doReturn(true).when(permissionService).hasPermission(any(),  any(),  any());

        PlanEntity planEntity = new PlanEntity();
        planEntity.setApi(API);
        doReturn(planEntity).when(planService).findById(PLAN);
    }

    @Test
    public void shouldGetSubscriptionsForApi() {
        final ApplicationListItem application = new ApplicationListItem();
        application.setId(APPLICATION);
        doReturn(newSet(application)).when(applicationService).findByUser(any());

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
        doReturn(subscriptionPage).when(subscriptionService).search(any(), any());

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
        SubscriptionInput subscriptionInput = new SubscriptionInput()
                .application(APPLICATION)
                .plan(PLAN)
                .request("request")
                ;

        final Response response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<NewSubscriptionEntity> argument = ArgumentCaptor.forClass(NewSubscriptionEntity.class);
        Mockito.verify(subscriptionService).create(argument.capture());
        assertEquals(APPLICATION, argument.getValue().getApplication());
        assertEquals(PLAN, argument.getValue().getPlan());
        assertEquals("request", argument.getValue().getRequest());

        final Subscription subscriptionResponse = response.readEntity(Subscription.class);
        assertNotNull(subscriptionResponse);
        assertEquals(SUBSCRIPTION, subscriptionResponse.getId());

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
                ;

        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION,  APPLICATION,  RolePermissionAction.CREATE);
        Response response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(OK_200, response.getStatus());

        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION,  APPLICATION,  RolePermissionAction.CREATE);
        response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(FORBIDDEN_403, response.getStatus());

    }

    @Test
    public void testPermissionsForListing() {
        reset(permissionService);

        doReturn(true).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION,  API,  RolePermissionAction.READ);
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION,  APPLICATION,  RolePermissionAction.READ);

        assertEquals(OK_200, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());

        //-----

        doReturn(true).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION,  API,  RolePermissionAction.READ);
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION,  APPLICATION,  RolePermissionAction.READ);

        assertEquals(FORBIDDEN_403, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());

        assertEquals(FORBIDDEN_403, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());

        //----

        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION,  API,  RolePermissionAction.READ);
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION,  APPLICATION,  RolePermissionAction.READ);

        assertEquals(OK_200, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());

        //----

        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION,  API,  RolePermissionAction.READ);
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION,  APPLICATION,  RolePermissionAction.READ);

        assertEquals(FORBIDDEN_403, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(FORBIDDEN_403, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());
    }
}
