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
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.services.ApiKeyService;
import io.gravitee.gateway.handlers.api.services.SubscriptionService;
import io.gravitee.gateway.jupiter.api.policy.SecurityToken;
import io.gravitee.node.cache.standalone.StandaloneCacheManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SubscriptionServiceTest {

    private static final String PLAN_ID = "my-test-plan-id";
    private static final String API_ID = "my-test-api-id";
    private static final String SUB_ID = "my-test-subscription-id";
    private static final String API_KEY = "my-test-api-key";
    private static final String CLIENT_ID = "my-test-client-id";

    @Mock
    private ApiKeyService apiKeyService;

    private SubscriptionService subscriptionService;

    @BeforeEach
    public void setup() {
        subscriptionService = new SubscriptionService(new StandaloneCacheManager(), apiKeyService);
    }

    @Test
    public void should_add_and_getById_accepted_subscription_in_cache() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);

        subscriptionService.save(subscription);

        // should find subscription in cache with relevant id
        Optional<Subscription> foundSubscription = subscriptionService.getById(SUB_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find with other keys
        assertFalse(subscriptionService.getById("sub2").isPresent());
        assertFalse(subscriptionService.getById("sub3").isPresent());
    }

    @Test
    public void should_add_and_getByApiAndClientId_accepted_subscription_in_cache() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);

        subscriptionService.save(subscription);

        // should find subscription in cache with relevant id
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, CLIENT_ID, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find with other keys
        assertFalse(subscriptionService.getByApiAndClientIdAndPlan("another-api", CLIENT_ID, PLAN_ID).isPresent());
        assertFalse(subscriptionService.getByApiAndClientIdAndPlan(API_ID, "another-client-id", PLAN_ID).isPresent());
    }

    @Test
    public void should_add_and_getBySecurityToken_with_clientId() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);
        subscriptionService.save(subscription);

        // should find subscription in cache with relevant security token
        SecurityToken securityToken = SecurityToken.forClientId(CLIENT_ID);
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find subscription in cache with another security token
        securityToken = SecurityToken.forClientId("another-client-id");
        assertFalse(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID).isPresent());
    }

    @Test
    public void should_add_and_getBySecurityToken_with_apiKey() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, null, PLAN_ID);
        subscriptionService.save(subscription);

        // mock apiKeyService : it finds related Apikey, linked to subscription
        ApiKey apiKey = new ApiKey();
        apiKey.setSubscription(SUB_ID);
        when(apiKeyService.getByApiAndKey(API_ID, API_KEY)).thenReturn(Optional.of(apiKey));

        // should find subscription in cache with relevant security token
        SecurityToken securityToken = SecurityToken.forApiKey(API_KEY);
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(subscription, foundSubscription.get());

        // should not find subscription in cache with another security token
        securityToken = SecurityToken.forApiKey("another-api-key");
        assertFalse(subscriptionService.getByApiAndSecurityToken(API_ID, securityToken, PLAN_ID).isPresent());
    }

    @Test
    public void should_remove_rejected_subscription_from_cache() {
        Subscription subscription = buildAcceptedSubscription(SUB_ID, API_ID, CLIENT_ID, PLAN_ID);

        subscriptionService.save(subscription);

        subscription.setStatus("REJECTED");

        subscriptionService.save(subscription);

        // rejected subscription has been removed from cache
        assertFalse(subscriptionService.getById(SUB_ID).isPresent());
    }

    @Test
    public void should_update_subscription_clientId_in_cache() {
        Subscription oldSubscription = buildAcceptedSubscription(SUB_ID, API_ID, "old-client-id", PLAN_ID);

        subscriptionService.save(oldSubscription);

        Subscription newSubscription = buildAcceptedSubscription(SUB_ID, API_ID, "new-client-id", PLAN_ID);

        subscriptionService.save(newSubscription);

        // should find subscription in cache with its new client id
        Optional<Subscription> foundSubscription = subscriptionService.getByApiAndClientIdAndPlan(API_ID, "new-client-id", PLAN_ID);
        assertTrue(foundSubscription.isPresent());
        assertSame(newSubscription, foundSubscription.get());

        // should not find it with its old client id
        assertFalse(subscriptionService.getByApiAndClientIdAndPlan(API_ID, "old-client-id", PLAN_ID).isPresent());
    }

    private Subscription buildAcceptedSubscription(String id, String api, String clientId, String plan) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setApi(api);
        subscription.setPlan(plan);
        subscription.setClientId(clientId);
        subscription.setStatus("ACCEPTED");
        return subscription;
    }
}
