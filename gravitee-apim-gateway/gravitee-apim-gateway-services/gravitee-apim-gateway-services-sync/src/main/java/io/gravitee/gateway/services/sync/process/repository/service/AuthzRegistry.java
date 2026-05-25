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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthzRegistry {

    private final Set<String> deployedEntityIds = ConcurrentHashMap.newKeySet();
    private final Map<String, Set<String>> entitiesByApi = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;

    @PostConstruct
    void bindMetrics() {
        if (meterRegistry == null) {
            return;
        }
        Gauge.builder("gamma.authz.registry.size", deployedEntityIds, Set::size)
            .description("Number of authz resources currently tracked by the gateway sync")
            .register(meterRegistry);
    }

    public void registerForApi(final String apiId, final Collection<String> entityIds) {
        if (apiId == null || apiId.isBlank() || entityIds == null) {
            return;
        }
        Set<String> sanitized = entitiesByApi.computeIfAbsent(apiId, k -> ConcurrentHashMap.newKeySet());
        for (String entityId : entityIds) {
            if (entityId != null && !entityId.isBlank()) {
                sanitized.add(entityId);
                deployedEntityIds.add(entityId);
            }
        }
    }

    public Set<String> entitiesForApi(final String apiId) {
        if (apiId == null) {
            return Set.of();
        }
        Set<String> tracked = entitiesByApi.get(apiId);
        return tracked == null ? Set.of() : Set.copyOf(tracked);
    }

    public void unregisterApi(final String apiId) {
        if (apiId == null) {
            return;
        }
        Set<String> tracked = entitiesByApi.remove(apiId);
        if (tracked != null) {
            deployedEntityIds.removeAll(tracked);
        }
    }

    public boolean isResourceDeployed(final String entityId) {
        if (entityId == null) {
            return false;
        }
        return deployedEntityIds.contains(entityId);
    }

    public int size() {
        return deployedEntityIds.size();
    }

    // VisibleForTesting
    Set<String> snapshotForTesting() {
        return Set.copyOf(deployedEntityIds);
    }
}
