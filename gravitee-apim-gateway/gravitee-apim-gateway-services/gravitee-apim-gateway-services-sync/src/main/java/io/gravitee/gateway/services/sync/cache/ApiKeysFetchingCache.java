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
package io.gravitee.gateway.services.sync.cache;

import io.gravitee.gateway.handlers.api.definition.ApiKey;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.model.Subscription;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@inheritDoc}
 *
 * This lazy cache implementation fetches Api keys in database if it was not yet fetched.
 * If the API key is not existing (or inactive), it's cached as an empty Optional, and won't be fetched again.
 */
public class ApiKeysFetchingCache extends ApiKeysCache {

    private static final long FETCH_ERROR_RETRY_DELAY = 30_000;
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeysFetchingCache.class);

    private final ApiKeyRepository apiKeyRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ScheduledExecutorService singleThreadExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ConcurrentHashMap<String, CompletableFuture<Optional<ApiKey>>> runningDatabaseSearches = new ConcurrentHashMap<>();

    public ApiKeysFetchingCache(
        Cache<String, Optional<ApiKey>> cache,
        ApiKeyRepository apiKeyRepository,
        SubscriptionRepository subscriptionRepository
    ) {
        super(cache);
        this.apiKeyRepository = apiKeyRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * {@inheritDoc}
     * If API key has not yet been cached, it fetches it in database and save it to the cache.
     * If the API key is not existing (or inactive), it's cached as an empty Optional, and won't be fetched again.
     *
     * @param api api
     * @param key api key value
     * @return Cached API key, or empty optional if not found
     * @throws TechnicalException
     */
    @Override
    public Optional<ApiKey> get(String api, String key) throws TechnicalException {
        String cacheKey = buildCacheKey(api, key);
        Optional<ApiKey> cachedApiKey = cache.get(cacheKey);

        // return api key if it has already been cached (as an active key, or an inactive)
        if (cachedApiKey != null) {
            return cachedApiKey;
        }

        // search api key and subscription from repository if it has never been fetched
        return runningDatabaseSearches.computeIfAbsent(cacheKey, k -> fetchApiKeyFuture(api, key, cacheKey)).join();
    }

    /**
     * {@inheritDoc}
     * It caches the api key as an Optional.empty(),
     * Meaning this API key has already been retrieved, but is not active.
     *
     * @param cacheKey api key to save to the cache
     * @return cached value
     */
    @Override
    protected Optional<ApiKey> saveInactive(String cacheKey) {
        Optional<ApiKey> cacheValue = Optional.empty();
        cache.put(cacheKey, cacheValue);
        return cacheValue;
    }

    private CompletableFuture<Optional<ApiKey>> fetchApiKeyFuture(String api, String key, String cacheKey) {
        return CompletableFuture
            .supplyAsync(
                () -> {
                    try {
                        return apiKeyRepository
                            .findByKeyAndApi(key, api)
                            .flatMap(
                                repositoryApiKey ->
                                    fetchSubscriptionByIdsAndApi(repositoryApiKey.getSubscriptions(), api)
                                        .map(subscription -> new ApiKey(repositoryApiKey, subscription))
                            );
                    } catch (TechnicalException e) {
                        throw new RuntimeException(e);
                    }
                }
            )
            .handle(
                (apiKey, throwable) -> {
                    runningDatabaseSearches.remove(cacheKey);
                    if (throwable != null) {
                        LOGGER.error("An error occurred while lazily fetching API key", throwable);
                        // as an error occurred during API key fetching, cache the key as inactive
                        // after FETCH_ERROR_RETRY_DELAY is expired, it will be evicted from cache, so fetching will be tried again
                        // this avoids fetch loops if an exceptional error occurs while querying the database
                        singleThreadExecutor.schedule(() -> cache.evict(cacheKey), FETCH_ERROR_RETRY_DELAY, TimeUnit.MILLISECONDS);
                        return saveInactive(cacheKey);
                    }
                    return apiKey.map(this::save).orElseGet(() -> saveInactive(cacheKey));
                }
            );
    }

    private Optional<Subscription> fetchSubscriptionByIdsAndApi(List<String> subscriptionsId, String api) {
        try {
            SubscriptionCriteria criteria = new SubscriptionCriteria.Builder().ids(subscriptionsId).apis(List.of(api)).build();
            return subscriptionRepository.search(criteria).stream().findFirst();
        } catch (TechnicalException e) {
            throw new RuntimeException(e);
        }
    }
}
