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

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscriptionResourceTest extends AbstractResourceTest {

    private ApiKeyEntity fakeApiKeyEntity;
    private SubscriptionEntity fakeSubscriptionEntity;
    private UserEntity fakeUserEntity;
    private PlanEntity fakePlanEntity;
    private ApplicationEntity fakeApplicationEntity;
    private static final String API_NAME = "my-api";
    private static final String SUBSCRIPTION_ID = "subscriptionId";
    private static final String FAKE_KEY = "fakeKey";

    @Override
    protected String contextPath() {
        return "apis/" + API_NAME + "/subscriptions/";
    }

    @Before
    public void setUp() {
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

        fakeUserEntity = new UserEntity();
        fakeUserEntity.setFirstname("firstName");
        fakeUserEntity.setLastname("lastName");

        fakePlanEntity = new PlanEntity();
        fakePlanEntity.setId("planId");
        fakePlanEntity.setName("planName");

        fakeApplicationEntity = new ApplicationEntity();
        fakeApplicationEntity.setId("applicationId");
        fakeApplicationEntity.setName("applicationName");
        fakeApplicationEntity.setType("applicationType");
        fakeApplicationEntity.setDescription("applicationDescription");
        fakeApplicationEntity.setPrimaryOwner(new PrimaryOwnerEntity(fakeUserEntity));

        when(userService.findById(any())).thenReturn(fakeUserEntity);
        when(planService.findById(any())).thenReturn(fakePlanEntity);
        when(applicationService.findById(any())).thenReturn(fakeApplicationEntity);
    }

    @Test
    public void shouldRenewApiKeyWithCustomApiKey() {
        when(apiKeyService.renew(anyString(), anyString())).thenReturn(fakeApiKeyEntity);
        when(parameterService.findAsBoolean(Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)).thenReturn(true);

        Response response = envTarget(SUBSCRIPTION_ID)
                .queryParam("customApiKey", "atLeast10CharsButLessThan64")
                .request()
                .post(null);

        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(fakeApiKeyEntity, response.readEntity(ApiKeyEntity.class));
        assertEquals(
                envTarget(SUBSCRIPTION_ID).path("keys").path(FAKE_KEY).queryParam("customApiKey", "atLeast10CharsButLessThan64").getUri().toString(),
                response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    public void shouldNotRenewApiKeyWithCustomApiKeyIfNotAllowed() {
        when(apiKeyService.renew(anyString(), anyString())).thenReturn(fakeApiKeyEntity);
        when(parameterService.findAsBoolean(Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED, ParameterReferenceType.ENVIRONMENT)).thenReturn(false);

        Response response = envTarget(SUBSCRIPTION_ID)
                .queryParam("customApiKey", "atLeast10CharsButLessThan64")
                .request()
                .post(null);

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldRenewApiKeyWithoutCustomApiKey() {
        when(apiKeyService.renew(anyString(), isNull())).thenReturn(fakeApiKeyEntity);

        Response response = envTarget(SUBSCRIPTION_ID)
                .request()
                .post(null);

        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(response.readEntity(ApiKeyEntity.class), fakeApiKeyEntity);
        assertEquals(envTarget(SUBSCRIPTION_ID).path("keys").path(FAKE_KEY).getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    public void shouldProcess() {

        ProcessSubscriptionEntity processSubscriptionEntity = new ProcessSubscriptionEntity();
        processSubscriptionEntity.setId(SUBSCRIPTION_ID);
        processSubscriptionEntity.setCustomApiKey("customApiKey");

        when(subscriptionService.process(any(ProcessSubscriptionEntity.class), any()))
                .thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID + "/_process")
                .request()
                .post(Entity.json(processSubscriptionEntity));

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void shouldNotProcessIfBadId() {

        ProcessSubscriptionEntity processSubscriptionEntity = new ProcessSubscriptionEntity();
        processSubscriptionEntity.setId("badId");
        processSubscriptionEntity.setCustomApiKey("customApiKey");

        when(subscriptionService.process(any(ProcessSubscriptionEntity.class), any()))
                .thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID + "/_process")
                .request()
                .post(Entity.json(processSubscriptionEntity));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldNotProcessIfNotValidApiKey() {

        ProcessSubscriptionEntity processSubscriptionEntity = new ProcessSubscriptionEntity();
        processSubscriptionEntity.setId(SUBSCRIPTION_ID);
        processSubscriptionEntity.setCustomApiKey("customApiKey;^");

        when(subscriptionService.process(any(ProcessSubscriptionEntity.class), any()))
                .thenReturn(fakeSubscriptionEntity);

        Response response = envTarget(SUBSCRIPTION_ID + "/_process")
                .request()
                .post(Entity.json(processSubscriptionEntity));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}