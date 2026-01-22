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

import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import java.util.Collection;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiProductRegistry {
    /**
     * Get API Product by ID and environment
     *
     * @param apiProductId the API Product ID
     * @param environmentId the environment ID
     * @return the API Product or null if not found
     */
    ReactableApiProduct get(String apiProductId, String environmentId);

    /**
     * Get all API Products
     *
     * @return collection of all API Products
     */
    Collection<ReactableApiProduct> getAll();

    /**
     * Register (add or update) an API Product in the registry
     *
     * @param apiProduct the API Product to register
     */
    void register(ReactableApiProduct apiProduct);

    /**
     * Remove API Product from registry
     *
     * @param apiProductId the API Product ID
     * @param environmentId the environment ID
     */
    void remove(String apiProductId, String environmentId);
}
