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
package io.gravitee.gateway.handlers.api.registry;

import io.gravitee.definition.model.v4.plan.AbstractPlan;
import java.util.List;

/**
 * Cache of product plan definitions for APIs that are part of API Products.
 * Populated when API Products are deployed; used when building the security chain
 * to iterate product plans before API plans.
 *
 * @author GraviteeSource Team
 */
public interface ProductPlanDefinitionCache {
    /**
     * Register plan definitions for an API Product.
     *
     * @param apiProductId the API Product ID
     * @param plans the plan definitions (from repository, converted to definition model)
     */
    void register(String apiProductId, List<? extends AbstractPlan> plans);

    /**
     * Remove plan definitions for an API Product.
     *
     * @param apiProductId the API Product ID
     */
    void unregister(String apiProductId);

    /**
     * Get plan definitions for an API Product.
     *
     * @param apiProductId the API Product ID
     * @return the plan definitions, or empty list if not found
     */
    List<? extends AbstractPlan> getByApiProductId(String apiProductId);
}
