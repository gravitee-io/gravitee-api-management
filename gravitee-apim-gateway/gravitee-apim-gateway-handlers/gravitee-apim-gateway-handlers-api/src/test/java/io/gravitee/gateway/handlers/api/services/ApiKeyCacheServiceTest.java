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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import io.gravitee.gateway.api.service.ApiKey;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.DigestUtils;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class ApiKeyCacheServiceTest {

    private ApiKeyCacheService apiKeyService;
    private Map<String, ApiKey> cacheApiKeys;
    private Map<String, ApiKey> cacheMd5ApiKeys;
    private Map<String, Set<String>> cacheApiKeysByApi;

    @BeforeEach
    public void beforeEach() throws Exception {
        apiKeyService = new ApiKeyCacheService();
        cacheApiKeys = (Map<String, ApiKey>) ReflectionTestUtils.getField(apiKeyService, "cacheApiKeys");
        cacheMd5ApiKeys = (Map<String, ApiKey>) ReflectionTestUtils.getField(apiKeyService, "cacheMd5ApiKeys");
        cacheApiKeysByApi = (Map<String, Set<String>>) ReflectionTestUtils.getField(apiKeyService, "cacheApiKeysByApi");
    }

    @Nested
    class RegisterTest {

        @Test
        void should_register_apiKey_when_apiKey_is_active() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.register(apiKey);

            String cacheKey = apiKeyService.buildCacheKey(apiKey);
            ApiKey actual = cacheApiKeys.get(cacheKey);
            assertThat(actual).isNotNull();
            assertThat(actual).isEqualTo(apiKey);

            String md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            ApiKey md5Actual = cacheMd5ApiKeys.get(md5CacheKey);
            assertThat(md5Actual).isNotNull();
            assertThat(md5Actual).isEqualTo(apiKey);

            Set<String> actuals = cacheApiKeysByApi.get("my-api");
            assertThat(actuals.contains(cacheKey)).isTrue();
        }

        @Test
        void should_not_register_apiKey_when_apiKey_is_inactive() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", false);

            apiKeyService.register(apiKey);

            String cacheKey = apiKeyService.buildCacheKey(apiKey);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            String md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            assertThat(cacheApiKeysByApi.get("my-api")).isNull();
        }
    }

    @Nested
    class UnregisterTest {

        @Test
        void should_unregister_apiKey_when_registering_inactive_apiKey() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.register(apiKey);

            ApiKey apiKeyInactive = buildApiKey("my-api", "my-key", false);

            apiKeyService.register(apiKeyInactive);

            String cacheKey = apiKeyService.buildCacheKey(apiKey);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            String md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            assertThat(cacheApiKeysByApi.get("my-api")).isNull();
        }

        @Test
        void should_do_nothing_when_unregistering_already_unregistered_apiKey() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.register(apiKey);

            ApiKey apiKeyToUnregister = buildApiKey("my-api", "my-key", true);

            apiKeyService.unregister(apiKeyToUnregister);

            String cacheKey = apiKeyService.buildCacheKey(apiKey);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            String md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            assertThat(cacheApiKeysByApi.get("my-api")).isNull();
        }

        @Test
        void should_unregister_apiKey() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.unregister(apiKey);

            String cacheKey = apiKeyService.buildCacheKey(apiKey);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            String md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            assertThat(cacheApiKeysByApi.get("my-api")).isNull();
        }

        @Test
        void should_unregister_one_apiKey_when_many_registered_by_apis() {
            for (int i = 0; i < 5; i++) {
                ApiKey apiKey = buildApiKey("my-api", "my-key-" + i, true);
                apiKeyService.register(apiKey);
            }
            ApiKey apiKey1 = buildApiKey("my-api", "my-key-1", true);
            apiKeyService.unregister(apiKey1);

            String cacheKey = apiKeyService.buildCacheKey(apiKey1);
            assertThat(cacheApiKeys.get(cacheKey)).isNull();

            String md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey1);
            assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();

            Set<String> apiKeysByApi = cacheApiKeysByApi.get("my-api");
            assertThat(apiKeysByApi).isNotNull();
            assertThat(apiKeysByApi.size()).isEqualTo(4);
        }

        @Test
        void should_unregister_all_apiKeys_by_api() {
            for (int i = 0; i < 5; i++) {
                ApiKey apiKey = buildApiKey("my-api", "my-key-" + i, true);
                apiKeyService.register(apiKey);
            }
            Set<String> apiKeysByApi = cacheApiKeysByApi.get("my-api");
            assertThat(apiKeysByApi.size()).isEqualTo(5);

            apiKeyService.unregisterByApiId("my-api");

            for (int i = 0; i < 5; i++) {
                ApiKey apiKeyToUnregister = buildApiKey("my-api", "my-key-" + i, true);

                String cacheKey = apiKeyService.buildCacheKey(apiKeyToUnregister);
                assertThat(cacheApiKeys.get(cacheKey)).isNull();

                String md5CacheKey = apiKeyService.buildMd5CacheKey(apiKeyToUnregister);
                assertThat(cacheMd5ApiKeys.get(md5CacheKey)).isNull();
            }
            assertThat(cacheApiKeysByApi.get("my-api")).isNull();
        }
    }

    @Nested
    class getTest {

        @Test
        void should_get_apiKey_when_registered() {
            ApiKey apiKey = buildApiKey("my-api", "my-key", true);

            apiKeyService.register(apiKey);

            String cacheKey = apiKeyService.buildCacheKey(apiKey);
            ApiKey actual = cacheApiKeys.get(cacheKey);
            assertThat(actual).isNotNull();
            assertThat(actual).isEqualTo(apiKey);

            String md5CacheKey = apiKeyService.buildMd5CacheKey(apiKey);
            ApiKey md5Actual = cacheMd5ApiKeys.get(md5CacheKey);
            assertThat(md5Actual).isNotNull();
            assertThat(md5Actual).isEqualTo(apiKey);

            Set<String> actuals = cacheApiKeysByApi.get("my-api");
            assertThat(actuals.contains(cacheKey)).isTrue();

            Optional<ApiKey> keyFoundOpt = apiKeyService.getByApiAndKey("my-api", "my-key");
            assertThat(keyFoundOpt).isPresent();
            ApiKey apiKeyFound = keyFoundOpt.get();
            assertThat(apiKeyFound).isEqualTo(apiKey);

            Optional<ApiKey> md5KeyFoundOpt = apiKeyService.getByApiAndMd5Key("my-api", DigestUtils.md5DigestAsHex("my-key".getBytes()));
            assertThat(md5KeyFoundOpt).isPresent();
            ApiKey md5ApiKeyFound = md5KeyFoundOpt.get();
            assertThat(md5ApiKeyFound).isEqualTo(apiKey);
        }

        @Test
        void should_not_get_apiKey_when_not_registered() {
            assertThat(cacheApiKeys.size()).isEqualTo(0);
            assertThat(cacheMd5ApiKeys.size()).isEqualTo(0);
            assertThat(cacheApiKeysByApi.size()).isEqualTo(0);

            Optional<ApiKey> keyFoundOpt = apiKeyService.getByApiAndKey("my-api", "my-key");
            assertThat(keyFoundOpt).isEmpty();

            Optional<ApiKey> md5KeyFoundOpt = apiKeyService.getByApiAndMd5Key("my-api", DigestUtils.md5DigestAsHex("my-key".getBytes()));
            assertThat(md5KeyFoundOpt).isEmpty();
        }
    }

    private ApiKey buildApiKey(String api, String key, boolean active) {
        ApiKey apiKey = new ApiKey();
        apiKey.setApi(api);
        apiKey.setKey(key);
        apiKey.setActive(active);
        apiKey.setSubscription("subscription");
        apiKey.setApplication("application");
        return apiKey;
    }
}
