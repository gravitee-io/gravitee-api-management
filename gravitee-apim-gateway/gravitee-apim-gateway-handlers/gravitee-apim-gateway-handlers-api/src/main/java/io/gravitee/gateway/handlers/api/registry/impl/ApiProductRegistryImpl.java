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
package io.gravitee.gateway.handlers.api.registry.impl;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.definition.model.v4.plan.Plan;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistryKey;
import io.gravitee.gateway.handlers.api.sharding.ApiProductShardingFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import lombok.CustomLog;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiProductRegistryImpl implements ApiProductRegistry {

    private record ApiEnvironmentKey(String environmentId, String apiId) {}

    @VisibleForTesting
    protected final Map<ApiProductRegistryKey, ReactableApiProduct> registry = new ConcurrentHashMap<>();

    /**
     * Both index views are published as deeply immutable maps and never mutated afterwards, so a
     * reader either sees the previous version or the next one, never a half-updated one. That is
     * what makes the plain {@code volatile} enough here: it carries the reference across threads,
     * and immutability carries the contents.
     */
    @SuppressWarnings("java:S3077")
    private volatile Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> planEntriesByApi = Map.of();

    /**
     * Product IDs by API, across environments: request-path subscription lookups arrive with an API
     * and no environment. API IDs are globally unique, so dropping the environment loses nothing.
     */
    @SuppressWarnings("java:S3077")
    private volatile Map<String, Set<String>> productIdsByApi = Map.of();

    private final ReadWriteLock indexLock = new ReentrantReadWriteLock();
    private final GatewayConfiguration gatewayConfiguration;

    public ApiProductRegistryImpl(GatewayConfiguration gatewayConfiguration) {
        this.gatewayConfiguration = gatewayConfiguration;
    }

    @Override
    public ReactableApiProduct get(String apiProductId, String environmentId) {
        return registry.get(new ApiProductRegistryKey(apiProductId, environmentId));
    }

    @Override
    public Collection<ReactableApiProduct> getAll() {
        return registry.values();
    }

    @Override
    public void register(ReactableApiProduct apiProduct) {
        log.debug("Registering API Product [{}] for environment [{}]", apiProduct.getId(), apiProduct.getEnvironmentId());

        ApiProductRegistryKey key = new ApiProductRegistryKey(apiProduct.getId(), apiProduct.getEnvironmentId());
        ReactableApiProduct previous = registry.put(key, apiProduct);
        updateIndexForProduct(previous, apiProduct);
    }

    @Override
    public void remove(String apiProductId, String environmentId) {
        log.debug("Removing API Product [{}] from environment [{}]", apiProductId, environmentId);

        ApiProductRegistryKey key = new ApiProductRegistryKey(apiProductId, environmentId);
        ReactableApiProduct removed = registry.remove(key);

        if (removed != null) {
            log.debug("API Product [{}] removed from environment [{}]", apiProductId, environmentId);
            removeIndexForProduct(removed);
        } else {
            log.debug("API Product [{}] was not found in registry for environment [{}]", apiProductId, environmentId);
        }
    }

    public void clear() {
        log.debug("Clearing all API Products from registry");
        registry.clear();
        indexLock.writeLock().lock();
        try {
            publishIndex(Map.of());
        } finally {
            indexLock.writeLock().unlock();
        }
    }

    @Override
    public List<ApiProductPlanEntry> getApiProductPlanEntriesForApi(String apiId, String environmentId) {
        if (apiId == null || environmentId == null) {
            return List.of();
        }
        // Returned as is: published entries are immutable, so there is nothing to defend against
        // and nothing to copy on a path walked once per security chain construction.
        List<ApiProductPlanEntry> entries = planEntriesByApi.get(new ApiEnvironmentKey(environmentId, apiId));
        return entries != null ? entries : List.of();
    }

    @Override
    public Set<String> getApiProductIdsForApi(String apiId) {
        if (apiId == null) {
            return Set.of();
        }
        return productIdsByApi.getOrDefault(apiId, Set.of());
    }

    /**
     * Publishes both views of the index together. The product-ids view is derived from the plan
     * view rather than maintained in parallel, so the two can never disagree: an API appears here
     * only if its product exposes at least one plan this gateway serves — which is exactly when a
     * product subscription can be resolved for it.
     */
    private void publishIndex(Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> entriesByApi) {
        Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> frozenEntries = new HashMap<>();
        Map<String, Set<String>> productIds = new HashMap<>();
        entriesByApi.forEach((key, entries) -> {
            frozenEntries.put(key, List.copyOf(entries));
            entries.forEach(entry -> {
                Set<String> ids = productIds.computeIfAbsent(key.apiId(), api -> new HashSet<>());
                ids.add(entry.apiProductId());
            });
        });
        productIds.replaceAll((api, ids) -> Set.copyOf(ids));

        planEntriesByApi = Map.copyOf(frozenEntries);
        productIdsByApi = Map.copyOf(productIds);
    }

    /**
     * Working copy of the plan index, with its lists copied too.
     *
     * <p>A shallow {@code new HashMap<>(planEntriesByApi)} would share its lists with the published
     * index, and the rebuild below adds to and removes from them — mutating, in place, the very
     * lists that in-flight requests are reading.</p>
     */
    private Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> mutableCopyOfIndex() {
        Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> mutable = new HashMap<>();
        planEntriesByApi.forEach((key, entries) -> mutable.put(key, new ArrayList<>(entries)));
        return mutable;
    }

    private void updateIndexForProduct(ReactableApiProduct previous, ReactableApiProduct current) {
        indexLock.writeLock().lock();
        try {
            Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> mutable = mutableCopyOfIndex();
            if (previous != null) {
                removeProductFromMutableIndex(mutable, previous);
            }
            addProductToMutableIndex(mutable, current);
            publishIndex(mutable);
        } finally {
            indexLock.writeLock().unlock();
        }
        log.debug("API Product registry index updated for product [{}]: {} API entries total", current.getId(), planEntriesByApi.size());
    }

    private void removeIndexForProduct(ReactableApiProduct product) {
        indexLock.writeLock().lock();
        try {
            Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> mutable = mutableCopyOfIndex();
            removeProductFromMutableIndex(mutable, product);
            publishIndex(mutable);
        } finally {
            indexLock.writeLock().unlock();
        }
        log.debug(
            "API Product registry index updated after removing product [{}]: {} API entries total",
            product.getId(),
            planEntriesByApi.size()
        );
    }

    private void addProductToMutableIndex(Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> index, ReactableApiProduct product) {
        if (!ApiProductShardingFilter.matchesProductTags(gatewayConfiguration, product.getTags())) {
            log.debug(
                "API Product [{}] skipped during indexing: product tags {} do not match gateway sharding tags",
                product.getId(),
                product.getTags()
            );
            return;
        }
        if (product.getEnvironmentId() == null || product.getApiIds() == null || product.getPlans() == null) {
            return;
        }
        for (String apiId : product.getApiIds()) {
            for (Plan plan : product.getPlans()) {
                addPlanToIndexIfEligible(index, product, apiId, plan);
            }
        }
    }

    private void addPlanToIndexIfEligible(
        Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> index,
        ReactableApiProduct product,
        String apiId,
        Plan plan
    ) {
        if (plan.getStatus() != PlanStatus.PUBLISHED && plan.getStatus() != PlanStatus.DEPRECATED) {
            return;
        }
        if (!ApiProductShardingFilter.matchesPlanTags(gatewayConfiguration, plan.getTags())) {
            log.debug(
                "Plan [{}] of product [{}] skipped: plan tags {} do not match gateway tags",
                plan.getId(),
                product.getId(),
                plan.getTags()
            );
            return;
        }
        ApiProductPlanEntry entry = new ApiProductPlanEntry(product.getId(), plan);
        List<ApiProductPlanEntry> entries = index.computeIfAbsent(new ApiEnvironmentKey(product.getEnvironmentId(), apiId), k ->
            new ArrayList<>()
        );
        entries.add(entry);
    }

    private static void removeProductFromMutableIndex(
        Map<ApiEnvironmentKey, List<ApiProductPlanEntry>> index,
        ReactableApiProduct product
    ) {
        if (product.getEnvironmentId() == null || product.getApiIds() == null) {
            return;
        }
        String productId = product.getId();
        for (String apiId : product.getApiIds()) {
            ApiEnvironmentKey key = new ApiEnvironmentKey(product.getEnvironmentId(), apiId);
            List<ApiProductPlanEntry> entries = index.get(key);
            if (entries != null) {
                entries.removeIf(e -> productId.equals(e.apiProductId()));
                if (entries.isEmpty()) {
                    index.remove(key);
                }
            }
        }
    }
}
