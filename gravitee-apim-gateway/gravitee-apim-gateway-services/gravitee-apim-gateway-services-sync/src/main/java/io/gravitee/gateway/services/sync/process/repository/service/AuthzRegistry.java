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
package io.gravitee.gateway.services.sync.process.repository.service;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AuthzRegistry {

    private final Set<String> deployedEntityIds = ConcurrentHashMap.newKeySet();

    public void register(final Collection<String> entityIds) {
        if (entityIds == null) {
            return;
        }
        for (String entityId : entityIds) {
            if (entityId != null && !entityId.isBlank()) {
                deployedEntityIds.add(entityId);
            }
        }
    }

    public void unregister(final Collection<String> entityIds) {
        if (entityIds == null) {
            return;
        }
        for (String entityId : entityIds) {
            if (entityId != null && !entityId.isBlank()) {
                deployedEntityIds.remove(entityId);
            }
        }
    }

    public boolean isResourceDeployed(final String entityId) {
        if (entityId == null) {
            return false;
        }
        return deployedEntityIds.contains(entityId);
    }

    Set<String> snapshot() {
        return Set.copyOf(deployedEntityIds);
    }
}
