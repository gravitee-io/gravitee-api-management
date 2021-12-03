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

import static org.junit.Assert.*;

import io.gravitee.node.api.cache.CacheConfiguration;
import io.gravitee.node.cache.standalone.StandaloneCache;
import io.gravitee.repository.management.model.ApiKey;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeysCacheTest {

    private final ApiKeysCache apiKeysCache = new ApiKeysCache(new StandaloneCache<>("ApiKeysCacheTest", new CacheConfiguration()));

    @Mock
    private ApiKey cachedApiKey1;

    @Mock
    private ApiKey cachedApiKey2;

    @Mock
    private ApiKey cachedApiKey3;

    @Before
    public void initInternalCache() {
        apiKeysCache.cache.put("another-apiId.another-key", cachedApiKey1);
        apiKeysCache.cache.put("api-id.key-id", cachedApiKey2);
        apiKeysCache.cache.put("another-apiId2.another-key", cachedApiKey3);
    }

    @Test
    public void get_should_return_apikey_when_it_exists() {
        ApiKey resultKey = apiKeysCache.get("api-id", "key-id");

        assertSame(cachedApiKey2, resultKey);
    }

    @Test
    public void get_should_return_null_when_it_doesnt_exists() {
        ApiKey resultKey = apiKeysCache.get("api-id6", "key-id");

        assertNull(resultKey);
    }

    @Test
    public void put_should_put_apikey_in_internal_map() {
        ApiKey apiKey = new ApiKey();
        apiKey.setApi("api-id6");
        apiKey.setKey("key-id");

        apiKeysCache.put(apiKey);

        assertSame(apiKey, apiKeysCache.cache.get("api-id6.key-id"));
    }

    @Test
    public void remove_should_remove_apikey_from_internal_map() {
        ApiKey apiKey = new ApiKey();
        apiKey.setApi("api-id");
        apiKey.setKey("key-id");

        apiKeysCache.remove(apiKey);

        assertEquals(2, apiKeysCache.cache.size());
        assertNull(apiKeysCache.cache.get("api-id.key-id"));
    }
}
