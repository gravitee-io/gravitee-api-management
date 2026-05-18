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

import io.gravitee.gamma.repository.authorization.api.AuthorizationEntityRepository;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntityKind;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.dao.DuplicateKeyException;

public final class InMemoryEntityRepository implements AuthorizationEntityRepository {

    private final ConcurrentMap<String, AuthorizationEntity> store = new ConcurrentHashMap<>();

    @Override
    public Optional<AuthorizationEntity> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public AuthorizationEntity create(AuthorizationEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        Optional<AuthorizationEntity> conflict = findByEnvironmentIdAndEntityId(entity.environmentId(), entity.entityId());
        if (conflict.isPresent() && !conflict.get().id().equals(entity.id())) {
            throw new DuplicateKeyException(
                "entityId '" + entity.entityId() + "' is already used in environment '" + entity.environmentId() + "'"
            );
        }
        store.put(entity.id(), entity);
        return entity;
    }

    @Override
    public AuthorizationEntity update(AuthorizationEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        store.put(entity.id(), entity);
        return entity;
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public Set<AuthorizationEntity> findAll() {
        return Set.copyOf(store.values());
    }

    @Override
    public Optional<AuthorizationEntity> findByEnvironmentIdAndId(String environmentId, String id) {
        return findById(id).filter(e -> environmentId.equals(e.environmentId()));
    }

    @Override
    public Optional<AuthorizationEntity> findByEnvironmentIdAndEntityId(String environmentId, String entityId) {
        Objects.requireNonNull(entityId, "entityId");
        return findAllByEnvironmentId(environmentId)
            .stream()
            .filter(e -> entityId.equals(e.entityId()))
            .findFirst();
    }

    @Override
    public List<AuthorizationEntity> findAllByEnvironmentId(String environmentId) {
        return store
            .values()
            .stream()
            .filter(e -> Objects.equals(environmentId, e.environmentId()))
            .toList();
    }

    @Override
    public List<AuthorizationEntity> findAllByEnvironmentIdAndKind(String environmentId, AuthorizationEntityKind kind) {
        Objects.requireNonNull(kind, "kind");
        return findAllByEnvironmentId(environmentId)
            .stream()
            .filter(e -> e.kind() == kind)
            .toList();
    }

    @Override
    public List<AuthorizationEntity> findAllByEnvironmentIdAndEntityIdStartingWith(String environmentId, String entityIdPrefix) {
        Objects.requireNonNull(entityIdPrefix, "entityIdPrefix");
        return findAllByEnvironmentId(environmentId)
            .stream()
            .filter(e -> e.entityId().startsWith(entityIdPrefix))
            .toList();
    }

    @Override
    public long deleteByEnvironmentIdAndId(String environmentId, String id) {
        AuthorizationEntity existing = store.get(id);
        if (existing == null || !Objects.equals(environmentId, existing.environmentId())) {
            return 0L;
        }
        store.remove(id);
        return 1L;
    }

    @Override
    public long deleteByEnvironmentIdAndEntityId(String environmentId, String entityId) {
        Optional<AuthorizationEntity> existing = findByEnvironmentIdAndEntityId(environmentId, entityId);
        if (existing.isEmpty()) {
            return 0L;
        }
        store.remove(existing.get().id());
        return 1L;
    }
}
