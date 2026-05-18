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
package io.gravitee.apim.authorization.repository;

import io.gravitee.apim.authorization.api.EntityRepository;
import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.domain.EntityKind;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.dao.DuplicateKeyException;

public final class InMemoryEntityRepository implements EntityRepository {

    private final ConcurrentMap<Key, Entity> store = new ConcurrentHashMap<>();

    @Override
    public Entity save(Entity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        Optional<Entity> conflict = findByEntityId(entity.environmentId(), entity.entityId());
        if (conflict.isPresent() && !conflict.get().id().equals(entity.id())) {
            throw new DuplicateKeyException(
                "entityId '" + entity.entityId() + "' is already used in environment '" + entity.environmentId() + "'"
            );
        }
        store.put(new Key(entity.environmentId(), entity.id()), entity);
        return entity;
    }

    @Override
    public Optional<Entity> findById(String environmentId, String id) {
        return Optional.ofNullable(store.get(new Key(environmentId, id)));
    }

    @Override
    public Optional<Entity> findByEntityId(String environmentId, String entityId) {
        Objects.requireNonNull(entityId, "entityId");
        return findAll(environmentId)
            .stream()
            .filter(e -> entityId.equals(e.entityId()))
            .findFirst();
    }

    @Override
    public List<Entity> findAll(String environmentId) {
        return store
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().environmentId().equals(environmentId))
            .map(java.util.Map.Entry::getValue)
            .toList();
    }

    @Override
    public List<Entity> findByKind(String environmentId, EntityKind kind) {
        Objects.requireNonNull(kind, "kind");
        return findAll(environmentId)
            .stream()
            .filter(e -> e.kind() == kind)
            .toList();
    }

    @Override
    public List<Entity> findByEntityIdPrefix(String environmentId, String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return findAll(environmentId)
            .stream()
            .filter(e -> e.entityId().startsWith(prefix))
            .toList();
    }

    @Override
    public boolean deleteById(String environmentId, String id) {
        return store.remove(new Key(environmentId, id)) != null;
    }

    @Override
    public boolean deleteByEntityId(String environmentId, String entityId) {
        Optional<Entity> existing = findByEntityId(environmentId, entityId);
        return existing.isPresent() && deleteById(environmentId, existing.get().id());
    }

    private record Key(String environmentId, String id) {}
}
