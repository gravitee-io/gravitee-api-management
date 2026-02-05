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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.core.model.SubscriptionFixtures;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.apim.core.subscription.use_case.AcceptSubscriptionUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.NewSubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionConfigurationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    AcceptSubscriptionUseCase acceptSubscriptionUseCase;

    private SubscriptionEntity fakeSubscriptionEntity;
    private UserEntity fakeUserEntity;
    private PlanEntity fakePlanEntity;
    private ApplicationEntity fakeApplicationEntity;

    @Override
    protected String contextPath() {
        return "apis/" + API_NAME + "/subscriptions";
    }

    @Before
    public void init() {
        reset(subscriptionService);
        reset(userService);
        reset(planSearchService);
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
        when(planSearchService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(fakePlanEntity);
        when(applicationService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(fakeApplicationEntity);

        when(permissionService.hasPermission(any(), any(), any(), any())).thenReturn(true);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
        reset(acceptSubscriptionUseCase);
    }

    @Test
    public void shouldCreateSubscriptionAndProcessWithCustomApiKey() {
        final String customApiKey = "atLeast10CharsButLessThan64";

        when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any())).thenReturn(
            fakeSubscriptionEntity
        );
        doReturn(
            new AcceptSubscriptionUseCase.Output(
                SubscriptionFixtures.aSubscription().toBuilder().id(fakeSubscriptionEntity.getId()).build()
            )
        )
            .when(acceptSubscriptionUseCase)
            .execute(any());
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(true);

        Response response = envTarget()
            .queryParam("application", APP_NAME)
            .queryParam("plan", PLAN_NAME)
            .queryParam("customApiKey", customApiKey)
            .queryParam("apiKeyMode", ApiKeyMode.EXCLUSIVE)
            .request()
            .post(null);

        MAPIAssertions.assertThat(response)
            .hasStatus(CREATED_201)
            .hasHeader(
                Map.entry(
                    "Location",
                    envTarget()
                        .path(FAKE_SUBSCRIPTION_ID)
                        .queryParam("apiKeyMode", ApiKeyMode.EXCLUSIVE)
                        .queryParam("customApiKey", customApiKey)
                        .getUri()
                        .toString()
                )
            );

        verify(subscriptionService, times(1)).create(
            eq(GraviteeContext.getExecutionContext()),
            any(NewSubscriptionEntity.class),
            eq(customApiKey)
        );

        ArgumentCaptor<AcceptSubscriptionUseCase.Input> captor = ArgumentCaptor.forClass(AcceptSubscriptionUseCase.Input.class);
        verify(acceptSubscriptionUseCase, times(1)).execute(captor.capture());
        SoftAssertions.assertSoftly(soft -> {
            var input = captor.getValue();
            soft.assertThat(input.referenceId()).isEqualTo(API_NAME);
            soft.assertThat(input.referenceType()).isEqualTo(SubscriptionReferenceType.API);
            soft.assertThat(input.subscriptionId()).isEqualTo(FAKE_SUBSCRIPTION_ID);
            soft.assertThat(input.customKey()).isEqualTo(customApiKey);
            soft.assertThat(input.startingAt()).isStrictlyBetween(ZonedDateTime.now().minusSeconds(5), ZonedDateTime.now().plusSeconds(5));
        });
    }

    @Test
    public void shouldNotCreateSubscriptionAndProcessWithCustomApiKeyIfNotAllowed() {
        final String customApiKey = "atLeast10CharsButLessThan64";

        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.PLAN_SECURITY_APIKEY_CUSTOM_ALLOWED,
                ParameterReferenceType.ENVIRONMENT
            )
        ).thenReturn(false);

        Response response = envTarget()
            .queryParam("application", APP_NAME)
            .queryParam("plan", PLAN_NAME)
            .queryParam("customApiKey", customApiKey)
            .request()
            .post(null);

        MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);

        verifyNoMoreInteractions(acceptSubscriptionUseCase, subscriptionService);
    }

    @Test
    public void shouldCreateSubscriptionAndProcessWithoutCustomApiKey() {
        when(subscriptionService.create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), any())).thenReturn(
            fakeSubscriptionEntity
        );
        doReturn(
            new AcceptSubscriptionUseCase.Output(
                SubscriptionFixtures.aSubscription().toBuilder().id(fakeSubscriptionEntity.getId()).build()
            )
        )
            .when(acceptSubscriptionUseCase)
            .execute(any());

        ArgumentCaptor<String> customApiKeyCaptor = ArgumentCaptor.forClass(String.class);

        Response response = envTarget().queryParam("application", APP_NAME).queryParam("plan", PLAN_NAME).request().post(null);

        MAPIAssertions.assertThat(response)
            .hasStatus(CREATED_201)
            .hasHeader(Map.entry("Location", envTarget().path(FAKE_SUBSCRIPTION_ID).getUri().toString()));

        verify(subscriptionService, times(1)).create(eq(GraviteeContext.getExecutionContext()), any(NewSubscriptionEntity.class), eq(null));

        ArgumentCaptor<AcceptSubscriptionUseCase.Input> captor = ArgumentCaptor.forClass(AcceptSubscriptionUseCase.Input.class);
        verify(acceptSubscriptionUseCase, times(1)).execute(captor.capture());
        SoftAssertions.assertSoftly(soft -> {
            var input = captor.getValue();
            soft.assertThat(input.referenceId()).isEqualTo(API_NAME);
            soft.assertThat(input.referenceType()).isEqualTo(SubscriptionReferenceType.API);
            soft.assertThat(input.subscriptionId()).isEqualTo(FAKE_SUBSCRIPTION_ID);
            soft.assertThat(input.customKey()).isNull();
            soft.assertThat(input.startingAt()).isStrictlyBetween(ZonedDateTime.now().minusSeconds(5), ZonedDateTime.now().plusSeconds(5));
        });
    }

    @Test
    public void shouldCreateSubscriptionWithConfiguration() {
        NewSubscriptionConfigurationEntity configuration = mock(NewSubscriptionConfigurationEntity.class);
        SubscriptionConfigurationEntity configurationEntity = new SubscriptionConfigurationEntity();
        configurationEntity.setEntrypointConfiguration("{}");
        when(configuration.getConfiguration()).thenReturn(configurationEntity);

        ArgumentCaptor<NewSubscriptionEntity> newSubscriptionEntityCaptor = ArgumentCaptor.forClass(NewSubscriptionEntity.class);

        when(
            subscriptionService.create(eq(GraviteeContext.getExecutionContext()), newSubscriptionEntityCaptor.capture(), any())
        ).thenReturn(fakeSubscriptionEntity);
        doReturn(
            new AcceptSubscriptionUseCase.Output(
                SubscriptionFixtures.aSubscription().toBuilder().id(fakeSubscriptionEntity.getId()).build()
            )
        )
            .when(acceptSubscriptionUseCase)
            .execute(any());

        Response response = envTarget()
            .queryParam("application", APP_NAME)
            .queryParam("plan", PLAN_NAME)
            .request()
            .post(Entity.json(configuration));

        MAPIAssertions.assertThat(response)
            .hasStatus(CREATED_201)
            .hasHeader(Map.entry("Location", envTarget().path(FAKE_SUBSCRIPTION_ID).getUri().toString()));

        verify(subscriptionService, times(1)).create(
            eq(GraviteeContext.getExecutionContext()),
            newSubscriptionEntityCaptor.capture(),
            any()
        );
        assertThat(newSubscriptionEntityCaptor.getValue().getConfiguration()).isEqualTo(configurationEntity);
    }

    @Test
    public void get_canCreate_should_return_http_400_if_key_query_param_omitted() {
        Response response = envTarget("/_canCreate").queryParam("application", APP_NAME).request().get();

        verifyNoInteractions(apiKeyService);
        MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    public void get_canCreate_should_return_http_400_if_key_query_param_is_invalid_format() {
        Response response = envTarget("/_canCreate").queryParam("key", "short").queryParam("application", APP_NAME).request().get();

        verifyNoInteractions(apiKeyService);
        MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
    }

    @Test
    public void get_canCreate_should_return_http_400_if_application_query_param_omitted() {
        Response response = envTarget("/_canCreate").queryParam("key", API_KEY).request().get();

        verifyNoInteractions(apiKeyService);
        MAPIAssertions.assertThat(response).hasStatus(BAD_REQUEST_400);
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
        when(apiKeyService.canCreate(GraviteeContext.getExecutionContext(), API_KEY, API_NAME, APP_NAME)).thenThrow(
            TechnicalManagementException.class
        );

        Response response = envTarget("/_canCreate").queryParam("key", API_KEY).queryParam("application", APP_NAME).request().get();

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void get_subscriptions_with_expand_security_queryParam_should_call_service_with_boolean() {
        when(subscriptionService.search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(false), eq(true))).thenReturn(
            new Page<>(List.of(new SubscriptionEntity()), 1, 1, 1)
        );
        when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget("/").queryParam("expand", "security").request().get();

        assertEquals(OK_200, response.getStatus());
        verify(subscriptionService, times(1)).search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(false), eq(true));
    }

    @Test
    public void get_subscriptions_with_expand_security_and_keys_queryParam_should_call_service_with_boolean() {
        when(subscriptionService.search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(true), eq(true))).thenReturn(
            new Page<>(List.of(new SubscriptionEntity()), 1, 1, 1)
        );
        when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget("/").queryParam("expand", "security", "keys").request().get();

        assertEquals(OK_200, response.getStatus());
        verify(subscriptionService, times(1)).search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(true), eq(true));
    }

    @Test
    public void get_subscriptions_with_expand_keys_queryParam_should_call_service_with_boolean() {
        when(subscriptionService.search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(true), eq(false))).thenReturn(
            new Page<>(List.of(new SubscriptionEntity()), 1, 1, 1)
        );
        when(subscriptionService.getMetadata(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget("/").queryParam("expand", "keys").request().get();

        assertEquals(OK_200, response.getStatus());
        verify(subscriptionService, times(1)).search(eq(GraviteeContext.getExecutionContext()), any(), any(), eq(true), eq(false));
    }

    @Test
    public void get_subscriptions_with_default_status() {
        when(subscriptionService.search(any(ExecutionContext.class), any(), any(), anyBoolean(), anyBoolean())).thenReturn(
            new Page<>(List.of(new SubscriptionEntity()), 1, 1, 1)
        );
        when(subscriptionService.getMetadata(any(ExecutionContext.class), any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget().request().get();

        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<SubscriptionQuery> subscriptionQueryCaptor = ArgumentCaptor.forClass(SubscriptionQuery.class);

        verify(subscriptionService, times(1)).search(
            any(ExecutionContext.class),
            subscriptionQueryCaptor.capture(),
            any(),
            anyBoolean(),
            anyBoolean()
        );

        SubscriptionQuery subscriptionQuery = subscriptionQueryCaptor.getValue();
        assertThat(subscriptionQuery).extracting(SubscriptionQuery::getStatuses).isEqualTo(List.of(SubscriptionStatus.ACCEPTED));
    }

    @Test
    public void get_subscriptions_with_status_from_query_params() {
        when(subscriptionService.search(any(ExecutionContext.class), any(), any(), anyBoolean(), anyBoolean())).thenReturn(
            new Page<>(List.of(new SubscriptionEntity()), 1, 1, 1)
        );
        when(subscriptionService.getMetadata(any(ExecutionContext.class), any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget().queryParam("status", "PENDING, REJECTED, ACCEPTED").request().get();

        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<SubscriptionQuery> subscriptionQueryCaptor = ArgumentCaptor.forClass(SubscriptionQuery.class);

        verify(subscriptionService, times(1)).search(
            any(ExecutionContext.class),
            subscriptionQueryCaptor.capture(),
            any(),
            anyBoolean(),
            anyBoolean()
        );

        SubscriptionQuery subscriptionQuery = subscriptionQueryCaptor.getValue();
        assertThat(subscriptionQuery)
            .extracting(SubscriptionQuery::getStatuses)
            .isEqualTo(List.of(SubscriptionStatus.PENDING, SubscriptionStatus.REJECTED, SubscriptionStatus.ACCEPTED));
    }

    @Test
    public void shouldExportMoreThan100SubscriptionsInCSV() {
        when(
            subscriptionService.search(any(ExecutionContext.class), any(SubscriptionQuery.class), any(), anyBoolean(), anyBoolean())
        ).thenReturn(new Page<>(List.of(new SubscriptionEntity()), 1, 1, 1));
        when(subscriptionService.getMetadata(any(ExecutionContext.class), any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget("/export").queryParam("size", "10000").queryParam("page", "1").request().get();

        assertEquals(OK_200, response.getStatus());
    }
}
