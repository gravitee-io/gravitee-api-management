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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApiKeyEntity;
import io.gravitee.rest.api.portal.rest.model.Key;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SubscriptionKeysResourceTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "subscriptions/";
    }
    private static final String SUBSCRIPTION = "my-subscription";
    private static final String ANOTHER_SUBSCRIPTION = "my-other-ubscription";
    private static final String KEY = "my-key";

    private ApiKeyEntity apiKeyEntity;
    
    @Before
    public void init() {
        reset(apiKeyService);
        reset(keyMapper);
        
        
    }
    
    @Test
    public void shouldRenewSubscription() {
        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey(KEY);
        apiKeyEntity.setSubscription(SUBSCRIPTION);
        
        doReturn(apiKeyEntity).when(apiKeyService).renew(SUBSCRIPTION);
        doReturn(new Key().id(KEY)).when(keyMapper).convert(apiKeyEntity);
        
        final Response response = target(SUBSCRIPTION).path("keys/_renew").request().post(null);
        assertEquals(HttpStatusCode.CREATED_201, response.getStatus());
        
        Mockito.verify(apiKeyService).renew(SUBSCRIPTION);
        
        Key key = response.readEntity(Key.class);
        assertNotNull(key);
        assertEquals(KEY, key.getId());
    }
    
    @Test
    public void shouldRevokeKey() {
        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey(KEY);
        apiKeyEntity.setSubscription(SUBSCRIPTION);
        
        doReturn(apiKeyEntity).when(apiKeyService).findByKey(KEY);
        
        final Response response = target(SUBSCRIPTION).path("keys/"+KEY+"/_revoke").request().post(null);
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        
        Mockito.verify(apiKeyService).revoke(KEY, true);
        
        assertFalse(response.hasEntity());
    }
    
    @Test
    public void shouldRevokeKeyNoSubscription() {
        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey(KEY);
        
        doReturn(apiKeyEntity).when(apiKeyService).findByKey(KEY);
        
        final Response response = target(SUBSCRIPTION).path("keys/"+KEY+"/_revoke").request().post(null);
        assertEquals(HttpStatusCode.NO_CONTENT_204, response.getStatus());
        
        Mockito.verify(apiKeyService).revoke(KEY, true);
        
        assertFalse(response.hasEntity());
    }
    
    @Test
    public void shouldNotRevokeKey() {
        apiKeyEntity = new ApiKeyEntity();
        apiKeyEntity.setKey(KEY);
        apiKeyEntity.setSubscription(ANOTHER_SUBSCRIPTION);
        
        doReturn(apiKeyEntity).when(apiKeyService).findByKey(KEY);
        
        final Response response = target(SUBSCRIPTION).path("keys/"+KEY+"/_revoke").request().post(null);
        assertEquals(HttpStatusCode.BAD_REQUEST_400, response.getStatus());
        
        assertEquals("'keyId' parameter does not correspond to the subscription", response.readEntity(String.class));
    }

    
}
