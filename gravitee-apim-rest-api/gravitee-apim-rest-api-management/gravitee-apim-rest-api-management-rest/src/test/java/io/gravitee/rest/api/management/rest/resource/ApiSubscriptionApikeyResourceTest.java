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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ApiSubscriptionApikeyResourceTest extends AbstractResourceTest {

    private static final String API_ID = "my-api";
    private static final String APIKEY_ID = "my-apikey";
    private static final String SUBSCRIPTION_ID = "my-subscription";

    @Override
    protected String contextPath() {
        return "apis/" + API_ID + "/subscriptions/" + SUBSCRIPTION_ID + "/apikeys/" + APIKEY_ID;
    }

    @Before
    public void setUp() {
        reset(apiKeyService);
        GraviteeContext.cleanContext();
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test
    public void delete_should_call_revoke_service_and_return_http_204() {
        ApiKeyEntity existingApiKey = new ApiKeyEntity();
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        existingApiKey.setSubscriptions(Set.of(subscription));
        when(apiKeyService.findById(APIKEY_ID)).thenReturn(existingApiKey);

        Response response = envTarget().request().delete();

        verify(apiKeyService, times(1)).revoke(existingApiKey, true);
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void delete_should_return_http_400_when_apikey_on_another_subscription() {
        ApiKeyEntity existingApiKey = new ApiKeyEntity();
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId("another-subscription");
        existingApiKey.setSubscriptions(Set.of(subscription));

        when(apiKeyService.findById(APIKEY_ID)).thenReturn(existingApiKey);

        Response response = envTarget().request().delete();

        verify(apiKeyService, never()).revoke(any(ApiKeyEntity.class), any(Boolean.class));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void delete_should_return_http_500_on_exception() {
        doThrow(TechnicalManagementException.class).when(apiKeyService).revoke(any(String.class), any(Boolean.class));

        Response response = envTarget().request().delete();

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void put_should_return_http_400_if_entity_id_does_not_match() {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId("another-api-key-id");

        Response response = envTarget().request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }

    @Test
    public void put_should_call_service_update_and_return_http_200() {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(APIKEY_ID);

        Response response = envTarget().request().put(Entity.json(apiKey));

        verify(apiKeyService, times(1)).update(apiKey);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void put_should_return_http_500_on_exception() {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setId(APIKEY_ID);

        when(apiKeyService.update(any())).thenThrow(TechnicalManagementException.class);

        Response response = envTarget().request().put(Entity.json(apiKey));

        assertEquals(HttpStatusCode.INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    public void post_on_reactivate_should_call_reactivate_service_and_return_http_200() {
        ApiKeyEntity existingApiKey = new ApiKeyEntity();
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        existingApiKey.setSubscriptions(Set.of(subscription));
        when(apiKeyService.findById(APIKEY_ID)).thenReturn(existingApiKey);

        Response response = envTarget("/_reactivate").request().post(null);

        verify(apiKeyService, times(1)).reactivate(existingApiKey);
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
    }

    @Test
    public void post_on_reactivate_should_return_http_400_when_apikey_on_another_subscription() {
        ApiKeyEntity existingApiKey = new ApiKeyEntity();
        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId("another-subscription");
        existingApiKey.setSubscriptions(Set.of(subscription));

        when(apiKeyService.findById(APIKEY_ID)).thenReturn(existingApiKey);

        Response response = envTarget("/_reactivate").request().post(null);

        verify(apiKeyService, never()).reactivate(any(ApiKeyEntity.class));
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
    }
}
