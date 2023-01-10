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

import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;

import io.gravitee.gateway.handlers.api.definition.ApiKey;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.repository.exceptions.TechnicalException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API keys are cached using a combination of apiKey and apiId as cache key.
 *
 * @author GraviteeSource Team
 */
public class ApiKeysCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeysCache.class);

    protected Cache<String, Optional<ApiKey>> cache;

    public ApiKeysCache(Cache<String, Optional<ApiKey>> cache) {
        this.cache = cache;
    }

    /**
     * Get an API key from the cache
     *
     * @param api api
     * @param key api key value
     * @return Cached API key, or empty optional if not found
     * @throws TechnicalException
     */
    public Optional<ApiKey> get(String api, String key) throws TechnicalException {
        Optional<ApiKey> cachedValue = cache.get(buildCacheKey(api, key));
        return cachedValue != null ? cachedValue : Optional.empty();
    }

    /**
     * Save an API key to the cache.
     *
     * @param apiKey api key to save to the cache
     * @return cached value
     */
    public Optional<ApiKey> save(ApiKey apiKey) {
        if (!apiKey.isActive()) {
            return saveInactive(apiKey);
        }
        return saveActive(apiKey);
    }

    /**
     * Save an active API key to the cache
     *
     * @param apiKey api key to save to the cache
     * @return cached value
     */
    private Optional<ApiKey> saveActive(ApiKey apiKey) {
        LOGGER.debug("Caching an api-key [id: {}] [plan: {}] [app: {}]", apiKey.getId(), apiKey.getPlan(), apiKey.getApplication());
        Optional<ApiKey> cacheValue = Optional.of(apiKey);
        cache.put(buildCacheKey(apiKey), cacheValue);
        return cacheValue;
    }

    /**
     * Save an inactive API key to the cache.
     *
     * @param cacheKey cache key
     * @return cached value
     */
    protected Optional<ApiKey> saveInactive(String cacheKey) {
        cache.evict(cacheKey);
        return Optional.empty();
    }

    protected Optional<ApiKey> saveInactive(String api, String key) {
        return saveInactive(buildCacheKey(api, key));
    }

    protected String buildCacheKey(String api, String key) {
        return String.format("%s.%s", api, key);
    }

    private Optional<ApiKey> saveInactive(ApiKey apiKey) {
        return saveInactive(buildCacheKey(apiKey));
    }

    private String buildCacheKey(ApiKey apiKey) {
        return buildCacheKey(apiKey.getApi(), apiKey.getKey());
    }
}
