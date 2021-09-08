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
package io.gravitee.gateway.services.sync.apikeys;

import static org.junit.Assert.assertEquals;

import io.gravitee.repository.management.model.ApiKey;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeysCacheServiceTest {

    @Test
    public void buildCacheKey_from_strings_should_concatenate_api_and_key() {
        String cacheKey = ApiKeysCacheService.buildCacheKey("my-api-id", "my-api-key");
        assertEquals("my-api-id.my-api-key", cacheKey);
    }

    @Test
    public void buildCacheKey_from_apiKey_should_concatenate_api_and_key() {
        ApiKey apiKey = new ApiKey();
        apiKey.setApi("my-api-id");
        apiKey.setKey("my-api-key");

        String cacheKey = ApiKeysCacheService.buildCacheKey(apiKey);
        assertEquals("my-api-id.my-api-key", cacheKey);
    }
}
