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

import static io.gravitee.repository.management.model.Subscription.Status.ACCEPTED;
import static io.gravitee.repository.management.model.Subscription.Status.CLOSED;
import static io.gravitee.repository.management.model.Subscription.Status.PAUSED;
import static io.gravitee.repository.management.model.Subscription.Status.PENDING;
import static java.util.stream.Collectors.groupingBy;

import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.services.sync.process.common.mapper.SubscriptionMapper;
import io.gravitee.gateway.services.sync.process.common.model.SyncException;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.Order;
import io.gravitee.repository.management.api.search.SubscriptionCriteria;
import io.gravitee.repository.management.api.search.SubscriptionCursor;
import io.gravitee.repository.management.api.search.builder.SortableBuilder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;

@CustomLog
public class SubscriptionAppender {

    private static final List<String> INITIAL_STATUS = List.of(ACCEPTED.name());
    private static final List<String> INCREMENTAL_STATUS = List.of(ACCEPTED.name(), CLOSED.name(), PAUSED.name(), PENDING.name());
    /** Enough ids to go look one up, few enough that a broken dataset cannot flood the log. */
    private static final int WARN_SAMPLE_SIZE = 5;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final ApiProductRegistry apiProductRegistry;
    private final int bulkItems;

    public SubscriptionAppender(
        SubscriptionRepository subscriptionRepository,
        SubscriptionMapper subscriptionMapper,
        ApiProductRegistry apiProductRegistry,
        int bulkItems
    ) {
        if (bulkItems <= 0) {
            throw new IllegalArgumentException("bulkItems must be > 0 (got " + bulkItems + ")");
        }
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionMapper = subscriptionMapper;
        this.apiProductRegistry = apiProductRegistry;
        this.bulkItems = bulkItems;
    }

    /**
     * Fetching subscriptions for given deployables
     * @param deployables the deployables to update
     * @return the deployables updated with subscriptions
     */
    public List<ApiReactorDeployable> appends(
        final boolean initialSync,
        final List<ApiReactorDeployable> deployables,
        final Set<String> environments
    ) {
        final Map<String, ApiReactorDeployable> deployableByApi = deployables
            .stream()
            .collect(Collectors.toMap(ApiReactorDeployable::apiId, d -> d));

        // A Set, not a List: the same product plan is collected once per member API in the batch, and
        // every duplicate widened the repository's plan criteria for no extra rows.
        Set<String> allPlans = new HashSet<>(collectApiPlans(deployableByApi));

        deployableByApi.forEach((apiId, deployable) -> {
            Set<String> envs = environments != null && !environments.isEmpty()
                ? environments
                : Optional.ofNullable(deployable.reactableApi())
                    .map(a -> a.getEnvironmentId())
                    .map(Set::of)
                    .orElse(Set.of());
            Set<String> apiProductPlans = collectApiProductPlans(apiId, envs);
            deployable.subscribablePlans().addAll(apiProductPlans);
            deployable.apiKeyPlans().addAll(apiProductPlans);
            allPlans.addAll(apiProductPlans);
        });

        if (!allPlans.isEmpty()) {
            // Restrict the API-Product explosion to this batch's APIs. Product subscriptions were
            // otherwise exploded across every API in the product and each leg outside the batch
            // discarded here — for a product spanning P APIs synced in batches of B, that allocated
            // P/B times more legs than it kept, and logged the full subscription id list per
            // discarded API.
            // Plain API subscriptions are still mapped unfiltered, so the warning below keeps
            // reporting the case it was written for: a subscription whose api disagrees with the
            // API owning its plan.
            Map<String, List<Subscription>> subscriptionsByApi = loadSubscriptions(
                initialSync,
                allPlans,
                environments,
                deployableByApi.keySet()
            );
            subscriptionsByApi.forEach((api, subscriptions) -> {
                ApiReactorDeployable deployable = deployableByApi.get(api);
                if (deployable == null) {
                    // A count and a capped sample, never the whole id list. Joining every id
                    // produced single log lines of 64 KB, built as a method argument — so allocated
                    // before the level check, at any configured level.
                    log.warn(
                        "Cannot find api {} for {} subscription(s), sample: [{}]",
                        api,
                        subscriptions.size(),
                        subscriptions.stream().limit(WARN_SAMPLE_SIZE).map(Subscription::getId).collect(Collectors.joining(","))
                    );
                } else {
                    deployable.subscriptions(subscriptions);
                }
            });
        }
        return deployables;
    }

    private List<String> collectApiPlans(Map<String, ApiReactorDeployable> deployableByApi) {
        return deployableByApi.values().stream().map(ApiReactorDeployable::subscribablePlans).flatMap(Collection::stream).toList();
    }

    private Set<String> collectApiProductPlans(String apiId, Set<String> envs) {
        return envs
            .stream()
            .flatMap(envId -> apiProductRegistry.getApiProductPlanEntriesForApi(apiId, envId).stream())
            .map(e -> e.plan().getId())
            .collect(Collectors.toSet());
    }

    protected Map<String, List<Subscription>> loadSubscriptions(
        final boolean initialSync,
        final Collection<String> plans,
        final Set<String> environments
    ) {
        return loadSubscriptions(initialSync, plans, environments, null);
    }

    /**
     * @param retainedApis APIs whose legs should be materialised, or {@code null} to keep every leg.
     */
    protected Map<String, List<Subscription>> loadSubscriptions(
        final boolean initialSync,
        final Collection<String> plans,
        final Set<String> environments,
        final Set<String> retainedApis
    ) {
        SubscriptionCriteria.SubscriptionCriteriaBuilder criteriaBuilder = SubscriptionCriteria.builder()
            .plans(plans)
            .environments(environments);
        if (initialSync) {
            criteriaBuilder.statuses(INITIAL_STATUS).endingAtAfter(Instant.now().toEpochMilli()).includeWithoutEnd(true);
        } else {
            criteriaBuilder.statuses(INCREMENTAL_STATUS);
        }
        SubscriptionCriteria criteria = criteriaBuilder.build();
        // Warmup pages by (plan, id): the sort field "plan" pairs with the byPlanAndId cursor below
        // so the repository's order and keyset seek agree (an "id"-field request would seek/sort by
        // id only — the legacy fallback — and mismatch the (plan, id) cursor).
        var sortable = new SortableBuilder().field("plan").order(Order.ASC).build();

        Map<String, List<Subscription>> grouped = new HashMap<>();
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
                    return grouped;
                }
                for (io.gravitee.repository.management.model.Subscription record : page) {
                    subscriptionMapper
                        .to(record, retainedApis)
                        .forEach(converted -> {
                            converted.setForceDispatch(true);
                            grouped.computeIfAbsent(converted.getApi(), k -> new ArrayList<>()).add(converted);
                        });
                }
                if (page.size() < bulkItems) {
                    return grouped;
                }
                cursor = SubscriptionCursor.byPlanAndId(page.getLast().getPlan(), page.getLast().getId());
            }
        } catch (Exception ex) {
            throw new SyncException("Error occurred when retrieving subscriptions", ex);
        }
    }
}
