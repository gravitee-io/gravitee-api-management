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
package io.gravitee.gateway.handlers.api.service;

import static org.junit.jupiter.api.Assertions.*;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.services.SubscriptionService;
import io.gravitee.node.cache.standalone.StandaloneCacheManager;
import org.junit.jupiter.api.Test;

public class SubscriptionServiceTest {

    SubscriptionService subscriptionService = new SubscriptionService(new StandaloneCacheManager());

    @Test
    public void should_add_and_getById_accepted_subscription_in_cache() {
        Subscription subscription = buildAcceptedSubscription("sub1", "my-api", "my-client-id");

        subscriptionService.save(subscription);

        // should find subscription in cache with relevant id
        assertTrue(subscriptionService.getById("sub1").isPresent());
        assertSame(subscription, subscriptionService.getById("sub1").get());

        // should not find with other keys
        assertFalse(subscriptionService.getById("sub2").isPresent());
        assertFalse(subscriptionService.getById("sub3").isPresent());
    }

    @Test
    public void should_add_and_getByApiAndClientId_accepted_subscription_in_cache() {
        Subscription subscription = buildAcceptedSubscription("sub1", "my-api", "my-client-id");

        subscriptionService.save(subscription);

        // should find subscription in cache with relevant id
        assertTrue(subscriptionService.getByApiAndClientId("my-api", "my-client-id").isPresent());
        assertSame(subscription, subscriptionService.getByApiAndClientId("my-api", "my-client-id").get());

        // should not find with other keys
        assertFalse(subscriptionService.getByApiAndClientId("another-api", "my-client-id").isPresent());
        assertFalse(subscriptionService.getByApiAndClientId("my-api", "another-client-id").isPresent());
    }

    @Test
    public void should_remove_rejected_subscription_from_cache() {
        Subscription subscription = buildAcceptedSubscription("sub1", "my-api", "my-client-id");

        subscriptionService.save(subscription);

        subscription.setStatus("REJECTED");

        subscriptionService.save(subscription);

        // rejected subscription has been removed from cache
        assertFalse(subscriptionService.getById("sub1").isPresent());
    }

    @Test
    public void should_update_subscription_clientId_in_cache() {
        Subscription oldSubscription = buildAcceptedSubscription("sub1", "my-api", "old-client-id");

        subscriptionService.save(oldSubscription);

        Subscription newSubscription = buildAcceptedSubscription("sub1", "my-api", "new-client-id");

        subscriptionService.save(newSubscription);

        // should find subscription in cache with its new client id
        assertTrue(subscriptionService.getByApiAndClientId("my-api", "new-client-id").isPresent());
        assertSame(newSubscription, subscriptionService.getByApiAndClientId("my-api", "new-client-id").get());

        // should not find it with its old client id
        assertFalse(subscriptionService.getByApiAndClientId("my-api", "old-client-id").isPresent());
    }

    private Subscription buildAcceptedSubscription(String id, String api, String clientId) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setApi(api);
        subscription.setClientId(clientId);
        subscription.setStatus("ACCEPTED");
        return subscription;
    }
}
