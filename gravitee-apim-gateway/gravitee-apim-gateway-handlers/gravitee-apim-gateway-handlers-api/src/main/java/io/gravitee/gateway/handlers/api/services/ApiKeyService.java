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

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.node.api.cache.Cache;
import io.gravitee.node.api.cache.CacheManager;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyService implements io.gravitee.gateway.api.service.ApiKeyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyService.class);
    private static final String CACHE_NAME = "API_KEYS";

    private final Cache<String, ApiKey> cache;

    public ApiKeyService(CacheManager cacheManager) {
        cache = cacheManager.getOrCreateCache(CACHE_NAME);
    }

    @Override
    public void save(ApiKey apiKey) {
        String cacheKey = buildCacheKey(apiKey);
        if (apiKey.isRevoked() || apiKey.isPaused()) {
            LOGGER.debug(
                "Remove an api-key from cache [id: {}] [plan: {}] [app: {}]",
                apiKey.getId(),
                apiKey.getPlan(),
                apiKey.getApplication()
            );
            cache.evict(cacheKey);
        } else {
            LOGGER.debug("Caching an api-key [id: {}] [plan: {}] [app: {}]", apiKey.getId(), apiKey.getPlan(), apiKey.getApplication());
            cache.put(cacheKey, apiKey);
        }
    }

    @Override
    public Optional<ApiKey> getByApiAndKey(String api, String key) {
        return Optional.ofNullable(cache.get(buildCacheKey(api, key)));
    }

    private String buildCacheKey(ApiKey apiKey) {
        return buildCacheKey(apiKey.getApi(), apiKey.getKey());
    }

    private String buildCacheKey(String api, String key) {
        return String.format("%s.%s", api, key);
    }
}
