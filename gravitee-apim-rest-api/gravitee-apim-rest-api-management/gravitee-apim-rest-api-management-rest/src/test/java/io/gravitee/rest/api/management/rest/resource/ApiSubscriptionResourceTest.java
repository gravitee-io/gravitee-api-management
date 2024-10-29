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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import com.fasterxml.jackson.databind.JsonNode;
import fixtures.core.model.PlanFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.PlanCrudServiceInMemory;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.use_case.RejectSubscriptionUseCase;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.ProcessSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.TransferSubscriptionEntity;
import io.gravitee.rest.api.model.UpdateSubscriptionEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscriptionResourceTest extends AbstractResourceTest {

    @Autowired
    SubscriptionCrudServiceInMemory subscriptionCrudService;

    @Autowired
    PlanCrudServiceInMemory planCrudService;

    @Autowired
    RejectSubscriptionUseCase rejectSubscriptionUseCase;

    private ApiKeyEntity fakeApiKeyEntity;
    private SubscriptionEntity fakeSubscriptionEntity;
    private UserEntity fakeUserEntity;
    private PlanEntity fakePlanEntity;
    private ApplicationEntity fakeApplicationEntity;
    private static final String API_NAME = "my-api";
    private static final String SUBSCRIPTION_ID = "subscriptionId";
    private static final String PLAN_ID = "my-plan";
    private static final String FAKE_KEY = "fakeKey";

    @Override
    protected String contextPath() {
        return "apis/" + API_NAME + "/subscriptions/";
    }

    @Before
    public void init() {
        reset(apiKeyService);
        reset(subscriptionService);
        reset(userService);
        reset(planService);
        reset(applicationService);
        reset(parameterService);
        fakeApiKeyEntity = new ApiKeyEntity();
        fakeApiKeyEntity.setKey(FAKE_KEY);

        fakeSubscriptionEntity = new SubscriptionEntity();
        fakeSubscriptionEntity.setId(SUBSCRIPTION_ID);
        fakeSubscriptionEntity.setPlan(PLAN_ID);
        fakeSubscriptionEntity.setApi(API_NAME);

        fakeUserEntity = new UserEntity();
        fakeUserEntity.setFirstname("firstName");
        fakeUserEntity.setLastname("lastName");

        fakePlanEntity = new PlanEntity();
        fakePlanEntity.setId(PLAN_ID);
        fakePlanEntity.setName("planName");

        fakeApplicationEntity = new ApplicationEntity();
        fakeApplicationEntity.setId("applicationId");
        fakeApplicationEntity.setName("applicationName");
        fakeApplicationEntity.setType("applicationType");
        fakeApplicationEntity.setDescription("applicationDescription");
        fakeApplicationEntity.setApiKeyMode(ApiKeyMode.UNSPECIFIED);
        fakeApplicationEntity.setPrimaryOwner(new PrimaryOwnerEntity(fakeUserEntity));

        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any(), anyBoolean())).thenReturn(fakeUserEntity);
        when(planService.findById(any(), any())).thenReturn(fakePlanEntity);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(fakeApplicationEntity);
        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @Test
    public void shouldReject() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.aPlanHttpV4().toBuilder().id(PLAN_ID).build().setPlanStatus(PlanStatus.PUBLISHED));
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .apiId(API_NAME)
                .subscribedBy("subscriber")
                .planId(plan.getId())
                .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.PENDING)
                .build()
        );
        when(planSearchService.findById(any(), any())).thenReturn(PlanEntity.builder().id(plan.getId()).name(plan.getName()).build());

        ProcessSubscriptionEntity processSubscriptionEntity = new ProcessSubscriptionEntity();
        processSubscriptionEntity.setId(subscription.getId());
        processSubscriptionEntity.setCustomApiKey("customApiKey");

        Response response = envTarget(SUBSCRIPTION_ID + "/_process").request().post(Entity.json(processSubscriptionEntity));

        MAPIAssertions.assertThat(response).hasStatus(OK_200).asJson().extracting(json -> json.getString("status")).isEqualTo("REJECTED");
    }

    @Test
    public void shouldAccept() {
        // Given
        var plan = givenExistingPlan(PlanFixtures.aPlanHttpV4().toBuilder().id(PLAN_ID).build().setPlanStatus(PlanStatus.PUBLISHED));
        var subscription = givenExistingSubscription(
            SubscriptionFixtures
                .aSubscription()
                .toBuilder()
                .id(SUBSCRIPTION_ID)
                .apiId(API_NAME)
                .subscribedBy("subscriber")
                .planId(plan.getId())
                .status(io.gravitee.apim.core.subscription.model.SubscriptionEntity.Status.PENDING)
                .build()
        );
        when(planSearchService.findById(any(), any())).thenReturn(PlanEntity.builder().id(plan.getId()).name(plan.getName()).build());

        ProcessSubscriptionEntity processSubscriptionEntity = new ProcessSubscriptionEntity();
        processSubscriptionEntity.setId(subscription.getId());
        processSubscriptionEntity.setAccepted(true);
        processSubscriptionEntity.setCustomApiKey("customApiKey");

        Response response = envTarget(SUBSCRIPTION_ID + "/_process").request().post(Entity.json(processSubscriptionEntity));

        MAPIAssertions.assertThat(response).hasStatus(OK_200).asJson().extracting(json -> json.getString("status")).isEqualTo("ACCEPTED");
    }

    @Test
    public void shouldNotProcessIfBadId() {
        ProcessSubscriptionEntity processSubscriptionEntity = new ProcessSubscriptionEntity();
        processSubscriptionEntity.setId("badId");
        processSubscriptionEntity.setCustomApiKey("customApiKey");

        Response response = envTarget(SUBSCRIPTION_ID + "/_process").request().post(Entity.json(processSubscriptionEntity));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotProcessIfNotValidApiKey() {
        ProcessSubscriptionEntity processSubscriptionEntity = new ProcessSubscriptionEntity();
        processSubscriptionEntity.setId(SUBSCRIPTION_ID);
        processSubscriptionEntity.setCustomApiKey("customApiKey;^");

        Response response = envTarget(SUBSCRIPTION_ID + "/_process").request().post(Entity.json(processSubscriptionEntity));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void getApiSubscription_onPlanV3() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        PlanEntity planV3 = new PlanEntity();
        planV3.setSecurity(PlanSecurityType.OAUTH2);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV3);

        Response response = envTarget(SUBSCRIPTION_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        JsonNode responseBody = response.readEntity(JsonNode.class);
        assertEquals(PlanSecurityType.OAUTH2.name(), responseBody.get("plan").get("security").asText());
    }

    @Test
    public void getApiSubscription_onPlanV4() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        PlanSecurity planSecurity = new PlanSecurity();
        planSecurity.setType("oauth2");
        planV4.setSecurity(planSecurity);
        planV4.setMode(PlanMode.STANDARD);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        Response response = envTarget(SUBSCRIPTION_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        JsonNode responseBody = response.readEntity(JsonNode.class);
        assertEquals("oauth2", responseBody.get("plan").get("security").asText());
    }

    @Test
    public void getApiSubscription_onPushPlanV4() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        io.gravitee.rest.api.model.v4.plan.PlanEntity planV4 = new io.gravitee.rest.api.model.v4.plan.PlanEntity();
        planV4.setMode(PlanMode.PUSH);
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID))).thenReturn(planV4);

        Response response = envTarget(SUBSCRIPTION_ID).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        JsonNode responseBody = response.readEntity(JsonNode.class);
        assertNull(responseBody.get("plan").get("security"));
    }

    @Test
    public void getNotGetApiSubscriptionIfSubscriptionDoesNotBelongToApi() {
        fakeSubscriptionEntity.setApi("Another_api");
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID).request().get();

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotTransferIfSubscriptionDoesNotBelongToApi() {
        TransferSubscriptionEntity transferSubscriptionEntity = new TransferSubscriptionEntity();
        transferSubscriptionEntity.setId(SUBSCRIPTION_ID);
        transferSubscriptionEntity.setPlan(PLAN_ID);

        fakeSubscriptionEntity.setApi("Another_api");
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID + "/_transfer").request().post(Entity.json(transferSubscriptionEntity));

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void shouldNotTUpdateIfSubscriptionDoesNotBelongToApi() {
        UpdateSubscriptionEntity updateSubscriptionEntity = new UpdateSubscriptionEntity();
        updateSubscriptionEntity.setId(SUBSCRIPTION_ID);

        fakeSubscriptionEntity.setApi("Another_api");
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID).request().put(Entity.json(updateSubscriptionEntity));

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    private io.gravitee.apim.core.subscription.model.SubscriptionEntity givenExistingSubscription(
        io.gravitee.apim.core.subscription.model.SubscriptionEntity subscription
    ) {
        subscriptionCrudService.initWith(List.of(subscription));
        return subscription;
    }

    private Plan givenExistingPlan(Plan plan) {
        planCrudService.initWith(List.of(plan));
        return plan;
    }
}
