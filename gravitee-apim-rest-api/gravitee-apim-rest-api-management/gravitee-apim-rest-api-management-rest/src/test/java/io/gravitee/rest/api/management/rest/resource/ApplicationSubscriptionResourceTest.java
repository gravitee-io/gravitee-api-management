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
package io.gravitee.rest.api.management.rest.resource;

import static jakarta.ws.rs.client.Entity.json;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionConsumerStatus;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ApplicationSubscriptionResourceTest extends AbstractResourceTest {

    private SubscriptionEntity fakeSubscriptionEntity;
    private ApplicationEntity fakeApplicationEntity;
    private static final String APPLICATION_ID = "my-application";
    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String PLAN_ID = "my-plan";
    private static final String API_ID = "my-api";

    @Override
    protected String contextPath() {
        return "applications/" + APPLICATION_ID + "/subscriptions/";
    }

    @Before
    public void init() {
        reset(subscriptionService, userService, applicationService, planSearchService, apiSearchServiceV4, permissionService);

        fakeSubscriptionEntity = new SubscriptionEntity();
        fakeSubscriptionEntity.setId(SUBSCRIPTION_ID);
        fakeSubscriptionEntity.setApi(API_ID);
        fakeSubscriptionEntity.setReferenceId(API_ID);
        fakeSubscriptionEntity.setReferenceType("API");
        fakeSubscriptionEntity.setPlan(PLAN_ID);
        fakeSubscriptionEntity.setApplication(APPLICATION_ID);

        fakeApplicationEntity = new ApplicationEntity();
        fakeApplicationEntity.setId(APPLICATION_ID);

        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(fakeApplicationEntity);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void shouldGetApplicationSubscription_onPlanV2() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any(), anyBoolean())).thenReturn(mock(UserEntity.class));

        ApiEntity apiV2 = new ApiEntity();
        apiV2.setPrimaryOwner(new PrimaryOwnerEntity());
        when(
            apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID), eq(false), eq(false), eq(false))
        ).thenReturn(apiV2);

        PlanEntity planV2 = new PlanEntity();
        planV2.setSecurity(PlanSecurityType.OAUTH2);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV2);

        Response response = envTarget(SUBSCRIPTION_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        JsonNode responseBody = response.readEntity(JsonNode.class);
        assertEquals(PlanSecurityType.OAUTH2.name(), responseBody.get("plan").get("security").asText());
    }

    @Test
    public void shouldGetApplicationSubscription_onPlanV4() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any(), anyBoolean())).thenReturn(mock(UserEntity.class));

        io.gravitee.rest.api.model.v4.api.ApiEntity apiV4 = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiV4.setPrimaryOwner(new PrimaryOwnerEntity());
        when(
            apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID), eq(false), eq(false), eq(false))
        ).thenReturn(apiV4);

        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType("oauth2");
        planV4.setSecurity(planSecurity);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        Response response = envTarget(SUBSCRIPTION_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        JsonNode responseBody = response.readEntity(JsonNode.class);
        assertEquals(PlanSecurityType.OAUTH2.name(), responseBody.get("plan").get("security").asText());
    }

    @Test
    public void shouldUpdateSubscriptionConfiguration() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);
        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();

        when(
            subscriptionService.update(eq(GraviteeContext.getExecutionContext()), any(UpdateSubscriptionConfigurationEntity.class))
        ).thenReturn(new SubscriptionEntity());

        Response response = envTarget(SUBSCRIPTION_ID).request().put(json(updateSubscriptionConfigurationEntity));

        assertEquals(200, response.getStatus());
        verify(subscriptionService).findById(SUBSCRIPTION_ID);
        verify(subscriptionService).update(eq(GraviteeContext.getExecutionContext()), any(UpdateSubscriptionConfigurationEntity.class));
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    public void shouldResumeSubscriptionByConsumer() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);
        io.gravitee.rest.api.model.v4.api.ApiEntity apiV4 = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiV4.setPrimaryOwner(new PrimaryOwnerEntity());
        when(
            apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID), eq(false), eq(false), eq(false))
        ).thenReturn(apiV4);

        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any(), anyBoolean())).thenReturn(mock(UserEntity.class));
        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType("oauth2");
        planV4.setSecurity(planSecurity);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        fakeSubscriptionEntity.setConsumerStatus(SubscriptionConsumerStatus.STARTED);
        when(subscriptionService.resumeConsumer(any(), any())).thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID).path("_changeConsumerStatus").queryParam("status", "STARTED").request().post(null);

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        JsonNode responseBody = response.readEntity(JsonNode.class);
        assertEquals("STARTED", responseBody.get("consumerStatus").asText());
        verify(subscriptionService, times(1)).resumeConsumer(any(), any());
    }

    @Test
    public void shouldPauseSubscriptionByConsumer() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);
        io.gravitee.rest.api.model.v4.api.ApiEntity apiV4 = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiV4.setPrimaryOwner(new PrimaryOwnerEntity());
        when(
            apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID), eq(false), eq(false), eq(false))
        ).thenReturn(apiV4);

        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any(), anyBoolean())).thenReturn(mock(UserEntity.class));
        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType("oauth2");
        planV4.setSecurity(planSecurity);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        fakeSubscriptionEntity.setConsumerStatus(SubscriptionConsumerStatus.STOPPED);
        when(subscriptionService.pauseConsumer(any(), any())).thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID).path("_changeConsumerStatus").queryParam("status", "STOPPED").request().post(null);

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        JsonNode responseBody = response.readEntity(JsonNode.class);
        assertEquals("STOPPED", responseBody.get("consumerStatus").asText());
        verify(subscriptionService, times(1)).pauseConsumer(any(), any());
    }

    @Test
    public void shouldHaveBadRequestIfTryingAWrongConsumerStatus() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID).path("_changeConsumerStatus").queryParam("status", "INVALID").request().post(null);

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotGetApplicationSubscriptionBecauseDoesNotBelongToApplication() {
        fakeSubscriptionEntity.setApplication("Another_application");
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID).request().get();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotUpdateApplicationSubscriptionBecauseDoesNotBelongToApplication() {
        fakeSubscriptionEntity.setApplication("Another_application");
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);
        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();

        Response response = envTarget(SUBSCRIPTION_ID).request().put(json(updateSubscriptionConfigurationEntity));

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotChangeConsumerStatusOfApplicationSubscriptionBecauseDoesNotBelongToApplication() {
        fakeSubscriptionEntity.setApplication("Another_application");
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID).path("_changeConsumerStatus").queryParam("status", "STARTED").request().post(null);

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }
}
