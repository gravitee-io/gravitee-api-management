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
package io.gravitee.gateway.services.sync.process.repository.synchronizer.authz;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks, per runtime key ({@code environmentId:id}), the set of PDP scopes a document (entity or
 * policy) is currently applied to on this node. A PUBLISH reconciles removals as
 * {@code dropped = applied − newTarget} without a per-event wire delta.
 *
 * <p>Shared between the {@link AbstractAuthzReactorSynchronizer} that owns the document type and the
 * {@link AuthzPdpSynchronizer} hydration path: when a freshly provisioned scope is backfilled, the
 * scope is recorded here too, so a later narrowing PUBLISH can still evict the document from it.
 * Without this, hydration-applied scopes would be invisible to the eviction computation and linger.
 */
public class AuthzScopePlacement {

    private final ConcurrentHashMap<String, Set<String>> placement = new ConcurrentHashMap<>();

    public Set<String> applied(String runtimeKey) {
        return placement.getOrDefault(runtimeKey, Set.of());
    }

    public void replace(String runtimeKey, Set<String> scopes) {
        placement.put(runtimeKey, scopes == null ? Set.of() : Set.copyOf(scopes));
    }

    public void forget(String runtimeKey) {
        placement.remove(runtimeKey);
    }

    /** Add a single scope to the placement, e.g. when a hydration backfill applies the document to a
     *  newly provisioned scope outside the normal PUBLISH path. */
    public void addScope(String runtimeKey, String scope) {
        placement.compute(runtimeKey, (k, current) -> {
            Set<String> next = current == null ? new HashSet<>() : new HashSet<>(current);
            next.add(scope);
            return Set.copyOf(next);
        });
    }
}
