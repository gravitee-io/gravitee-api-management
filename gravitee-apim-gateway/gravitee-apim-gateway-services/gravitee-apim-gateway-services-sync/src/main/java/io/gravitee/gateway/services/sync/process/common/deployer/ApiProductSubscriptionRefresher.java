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
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.services.ApiKeyCacheService;
import io.gravitee.gateway.handlers.api.services.SubscriptionCacheService;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.gateway.services.sync.process.repository.mapper.ApiKeyMapper;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.ApiKeyCriteria;
import io.gravitee.repository.management.api.search.ApiKeyCursor;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.SubscriptionCursor;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import io.reactivex.rxjava3.core.Completable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;

/**
 * Owns the runtime state of API Product subscriptions: the product deployment loads them, the
 * product undeployment evicts them.
 *
 * <p>A product subscription is not attached to any member API, so the API synchronizer no longer
 * sees it — {@code SubscriptionAppender} only collects the APIs' own plans. This class is the single
 * writer for that state, which removes the double registration a member API's deployment used to
 * cause.</p>
 */
@CustomLog
public class ApiProductSubscriptionRefresher {

    private static final List<String> INCREMENTAL_STATUS = List.of(ACCEPTED.name(), CLOSED.name(), PAUSED.name(), PENDING.name());

    private final SubscriptionRepository subscriptionRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final SubscriptionCacheService subscriptionService;
    private final ApiKeyCacheService apiKeyService;
    private final int bulkItems;
    private final int subscriptionsChunkSize;

    public ApiProductSubscriptionRefresher(
        SubscriptionRepository subscriptionRepository,
        ApiKeyRepository apiKeyRepository,
        SubscriptionMapper subscriptionMapper,
        ApiKeyMapper apiKeyMapper,
        SubscriptionCacheService subscriptionService,
        ApiKeyCacheService apiKeyService,
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
     * Refreshes the runtime state of the subscriptions taken on the given API Product plans.
     *
     * <p>Subscriptions are walked page by page and registered along with their API keys, so a
     * product with hundreds of thousands of subscriptions never has to be held in memory at once.</p>
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
                forEachSubscriptionPage(subscribablePlans, environments, page -> {
                    page.forEach(subscription ->
                        quietly(() -> subscriptionService.register(subscription), subscription.getId(), "subscription")
                    );
                    loadApiKeys(page, environments).forEach(apiKey ->
                        quietly(() -> apiKeyService.register(apiKey), apiKey.getId(), "API key")
                    );
                });
            } catch (SyncException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new SyncException("Error occurred when refreshing subscriptions for API Product", ex);
            }
        });
    }

    /**
     * Evicts every subscription and API key taken on the given API Product.
     *
     * <p>Both caches index a product subscription under the product itself, so this is a single
     * bulk eviction — no fan-out over the member APIs, and undeploying one member API never drops a
     * subscription the product's other APIs still serve.</p>
     *
     * @param apiProductId the API Product being undeployed
     */
    public Completable unregisterByApiProduct(final String apiProductId) {
        if (apiProductId == null) {
            return Completable.complete();
        }
        // Only Errors are trapped here, and translated rather than swallowed: ApiProductDeployer#undeploy
        // turns a failure into a SyncException and stops, so absorbing one would mark the product gone while
        // its subscription certificates are still in the trust store and its API keys still cached.
        return Completable.fromRunnable(() ->
            SyncIsolation.translateErrors(
                () -> "unregister subscriptions and API keys of API Product [%s]".formatted(apiProductId),
                () -> {
                    subscriptionService.unregisterByApiProductId(apiProductId);
                    apiKeyService.unregisterByApiProductId(apiProductId);
                    log.debug("Unregistered subscriptions and API keys of API Product [{}]", apiProductId);
                }
            )
        );
    }

    private void quietly(final Runnable action, final String itemId, final String type) {
        SyncIsolation.isolate(
            () -> "deploy %s [%s]".formatted(type, itemId),
            action,
            t -> log.warn("Failed to deploy {} [{}]", type, itemId, t)
        );
    }

    /**
     * Walks the subscriptions of the given plans, one repository page at a time.
     *
     * <p>Warmup pages by (plan, id): the sort field "plan" pairs with the byPlanAndId cursor so the
     * repository's order and keyset seek agree — same contract as {@code SubscriptionAppender}.</p>
     */
    private void forEachSubscriptionPage(
        final Set<String> plans,
        final Set<String> environments,
        final Consumer<List<Subscription>> pageConsumer
    ) {
        SubscriptionCriteria criteria = SubscriptionCriteria.builder()
            .plans(List.copyOf(plans))
            .environments(environments)
            .statuses(INCREMENTAL_STATUS)
            .build();
        var sortable = new SortableBuilder().field("plan").order(Order.ASC).build();

        SubscriptionCursor cursor = null;
        try {
            while (true) {
                List<io.gravitee.repository.management.model.Subscription> page = subscriptionRepository.searchAfter(
                    criteria,
                    sortable,
                    cursor,
                    bulkItems
                );
                if (page == null || page.isEmpty()) {
                    return;
                }
                List<Subscription> converted = page
                    .stream()
                    .flatMap(record -> subscriptionMapper.to(record).stream())
                    .peek(subscription -> subscription.setForceDispatch(true))
                    .toList();
                pageConsumer.accept(converted);
                if (page.size() < bulkItems) {
                    return;
                }
                cursor = SubscriptionCursor.byPlanAndId(page.getLast().getPlan(), page.getLast().getId());
            }
        } catch (SyncException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SyncException("Error occurred when retrieving subscriptions for API Product", ex);
        }
    }

    private List<ApiKey> loadApiKeys(final List<Subscription> subscriptions, final Set<String> environments) {
        if (subscriptions.isEmpty()) {
            return List.of();
        }

        Map<String, List<Subscription>> subscriptionsById = subscriptions.stream().collect(Collectors.groupingBy(Subscription::getId));
        // Sort for deterministic chunk boundaries — same rationale as ApiKeyAppender.
        List<String> subscriptionIds = new ArrayList<>(subscriptionsById.keySet());
        subscriptionIds.sort(Comparator.naturalOrder());

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
                                List<Subscription> subsForId = subscriptionsById.get(subscriptionId);
                                if (subsForId == null || subsForId.isEmpty()) {
                                    return Stream.<ApiKey>empty();
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
