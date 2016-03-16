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
package io.gravitee.repository;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.management.model.ApiKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static io.gravitee.repository.utils.DateUtils.parse;

public class ApiKeyRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/apikey-tests/";
    }

    @Test
    public void createKeyTest() throws Exception {
        String key = "apiKey";

        ApiKey apiKey = new ApiKey();
        apiKey.setKey(key);
        apiKey.setCreatedAt(new Date());
        apiKey.setExpiration(parse("11/02/2016"));

        apiKeyRepository.create("application1", "api1", apiKey);

        Optional<ApiKey> optional = apiKeyRepository.retrieve(key);
        Assert.assertTrue("ApiKey not found", optional.isPresent());

        ApiKey keyFound = optional.get();

        Assert.assertNotNull("ApiKey not found", keyFound);

        Assert.assertEquals("Key value saved doesn't match", apiKey.getKey(), keyFound.getKey());
        Assert.assertEquals("Key expiration doesn't match", apiKey.getExpiration(), keyFound.getExpiration());
    }

    @Test
    public void retrieveKeyTest() throws Exception {
        String key = "d449098d-8c31-4275-ad59-8dd707865a33";

        Optional<ApiKey> optional = apiKeyRepository.retrieve(key);

        Assert.assertTrue("ApiKey not found", optional.isPresent());

        ApiKey keyFound = optional.get();
        Assert.assertNotNull("ApiKey not found", keyFound);
        Assert.assertNotNull("No API relative to the key", keyFound.getApi());
    }

    @Test
    public void retrieveMissingKeyTest() throws Exception {
        String key = "d449098d-8c31-4275-ad59-000000000";

        Optional<ApiKey> optional = apiKeyRepository.retrieve(key);

        Assert.assertFalse("Invalid ApiKey found", optional.isPresent());
    }

    @Test
    public void findByApplicationTest() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findByApplication("application1");

        Assert.assertNotNull("ApiKey not found", apiKeys);
        Assert.assertEquals("Invalid number of ApiKey found", 2, apiKeys.size());
    }

    @Test
    public void findByApplicationNoResult() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findByApplication("application-no-api-key");
        Assert.assertNotNull("ApiKey Set is null", apiKeys);

        Assert.assertTrue("Api found on application with no api", apiKeys.isEmpty());
    }

    @Test
    public void findByApplicationAndApi() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findByApplicationAndApi("application1", "api1");

        Assert.assertNotNull("ApiKey Set is null", apiKeys);
        Assert.assertEquals("Invalid number of ApiKey found", 1, apiKeys.size());
    }
}
