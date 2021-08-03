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

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationSubscriptionsResourceTest extends AbstractResourceTest {

    private static final String SUBSCRIPTION = "my-subscription";
    private static final String APPLICATION = "my-application";
    private static final String PLAN = "my-plan";
    private static final String NEW_APIKEY = "my-new-apikey";

    @Override
    protected String contextPath() {
        return "applications";
    }

    @Test
    public void shouldCreateSubscription() {
        reset(apiService, planService, subscriptionService, userService);

        NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
        newSubscriptionEntity.setApplication(APPLICATION);
        newSubscriptionEntity.setPlan(PLAN);
        newSubscriptionEntity.setRequest("request");

        when(planService.findById(any())).thenReturn(mock(PlanEntity.class));

        SubscriptionEntity createdSubscription = new SubscriptionEntity();
        createdSubscription.setId(SUBSCRIPTION);
        when(subscriptionService.create(any())).thenReturn(createdSubscription);
        when(userService.findById(any(), anyBoolean())).thenReturn(mock(UserEntity.class));

        ApiEntity foundApi = new ApiEntity();
        foundApi.setPrimaryOwner(mock(PrimaryOwnerEntity.class));
        when(apiService.findById(any())).thenReturn(foundApi);

        final Response response = envTarget()
            .path(APPLICATION)
            .path("subscriptions")
            .queryParam("plan", PLAN)
            .request()
            .post(Entity.json(newSubscriptionEntity));
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path(APPLICATION).path("subscriptions").path(SUBSCRIPTION).getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }

    @Test
    public void shouldRenewApiKeyForApplicationSubscription() {
        reset(apiKeyService);

        ApiKeyEntity newApiKeyEntity = new ApiKeyEntity();
        newApiKeyEntity.setKey(NEW_APIKEY);
        when(apiKeyService.renew(SUBSCRIPTION)).thenReturn(newApiKeyEntity);

        final Response response = envTarget().path(APPLICATION).path("subscriptions").path(SUBSCRIPTION).request().post(null);
        assertEquals(CREATED_201, response.getStatus());
        assertEquals(
            envTarget().path(APPLICATION).path("subscriptions").path(SUBSCRIPTION).path("keys").path(NEW_APIKEY).getUri().toString(),
            response.getHeaders().getFirst(HttpHeaders.LOCATION)
        );
    }
}
