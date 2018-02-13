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
import io.gravitee.repository.management.api.search.ApiKeyCriteria.Builder;
import io.gravitee.repository.management.model.ApiKey;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Collections.singleton;
import static org.junit.Assert.*;

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
        apiKey.setExpireAt(parse("11/02/2016"));

        apiKeyRepository.create(apiKey);

        Optional<ApiKey> optional = apiKeyRepository.findById(key);
        Assert.assertTrue("ApiKey not found", optional.isPresent());

        ApiKey keyFound = optional.get();

        Assert.assertNotNull("ApiKey not found", keyFound);

        Assert.assertEquals("Key value saved doesn't match", apiKey.getKey(), keyFound.getKey());
        Assert.assertEquals("Key expiration doesn't match", apiKey.getExpireAt(), keyFound.getExpireAt());
    }

    @Test
    public void retrieveKeyTest() throws Exception {
        String key = "d449098d-8c31-4275-ad59-8dd707865a33";

        Optional<ApiKey> optional = apiKeyRepository.findById(key);

        Assert.assertTrue("ApiKey not found", optional.isPresent());

        ApiKey keyFound = optional.get();
        Assert.assertNotNull("ApiKey not found", keyFound);
        Assert.assertNotNull("No subscription relative to the key", keyFound.getSubscription());
    }

    @Test
    public void retrieveMissingKeyTest() throws Exception {
        String key = "d449098d-8c31-4275-ad59-000000000";

        Optional<ApiKey> optional = apiKeyRepository.findById(key);

        Assert.assertFalse("Invalid ApiKey found", optional.isPresent());
    }

    @Test
    public void findBySubscriptionTest() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findBySubscription("subscription1");

        Assert.assertNotNull("ApiKey not found", apiKeys);
        Assert.assertEquals("Invalid number of ApiKey found", 2, apiKeys.size());
    }

    @Test
    public void findBySubscriptionNoResult() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findBySubscription("subscription-no-api-key");
        Assert.assertNotNull("ApiKey Set is null", apiKeys);

        Assert.assertTrue("Api found on subscription with no api", apiKeys.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateUnknownApiKey() throws Exception {
        ApiKey unknownApiKey = new ApiKey();
        unknownApiKey.setKey("unknown");
        apiKeyRepository.update(unknownApiKey);
        fail("An unknown apiKey should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdateNull() throws Exception {
        apiKeyRepository.update(null);
        fail("A null apiKey should not be updated");
    }

    @Test
    public void shouldFindByCriteriaWithoutTimeRange() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
                new Builder().
                        includeRevoked(false).
                        plans(singleton("plan1")).
                        build());

        assertNotNull("found api key", apiKeys);
        assertFalse("found api key", apiKeys.isEmpty());
        assertEquals("found 2 apikeys", 2, apiKeys.size());
        assertTrue(Arrays.asList("findByCriteria1","findByCriteria2").contains(apiKeys.get(0).getKey()));
        assertTrue(Arrays.asList("findByCriteria1","findByCriteria2").contains(apiKeys.get(1).getKey()));
        assertNotEquals(apiKeys.get(0).getKey(), apiKeys.get(1).getKey());
    }

    @Test
    public void shouldFindByCriteriaWithTimeRange() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
                new Builder().
                        includeRevoked(false).
                        from(1486771200000L).
                        to(1486771400000L).
                        plans(singleton("plan1")).
                        build());

        assertNotNull("found api key", apiKeys);
        assertFalse("found api key", apiKeys.isEmpty());
        assertEquals("found 1 api key " + apiKeys.stream().map(ApiKey::getKey).collect(Collectors.toList()), 1, apiKeys.size());
        assertEquals("findByCriteria1", apiKeys.get(0).getKey());
    }

    @Test
    public void shouldFindByCriteriaWithoutTimeRangeAndRevoked() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
                new Builder().
                        includeRevoked(true).
                        plans(singleton("plan1")).
                        build());

        assertNotNull("found api key", apiKeys);
        assertFalse("found api key", apiKeys.isEmpty());
        assertEquals("found 3 apikeys", 3, apiKeys.size());
        assertTrue(Arrays.asList("findByCriteria1","findByCriteria2","findByCriteria1Revoked").contains(apiKeys.get(0).getKey()));
        assertTrue(Arrays.asList("findByCriteria1","findByCriteria2","findByCriteria1Revoked").contains(apiKeys.get(1).getKey()));
        assertTrue(Arrays.asList("findByCriteria1","findByCriteria2","findByCriteria1Revoked").contains(apiKeys.get(2).getKey()));
        assertNotEquals(apiKeys.get(0).getKey(), apiKeys.get(1).getKey());
        assertNotEquals(apiKeys.get(0).getKey(), apiKeys.get(2).getKey());
        assertNotEquals(apiKeys.get(2).getKey(), apiKeys.get(1).getKey());
    }
}
