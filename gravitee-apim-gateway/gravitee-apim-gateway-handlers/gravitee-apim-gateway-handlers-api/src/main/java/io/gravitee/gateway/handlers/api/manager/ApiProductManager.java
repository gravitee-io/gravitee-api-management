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
package io.gravitee.gateway.handlers.api.manager;

import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.reactivex.rxjava3.core.Completable;
import java.util.Collection;

/**
 * @author Arpit Mishra (arpit.mishra at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiProductManager {
    /**
     * Register (deploy) an API Product
     *
     * @param apiProduct the API Product to register
     */
    void register(ReactableApiProduct apiProduct);

    /**
     * Register (deploy or update) an API Product, running the given Completable before emitting
     * the change event. Use this when subscription/API key cache must be populated before
     * the event is emitted. The Completable is chained so the event is emitted only after
     * it completes, avoiding thread blocking.
     *
     * @param apiProduct the API Product to register
     * @param doBeforeEmit Completable to run after product is in registry but before the event is emitted
     * @return Completable that completes when registration and event emission are done
     */
    Completable register(ReactableApiProduct apiProduct, Completable doBeforeEmit);

    /**
     * Unregister (undeploy) an API Product
     *
     * @param apiProductId the ID of the API Product to unregister
     */
    void unregister(String apiProductId);

    /**
     * Get an API Product by ID
     *
     * @param apiProductId the ID of the API Product
     * @return the API Product or null if not found
     */
    ReactableApiProduct get(String apiProductId);

    /**
     * Get all registered API Products
     *
     * @return collection of all API Products
     */
    Collection<ReactableApiProduct> getApiProducts();
}
