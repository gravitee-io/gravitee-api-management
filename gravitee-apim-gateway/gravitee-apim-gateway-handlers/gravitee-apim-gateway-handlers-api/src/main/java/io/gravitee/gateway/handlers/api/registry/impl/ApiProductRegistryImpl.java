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
import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.handlers.api.registry.ApiProductPlanDefinitionCache;
import io.gravitee.gateway.handlers.api.registry.ApiProductRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class ApiProductRegistryImpl implements ApiProductRegistry {

    @VisibleForTesting
    protected final Map<ApiProductRegistryKey, ReactableApiProduct> registry = new ConcurrentHashMap<>();

    private final ApiProductPlanDefinitionCache apiProductPlanDefinitionCache;

    public ApiProductRegistryImpl(ApiProductPlanDefinitionCache apiProductPlanDefinitionCache) {
        this.apiProductPlanDefinitionCache = apiProductPlanDefinitionCache;
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
        ReactableApiProduct previousApiProduct = registry.put(key, apiProduct);

        if (previousApiProduct != null) {
            log.debug(
                "API Product [{}] was already registered for environment [{}]; replaced with new version",
                apiProduct.getId(),
                apiProduct.getEnvironmentId()
            );
        }
    }

    @Override
    public void remove(String apiProductId, String environmentId) {
        log.debug("Removing API Product [{}] from environment [{}]", apiProductId, environmentId);

        ApiProductRegistryKey key = new ApiProductRegistryKey(apiProductId, environmentId);
        ReactableApiProduct removed = registry.remove(key);

        if (removed != null) {
            log.debug("API Product [{}] removed from environment [{}]", apiProductId, environmentId);
        } else {
            log.debug("API Product [{}] was not found in registry for environment [{}]", apiProductId, environmentId);
        }
    }

    /**
     * Clear all API Products from the registry.
     * Used for cleanup during shutdown or testing.
     */
    public void clear() {
        log.debug("Clearing all API Products from registry");
        registry.clear();
    }

    @Override
    public List<ApiProductPlanEntry> getApiProductPlanEntriesForApi(String apiId, String environmentId) {
        List<ApiProductPlanEntry> entries = new ArrayList<>();

        if (apiId == null || environmentId == null) {
            return entries;
        }

        // Iterate all registered products in the environment
        for (ReactableApiProduct product : registry.values()) {
            if (
                product.getEnvironmentId() != null &&
                product.getEnvironmentId().equals(environmentId) &&
                product.getApiIds() != null &&
                product.getApiIds().contains(apiId)
            ) {
                // Get plans for this product from cache
                List<? extends AbstractPlan> plans = apiProductPlanDefinitionCache.getByApiProductId(product.getId());
                if (plans != null) {
                    for (AbstractPlan plan : plans) {
                        entries.add(new ApiProductPlanEntry(product.getId(), plan));
                    }
                }
            }
        }

        return entries;
    }

    /**
     * Registry key combining API Product ID and Environment ID for scoping.
     */
    record ApiProductRegistryKey(String apiProductId, String environmentId) {}
}
