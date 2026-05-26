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
import io.gravitee.repository.management.api.search.ApiKeyCursor;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.ApiKey;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    public void create_should_create_federated_apiKey() throws Exception {
        String id = "id-of-new-apikey";

        ApiKey apiKey = ApiKey.builder()
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
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().expireAfter(30019401755L).environments(Set.of("DEFAULT", "env5", "env7", "env8")).build()
        );

        assertEquals("found 2 API Keys", 2, apiKeys.size());

        List<String> expectedKeys = List.of("d449098d-8c31-4275-ad59-8dd707865a34", "d449098d-8c31-4275-ad59-8dd707865a35");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_after_dates_including_no_expire_date() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder()
                .expireAfter(30019401755L)
                .includeWithoutExpiration(true)
                .environments(Set.of("DEFAULT", "env5", "env7", "env8"))
                .build()
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
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder()
                .expireBefore(30019401755L)
                .environments(Set.of("DEFAULT", "env4", "env5", "env6", "env7", "env8"))
                .build()
        );

        assertEquals("found 4 API Keys", 4, apiKeys.size());

        List<String> expectedKeys = List.of("findByCriteria2", "findByCriteria1", "12345678", "12345678");
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void findByCriteria_should_find_by_criteria_with_expire_at_before_dates_including_no_expire_date() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder()
                .expireBefore(30019401755L)
                .includeWithoutExpiration(true)
                .environments(Set.of("DEFAULT", "env4", "env5", "env6", "env7", "env8"))
                .build()
        );

        assertEquals("found 6 API Keys", 6, apiKeys.size());

        List<String> expectedKeys = List.of(
            "findByCriteria2",
            "the-key-of-api-key-7",
            "the-key-of-api-key-8",
            "findByCriteria1",
            "12345678",
            "12345678"
        );
        assertTrue(expectedKeys.containsAll(apiKeys.stream().map(ApiKey::getKey).collect(toList())));
    }

    @Test
    public void findByCriteria_should_read_subscriptions_list() throws Exception {
        List<ApiKey> apiKeys = apiKeyRepository.findByCriteria(
            ApiKeyCriteria.builder().expireAfter(30019401755L).environments(Set.of("DEFAULT", "env5", "env7", "env8")).build()
        );
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

        assertThat(apiKeys).extracting(ApiKey::getId).contains("id-of-apikey-8");
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
    public void should_find_by_key_and_environment_id() throws TechnicalException {
        List<ApiKey> apiKeys = apiKeyRepository.findByKeyAndEnvironmentId("12345678", "env7");

        assertEquals(1, apiKeys.size());
        assertEquals("env7", apiKeys.get(0).getEnvironmentId());
        assertEquals("12345678", apiKeys.get(0).getKey());
    }

    @Test
    public void searchAfter_shouldReturnSingleApiKeyForEnvironment() throws TechnicalException {
        List<ApiKey> page = apiKeyRepository.searchAfter(
            ApiKeyCriteria.builder().environments(singleton("env-ka-1")).build(),
            new SortableBuilder().field("updatedAt").order(Order.ASC).build(),
            null,
            10
        );

        assertNotNull(page);
        assertEquals(1, page.size());
        assertEquals("apikey-ka-1", page.getFirst().getId());
    }

    @Test
    public void searchAfter_shouldTerminateOnExactMultipleOfPageSize() throws TechnicalException {
        ApiKeyCriteria criteria = ApiKeyCriteria.builder().environments(singleton("env-ka-term")).build();
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();
        int pageSize = 2;

        List<ApiKey> page1 = apiKeyRepository.searchAfter(criteria, sortable, null, pageSize);
        assertEquals(2, page1.size());

        ApiKey last = page1.getLast();
        List<ApiKey> page2 = apiKeyRepository.searchAfter(
            criteria,
            sortable,
            ApiKeyCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId()),
            pageSize
        );
        assertEquals(2, page2.size());

        last = page2.getLast();
        List<ApiKey> page3 = apiKeyRepository.searchAfter(
            criteria,
            sortable,
            ApiKeyCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId()),
            pageSize
        );
        assertTrue("Page after exact-multiple total must be empty", page3.isEmpty());
    }

    @Test
    public void searchAfter_shouldFilterByIncludeRevokedAndExpireAfter() throws TechnicalException {
        // includeRevoked=false (default) + expireAfter(now) + includeWithoutExpiration(true) is the
        // warmup criteria shape. Of the four flt fixtures: revoked one excluded; expired one excluded
        // (expireAt < expireAfter); active one and no-expire one included.
        ApiKeyCriteria criteria = ApiKeyCriteria.builder()
            .environments(singleton("env-ka-flt"))
            .expireAfter(10000L)
            .includeWithoutExpiration(true)
            .build();
        var sortable = new SortableBuilder().field("id").order(Order.ASC).build();

        Set<String> collected = new LinkedHashSet<>();
        ApiKeyCursor cursor = null;
        int pageSize = 10;
        int guard = 5;
        while (guard-- > 0) {
            List<ApiKey> page = apiKeyRepository.searchAfter(criteria, sortable, cursor, pageSize);
            if (page.isEmpty()) {
                break;
            }
            for (ApiKey k : page) {
                collected.add(k.getId());
            }
            cursor = ApiKeyCursor.byId(page.getLast().getId());
            if (page.size() < pageSize) {
                break;
            }
        }

        assertEquals(Set.of("apikey-ka-flt-active", "apikey-ka-flt-noexpire"), collected);
    }

    @Test
    public void searchAfter_shouldHonourExpireAfterWithIncludeWithoutExpiration() throws TechnicalException {
        // Compare searchAfter and legacy findByCriteria for the warmup criteria shape — they must
        // return identical id sets.
        ApiKeyCriteria criteria = ApiKeyCriteria.builder()
            .environments(singleton("env-ka-flt"))
            .expireAfter(10000L)
            .includeWithoutExpiration(true)
            .build();
        var sortable = new SortableBuilder().field("id").order(Order.ASC).build();

        Set<String> viaSearchAfter = new LinkedHashSet<>();
        ApiKeyCursor cursor = null;
        int pageSize = 50;
        int guard = 5;
        while (guard-- > 0) {
            List<ApiKey> page = apiKeyRepository.searchAfter(criteria, sortable, cursor, pageSize);
            if (page.isEmpty()) {
                break;
            }
            page.forEach(k -> viaSearchAfter.add(k.getId()));
            cursor = ApiKeyCursor.byId(page.getLast().getId());
            if (page.size() < pageSize) {
                break;
            }
        }

        Set<String> viaLegacy = apiKeyRepository.findByCriteria(criteria).stream().map(ApiKey::getId).collect(Collectors.toSet());

        assertEquals(
            "searchAfter and legacy findByCriteria must return identical set for expireAfter+includeWithoutExpiration",
            viaLegacy,
            viaSearchAfter
        );
    }

    @Test
    public void searchAfter_shouldReturnCompleteSubscriptionsListAndNotShortPageDueToJoin() throws TechnicalException {
        // Regression: JDBC LIMIT applied to keys LEFT JOIN key_subscriptions would consume
        // key_subscriptions rows against the page budget. With pageSize=2 and two api keys
        // (3 + 2 subscription rows), a naive LIMIT 2 on the join returns only the first key
        // with partial subscriptions list. The page must contain exactly the two api keys with
        // their complete subscriptions lists.
        ApiKeyCriteria criteria = ApiKeyCriteria.builder().environments(singleton("env-ka-join")).build();
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();

        List<ApiKey> page = apiKeyRepository.searchAfter(criteria, sortable, null, 2);
        assertEquals(2, page.size());
        ApiKey a = page
            .stream()
            .filter(k -> "apikey-ka-join-a".equals(k.getId()))
            .findFirst()
            .orElseThrow();
        ApiKey b = page
            .stream()
            .filter(k -> "apikey-ka-join-b".equals(k.getId()))
            .findFirst()
            .orElseThrow();
        assertEquals(Set.of("sub-ka-join-a1", "sub-ka-join-a2", "sub-ka-join-a3"), Set.copyOf(a.getSubscriptions()));
        assertEquals(Set.of("sub-ka-join-b1", "sub-ka-join-b2"), Set.copyOf(b.getSubscriptions()));

        // Short page contract: next call past last key returns empty
        ApiKey last = page.getLast();
        List<ApiKey> next = apiKeyRepository.searchAfter(
            criteria,
            sortable,
            ApiKeyCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId()),
            2
        );
        assertTrue(next.isEmpty());
    }

    @Test
    public void searchAfter_shouldPaginateByIdWithSubscriptionsFilter() throws TechnicalException {
        ApiKeyCriteria criteria = ApiKeyCriteria.builder()
            .subscriptions(Set.of("sub-ka-warmup-a", "sub-ka-warmup-b", "sub-ka-warmup-c"))
            .build();
        var sortable = new SortableBuilder().field("id").order(Order.ASC).build();

        Set<String> collected = new LinkedHashSet<>();
        ApiKeyCursor cursor = null;
        int pageSize = 2;
        int guard = 10;
        while (guard-- > 0) {
            List<ApiKey> page = apiKeyRepository.searchAfter(criteria, sortable, cursor, pageSize);
            if (page.isEmpty()) {
                break;
            }
            for (ApiKey k : page) {
                assertTrue("Duplicate id across pages: " + k.getId(), collected.add(k.getId()));
            }
            ApiKey last = page.getLast();
            cursor = ApiKeyCursor.byId(last.getId());
            if (page.size() < pageSize) {
                break;
            }
        }

        assertEquals(
            "Warmup IN filter returns matching api keys exactly — no leak from unrelated subscription",
            Set.of("apikey-ka-warmup-1", "apikey-ka-warmup-2", "apikey-ka-warmup-3"),
            collected
        );
    }

    @Test
    public void searchAfter_shouldNotSkipOrDuplicateAcrossSameMsBoundary() throws TechnicalException {
        ApiKeyCriteria criteria = ApiKeyCriteria.builder().environments(singleton("env-ka-3")).build();
        var sortable = new SortableBuilder().field("updatedAt").order(Order.ASC).build();

        Set<String> collected = new LinkedHashSet<>();
        ApiKeyCursor cursor = null;
        int pageSize = 2;
        int guard = 10;
        while (guard-- > 0) {
            List<ApiKey> page = apiKeyRepository.searchAfter(criteria, sortable, cursor, pageSize);
            if (page.isEmpty()) {
                break;
            }
            for (ApiKey k : page) {
                assertTrue("Duplicate id across pages: " + k.getId(), collected.add(k.getId()));
            }
            ApiKey last = page.getLast();
            cursor = ApiKeyCursor.byUpdatedAt(last.getUpdatedAt().getTime(), last.getId());
            if (page.size() < pageSize) {
                break;
            }
        }

        assertEquals(
            "All 5 same-ms api keys returned exactly once",
            Set.of("apikey-ka-3-a", "apikey-ka-3-b", "apikey-ka-3-c", "apikey-ka-3-d", "apikey-ka-3-e"),
            collected
        );
    }

    @Test
    public void searchAfter_shouldRejectUnsupportedSortableField() {
        var sortable = new SortableBuilder().field("createdAt").order(Order.ASC).build();
        try {
            apiKeyRepository.searchAfter(ApiKeyCriteria.builder().build(), sortable, null, 10);
            fail("Expected TechnicalException for unsupported sortable field");
        } catch (TechnicalException expected) {
            assertTrue(expected.getMessage().toLowerCase().contains("createdat"));
        }
    }

    @Test
    public void searchAfter_shouldReturnEmptyWhenNoMatch() throws TechnicalException {
        List<ApiKey> page = apiKeyRepository.searchAfter(
            ApiKeyCriteria.builder().environments(singleton("env-ka-empty")).build(),
            new SortableBuilder().field("updatedAt").order(Order.ASC).build(),
            null,
            10
        );

        assertNotNull(page);
        assertTrue(page.isEmpty());
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
        assertEquals(Set.copyOf(beforeDeletion), Set.copyOf(deleted));
        assertEquals(0, nbAfterDeletion);
    }
}
