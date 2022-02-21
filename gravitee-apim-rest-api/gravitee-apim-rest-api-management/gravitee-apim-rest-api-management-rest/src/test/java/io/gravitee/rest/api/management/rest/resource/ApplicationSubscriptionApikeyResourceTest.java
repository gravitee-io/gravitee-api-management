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
import io.gravitee.rest.api.model.ApiKeyMode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

/**
 * @author GraviteeSource Team
 */
public class ApplicationSubscriptionApikeyResourceTest extends AbstractResourceTest {

    private static final String APPLICATION_ID = "my-application";
    private static final String SUBSCRIPTION_ID = "my-subscription";
    private static final String APIKEY_ID = "my-apikey";

    @Override
    protected String contextPath() {
        return "applications/" + APPLICATION_ID + "/subscriptions/" + SUBSCRIPTION_ID + "/apikeys/" + APIKEY_ID;
    }

    @Before
    public void setUp() {
        reset(apiKeyService);
    }

    @Test
    public void delete_should_call_revoke_service_and_return_http_204() {
        ApiKeyEntity existingApiKey = new ApiKeyEntity();
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION_ID);
        existingApiKey.setSubscriptions(Set.of(subscriptionEntity));
        when(apiKeyService.findById(APIKEY_ID)).thenReturn(existingApiKey);

        Response response = envTarget().request().delete();

        verify(apiKeyService, times(1)).revoke(existingApiKey, true);
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
    }

    @Test
    public void delete_should_return_http_400_when_apikey_on_another_subscription() {
        ApiKeyEntity existingApiKey = new ApiKeyEntity();
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId("another-subscription");
        existingApiKey.setSubscriptions(Set.of(subscriptionEntity));

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
}
