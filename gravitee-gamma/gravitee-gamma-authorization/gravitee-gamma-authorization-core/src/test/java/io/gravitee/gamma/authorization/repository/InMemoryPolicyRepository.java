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

import io.gravitee.gamma.authorization.api.PolicyRepository;
import io.gravitee.gamma.authorization.domain.Policy;
import io.gravitee.gamma.authorization.domain.PolicyKind;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryPolicyRepository implements PolicyRepository {

    private final ConcurrentMap<Key, Policy> store = new ConcurrentHashMap<>();

    @Override
    public Policy save(Policy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        store.put(new Key(policy.environmentId(), policy.id()), policy);
        return policy;
    }

    @Override
    public Optional<Policy> findById(String environmentId, String id) {
        return Optional.ofNullable(store.get(new Key(environmentId, id)));
    }

    @Override
    public List<Policy> findAll(String environmentId) {
        return store
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().environmentId().equals(environmentId))
            .map(java.util.Map.Entry::getValue)
            .toList();
    }

    @Override
    public List<Policy> findByKind(String environmentId, PolicyKind kind) {
        return findAll(environmentId)
            .stream()
            .filter(policy -> policy.kind() == kind)
            .toList();
    }

    @Override
    public List<Policy> findByEntityId(String environmentId, String entityId) {
        Objects.requireNonNull(entityId, "entityId");
        return findAll(environmentId)
            .stream()
            .filter(policy -> entityId.equals(policy.entityId()))
            .toList();
    }

    @Override
    public boolean deleteById(String environmentId, String id) {
        return store.remove(new Key(environmentId, id)) != null;
    }

    private record Key(String environmentId, String id) {}
}
