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
package io.gravitee.rest.api.management.rest.resource;

import static io.gravitee.rest.api.model.SubscriptionStatus.CLOSED;
import static io.gravitee.rest.api.model.v4.plan.PlanValidationType.AUTO;
import static io.gravitee.rest.api.model.v4.plan.PlanValidationType.MANUAL;
import static javax.ws.rs.client.Entity.json;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.repository.management.model.Subscription;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.util.Map;
import javax.ws.rs.core.Response;
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
    public void setUp() {
        reset(subscriptionService, userService, applicationService, planSearchService, apiSearchServiceV4, permissionService);

        fakeSubscriptionEntity = new SubscriptionEntity();
        fakeSubscriptionEntity.setId(SUBSCRIPTION_ID);
        fakeSubscriptionEntity.setApi(API_ID);
        fakeSubscriptionEntity.setPlan(PLAN_ID);

        fakeApplicationEntity = new ApplicationEntity();
        fakeApplicationEntity.setId(APPLICATION_ID);

        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(fakeApplicationEntity);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void shouldGetApplicationSubscription_onPlanV3() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any(), anyBoolean())).thenReturn(mock(UserEntity.class));

        ApiEntity apiV3 = new ApiEntity();
        apiV3.setPrimaryOwner(new PrimaryOwnerEntity());
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID))).thenReturn(apiV3);

        PlanEntity planV3 = new PlanEntity();
        planV3.setSecurity(PlanSecurityType.OAUTH2);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV3);

        Response response = envTarget(SUBSCRIPTION_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        JsonNode responseBody = response.readEntity(JsonNode.class);
        assertEquals("oauth2", responseBody.get("plan").get("security").asText());
    }

    @Test
    public void shouldGetApplicationSubscription_onPlanV4() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any(), anyBoolean())).thenReturn(mock(UserEntity.class));

        io.gravitee.rest.api.model.v4.api.ApiEntity apiV4 = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiV4.setPrimaryOwner(new PrimaryOwnerEntity());
        when(apiSearchServiceV4.findGenericById(eq(GraviteeContext.getExecutionContext()), eq(API_ID))).thenReturn(apiV4);

        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType("oauth2");
        planV4.setSecurity(planSecurity);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        Response response = envTarget(SUBSCRIPTION_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        JsonNode responseBody = response.readEntity(JsonNode.class);
        assertEquals("oauth2", responseBody.get("plan").get("security").asText());
    }

    @Test
    public void shouldNotUpdateSubscriptionCauseNotFound() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(SubscriptionNotFoundException.class);

        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        Response response = envTarget(SUBSCRIPTION_ID).request().put(json(updateSubscriptionConfigurationEntity));

        assertEquals(404, response.getStatus());
        verify(subscriptionService).findById(SUBSCRIPTION_ID);
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    public void shouldNotUpdateSubscriptionCauseClosed() {
        fakeSubscriptionEntity.setStatus(CLOSED);
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        Response response = envTarget(SUBSCRIPTION_ID).request().put(json(updateSubscriptionConfigurationEntity));

        assertEquals(400, response.getStatus());
        verify(subscriptionService).findById(SUBSCRIPTION_ID);
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    public void shouldUpdateSubscriptionConfigurationOnPlanWithAutomaticValidation() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        planV4.setValidation(AUTO);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setConfiguration("{\"my\":\"configuration\"}");
        updateSubscriptionConfigurationEntity.setFilter("my-filter");
        updateSubscriptionConfigurationEntity.setMetadata(Map.of("my-metadata", "my-value"));
        Response response = envTarget(SUBSCRIPTION_ID).request().put(json(updateSubscriptionConfigurationEntity));

        assertEquals(200, response.getStatus());
        verify(subscriptionService).findById(SUBSCRIPTION_ID);

        // verify subscription has been updated without any status change
        verify(subscriptionService)
            .update(
                eq(GraviteeContext.getExecutionContext()),
                argThat(
                    sub ->
                        sub.getConfiguration().equals("{\"my\":\"configuration\"}") &&
                        sub.getFilter().equals("my-filter") &&
                        sub.getMetadata().equals(Map.of("my-metadata", "my-value"))
                ),
                (Subscription.Status) eq(null)
            );
        verifyNoMoreInteractions(subscriptionService);
    }

    @Test
    public void shouldUpdateSubscriptionConfigurationOnPlanWithManualValidation() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        planV4.setValidation(MANUAL);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        UpdateSubscriptionConfigurationEntity updateSubscriptionConfigurationEntity = new UpdateSubscriptionConfigurationEntity();
        updateSubscriptionConfigurationEntity.setConfiguration("{\"my\":\"configuration\"}");
        updateSubscriptionConfigurationEntity.setFilter("my-filter");
        updateSubscriptionConfigurationEntity.setMetadata(Map.of("my-metadata", "my-value"));
        Response response = envTarget(SUBSCRIPTION_ID).request().put(json(updateSubscriptionConfigurationEntity));

        assertEquals(200, response.getStatus());
        verify(subscriptionService).findById(SUBSCRIPTION_ID);

        // verify subscription has been updated with PENDING status
        verify(subscriptionService)
            .update(
                eq(GraviteeContext.getExecutionContext()),
                argThat(
                    sub ->
                        sub.getConfiguration().equals("{\"my\":\"configuration\"}") &&
                        sub.getFilter().equals("my-filter") &&
                        sub.getMetadata().equals(Map.of("my-metadata", "my-value"))
                ),
                eq(Subscription.Status.PENDING)
            );
        verifyNoMoreInteractions(subscriptionService);
    }
}
