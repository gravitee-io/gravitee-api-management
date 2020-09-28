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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.parameters.Key;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscriptionsResourceTest extends AbstractResourceTest {

    private static final String API_NAME = "my-api";
    private static final String APP_NAME = "my-app";
    private static final String PLAN_NAME = "my-plan";
    private static final String FAKE_SUBSCRIPTION_ID = "subscriptionId";


    private SubscriptionEntity fakeSubscriptionEntity;
    private UserEntity fakeUserEntity;
    private PlanEntity fakePlanEntity;
    private ApplicationEntity fakeApplicationEntity;

    @Override
    protected String contextPath() {
        return "apis/" + API_NAME + "/subscriptions";
    }

    @Before
    public void setUp() {
        reset(subscriptionService);
        reset(userService);
        reset(planService);
        reset(applicationService);
        reset(parameterService);

        fakeSubscriptionEntity = new SubscriptionEntity();
        fakeSubscriptionEntity.setId(FAKE_SUBSCRIPTION_ID);
        fakeSubscriptionEntity.setStatus(SubscriptionStatus.PENDING);

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
    public void shouldCreateSubscriptionAndProcessWithCustomApiKey() {

        final String customApiKey = "atLeast10CharsButLessThan64";

        when(subscriptionService.create(any(NewSubscriptionEntity.class), any())).thenReturn(fakeSubscriptionEntity);
        when(subscriptionService.process(any(ProcessSubscriptionEntity.class), any()))
                .thenReturn(fakeSubscriptionEntity);
        when(parameterService.findAsBoolean(Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED)).thenReturn(true);

        ArgumentCaptor<String> customApiKeyCaptor = ArgumentCaptor.forClass(String.class);

        Response response = envTarget()
                .queryParam("application", APP_NAME)
                .queryParam("plan", PLAN_NAME)
                .queryParam("customApiKey", customApiKey)
                .request()
                .post(null);

        verify(subscriptionService, times(1))
                .create(any(NewSubscriptionEntity.class), customApiKeyCaptor.capture());
        verify(subscriptionService, times(1))
                .process(any(ProcessSubscriptionEntity.class), any());
        assertEquals(customApiKeyCaptor.getValue(), customApiKey);
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals("/apis/" + API_NAME + "/subscriptions/" + FAKE_SUBSCRIPTION_ID, response.getLocation().getPath());
    }

    @Test
    public void shouldNotCreateSubscriptionAndProcessWithCustomApiKeyIfNotAllowed() {

        final String customApiKey = "atLeast10CharsButLessThan64";

        when(subscriptionService.create(any(NewSubscriptionEntity.class))).thenReturn(fakeSubscriptionEntity);
        when(subscriptionService.process(any(ProcessSubscriptionEntity.class), any()))
                .thenReturn(fakeSubscriptionEntity);
        when(parameterService.findAsBoolean(Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED)).thenReturn(false);

        Response response = envTarget()
                .queryParam("application", APP_NAME)
                .queryParam("plan", PLAN_NAME)
                .queryParam("customApiKey", customApiKey)
                .request()
                .post(null);

        verify(subscriptionService, times(0))
                .create(any(), any());
        verify(subscriptionService, times(0))
                .process(any(ProcessSubscriptionEntity.class), any());
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldCreateSubscriptionAndProcessWithoutCustomApiKey() {

        final String customApiKey = null;

        when(subscriptionService.create(any(NewSubscriptionEntity.class), any()))
                .thenReturn(fakeSubscriptionEntity);
        when(subscriptionService.process(any(ProcessSubscriptionEntity.class), any()))
                .thenReturn(fakeSubscriptionEntity);

        ArgumentCaptor<String> customApiKeyCaptor = ArgumentCaptor.forClass(String.class);

        Response response = envTarget()
                .queryParam("application", APP_NAME)
                .queryParam("plan", PLAN_NAME)
                .request()
                .post(null);

        verify(subscriptionService, times(1))
                .create(any(NewSubscriptionEntity.class), customApiKeyCaptor.capture());
        verify(subscriptionService, times(1))
                .process(any(ProcessSubscriptionEntity.class), any());
        assertEquals(customApiKeyCaptor.getValue(), customApiKey);
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals("/apis/" + API_NAME + "/subscriptions/" + FAKE_SUBSCRIPTION_ID, response.getLocation().getPath());
    }
}