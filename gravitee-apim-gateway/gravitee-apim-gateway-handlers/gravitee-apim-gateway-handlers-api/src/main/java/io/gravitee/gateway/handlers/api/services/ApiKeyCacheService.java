/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.api.services;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApiKeyCacheService implements ApiKeyService {

    private final Map<String, ApiKey> cacheApiKeys = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> cacheApiKeysByApi = new ConcurrentHashMap<>();

    @Override
    public void register(final ApiKey apiKey) {
        if (apiKey.isActive()) {
            String cacheKey = buildCacheKey(apiKey);
            log.debug(
                "Load active api-key [id: {}] [api: {}] [plan: {}] [app: {}]",
                apiKey.getId(),
                apiKey.getApi(),
                apiKey.getPlan(),
                apiKey.getApplication()
            );
            cacheApiKeys.put(cacheKey, apiKey);
            Set<String> keysByApi = cacheApiKeysByApi.get(apiKey.getApi());
            if (keysByApi == null) {
                keysByApi = new HashSet<>();
            }
            keysByApi.add(cacheKey);
            cacheApiKeysByApi.put(apiKey.getApi(), keysByApi);
        } else {
            unregister(apiKey);
        }
    }

    @Override
    public void unregister(final ApiKey apiKey) {
        String cacheKey = buildCacheKey(apiKey);
        log.debug(
            "Unload inactive api-key [id: {}] [api: {}] [plan: {}] [app: {}]",
            apiKey.getId(),
            apiKey.getApi(),
            apiKey.getPlan(),
            apiKey.getApplication()
        );
        if (cacheApiKeys.remove(cacheKey) != null) {
            Set<String> keysByApi = cacheApiKeysByApi.get(apiKey.getApi());
            if (keysByApi != null && keysByApi.remove(cacheKey)) {
                if (keysByApi.isEmpty()) {
                    cacheApiKeysByApi.remove(apiKey.getApi());
                } else {
                    cacheApiKeysByApi.put(apiKey.getApi(), keysByApi);
                }
            }
        }
    }

    @Override
    public void unregisterByApiId(final String apiId) {
        log.debug("Unload all api-key by api [api_id: {}]", apiId);
        Set<String> keysByApi = cacheApiKeysByApi.remove(apiId);
        if (keysByApi != null) {
            keysByApi.forEach(cacheKey -> {
                ApiKey evictedApiKey = cacheApiKeys.remove(cacheKey);
                if (evictedApiKey != null) {
                    log.debug(
                        "Unload inactive api-key [id: {}] [api: {}] [plan: {}] [app: {}]",
                        evictedApiKey.getId(),
                        evictedApiKey.getApi(),
                        evictedApiKey.getPlan(),
                        evictedApiKey.getApplication()
                    );
                }
            });
        }
    }

    @Override
    public Optional<ApiKey> getByApiAndKey(String api, String key) {
        return Optional.ofNullable(cacheApiKeys.get(buildCacheKey(api, key)));
    }

    String buildCacheKey(ApiKey apiKey) {
        return buildCacheKey(apiKey.getApi(), apiKey.getKey());
    }

    String buildCacheKey(String api, String key) {
        return String.format("%s.%s", api, key);
    }
}
