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

import io.gravitee.definition.model.v4.plan.AbstractPlan;
import io.gravitee.gateway.handlers.api.registry.ProductPlanDefinitionCache;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * @author GraviteeSource Team
 */
@CustomLog
public class ProductPlanDefinitionCacheImpl implements ProductPlanDefinitionCache {

    private final ConcurrentHashMap<String, List<? extends AbstractPlan>> cache = new ConcurrentHashMap<>();

    @Override
    public void register(String apiProductId, List<? extends AbstractPlan> plans) {
        if (apiProductId != null && plans != null && !plans.isEmpty()) {
            cache.put(apiProductId, List.copyOf(plans));
            log.debug("Registered {} plan definitions for API Product [{}]", plans.size(), apiProductId);
        }
    }

    @Override
    public void unregister(String apiProductId) {
        if (apiProductId != null) {
            cache.remove(apiProductId);
            log.debug("Unregistered plan definitions for API Product [{}]", apiProductId);
        }
    }

    @Override
    public List<? extends AbstractPlan> getByApiProductId(String apiProductId) {
        if (apiProductId == null) {
            return Collections.emptyList();
        }
        List<? extends AbstractPlan> plans = cache.get(apiProductId);
        return plans != null ? plans : Collections.emptyList();
    }
}
