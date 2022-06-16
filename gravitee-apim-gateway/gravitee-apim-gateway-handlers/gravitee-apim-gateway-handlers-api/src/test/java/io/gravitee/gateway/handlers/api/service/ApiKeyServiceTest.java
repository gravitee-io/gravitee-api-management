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

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.handlers.api.services.ApiKeyService;
import io.gravitee.node.cache.standalone.StandaloneCacheManager;
import org.junit.jupiter.api.Test;

public class ApiKeyServiceTest {

    ApiKeyService apiKeyService = new ApiKeyService(new StandaloneCacheManager());

    @Test
    public void should_add_and_get_active_apiKey_in_cache() {
        ApiKey apiKey = buildApiKey("my-api", "my-key");

        apiKeyService.save(apiKey);

        // should find api key in cache with relevant api/key
        assertTrue(apiKeyService.getByApiAndKey("my-api", "my-key").isPresent());
        assertSame(apiKey, apiKeyService.getByApiAndKey("my-api", "my-key").get());

        // should not find with other keys
        assertFalse(apiKeyService.getByApiAndKey("another-api", "my-key").isPresent());
        assertFalse(apiKeyService.getByApiAndKey("my-api", "another-key").isPresent());
    }

    @Test
    public void should_remove_expired_apiKey_from_cache() {
        ApiKey apiKey = buildApiKey("my-api", "my-key");

        apiKeyService.save(apiKey);

        apiKey.setRevoked(true);

        apiKeyService.save(apiKey);

        // revoked API key has been removed from cache
        assertFalse(apiKeyService.getByApiAndKey("my-api", "my-key").isPresent());
    }

    @Test
    public void should_remove_paused_apiKey_from_cache() {
        ApiKey apiKey = buildApiKey("my-api", "my-key");

        apiKeyService.save(apiKey);

        apiKey.setPaused(true);

        apiKeyService.save(apiKey);

        // paused API key has been removed from cache
        assertFalse(apiKeyService.getByApiAndKey("my-api", "my-key").isPresent());
    }

    private ApiKey buildApiKey(String api, String key) {
        ApiKey apiKey = new ApiKey();
        apiKey.setApi(api);
        apiKey.setKey(key);
        return apiKey;
    }
}
