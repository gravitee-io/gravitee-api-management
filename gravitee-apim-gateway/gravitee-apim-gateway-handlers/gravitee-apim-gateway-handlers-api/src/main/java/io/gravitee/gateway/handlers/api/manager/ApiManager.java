/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.api.manager;

import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.model.ReactableApi;

import java.util.Collection;

/**
 * This manager interface acts as a bridge between the source of {@link Api} (*.json files in case of
 * local registry and sync scheduler when using the sync mode) and the {@link io.gravitee.gateway.reactor.Reactor}.
 * This means that all actions handled by the reactor must be done by using this manager and not directly by emitting
 * internal event.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface ApiManager {
    /**
     * Register an API definition. It is a "create or update" operation, if the api was previously existing, the
     * definition is updated accordingly.
     * @param api
     * @return
     */
    boolean register(ReactableApi<?> api);

    void unregister(String apiId);

    void refresh();

    /**
     * Returns a collection of deployed {@link Api}s.
     * @return A collection of deployed  {@link Api}s.
     */
    Collection<ReactableApi<?>> apis();

    /**
     * Retrieve a deployed {@link Api} using its ID.
     * @param apiId The ID of the deployed API.
     * @return A deployed {@link Api}
     */
    ReactableApi<?> get(String apiId);
}
