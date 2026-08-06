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
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.util.DigestUtils;

/**
 * Runtime cache of active API keys.
 *
 * <p>An API key is cached once, under the scope of the subscription it belongs to — the API for a
 * regular subscription, the API Product for an API Product subscription. Lookups arrive with an API
 * and probe that API's own scope first, then the products the API belongs to, mirroring
 * {@link SubscriptionCacheService}. The scope is carried by {@code ApiKey.api}, which the sync
 * mapper fills from the subscription.</p>
 */
@CustomLog
@RequiredArgsConstructor
public class ApiKeyCacheService implements ApiKeyService {

    private final ApiProductRegistry apiProductRegistry;

    // ConcurrentMap on purpose (not plain Map): register()/unregister() rely on thread-safe
    // compute()/computeIfPresent() for the per-scope index. Unlike SubscriptionCacheService, the
    // lambdas here are idempotent, so any honest ConcurrentMap implementation is acceptable.
    private final ConcurrentMap<CacheKey, ApiKey> cacheApiKeys = new ConcurrentHashMap<>();
    private final ConcurrentMap<CacheKey, ApiKey> cacheMd5ApiKeys = new ConcurrentHashMap<>();
    // Holds api-key values only (the scope id is already the map key); unregisterByApiId()
    // re-derives the cache keys from them.
    private final ConcurrentMap<String, Set<String>> cacheApiKeysByScope = new ConcurrentHashMap<>();

    /**
     * Cache key referencing the api-key's own field strings instead of a formatted composite
     * String: no String.format and no byte[] allocation on the request hot path
     * ({@link #getByApiAndKey(String, String)} runs for every API-key authenticated request).
     *
     * @param scope the API id, or the API Product id for an API Product subscription's key
     */
    record CacheKey(String scope, String key) {}

    /**
     * The entity this key is cached under: the API for a regular subscription's key, the API
     * Product for an API Product subscription's key.
     *
     * <p>{@link ApiKey} lives in the external {@code gravitee-gateway-api} artifact and has no
     * scope field of its own, so the value rides on {@code api} — {@code ApiKeyMapper} writes
     * {@code SubscriptionScope.of(subscription)} there. Reading it through this method rather than
     * calling {@code getApi()} inline keeps that indirection visible at every use site instead of
     * only in the class javadoc.</p>
     */
    private static String scopeOf(final ApiKey apiKey) {
        return apiKey.getApi();
    }

    @Override
    public void register(final ApiKey apiKey) {
        if (apiKey.isActive()) {
            CacheKey cacheKey = buildCacheKey(apiKey);
            log.debug(
                "Load active api-key [id: {}] [scope: {}] [plan: {}] [app: {}]",
                apiKey.getId(),
                scopeOf(apiKey),
                apiKey.getPlan(),
                apiKey.getApplication()
            );
            cacheApiKeys.put(cacheKey, apiKey);
            /*
             FIXME: Kafka Gateway - find a way to not systematically cache md5 version of apiKey.
              We could use md5 cache only if `cache.apikey.md5` property is true (`config.kafka.enabled`value by default)
              Or
              We could also add a `md5Key` field in the `keys` collection, populated only when it's required.
              Based on that, we could cache only what is required
             */
            cacheMd5ApiKeys.put(buildMd5CacheKey(apiKey), apiKey);
            // compute() so concurrent register/unregister calls for the same API (sync appenders
            // run in parallel) cannot lose updates or mutate a plain HashSet across threads.
            cacheApiKeysByScope.compute(scopeOf(apiKey), (scope, keysByApi) -> {
                Set<String> keys = keysByApi != null ? keysByApi : ConcurrentHashMap.newKeySet();
                keys.add(apiKey.getKey());
                return keys;
            });
        } else {
            unregister(apiKey);
        }
    }

    @Override
    public void unregister(final ApiKey apiKey) {
        CacheKey cacheKey = buildCacheKey(apiKey);
        log.debug(
            "Unload inactive api-key [id: {}] [scope: {}] [plan: {}] [app: {}]",
            apiKey.getId(),
            scopeOf(apiKey),
            apiKey.getPlan(),
            apiKey.getApplication()
        );
        if (cacheApiKeys.remove(cacheKey) != null) {
            cacheMd5ApiKeys.remove(buildMd5CacheKey(apiKey));
            cacheApiKeysByScope.computeIfPresent(scopeOf(apiKey), (scope, keysByApi) -> {
                keysByApi.remove(apiKey.getKey());
                return keysByApi.isEmpty() ? null : keysByApi;
            });
        }
    }

    @Override
    public void unregisterByApiId(final String apiId) {
        log.debug("Unload all api-key by api [api_id: {}]", apiId);
        unregisterByScopeId(apiId);
    }

    /**
     * Unregisters every API key of the given API Product.
     *
     * <p>Undeploying one member API must not drop the keys of a product subscription — the other
     * member APIs still serve them — so {@link #unregisterByApiId(String)} no longer reaches them.
     * This is the matching entry point, driven by the API Product undeployment.</p>
     */
    public void unregisterByApiProductId(final String apiProductId) {
        log.debug("Unload all api-key by api product [api_product_id: {}]", apiProductId);
        unregisterByScopeId(apiProductId);
    }

    private void unregisterByScopeId(final String scopeId) {
        Set<String> keysByApi = cacheApiKeysByScope.remove(scopeId);
        if (keysByApi != null) {
            keysByApi.forEach(key -> {
                ApiKey evictedApiKey = cacheApiKeys.remove(buildCacheKey(scopeId, key));
                if (evictedApiKey != null) {
                    cacheMd5ApiKeys.remove(buildMd5CacheKey(evictedApiKey));
                    log.debug(
                        "Unload inactive api-key [id: {}] [scope: {}] [plan: {}] [app: {}]",
                        evictedApiKey.getId(),
                        scopeOf(evictedApiKey),
                        evictedApiKey.getPlan(),
                        evictedApiKey.getApplication()
                    );
                }
            });
        }
    }

    @Override
    public Optional<ApiKey> getByApiAndKey(String api, String key) {
        return lookup(cacheApiKeys, api, key);
    }

    @Override
    public Optional<ApiKey> getByApiAndMd5Key(String api, String md5ApiKey) {
        return lookup(cacheMd5ApiKeys, api, md5ApiKey);
    }

    /**
     * Probes the API's own scope, then the products the API belongs to. Most APIs belong to no
     * product, in which case the loop does not run and this costs one map lookup as before.
     */
    private Optional<ApiKey> lookup(final ConcurrentMap<CacheKey, ApiKey> cache, final String api, final String key) {
        ApiKey direct = cache.get(buildCacheKey(api, key));
        if (direct != null) {
            return Optional.of(direct);
        }
        for (String productId : apiProductRegistry.getApiProductIdsForApi(api)) {
            ApiKey fromProduct = cache.get(buildCacheKey(productId, key));
            if (fromProduct != null) {
                return Optional.of(fromProduct);
            }
        }
        return Optional.empty();
    }

    CacheKey buildCacheKey(ApiKey apiKey) {
        return buildCacheKey(scopeOf(apiKey), apiKey.getKey());
    }

    CacheKey buildCacheKey(String scope, String key) {
        return new CacheKey(scope, key);
    }

    CacheKey buildMd5CacheKey(ApiKey apiKey) {
        return buildCacheKey(scopeOf(apiKey), DigestUtils.md5DigestAsHex(apiKey.getKey().getBytes(StandardCharsets.UTF_8)));
    }
}
