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
package io.gravitee.gateway.core.sync.impl;

import io.gravitee.gateway.core.model.Api;

import java.util.HashMap;
import java.util.Map;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class SyncStateManager {

    private final Map<String, Api> cacheApis = new HashMap<>();

    public void handle(io.gravitee.repository.model.Api api) {
        Api cachedApi = cacheApis.get(api.getName());

        if (cachedApi != null) {
            // We already know this API
            if (! cachedApi.getUpdatedAt().equals(api.getUpdatedAt())) {
                // The API has been updated between two sync
            }
        } else {
            // A new API has been defined

        }
    }
}
