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
package io.gravitee.repository.management;

import static io.gravitee.repository.utils.DateUtils.compareDate;
import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class ApiKeyRepositoryTest extends AbstractManagementRepositoryTest {

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
        apiKey.setEnvironmentId("DEFAULT");
        apiKey.setCreatedAt(new Date());
        apiKey.setRevoked(true);
        apiKey.setPaused(true);
        apiKey.setExpireAt(parse("11/02/2016"));
        apiKey.setDaysToExpirationOnLastNotification(30);

        apiKeyRepository.create(apiKey);

        Optional<ApiKey> optional = apiKeyRepository.findById(id);
        assertTrue("API Key not found", optional.isPresent());

        ApiKey keyFound = optional.get();

        assertNotNull("API Key not found", keyFound);

        assertEquals("Key value saved doesn't match", apiKey.getKey(), keyFound.getKey());
        assertTrue("Key pollInterval doesn't match", compareDate(apiKey.getExpireAt(), keyFound.getExpireAt()));
        assertEquals("Key paused status doesn't match", apiKey.isPaused(), keyFound.isPaused());
        assertEquals("Key revoked status doesn't match", apiKey.isRevoked(), keyFound.isRevoked());
        assertEquals(
            "Days to pollInterval on last notification don't match",
            apiKey.getDaysToExpirationOnLastNotification(),
            keyFound.getDaysToExpirationOnLastNotification()
        );
    }

    @Test
    public void create_should_create_federated_apiKey() throws Exception {
        String id = "id-of-new-apikey";

        ApiKey apiKey = ApiKey
            .builder()
            .id(id)
            .key("apiKey")
            .environmentId("DEFAULT")
            .subscriptions(List.of("subscription-id"))
            .createdAt(new Date())
            .federated(true)
            .build();

        apiKeyRepository.create(apiKey);

        Optional<ApiKey> result = apiKeyRepository.findById(id);
        assertThat(result).get().usingRecursiveComparison().isEqualTo(apiKey);
    }

    @Test
    public void findById_should_find_apikey() throws Exception {
        Optional<ApiKey> optional = apiKeyRepository.findById("id-of-apikey-1");

        assertTrue("API Key not found", optional.isPresent());

        ApiKey keyFound = optional.get();
        assertNotNull("API Key not found", keyFound);

        assertNotNull("No subscriptions relative to the key", keyFound.getSubscriptions());
        assertEquals("Subscriptions count does not match", 1, keyFound.getSubscriptions().size());
        assertTrue("Key paused status doesn't match", keyFound.isPaused());
        assertTrue("Key revoked status doesn't match", keyFound.isRevoked());
        assertEquals(
            "Days to pollInterval on last notification don't match",
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

        assertFalse("Invalid API Key found", apiKey.isPresent());
    }

    @Test
    public void findByKey_should_find_all_matching_API_Keys() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByKey("d449098d-8c31-4275-ad59-8dd707865a34");

        assertFalse("API Keys not found", apiKeys.isEmpty());

        assertNotNull("API Keys not found", apiKeys);
        assertEquals(2, apiKeys.size());
        assertTrue(Set.of("id-of-apikey-1", "id-of-apikey-2").containsAll(apiKeys.stream().map(ApiKey::getId).collect(toList())));
    }

    @Test
    public void findBykey_should_return_empty_list_if_key_not_found() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByKey("unknown-api-key-d449098d-8c31-42");

        assertTrue("Invalid API Keys found", apiKeys.isEmpty());
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
    public void findBySubscription_should_find_API_Keys_list() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findBySubscription("subscription1");

        assertNotNull("API Keys not found", apiKeys);
        assertEquals("Invalid number of ApiKey found", 2, apiKeys.size());
    }

    @Test
    public void findBySubscription_should_return_empty_list_if_not_found() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findBySubscription("subscription-no-api-key");

        assertNotNull("ApiKey Set is null", apiKeys);
        assertTrue("Api found on subscription with no api", apiKeys.isEmpty());
    }

    @Test
    public void findBySubscription_should_read_all_subscriptions() throws Exception {
        Set<ApiKey> apiKeys = apiKeyRepository.findBySubscription("subscriptionX");

        assertEquals(1, apiKeys.size());

        ApiKey apiKey = apiKeys.iterator().next();
        assertTrue(Set.of("subscription2", "subscriptionX").containsAll(apiKey.getSubscriptions()));
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
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().includeRevoked(false).subscriptions(singleton("sub3")).build()
        );

        assertNotNull("found API Key", apiKeys);
        assertFalse("found API Key", apiKeys.isEmpty());
        assertEquals("found 2 API Keys", 2, apiKeys.size());

        List<String> expectedKeys = List.of("findByCriteria2", "findByCriteria1");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
        assertNotEquals(apiKeys.get(0).getKey(), apiKeys.get(1).getKey());
    }

    @Test
    public void findByCriteria_should_find_by_subscriptions() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().includeRevoked(false).subscriptions(List.of("sub4", "sub5", "sub6")).build()
        );
        assertEquals(1, apiKeys.size());
        assertTrue(Set.of("sub4", "sub5", "sub6").containsAll(apiKeys.get(0).getSubscriptions()));
    }

    @Test
    public void findByCriteria_should_find_by_environments() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().includeRevoked(false).environments(Set.of("DEFAULT")).build()
        );
        assertThat(apiKeys).hasSize(1);
        assertThat(apiKeys).extracting(ApiKey::getId).containsOnly("id-of-apikey-2");
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_time_range() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().includeRevoked(false).from(1486771200000L).to(1486771400000L).subscriptions(singleton("sub3")).build()
        );

        assertNotNull("found API Key", apiKeys);
        assertFalse("found API Key", apiKeys.isEmpty());
        assertEquals("found 1 API Key " + apiKeys.stream().map(ApiKey::getKey).collect(toList()), 1, apiKeys.size());
        assertEquals("findByCriteria1", apiKeys.get(0).getKey());
    }

    @Test
    public void findByCriteria_should_find_multiple_by_subscriptions_criteria_with_time_range() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().from(1486771200000L).to(1486771900000L).subscriptions(Set.of("sub3", "sub4")).build()
        );

        assertEquals(3, apiKeys.size());
        assertTrue(
            Set.of("id-of-apikey-4", "id-of-apikey-6", "id-of-apikey-7").containsAll(apiKeys.stream().map(ApiKey::getId).collect(toList()))
        );
    }

    @Test
    public void findByCriteria_should_find_by_criteria_without_time_range_and_revoked() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().includeRevoked(true).subscriptions(singleton("sub3")).build()
        );

        assertNotNull("found API Key", apiKeys);
        assertFalse("found API Key", apiKeys.isEmpty());
        assertEquals("found 3 API Keys", 3, apiKeys.size());

        List<String> expectedKeys = List.of("findByCriteria1", "findByCriteria2", "findByCriteria1Revoked");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_between_dates() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().expireAfter(1439022010000L).expireBefore(1439022020000L).build()
        );

        assertEquals("found 2 API Keys", 2, apiKeys.size());

        List<String> expectedKeys = List.of("d449098d-8c31-4275-ad59-8dd707865a34", "d449098d-8c31-4275-ad59-8dd707865a35");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_after_dates() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(ApiKeyCriteria.builder().expireAfter(30019401755L).build());

        assertEquals("found 2 API Keys", 2, apiKeys.size());

        List<String> expectedKeys = List.of("d449098d-8c31-4275-ad59-8dd707865a34", "d449098d-8c31-4275-ad59-8dd707865a35");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_after_dates_including_no_expire_date() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().expireAfter(30019401755L).includeWithoutExpiration(true).build()
        );

        assertEquals("found 4 API Keys", 4, apiKeys.size());

        List<String> expectedKeys = List.of(
            "the-key-of-api-key-7",
            "the-key-of-api-key-8",
            "d449098d-8c31-4275-ad59-8dd707865a34",
            "d449098d-8c31-4275-ad59-8dd707865a35"
        );
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_before_dates() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(ApiKeyCriteria.builder().expireBefore(30019401755L).build());

        assertEquals("found 2 API Keys", 2, apiKeys.size());

        List<String> expectedKeys = List.of("findByCriteria2", "findByCriteria1");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_before_dates_including_no_expire_date() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().expireBefore(30019401755L).includeWithoutExpiration(true).build()
        );

        assertEquals("found 4 API Keys", 4, apiKeys.size());

        List<String> expectedKeys = List.of("findByCriteria2", "the-key-of-api-key-7", "the-key-of-api-key-8", "findByCriteria1");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void findByCriteria_should_read_subscriptions_list() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(ApiKeyCriteria.builder().expireAfter(30019401755L).build());
        apiKeys.sort(Comparator.comparing(apikey -> apikey.getSubscriptions().size()));

        assertEquals(1, apiKeys.get(0).getSubscriptions().size());
        assertEquals("subscription1", apiKeys.get(0).getSubscriptions().get(0));
        assertEquals(2, apiKeys.get(1).getSubscriptions().size());
        assertTrue(List.of("subscription2", "subscriptionX").containsAll(apiKeys.get(1).getSubscriptions()));
    }

    @Test
    public void findByCriteria_should_find_federated_keys() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(ApiKeyCriteria.builder().includeFederated(true).build());

        assertThat(apiKeys)
            .extracting(ApiKey::getId, ApiKey::isFederated)
            .contains(tuple("id-of-federated-9", true), tuple("id-of-federated-10", true));
    }

    @Test
    public void findByApplication_should_find_api_keys() throws TechnicalException {
        List<ApiKey> apiKeys = apiKeyRepository.findByApplication("app1");
        assertEquals(2, apiKeys.size());
        ApiKey apiKey5 = apiKeys.get(0);
        assertEquals("id-of-apikey-5", apiKey5.getId());
        assertEquals(List.of("sub3"), apiKey5.getSubscriptions());
        ApiKey apiKey4 = apiKeys.get(1);
        assertEquals("id-of-apikey-4", apiKey4.getId());
        assertEquals(List.of("sub3"), apiKey4.getSubscriptions());
    }

    @Test
    public void findByKeyAndApi_should_return_fulfilled_optional() throws TechnicalException {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyAndApi("findByCriteria2", "api2");
        assertFalse(apiKeyOpt.isEmpty());
        ApiKey apiKey6 = apiKeyOpt.get();
        assertEquals("id-of-apikey-6", apiKey6.getId());
        assertEquals(List.of("sub3"), apiKey6.getSubscriptions());
    }

    @Test
    public void findByKeyAndApi_should_return_empty_optional() throws TechnicalException {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyAndApi("unknown-key", "unknown-id");
        assertTrue(apiKeyOpt.isEmpty());
    }

    @Test
    public void findById_should_return_key_with_its_subscription() throws TechnicalException {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById("id-of-apikey-7");
        assertFalse(apiKeyOpt.isEmpty());
        ApiKey apiKey = apiKeyOpt.get();
        assertEquals("id-of-apikey-7", apiKey.getId());
        assertEquals("the-key-of-api-key-7", apiKey.getKey());
        assertEquals("app4", apiKey.getApplication());
        assertNotNull(apiKey.getSubscriptions());
        assertEquals(3, apiKey.getSubscriptions().size());
        assertTrue(Set.of("sub4", "sub5", "sub6").containsAll(apiKey.getSubscriptions()));
    }

    @Test
    public void findAll_should_find_all_api_keys_even_with_no_subscription() throws TechnicalException {
        Set<ApiKey> apiKeys = apiKeyRepository.findAll();

        assertThat(apiKeys).hasSize(11).extracting(ApiKey::getId).contains("id-of-apikey-8");
    }

    @Test
    public void shouldSearchSortedById() throws TechnicalException {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().includeRevoked(true).subscription("sub3").build(),
            new SortableBuilder().order(Order.ASC).field("id").build()
        );
        assertNotNull("found API Key", apiKeys);
        assertFalse("found API Key", apiKeys.isEmpty());
        assertEquals("found 3 API Keys", 3, apiKeys.size());

        List<String> expectedKeys = List.of("findByCriteria1", "findByCriteria1Revoked", "findByCriteria2");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void shouldSearchSortedByCreated() throws TechnicalException {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().includeRevoked(true).subscription("sub3").build(),
            new SortableBuilder().order(Order.DESC).field("updatedAt").build()
        );
        assertNotNull("found API Key", apiKeys);
        assertFalse("found API Key", apiKeys.isEmpty());
        assertEquals("found 3 API Keys", 3, apiKeys.size());

        List<String> expectedKeys = List.of("findByCriteria2", "findByCriteria1Revoked", "findByCriteria1");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void should_add_subscription_to_apikey() throws TechnicalException {
        Optional<ApiKey> apiKey = apiKeyRepository.findById("id-of-apikey-2");
        assertTrue(apiKey.isPresent());

        List<String> subscriptions = apiKey.get().getSubscriptions();
        Assertions.assertThat(subscriptions).hasSize(2);
        Assertions.assertThat(subscriptions).contains("subscription2", "subscriptionX");

        Optional<ApiKey> updatedApiKey = apiKeyRepository.addSubscription("id-of-apikey-2", "newSubscription");
        assertTrue(updatedApiKey.isPresent());
        List<String> updatedSubscriptions = updatedApiKey.get().getSubscriptions();
        Assertions.assertThat(updatedSubscriptions).hasSize(3);
        Assertions.assertThat(updatedSubscriptions).contains("subscription2", "subscriptionX", "newSubscription");
    }

    @Test
    public void return_empty_if_apikey_does_not_exist() throws TechnicalException {
        Optional<ApiKey> updatedApiKey = apiKeyRepository.addSubscription("unknown_apikey_id", "newSubscription");
        assertTrue(updatedApiKey.isEmpty());
    }

    @Test
    public void should_delete_by_environment_id() throws TechnicalException {
        final var beforeDeletion = apiKeyRepository
            .findAll()
            .stream()
            .filter(apiKey -> "DEFAULT".equals(apiKey.getEnvironmentId()))
            .map(ApiKey::getId)
            .toList();

        var deleted = apiKeyRepository.deleteByEnvironmentId("DEFAULT");
        final var nbAfterDeletion = apiKeyRepository
            .findAll()
            .stream()
            .filter(apiKey -> "DEFAULT".equals(apiKey.getEnvironmentId()))
            .count();

        assertEquals(beforeDeletion.size(), deleted.size());
        assertEquals(beforeDeletion, deleted);
        assertEquals(0, nbAfterDeletion);
    }
}
