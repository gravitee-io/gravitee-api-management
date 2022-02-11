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
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.rest.model.PagedResult;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.pagedresult.Metadata;
import java.util.List;
import java.util.Set;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
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
    public void shouldGetSubscriptions_expandingSecurity() {
        reset(apiService, planService, subscriptionService, userService);

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setPlan(PLAN);

        PlanEntity plan = new PlanEntity();
        plan.setId(PLAN);
        plan.setSecurity(PlanSecurityType.API_KEY);

        when(subscriptionService.search(any(), any())).thenReturn(new Page<>(List.of(subscription), 1, 1, 1));
        when(planService.findByIdIn(Set.of(PLAN))).thenReturn(Set.of(plan));
        when(subscriptionService.getMetadata(any())).thenReturn(mock(Metadata.class));

        final Response response = envTarget().path(APPLICATION).path("subscriptions").queryParam("expand", "security").request().get();

        assertEquals(OK_200, response.getStatus());

        JsonNode responseBody = response.readEntity(JsonNode.class);

        assertTrue(responseBody.hasNonNull("data"));
        ArrayNode data = (ArrayNode) responseBody.get("data");
        assertEquals(1, data.size());

        JsonNode responseSubscription = data.get(0);
        assertTrue(responseSubscription.hasNonNull("security"));
        assertEquals("API_KEY", responseSubscription.get("security").asText());
    }
}
