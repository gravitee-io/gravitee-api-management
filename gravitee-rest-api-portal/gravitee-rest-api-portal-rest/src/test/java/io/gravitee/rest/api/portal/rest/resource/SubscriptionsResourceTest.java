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

import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.portal.rest.model.DataResponse;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.ErrorResponse;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;

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
    
    private Page<SubscriptionEntity> subscriptionEntityPage;
    
    @Before
    public void init() {
        resetAllMocks();
        
        SubscriptionEntity subscriptionEntity1 = new SubscriptionEntity();
        subscriptionEntity1.setId(SUBSCRIPTION);
        SubscriptionEntity subscriptionEntity2 = new SubscriptionEntity();
        subscriptionEntity2.setId(ANOTHER_SUBSCRIPTION);
        subscriptionEntityPage = new Page<SubscriptionEntity>(Arrays.asList(subscriptionEntity1, subscriptionEntity2), 1, 2, 2);
        doReturn(subscriptionEntityPage).when(subscriptionService).search(any(), any());

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
        final Response response = target().queryParam("apiId", API).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        DataResponse subscriptionResponse = response.readEntity(DataResponse.class);
        assertEquals(2, subscriptionResponse.getData().size());
    }
    
    @Test
    public void shouldGetNoSubscription() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        
        ErrorResponse errorResponse = response.readEntity(ErrorResponse.class);
        List<Error> errors = errorResponse.getErrors();
        assertNotNull(errors);
        assertEquals(1, errors.size());
        Error error = errors.get(0);
        assertEquals("400", error.getCode());
        assertEquals("javax.ws.rs.BadRequestException", error.getTitle());
        assertEquals("At least an api or an application must be provided.", error.getDetail());
    }
    
    @Test
    public void shouldGetNoPublishedApiAndNoLink() {
        doReturn(new Page<SubscriptionEntity>(Collections.EMPTY_LIST, 1, 0, 0)).when(subscriptionService).search(any(), any());

        //Test with default limit
        final Response response = target().queryParam("apiId", API).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        DataResponse subscriptionResponse = response.readEntity(DataResponse.class);
        assertEquals(0, subscriptionResponse.getData().size());
        
        Links links = subscriptionResponse.getLinks();
        assertNull(links);
        
        //Test with small limit
        final Response anotherResponse = target().queryParam("apiId", API).queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());
        
        subscriptionResponse = anotherResponse.readEntity(DataResponse.class);
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
        assertEquals(OK_200, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());
        
        //----
        
        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION,  API,  RolePermissionAction.READ);
        doReturn(true).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION,  APPLICATION,  RolePermissionAction.READ);

        assertEquals(OK_200, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(FORBIDDEN_403, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(OK_200, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());
        
        //----
        
        doReturn(false).when(permissionService).hasPermission(RolePermission.API_SUBSCRIPTION,  API,  RolePermissionAction.READ);
        doReturn(false).when(permissionService).hasPermission(RolePermission.APPLICATION_SUBSCRIPTION,  APPLICATION,  RolePermissionAction.READ);

        assertEquals(FORBIDDEN_403, target().queryParam("applicationId", APPLICATION).request().get().getStatus());
        assertEquals(FORBIDDEN_403, target().queryParam("apiId", API).request().get().getStatus());
        assertEquals(FORBIDDEN_403, target().queryParam("apiId", API).queryParam("applicationId", APPLICATION).request().get().getStatus());
    }
}
