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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import java.util.Arrays;
import java.util.HashSet;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.portal.rest.model.Error;
import io.gravitee.rest.api.portal.rest.model.Key;
import io.gravitee.rest.api.portal.rest.model.Subscription;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "subscriptions/";
    }
    private static final String SUBSCRIPTION = "my-subscription";
    private static final String UNKNOWN_SUBSCRIPTION = "unknown-subscription";

    private SubscriptionEntity subscriptionEntity;
    
    @Before
    public void init() {
        reset(apiKeyService);
        reset(subscriptionService);
        reset(subscriptionMapper);
        reset(keyMapper);
        
        subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.setId(SUBSCRIPTION);
        
        
        doReturn(subscriptionEntity).when(subscriptionService).findById(SUBSCRIPTION);
        doThrow(SubscriptionNotFoundException.class).when(subscriptionService).findById(UNKNOWN_SUBSCRIPTION);

        doReturn(new HashSet<ApiKeyEntity>(Arrays.asList(new ApiKeyEntity()))).when(apiKeyService).findBySubscription(SUBSCRIPTION);
        
        
        doReturn(new Subscription()).when(subscriptionMapper).convert(any());
        doReturn(new Key()).when(keyMapper).convert(any());
    }
    
    @Test
    public void shouldGetSubscription() {
        final Response response = target(SUBSCRIPTION).request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(subscriptionMapper).convert(subscriptionEntity);
        
        Subscription subscription = response.readEntity(Subscription.class);
        assertNotNull(subscription);
        assertNull(subscription.getKeys());
    }
    
    @Test
    public void shouldGetSubscriptionWithKeys() {
        final Response response = target(SUBSCRIPTION).queryParam("include", "keys").request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(subscriptionMapper).convert(subscriptionEntity);
        
        Subscription subscription = response.readEntity(Subscription.class);
        assertNotNull(subscription);
        assertNotNull(subscription.getKeys());
        assertFalse(subscription.getKeys().isEmpty());
    }
    
    @Test
    public void shouldNotGetSubscription() {
        final Response response = target(UNKNOWN_SUBSCRIPTION).request().get();
        assertEquals(HttpStatusCode.NOT_FOUND_404, response.getStatus());
        
        Error error = response.readEntity(Error.class);
        assertNotNull(error);
    }
    
    @Test
    public void shouldCloseSubscription() {
        final Response response = target(SUBSCRIPTION).path("_close").request().post(null);
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        
        assertFalse(response.hasEntity());
    }

}
