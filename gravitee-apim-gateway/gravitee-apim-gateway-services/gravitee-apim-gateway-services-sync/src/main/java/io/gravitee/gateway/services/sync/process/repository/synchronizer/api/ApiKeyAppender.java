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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.api;

import static java.util.stream.Collectors.groupingBy;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.ApiKeyCursor;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;

@CustomLog
public class ApiKeyAppender {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyMapper apiKeyMapper;
    private final int bulkItems;
    private final int subscriptionsChunkSize;

    public ApiKeyAppender(ApiKeyRepository apiKeyRepository, ApiKeyMapper apiKeyMapper, int bulkItems, int subscriptionsChunkSize) {
        if (bulkItems <= 0) {
            throw new IllegalArgumentException("bulkItems must be > 0 (got " + bulkItems + ")");
        }
        if (subscriptionsChunkSize <= 0) {
            throw new IllegalArgumentException("subscriptionsChunkSize must be > 0 (got " + subscriptionsChunkSize + ")");
        }
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyMapper = apiKeyMapper;
        this.bulkItems = bulkItems;
        this.subscriptionsChunkSize = subscriptionsChunkSize;
    }

    /**
     * Fetching API Keys for given deployables
     * @param deployables the deployables to update
     * @return the deployables updated with API Keys
     */
    public List<ApiReactorDeployable> appends(
        final boolean initialSync,
        final List<ApiReactorDeployable> deployables,
        final Set<String> environments
    ) {
        final Map<String, ApiReactorDeployable> deployableByApi = deployables
            .stream()
            .collect(Collectors.toMap(ApiReactorDeployable::apiId, d -> d));
        List<Subscription> allApiKeySubscriptions = deployableByApi
            .values()
            .stream()
            .filter(deployable -> deployable.subscriptions() != null && deployable.apiKeyPlans() != null)
            .flatMap(deployable ->
                deployable
                    .subscriptions()
                    .stream()
                    .filter(subscription -> subscription.getPlan() != null && deployable.apiKeyPlans().contains(subscription.getPlan()))
            )
            .collect(Collectors.toList());
        if (!allApiKeySubscriptions.isEmpty()) {
            Map<String, List<ApiKey>> apiKeyByApi = loadApiKey(initialSync, allApiKeySubscriptions, environments);
            apiKeyByApi.forEach((api, apiKeys) -> {
                ApiReactorDeployable deployable = deployableByApi.get(api);
                deployable.apiKeys(apiKeys);
            });
        }
        return deployables;
    }

    private Map<String, List<ApiKey>> loadApiKey(
        final boolean initialSync,
        final List<Subscription> subscriptions,
        final Set<String> environments
    ) {
        // Group subscriptions by ID to handle exploded API Product subscriptions (multiple subscriptions with same ID for different APIs)
        Map<String, List<Subscription>> subscriptionsByIdMulti = subscriptions.stream().collect(groupingBy(Subscription::getId));
        // Sort for deterministic chunk boundaries. Without this, HashMap iteration order picks the
        // chunk split, which is JDK-version-dependent — same input set could produce different
        // chunks across JVM upgrades, complicating reproducing customer incidents and silently
        // breaking tests that assume specific chunk membership.
        List<String> subscriptionIds = new ArrayList<>(subscriptionsByIdMulti.keySet());
        subscriptionIds.sort(java.util.Comparator.naturalOrder());

        var sortable = new SortableBuilder().field("id").order(Order.ASC).build();
        Map<String, List<ApiKey>> grouped = new HashMap<>();
        // A federated api key tied to multiple subscriptions can match more than one chunk's
        // `subscriptions IN` filter — dedup by key id so the appender doesn't double-count it.
        Set<String> seenKeyIds = new HashSet<>();

        try {
            for (int chunkStart = 0; chunkStart < subscriptionIds.size(); chunkStart += subscriptionsChunkSize) {
                List<String> chunk = subscriptionIds.subList(
                    chunkStart,
                    Math.min(chunkStart + subscriptionsChunkSize, subscriptionIds.size())
                );
                ApiKeyCriteria criteria = buildCriteria(initialSync, chunk, environments);

                ApiKeyCursor cursor = null;
                while (true) {
                    List<io.gravitee.repository.management.model.ApiKey> page = apiKeyRepository.searchAfter(
                        criteria,
                        sortable,
                        cursor,
                        bulkItems
                    );
                    if (page == null || page.isEmpty()) {
                        break;
                    }
                    for (io.gravitee.repository.management.model.ApiKey record : page) {
                        if (!seenKeyIds.add(record.getId())) {
                            continue;
                        }
                        record
                            .getSubscriptions()
                            .stream()
                            .flatMap(subscriptionId -> {
                                List<Subscription> subsForId = subscriptionsByIdMulti.get(subscriptionId);
                                if (subsForId == null || subsForId.isEmpty()) {
                                    return java.util.stream.Stream.empty();
                                }
                                return subsForId.stream().map(subscription -> apiKeyMapper.to(record, subscription));
                            })
                            .forEach(apiKey -> grouped.computeIfAbsent(apiKey.getApi(), k -> new ArrayList<>()).add(apiKey));
                    }
                    if (page.size() < bulkItems) {
                        break;
                    }
                    cursor = ApiKeyCursor.byId(page.getLast().getId());
                }
            }
            return grouped;
        } catch (Exception ex) {
            throw new SyncException("Error occurred when retrieving API Keys", ex);
        }
    }

    private static ApiKeyCriteria buildCriteria(boolean initialSync, List<String> subscriptionIds, Set<String> environments) {
        ApiKeyCriteria.ApiKeyCriteriaBuilder criteriaBuilder = ApiKeyCriteria.builder()
            .subscriptions(subscriptionIds)
            .environments(environments);
        if (initialSync) {
            criteriaBuilder.includeRevoked(false).expireAfter(Instant.now().toEpochMilli()).includeWithoutExpiration(true);
        } else {
            criteriaBuilder.includeRevoked(true);
        }
        return criteriaBuilder.build();
    }
}
