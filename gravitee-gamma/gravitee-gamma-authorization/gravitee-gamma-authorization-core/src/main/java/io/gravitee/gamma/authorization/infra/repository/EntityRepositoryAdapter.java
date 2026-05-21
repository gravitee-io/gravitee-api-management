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
package io.gravitee.gamma.authorization.infra.repository;

import io.gravitee.gamma.authorization.api.EntityRepository;
import io.gravitee.gamma.authorization.domain.Entity;
import io.gravitee.gamma.authorization.domain.EntityKind;
import io.gravitee.gamma.authorization.infra.adapter.AuthorizationAdapter;
import io.gravitee.gamma.authorization.service.EntityFilter;
import io.gravitee.gamma.repository.authorization.api.AuthorizationEntityRepository;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EntityRepositoryAdapter implements EntityRepository {

    private final AuthorizationEntityRepository storage;
    private final AuthorizationAdapter mapper;

    public EntityRepositoryAdapter(AuthorizationEntityRepository storage, AuthorizationAdapter mapper) {
        this.storage = storage;
        this.mapper = mapper;
    }

    @Override
    public Entity save(Entity entity) {
        var stored = mapper.toStorage(entity);
        var existing = storage.findById(stored.getId());
        var saved = existing.isPresent() ? storage.update(stored) : storage.create(stored);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Entity> findById(String environmentId, String id) {
        return storage.findByEnvironmentIdAndId(environmentId, id).map(mapper::toDomain);
    }

    @Override
    public Optional<Entity> findByEntityId(String environmentId, String entityId) {
        return storage.findByEnvironmentIdAndEntityId(environmentId, entityId).map(mapper::toDomain);
    }

    @Override
    public List<Entity> findAll(String environmentId) {
        return storage.findAllByEnvironmentId(environmentId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Entity> findByKind(String environmentId, EntityKind kind) {
        return storage.findAllByEnvironmentIdAndKind(environmentId, mapper.toStorage(kind)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Entity> findByEntityIdPrefix(String environmentId, String prefix) {
        return storage.findAllByEnvironmentIdAndEntityIdStartingWith(environmentId, prefix).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean deleteById(String environmentId, String id) {
        return storage.deleteByEnvironmentIdAndId(environmentId, id) > 0;
    }

    @Override
    public boolean deleteByEntityId(String environmentId, String entityId) {
        return storage.deleteByEnvironmentIdAndEntityId(environmentId, entityId) > 0;
    }

    @Override
    public PagedResult<Entity> findPage(String environmentId, EntityFilter filter, Pageable pageable) {
        EntityFilter f = filter == null ? EntityFilter.none() : filter;
        var storedKind = f.kind() == null ? null : mapper.toStorage(f.kind());
        return storage.findPage(environmentId, storedKind, f.source(), f.entityIdPrefix(), pageable).map(mapper::toDomain);
    }
}
