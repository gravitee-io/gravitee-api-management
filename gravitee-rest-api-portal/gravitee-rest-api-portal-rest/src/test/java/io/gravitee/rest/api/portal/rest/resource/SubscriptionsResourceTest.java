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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import java.util.Arrays;
import java.util.Collections;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.NewSubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.Links;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.portal.rest.model.SubscriptionInput;
import io.gravitee.rest.api.portal.rest.model.SubscriptionsResponse;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionsResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "subscriptions";
    }
    private static final String SUBSCRIPTION = "my-subscription";
    private static final String ANOTHER_SUBSCRIPTION = "my-other-subscription";

    private Page<SubscriptionEntity> subscriptionEntityPage;
    
    @Before
    public void init() {
        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        
        reset(subscriptionService);
        reset(subscriptionMapper);
        
        SubscriptionEntity subscriptionEntity1 = new SubscriptionEntity();
        subscriptionEntity1.setId(SUBSCRIPTION);
        SubscriptionEntity subscriptionEntity2 = new SubscriptionEntity();
        subscriptionEntity2.setId(ANOTHER_SUBSCRIPTION);
        subscriptionEntityPage = new Page<SubscriptionEntity>(Arrays.asList(subscriptionEntity1, subscriptionEntity2), 1, 2, 2);
        doReturn(subscriptionEntityPage).when(subscriptionService).search(any(), any());

        doReturn(new Subscription().id(SUBSCRIPTION)).when(subscriptionMapper).convert(subscriptionEntity1);
        doReturn(new Subscription().id(ANOTHER_SUBSCRIPTION)).when(subscriptionMapper).convert(subscriptionEntity2);

        SubscriptionEntity createdSubscription = new SubscriptionEntity();
        createdSubscription.setId(SUBSCRIPTION);
        doReturn(createdSubscription).when(subscriptionService).create(any());
    }
    
    @Test
    public void shouldGetSubscriptions() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        SubscriptionsResponse subscriptionResponse = response.readEntity(SubscriptionsResponse.class);
        assertEquals(2, subscriptionResponse.getData().size());
    }
    
    @Test
    public void shouldGetSubscriptionsWithPaginatedLink() {
        final Response response = target().queryParam("page", 1).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        SubscriptionsResponse subscriptionResponse = response.readEntity(SubscriptionsResponse.class);
        assertEquals(1, subscriptionResponse.getData().size());
        
        Links links = subscriptionResponse.getLinks();
        assertNotNull(links);
        
    }
    
    @Test
    public void shouldGetNoSubscription() {
        final Response response = target().queryParam("page", 10).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        
        Error errorResponse = response.readEntity(Error.class);
        assertEquals("400", errorResponse.getCode());
        assertEquals("javax.ws.rs.BadRequestException", errorResponse.getTitle());
        assertEquals("page is not valid", errorResponse.getDetail());
    }
    
    @Test
    public void shouldGetNoPublishedApiAndNoLink() {
        doReturn(new Page<SubscriptionEntity>(Collections.EMPTY_LIST, 1, 0, 0)).when(subscriptionService).search(any(), any());

        //Test with default limit
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        SubscriptionsResponse subscriptionResponse = response.readEntity(SubscriptionsResponse.class);
        assertEquals(0, subscriptionResponse.getData().size());
        
        Links links = subscriptionResponse.getLinks();
        assertNull(links);
        
        //Test with small limit
        final Response anotherResponse = target().queryParam("page", 2).queryParam("size", 1).request().get();
        assertEquals(HttpStatusCode.OK_200, anotherResponse.getStatus());
        
        subscriptionResponse = anotherResponse.readEntity(SubscriptionsResponse.class);
        assertEquals(0, subscriptionResponse.getData().size());
        
        links = subscriptionResponse.getLinks();
        assertNull(links);

    }
    
    @Test
    public void shouldCreateSubscription() {
        SubscriptionInput subscriptionInput = new SubscriptionInput()
                .application("application")
                .plan("plan")
                .request("request")
                ;
        
        final Response response = target().request().post(Entity.json(subscriptionInput));
        assertEquals(OK_200, response.getStatus());

        ArgumentCaptor<NewSubscriptionEntity> argument = ArgumentCaptor.forClass(NewSubscriptionEntity.class);
        Mockito.verify(subscriptionService).create(argument.capture());
        assertEquals("application", argument.getValue().getApplication());
        assertEquals("plan", argument.getValue().getPlan());
        assertEquals("request", argument.getValue().getRequest());

        final Subscription subscriptionResponse = response.readEntity(Subscription.class);
        assertNotNull(subscriptionResponse);
        assertEquals(SUBSCRIPTION, subscriptionResponse.getId());
        
    }
}
