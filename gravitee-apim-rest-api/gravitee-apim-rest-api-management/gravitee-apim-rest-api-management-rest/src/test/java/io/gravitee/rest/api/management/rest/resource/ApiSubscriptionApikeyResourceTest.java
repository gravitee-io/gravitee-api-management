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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fixtures.core.model.ApiKeyFixtures;
import fixtures.core.model.SubscriptionFixtures;
import inmemory.ApiKeyCrudServiceInMemory;
import inmemory.ApplicationCrudServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.SubscriptionCrudServiceInMemory;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
public class ApiSubscriptionApikeyResourceTest extends AbstractResourceTest {

    private static final String API_ID = "my-api";
    private static final String APIKEY_ID = "my-apikey";
    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String APPLICATION_ID = "my-application";

    @Autowired
    private ApiKeyCrudServiceInMemory apiKeyCrudServiceInMemory;

    @Autowired
    private ApplicationCrudServiceInMemory applicationCrudServiceInMemory;

    @Autowired
    private SubscriptionCrudServiceInMemory subscriptionCrudServiceInMemory;

    @Override
    protected String contextPath() {
        return "apis/" + API_ID + "/subscriptions/" + SUBSCRIPTION_ID + "/apikeys/" + APIKEY_ID;
    }

    @Before
    public void init() {
        reset(apiKeyService, subscriptionService, applicationService);
        GraviteeContext.cleanContext();
    }

    @After
    public void tearDown() {
        Stream.of(apiKeyCrudServiceInMemory, applicationCrudServiceInMemory, subscriptionCrudServiceInMemory).forEach(
            InMemoryAlternative::reset
        );

        reset(apiKeyCrudServiceInMemory);
        GraviteeContext.cleanContext();
    }

    @Test
    public void delete_should_revoke_and_return_http_204() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(
                SubscriptionFixtures.aSubscription()
                    .toBuilder()
                    .id(SUBSCRIPTION_ID)
                    .apiId(API_ID)
                    .referenceId(API_ID)
                    .referenceType(SubscriptionReferenceType.API)
                    .build()
            )
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().apiKeyMode(ApiKeyMode.EXCLUSIVE).id(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id(APIKEY_ID)
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID))
                    .build()
            )
        );

        Response response = envTarget().request().delete();

        Assertions.assertThat(apiKeyCrudServiceInMemory.storage())
            .extracting(
                io.gravitee.apim.core.api_key.model.ApiKeyEntity::getId,
                io.gravitee.apim.core.api_key.model.ApiKeyEntity::isRevoked
            )
            .containsExactly(tuple(APIKEY_ID, true));
        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NO_CONTENT_204);
    }

    @Test
    public void delete_should_return_http_404_when_apikey_on_another_subscription() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(
                SubscriptionFixtures.aSubscription()
                    .toBuilder()
                    .id(SUBSCRIPTION_ID)
                    .apiId(API_ID)
                    .referenceId(API_ID)
                    .referenceType(SubscriptionReferenceType.API)
                    .build()
            )
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().apiKeyMode(ApiKeyMode.EXCLUSIVE).id(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id(APIKEY_ID)
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of("another-subscription"))
                    .build()
            )
        );
        Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
        Assertions.assertThat(apiKeyCrudServiceInMemory.storage())
            .extracting(
                io.gravitee.apim.core.api_key.model.ApiKeyEntity::getId,
                io.gravitee.apim.core.api_key.model.ApiKeyEntity::isRevoked
            )
            .containsExactly(tuple(APIKEY_ID, false));
    }

    @Test
    public void delete_should_return_http_500_on_exception() {
        subscriptionCrudServiceInMemory.initWith(
            List.of(
                SubscriptionFixtures.aSubscription()
                    .toBuilder()
                    .id(SUBSCRIPTION_ID)
                    .apiId(API_ID)
                    .referenceId(API_ID)
                    .referenceType(SubscriptionReferenceType.API)
                    .build()
            )
        );
        applicationCrudServiceInMemory.initWith(
            List.of(BaseApplicationEntity.builder().apiKeyMode(ApiKeyMode.EXCLUSIVE).id(APPLICATION_ID).build())
        );
        apiKeyCrudServiceInMemory.initWith(
            List.of(
                ApiKeyFixtures.anApiKey()
                    .toBuilder()
                    .id(APIKEY_ID)
                    .applicationId(APPLICATION_ID)
                    .subscriptions(List.of(SUBSCRIPTION_ID))
                    .build()
            )
        );
        doThrow(TechnicalManagementException.class).when(apiKeyCrudServiceInMemory).update(any());

        Response response = envTarget().request().delete();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
    }

    @Test
    public void put_should_return_http_400_if_entity_id_does_not_match() {
        mockExistingSubscription();
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId("another-api-key-id");

        Response response = envTarget().request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void put_should_call_service_update_and_return_http_200() {
        ApplicationEntity application = mockExistingApplication(ApiKeyMode.EXCLUSIVE);
        SubscriptionEntity subscription = mockExistingSubscription();
        mockExistingApiKey(application, subscription);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(APIKEY_ID);

        Response response = envTarget().request().put(Entity.json(apiKey));

        verify(apiKeyService, times(1)).update(GraviteeContext.getExecutionContext(), apiKey);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void put_should_return_http_500_on_exception() {
        ApplicationEntity application = mockExistingApplication(ApiKeyMode.EXCLUSIVE);
        SubscriptionEntity subscription = mockExistingSubscription();
        mockExistingApiKey(application, subscription);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(APIKEY_ID);

        when(apiKeyService.update(eq(GraviteeContext.getExecutionContext()), any())).thenThrow(TechnicalManagementException.class);

        Response response = envTarget().request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void put_should_return_http_404_when_subscription_not_found() {
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenThrow(SubscriptionNotFoundException.class);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(APIKEY_ID);

        Response response = envTarget().request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void put_should_return_http_404_when_subscription_does_not_belong_to_api() {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setApi("Another_api");
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(APIKEY_ID);

        Response response = envTarget().request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void put_should_return_http_400_when_apikey_not_used_in_subscription() {
        mockExistingSubscription();

        ApplicationEntity application = mockExistingApplication(ApiKeyMode.EXCLUSIVE);
        SubscriptionEntity anotherExistingSubscription = new SubscriptionEntity();
        anotherExistingSubscription.setId("Another_subscription");
        anotherExistingSubscription.setApplication(APPLICATION_ID);
        anotherExistingSubscription.setApi(API_ID);
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(anotherExistingSubscription);

        ApiKeyEntity existingApiKey = new ApiKeyEntity();
        existingApiKey.setApplication(application);
        existingApiKey.setSubscriptions(Set.of(anotherExistingSubscription));
        when(apiKeyService.findById(GraviteeContext.getExecutionContext(), APIKEY_ID)).thenReturn(existingApiKey);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(APIKEY_ID);

        Response response = envTarget().request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void put_should_return_http_400_when_its_a_shared_api_key() {
        ApplicationEntity application = mockExistingApplication(ApiKeyMode.SHARED);
        SubscriptionEntity subscription = mockExistingSubscription();
        mockExistingApiKey(application, subscription);

        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(APIKEY_ID);

        Response response = envTarget().request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void post_on_reactivate_should_call_reactivate_service_and_return_http_200() {
        ApplicationEntity application = mockExistingApplication(ApiKeyMode.EXCLUSIVE);
        SubscriptionEntity subscription = mockExistingSubscription();
        ApiKeyEntity apiKey = mockExistingApiKey(application, subscription);

        Response response = envTarget("/_reactivate").request().post(null);

        verify(apiKeyService, times(1)).reactivate(GraviteeContext.getExecutionContext(), apiKey);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void post_on_reactivate_should_return_404_when_subscription_does_not_belong_to_api() {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setApi("Another_api");
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);

        Response response = envTarget("/_reactivate").request().post(null);
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
    }

    @Test
    public void post_on_reactivate_should_return_http_400_when_apikey_on_another_subscription() {
        ApplicationEntity application = mockExistingApplication(ApiKeyMode.EXCLUSIVE);
        SubscriptionEntity subscription = mockExistingSubscription();
        mockExistingApiKey(application, subscription);

        subscription.setId("another-subscription");

        Response response = envTarget("/_reactivate").request().post(null);

        verify(apiKeyService, never()).reactivate(eq(GraviteeContext.getExecutionContext()), any(ApiKeyEntity.class));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void post_on_reactivate_should_return_http_400_when_its_a_shared_api_key() {
        ApplicationEntity application = mockExistingApplication(ApiKeyMode.SHARED);
        SubscriptionEntity subscription = mockExistingSubscription();
        mockExistingApiKey(application, subscription);

        Response response = envTarget("/_reactivate").request().post(null);

        verify(apiKeyService, never()).reactivate(eq(GraviteeContext.getExecutionContext()), any(ApiKeyEntity.class));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    private ApplicationEntity mockExistingApplication(ApiKeyMode apiKeyMode) {
        ApplicationEntity application = new ApplicationEntity();
        application.setId(APPLICATION_ID);
        application.setApiKeyMode(apiKeyMode);
        when(applicationService.findById(GraviteeContext.getExecutionContext(), APPLICATION_ID)).thenReturn(application);
        return application;
    }

    private SubscriptionEntity mockExistingSubscription() {
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setApplication(APPLICATION_ID);
        subscription.setApi(API_ID);
        when(subscriptionService.findById(SUBSCRIPTION_ID)).thenReturn(subscription);
        return subscription;
    }

    private ApiKeyEntity mockExistingApiKey(ApplicationEntity application, SubscriptionEntity subscription) {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setApplication(application);
        apiKey.setSubscriptions(Set.of(subscription));
        when(apiKeyService.findById(GraviteeContext.getExecutionContext(), APIKEY_ID)).thenReturn(apiKey);
        return apiKey;
    }
}
