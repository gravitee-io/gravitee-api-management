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

import io.gravitee.node.api.cache.Cache;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API keys are cached using a combination of apiKey and apiId as cache key.
 *
 * @author GraviteeSource Team
 */
public class ApiKeysCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeysCache.class);

    protected Cache<String, ApiKey> cache;

    public ApiKeysCache(Cache<String, ApiKey> cache) {
        this.cache = cache;
    }

    public void remove(ApiKey apiKey) {
        LOGGER.debug(
            "Remove an api-key from cache [id: {}] [plan: {}] [app: {}]",
            apiKey.getId(),
            apiKey.getPlan(),
            apiKey.getApplication()
        );
        cache.evict(buildCacheKey(apiKey));
    }

    public void put(ApiKey apiKey) {
        LOGGER.debug("Caching an api-key [id: {}] [plan: {}] [app: {}]", apiKey.getId(), apiKey.getPlan(), apiKey.getApplication());
        cache.put(buildCacheKey(apiKey), apiKey);
    }

    public ApiKey get(String api, String key) {
        return cache.get(buildCacheKey(api, key));
    }

    private String buildCacheKey(ApiKey apiKey) {
        return buildCacheKey(apiKey.getApi(), apiKey.getKey());
    }

    private String buildCacheKey(String api, String key) {
        return String.format("%s.%s", api, key);
    }
}
