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

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiSubscriptionsResourceTest extends AbstractResourceTest {

    private static final String API_NAME = "my-api";
    private static final String APP_NAME = "my-app";
    private static final String PLAN_NAME = "my-plan";
    private static final String FAKE_SUBSCRIPTION_ID = "subscriptionId";
    private final String API_KEY = "my-api-key-123546";

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
        reset(apiKeyService);
        GraviteeContext.cleanContext();

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
        fakeApplicationEntity.setApiKeyMode(ApiKeyMode.EXCLUSIVE);

        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(fakeUserEntity);
        when(planService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(fakePlanEntity);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(fakeApplicationEntity);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void shouldCreateSubscriptionAndProcessWithCustomApiKey() {
        final String customApiKey = "atLeast10CharsButLessThan64";

        when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any()))
            .thenReturn(fakeSubscriptionEntity);
        when(subscriptionService.process(eq(GraviteeContext.getExecutionContext()), any(ProcessSubscriptionEntity.class), any()))
            .thenReturn(fakeSubscriptionEntity);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);

        ArgumentCaptor<String> customApiKeyCaptor = ArgumentCaptor.forClass(String.class);

        Response response = envTarget()
            .queryParam("application", APP_NAME)
            .queryParam("plan", PLAN_NAME)
            .queryParam("customApiKey", customApiKey)
            .request()
            .post(null);

        verify(subscriptionService, times(1))
            .create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), customApiKeyCaptor.capture());
        verify(subscriptionService, times(1))
            .process(eq(GraviteeContext.getExecutionContext()), any(ProcessSubscriptionEntity.class), any());
        assertEquals(customApiKeyCaptor.getValue(), customApiKey);
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path(FAKE_SUBSCRIPTION_ID).queryParam("customApiKey", customApiKey).getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }

    @Test
    public void shouldNotCreateSubscriptionAndProcessWithCustomApiKeyIfNotAllowed() {
        final String customApiKey = "atLeast10CharsButLessThan64";

        when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class)))
            .thenReturn(fakeSubscriptionEntity);
        when(subscriptionService.process(eq(GraviteeContext.getExecutionContext()), any(ProcessSubscriptionEntity.class), any()))
            .thenReturn(fakeSubscriptionEntity);
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(false);

        Response response = envTarget()
            .queryParam("application", APP_NAME)
            .queryParam("plan", PLAN_NAME)
            .queryParam("customApiKey", customApiKey)
            .request()
            .post(null);

        verify(subscriptionService, times(0)).create(eq(GraviteeContext.getExecutionContext()), any(), any());
        verify(subscriptionService, times(0))
            .process(eq(GraviteeContext.getExecutionContext()), any(ProcessSubscriptionEntity.class), any());
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void shouldCreateSubscriptionAndProcessWithoutCustomApiKey() {
        final String customApiKey = null;

        when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any()))
            .thenReturn(fakeSubscriptionEntity);
        when(subscriptionService.process(eq(GraviteeContext.getExecutionContext()), any(ProcessSubscriptionEntity.class), any()))
            .thenReturn(fakeSubscriptionEntity);

        ArgumentCaptor<String> customApiKeyCaptor = ArgumentCaptor.forClass(String.class);

        Response response = envTarget().queryParam("application", APP_NAME).queryParam("plan", PLAN_NAME).request().post(null);

        verify(subscriptionService, times(1))
            .create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), customApiKeyCaptor.capture());
        verify(subscriptionService, times(1))
            .process(eq(GraviteeContext.getExecutionContext()), any(ProcessSubscriptionEntity.class), any());
        assertEquals(customApiKeyCaptor.getValue(), customApiKey);
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        assertEquals(envTarget().path(FAKE_SUBSCRIPTION_ID).getUri().toString(), response.getHeaders().getFirst(HttpHeaders.LOCATION));
    }

    @Test
    public void get_canCreate_should_return_http_400_if_key_query_param_omitted() {
        Response response = envTarget("/_canCreate").queryParam("application", APP_NAME).request().get();

        verifyNoInteractions(apiKeyService);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void get_canCreate_should_return_http_400_if_key_query_param_is_invalid_format() {
        Response response = envTarget("/_canCreate").queryParam("key", "short").queryParam("application", APP_NAME).request().get();

        verifyNoInteractions(apiKeyService);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void get_canCreate_should_return_http_400_if_application_query_param_omitted() {
        Response response = envTarget("/_canCreate").queryParam("key", API_KEY).request().get();

        verifyNoInteractions(apiKeyService);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void get_canCreate_should_call_service_and_return_http_200_containing_true() {
        when(apiKeyService.canCreate(GraviteeContext.getExecutionContext(), API_KEY, API_NAME, APP_NAME)).thenReturn(true);

        Response response = envTarget("/_canCreate").queryParam("key", API_KEY).queryParam("application", APP_NAME).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertTrue(response.readEntity(Boolean.class));
    }

    @Test
    public void get_canCreate_should_call_service_and_return_http_200_containing_false() {
        when(apiKeyService.canCreate(GraviteeContext.getExecutionContext(), API_KEY, API_NAME, APP_NAME)).thenReturn(false);

        Response response = envTarget("/_canCreate").queryParam("key", API_KEY).queryParam("application", APP_NAME).request().get();

        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        assertFalse(response.readEntity(Boolean.class));
    }

    @Test
    public void get_canCreate_should_return_http_500_on_exception() {
        when(apiKeyService.canCreate(GraviteeContext.getExecutionContext(), API_KEY, API_NAME, APP_NAME))
            .thenThrow(TechnicalManagementException.class);

        Response response = envTarget("/_canCreate").queryParam("key", API_KEY).queryParam("application", APP_NAME).request().get();

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void get_subscriptions_with_expand_security_queryParam_should_call_service_with_boolean() {
        when(subscriptionService.search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(false), eq(true)))
            .thenReturn(new Page<>(List.of(new SubscriptionEntity()), 1, 1, 1));
        when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget("/").queryParam("expand", "security").request().get();

        assertEquals(OK_200, response.getStatus());
        verify(subscriptionService, times(1)).search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(false), eq(true));
    }

    @Test
    public void get_subscriptions_with_expand_security_and_keys_queryParam_should_call_service_with_boolean() {
        when(subscriptionService.search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(true), eq(true)))
            .thenReturn(new Page<>(List.of(new SubscriptionEntity()), 1, 1, 1));
        when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget("/").queryParam("expand", "security", "keys").request().get();

        assertEquals(OK_200, response.getStatus());
        verify(subscriptionService, times(1)).search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(true), eq(true));
    }

    @Test
    public void get_subscriptions_with_expand_keys_queryParam_should_call_service_with_boolean() {
        when(subscriptionService.search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(true), eq(false)))
            .thenReturn(new Page<>(List.of(new SubscriptionEntity()), 1, 1, 1));
        when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget("/").queryParam("expand", "keys").request().get();

        assertEquals(OK_200, response.getStatus());
        verify(subscriptionService, times(1)).search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(true), eq(false));
    }
}
