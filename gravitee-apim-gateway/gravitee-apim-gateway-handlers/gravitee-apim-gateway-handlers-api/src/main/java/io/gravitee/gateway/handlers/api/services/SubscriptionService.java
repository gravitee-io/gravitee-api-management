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
package io.gravitee.gateway.handlers.api.services;

import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheManager;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionService implements io.gravitee.gateway.api.service.SubscriptionService {

    private static final String CACHE_NAME_BY_API_AND_CLIENT_ID = "SUBSCRIPTIONS_BY_API_AND_CLIENT_ID";
    private static final String CACHE_NAME_BY_SUBSCRIPTION_ID = "SUBSCRIPTIONS_BY_ID";
    private final Cache<String, Subscription> cacheByApiClientId;
    private final Cache<String, Subscription> cacheBySubscriptionId;

    public SubscriptionService(CacheManager cacheManager) {
        cacheByApiClientId = cacheManager.getOrCreateCache(CACHE_NAME_BY_API_AND_CLIENT_ID);
        cacheBySubscriptionId = cacheManager.getOrCreateCache(CACHE_NAME_BY_SUBSCRIPTION_ID);
    }

    @Override
    public Optional<Subscription> getByApiAndClientIdAndPlan(String api, String clientId, String plan) {
        return Optional.ofNullable(cacheByApiClientId.get(buildClientIdCacheKey(api, clientId, plan)));
    }

    @Override
    public Optional<Subscription> getById(String subscriptionId) {
        return Optional.ofNullable(cacheBySubscriptionId.get(subscriptionId));
    }

    @Override
    public void save(Subscription subscription) {
        if (subscription.getClientId() != null) {
            saveInCacheByApiAndClientId(subscription);
        }
        saveInCacheBySubscriptionId(subscription);
    }

    private void saveInCacheBySubscriptionId(Subscription subscription) {
        if (ACCEPTED.name().equals(subscription.getStatus())) {
            cacheBySubscriptionId.put(subscription.getId(), subscription);
        } else {
            cacheBySubscriptionId.evict(subscription.getId());
        }
    }

    private void saveInCacheByApiAndClientId(Subscription subscription) {
        Subscription cachedSubscription = cacheBySubscriptionId.get(subscription.getId());
        String key = buildClientIdCacheKey(subscription);

        // remove subscription from cache if its client_id changed
        if (
            cachedSubscription != null &&
            cachedSubscription.getClientId() != null &&
            !cachedSubscription.getClientId().equals(subscription.getClientId())
        ) {
            cacheByApiClientId.evict(buildClientIdCacheKey(cachedSubscription));
        }

        // put or remove subscription from cache according to its status
        if (ACCEPTED.name().equals(subscription.getStatus())) {
            cacheByApiClientId.put(key, subscription);
        } else if (cachedSubscription != null) {
            cacheByApiClientId.evict(key);
        }
    }

    private String buildClientIdCacheKey(Subscription subscription) {
        return buildClientIdCacheKey(subscription.getApi(), subscription.getClientId(), subscription.getPlan());
    }

    private String buildClientIdCacheKey(String api, String clientId, String plan) {
        return String.format("%s.%s.%s", api, clientId, plan);
    }
}
