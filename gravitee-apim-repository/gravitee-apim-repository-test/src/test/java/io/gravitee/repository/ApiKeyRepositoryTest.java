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

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Collections.singleton;
import static org.junit.Assert.*;

import io.gravitee.repository.config.AbstractRepositoryTest;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.ApiKeyCriteria.Builder;
import io.gravitee.repository.management.model.ApiKey;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.Test;

public class ApiKeyRepositoryTest extends AbstractRepositoryTest {

    @Override
    protected String getTestCasesPath() {
        return "/data/apikey-tests/";
    }

    @Test
    public void create_should_create_apiKey_with_right_data() throws Exception {
        String id = "id-of-new-apikey";

        ApiKey apiKey = new ApiKey();
        apiKey.setId(id);
        apiKey.setKey("apiKey");
        apiKey.setCreatedAt(new Date());
        apiKey.setRevoked(true);
        apiKey.setPaused(true);
        apiKey.setExpireAt(parse("11/02/2016"));
        apiKey.setDaysToExpirationOnLastNotification(30);

        apiKeyRepository.create(apiKey);

        Optional<ApiKey> optional = apiKeyRepository.findById(id);
        assertTrue("ApiKey not found", optional.isPresent());

        ApiKey keyFound = optional.get();

        assertNotNull("ApiKey not found", keyFound);

        assertEquals("Key value saved doesn't match", apiKey.getKey(), keyFound.getKey());
        assertTrue("Key expiration doesn't match", compareDate(apiKey.getExpireAt(), keyFound.getExpireAt()));
        assertEquals("Key paused status doesn't match", apiKey.isPaused(), keyFound.isPaused());
        assertEquals("Key revoked status doesn't match", apiKey.isRevoked(), keyFound.isRevoked());
        assertEquals(
            "Days to expiration on last notification don't match",
            apiKey.getDaysToExpirationOnLastNotification(),
            keyFound.getDaysToExpirationOnLastNotification()
        );
    }

    @Test
    public void findById_should_find_apikey() throws Exception {
        Optional<ApiKey> optional = apiKeyRepository.findById("id-of-apikey-1");

        assertTrue("ApiKey not found", optional.isPresent());

        ApiKey keyFound = optional.get();
        assertNotNull("ApiKey not found", keyFound);

        assertNotNull("No subscriptions relative to the key", keyFound.getSubscriptions());
        assertEquals("Subscriptions count does not match", 1, keyFound.getSubscriptions().size());
        assertTrue("Key paused status doesn't match", keyFound.isPaused());
        assertTrue("Key revoked status doesn't match", keyFound.isRevoked());
        assertEquals(
            "Days to expiration on last notification don't match",
            Integer.valueOf(30),
            keyFound.getDaysToExpirationOnLastNotification()
        );

        /*
         * 3.17 backward compatibility checks
         */
        assertNotNull(keyFound.getSubscription());
        assertNotNull(keyFound.getApi());
    }

    @Test
    public void findById_should_return_optional_if_key_not_found() throws Exception {
        Optional<ApiKey> apiKey = apiKeyRepository.findById("id-of-apikey-112");

        assertFalse("Invalid ApiKey found", apiKey.isPresent());
    }

    @Test
    public void findByKey_should_find_all_matching_apikeys() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByKey("d449098d-8c31-4275-ad59-8dd707865a34");

        assertFalse("ApiKeys not found", apiKeys.isEmpty());

        assertNotNull("ApiKeys not found", apiKeys);
        assertEquals(2, apiKeys.size());
        assertTrue(apiKeys.stream().anyMatch(apiKey -> apiKey.getId().equals("id-of-apikey-1")));
        assertTrue(apiKeys.stream().anyMatch(apiKey -> apiKey.getId().equals("id-of-apikey-2")));
    }

    @Test
    public void findBykey_should_return_empty_list_if_key_not_found() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByKey("unknown-api-key-d449098d-8c31-42");

        assertTrue("Invalid ApiKeys found", apiKeys.isEmpty());
    }

    @Test
    public void findBykeyAndApi_should_return_key_if_found() throws Exception {
        Optional<ApiKey> apiKey = apiKeyRepository.findByKeyAndApi("d449098d-8c31-4275-ad59-8dd707865a34", "api2");

        assertTrue(apiKey.isPresent());
        assertEquals("id-of-apikey-2", apiKey.get().getId());
    }

    @Test
    public void findBykeyAndApi_should_return_empty_optional_if_not_found() throws Exception {
        Optional<ApiKey> apiKey = apiKeyRepository.findByKeyAndApi("d449098d-8c31-4275-ad59-8dd707865a34", "api2255");

        assertFalse(apiKey.isPresent());
    }

    @Test
    public void findBySubscription_should_find_apikeys_list() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findBySubscription("subscription1");

        assertNotNull("ApiKey not found", apiKeys);
        assertEquals("Invalid number of ApiKey found", 2, apiKeys.size());
    }

    @Test
    public void findBySubscription_should_return_empty_list_if_not_found() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findBySubscription("subscription-no-api-key");

        assertNotNull("ApiKey Set is null", apiKeys);
        assertTrue("Api found on subscription with no api", apiKeys.isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void update_should_throw_exception_when_updating_unknown_key() throws Exception {
        ApiKey unknownApiKey = new ApiKey();
        unknownApiKey.setId("unknown_key_id");

        apiKeyRepository.update(unknownApiKey);

        fail("An unknown apiKey should not be updated");
    }

    @Test(expected = IllegalStateException.class)
    public void update_should_throw_exception_when_updating_null_key() throws Exception {
        apiKeyRepository.update(null);
        fail("A null apiKey should not be updated");
    }

    @Test
    public void findByCriteria_should_find_by_criteria_without_time_range() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(new Builder().includeRevoked(false).plans(singleton("plan1")).build());

        assertNotNull("found api key", apiKeys);
        assertFalse("found api key", apiKeys.isEmpty());
        assertEquals("found 2 apikeys", 2, apiKeys.size());
        assertTrue(Arrays.asList("findByCriteria1", "findByCriteria2").contains(apiKeys.get(0).getKey()));
        assertTrue(Arrays.asList("findByCriteria1", "findByCriteria2").contains(apiKeys.get(1).getKey()));
        assertNotEquals(apiKeys.get(0).getKey(), apiKeys.get(1).getKey());
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_time_range() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            new Builder().includeRevoked(false).from(1486771200000L).to(1486771400000L).plans(singleton("plan1")).build()
        );

        assertNotNull("found api key", apiKeys);
        assertFalse("found api key", apiKeys.isEmpty());
        assertEquals("found 1 api key " + apiKeys.stream().map(ApiKey::getKey).collect(Collectors.toList()), 1, apiKeys.size());
        assertEquals("findByCriteria1", apiKeys.get(0).getKey());
    }

    @Test
    public void findByCriteria_should_find_by_criteria_without_time_range_and_revoked() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(new Builder().includeRevoked(true).plans(singleton("plan1")).build());

        assertNotNull("found api key", apiKeys);
        assertFalse("found api key", apiKeys.isEmpty());
        assertEquals("found 3 apikeys", 3, apiKeys.size());

        assertEquals("findByCriteria2", apiKeys.get(0).getKey());
        assertEquals("findByCriteria1Revoked", apiKeys.get(1).getKey());
        assertEquals("findByCriteria1", apiKeys.get(2).getKey());
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_between_dates() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            new Builder().expireAfter(1439022010000L).expireBefore(1439022020000L).build()
        );

        assertEquals("found 2 apikeys", 2, apiKeys.size());
        assertEquals("d449098d-8c31-4275-ad59-8dd707865a34", apiKeys.get(0).getKey());
        assertEquals("d449098d-8c31-4275-ad59-8dd707865a35", apiKeys.get(1).getKey());
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_after_dates() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(new Builder().expireAfter(30019401755L).build());

        assertEquals("found 2 apikeys", 2, apiKeys.size());
        assertEquals("d449098d-8c31-4275-ad59-8dd707865a34", apiKeys.get(0).getKey());
        assertEquals("d449098d-8c31-4275-ad59-8dd707865a35", apiKeys.get(1).getKey());
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_before_dates() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(new Builder().expireBefore(30019401755L).build());

        assertEquals("found 2 apikeys", 2, apiKeys.size());
        assertEquals("findByCriteria2", apiKeys.get(0).getKey());
        assertEquals("findByCriteria1", apiKeys.get(1).getKey());
    }

    @Test
    public void findByKeyAndApi_should_return_fulfilled_optional() throws TechnicalException {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyAndApi("findByCriteria2", "api2");
        assertFalse(apiKeyOpt.isEmpty());
        ApiKey apiKey6 = apiKeyOpt.get();
        assertEquals("id-of-apikey-6", apiKey6.getId());
        assertEquals(List.of("sub2"), apiKey6.getSubscriptions());
    }

    @Test
    public void findByKeyAndApi_should_return_empty_optional() throws TechnicalException {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyAndApi("uknown-key", "unknown-id");
        assertTrue(apiKeyOpt.isEmpty());
    }
}
