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
package io.gravitee.gamma.authorization.repository;

import io.gravitee.gamma.repository.authorization.api.AuthorizationPolicyRepository;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryPolicyRepository implements AuthorizationPolicyRepository {

    private final ConcurrentMap<String, AuthorizationPolicy> store = new ConcurrentHashMap<>();

    @Override
    public Optional<AuthorizationPolicy> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public AuthorizationPolicy create(AuthorizationPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        store.put(policy.id(), policy);
        return policy;
    }

    @Override
    public AuthorizationPolicy update(AuthorizationPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        store.put(policy.id(), policy);
        return policy;
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public Set<AuthorizationPolicy> findAll() {
        return Set.copyOf(store.values());
    }

    @Override
    public Optional<AuthorizationPolicy> findByEnvironmentIdAndId(String environmentId, String id) {
        return findById(id).filter(p -> environmentId.equals(p.environmentId()));
    }

    @Override
    public List<AuthorizationPolicy> findAllByEnvironmentId(String environmentId) {
        return store
            .values()
            .stream()
            .filter(p -> Objects.equals(environmentId, p.environmentId()))
            .toList();
    }

    @Override
    public List<AuthorizationPolicy> findAllByEnvironmentIdAndKind(String environmentId, AuthorizationPolicyKind kind) {
        Objects.requireNonNull(kind, "kind");
        return findAllByEnvironmentId(environmentId)
            .stream()
            .filter(p -> p.kind() == kind)
            .toList();
    }

    @Override
    public List<AuthorizationPolicy> findAllByEnvironmentIdAndEntityId(String environmentId, String entityId) {
        Objects.requireNonNull(entityId, "entityId");
        return findAllByEnvironmentId(environmentId)
            .stream()
            .filter(p -> entityId.equals(p.entityId()))
            .toList();
    }

    @Override
    public long deleteByEnvironmentIdAndId(String environmentId, String id) {
        AuthorizationPolicy existing = store.get(id);
        if (existing == null || !Objects.equals(environmentId, existing.environmentId())) {
            return 0L;
        }
        store.remove(id);
        return 1L;
    }
}
