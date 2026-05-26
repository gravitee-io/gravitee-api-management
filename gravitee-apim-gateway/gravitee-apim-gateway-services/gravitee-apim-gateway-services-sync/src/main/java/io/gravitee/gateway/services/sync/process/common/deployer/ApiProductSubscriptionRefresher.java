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
package io.gravitee.gateway.services.sync.process.common.deployer;

import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;
import static io.gravitee.repository.management.model.Subscription.Status.CLOSED;
import static io.gravitee.repository.management.model.Subscription.Status.PAUSED;
import static io.gravitee.repository.management.model.Subscription.Status.PENDING;

import io.gravitee.gateway.api.service.ApiKey;
import io.gravitee.gateway.api.service.ApiKeyService;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionService;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.ApiKeyCursor;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.gravitee.repository.management.model.SubscriptionReferenceType;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;

@CustomLog
public class ApiProductSubscriptionRefresher {

    private static final List<String> INCREMENTAL_STATUS = List.of(ACCEPTED.name(), CLOSED.name(), PAUSED.name(), PENDING.name());

    private final SubscriptionRepository subscriptionRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final SubscriptionService subscriptionService;
    private final ApiKeyService apiKeyService;
    private final int bulkItems;
    private final int subscriptionsChunkSize;

    public ApiProductSubscriptionRefresher(
        SubscriptionRepository subscriptionRepository,
        ApiKeyRepository apiKeyRepository,
        SubscriptionMapper subscriptionMapper,
        ApiKeyMapper apiKeyMapper,
        SubscriptionService subscriptionService,
        ApiKeyService apiKeyService,
        int bulkItems,
        int subscriptionsChunkSize
    ) {
        if (bulkItems <= 0) {
            throw new IllegalArgumentException("bulkItems must be > 0 (got " + bulkItems + ")");
        }
        if (subscriptionsChunkSize <= 0) {
            throw new IllegalArgumentException("subscriptionsChunkSize must be > 0 (got " + subscriptionsChunkSize + ")");
        }
        this.subscriptionRepository = subscriptionRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.subscriptionMapper = subscriptionMapper;
        this.apiKeyMapper = apiKeyMapper;
        this.subscriptionService = subscriptionService;
        this.apiKeyService = apiKeyService;
        this.bulkItems = bulkItems;
        this.subscriptionsChunkSize = subscriptionsChunkSize;
    }

    /**
     * Refreshes subscriptions for the given API Product plans.
     * Loads all subscriptions for the plans, explodes them to all APIs via SubscriptionMapper,
     * and deploys them using the subscription and API key deployers.
     *
     * @param subscribablePlans the plan IDs to refresh subscriptions for
     * @param environments the environments to filter by
     * @return a Completable that completes when subscriptions are refreshed
     */
    public Completable refresh(final Set<String> subscribablePlans, final Set<String> environments) {
        if (subscribablePlans == null || subscribablePlans.isEmpty()) {
            log.debug("No subscribable plans to refresh subscriptions for");
            return Completable.complete();
        }

        return Completable.fromRunnable(() -> {
            try {
                // Load subscriptions for the product plans
                List<Subscription> subscriptions = loadSubscriptions(subscribablePlans, environments);
                log.debug("Loaded {} subscriptions for API Product plans", subscriptions.size());

                // Deploy subscriptions
                subscriptions.forEach(subscription -> {
                    try {
                        subscriptionService.register(subscription);
                        log.debug("Deployed subscription [{}] for api [{}]", subscription.getId(), subscription.getApi());
                    } catch (Exception e) {
                        log.warn("Failed to deploy subscription [{}]", subscription.getId(), e);
                    }
                });

                // Load and deploy API keys
                List<ApiKey> apiKeys = loadApiKeys(subscriptions, environments);
                log.debug("Loaded {} API keys for subscriptions", apiKeys.size());

                apiKeys.forEach(apiKey -> {
                    try {
                        apiKeyService.register(apiKey);
                        log.debug("Deployed API key [{}] for api [{}]", apiKey.getId(), apiKey.getApi());
                    } catch (Exception e) {
                        log.warn("Failed to deploy API key [{}]", apiKey.getId(), e);
                    }
                });
            } catch (Exception ex) {
                throw new SyncException("Error occurred when refreshing subscriptions for API Product", ex);
            }
        });
    }

    /**
     * Unregisters subscriptions and API keys for APIs that were removed from the API Product.
     *
     * @param removedApiIds APIs that were removed from the product
     * @param subscribablePlans the product's plan IDs
     * @param environments the environments to filter by
     */
    public Completable unregisterRemovedApis(
        final Set<String> removedApiIds,
        final Set<String> subscribablePlans,
        final Set<String> environments
    ) {
        if (removedApiIds == null || removedApiIds.isEmpty() || subscribablePlans == null || subscribablePlans.isEmpty()) {
            return Completable.complete();
        }

        return Completable.fromRunnable(() -> {
            try {
                var repoSubs = loadSubscriptionModels(subscribablePlans, environments);
                var subsToUnregister = repoSubs
                    .stream()
                    .filter(s -> s.getReferenceType() == SubscriptionReferenceType.API_PRODUCT)
                    .flatMap(s -> removedApiIds.stream().map(id -> subscriptionMapper.toSubscriptionForApi(s, id)))
                    .toList();

                subsToUnregister.forEach(sub ->
                    unregisterQuietly(() -> subscriptionService.unregister(sub), sub.getId(), "subscription", sub.getApi())
                );
                loadApiKeys(subsToUnregister, environments)
                    .stream()
                    .filter(k -> removedApiIds.contains(k.getApi()))
                    .forEach(k -> unregisterQuietly(() -> apiKeyService.unregister(k), k.getId(), "API key", k.getApi()));
            } catch (Exception ex) {
                throw new SyncException("Error occurred when unregistering subscriptions for removed APIs from API Product", ex);
            }
        });
    }

    private void unregisterQuietly(Runnable action, String itemId, String type, String apiId) {
        try {
            action.run();
            log.debug("Unregistered {} [{}] for removed api [{}]", type, itemId, apiId);
        } catch (Exception e) {
            log.warn("Failed to unregister {} for api [{}]", type, apiId, e);
        }
    }

    private List<io.gravitee.repository.management.model.Subscription> loadSubscriptionModels(
        final Set<String> plans,
        final Set<String> environments
    ) {
        SubscriptionCriteria.SubscriptionCriteriaBuilder criteriaBuilder = SubscriptionCriteria.builder()
            .plans(plans)
            .environments(environments)
            .statuses(INCREMENTAL_STATUS);

        try {
            return subscriptionRepository.search(
                criteriaBuilder.build(),
                new SortableBuilder().field("updatedAt").order(Order.ASC).build()
            );
        } catch (Exception ex) {
            throw new SyncException("Error occurred when retrieving subscriptions for API Product", ex);
        }
    }

    private List<Subscription> loadSubscriptions(final Set<String> plans, final Set<String> environments) {
        List<io.gravitee.repository.management.model.Subscription> repoSubscriptions = loadSubscriptionModels(plans, environments);
        return repoSubscriptions
            .stream()
            .flatMap(subscription -> subscriptionMapper.to(subscription).stream())
            .peek(subscriptionConverted -> subscriptionConverted.setForceDispatch(true))
            .toList();
    }

    private List<ApiKey> loadApiKeys(final List<Subscription> subscriptions, final Set<String> environments) {
        if (subscriptions.isEmpty()) {
            return List.of();
        }

        Map<String, List<Subscription>> subscriptionsByIdMulti = subscriptions.stream().collect(Collectors.groupingBy(Subscription::getId));
        List<String> subscriptionIds = new ArrayList<>(subscriptionsByIdMulti.keySet());

        var sortable = new SortableBuilder().field("id").order(Order.ASC).build();
        List<ApiKey> result = new ArrayList<>();
        // A federated api key tied to multiple subscriptions can match more than one chunk's
        // `subscriptions IN` filter — dedup by key id so the refresher doesn't double-count it.
        Set<String> seenKeyIds = new HashSet<>();

        try {
            for (int chunkStart = 0; chunkStart < subscriptionIds.size(); chunkStart += subscriptionsChunkSize) {
                List<String> chunk = subscriptionIds.subList(
                    chunkStart,
                    Math.min(chunkStart + subscriptionsChunkSize, subscriptionIds.size())
                );
                ApiKeyCriteria criteria = ApiKeyCriteria.builder()
                    .subscriptions(chunk)
                    .environments(environments)
                    .includeRevoked(true)
                    .build();

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
                            .forEach(result::add);
                    }
                    if (page.size() < bulkItems) {
                        break;
                    }
                    cursor = ApiKeyCursor.byId(page.getLast().getId());
                }
            }
            return result;
        } catch (Exception ex) {
            throw new SyncException("Error occurred when retrieving API Keys for API Product", ex);
        }
    }
}
