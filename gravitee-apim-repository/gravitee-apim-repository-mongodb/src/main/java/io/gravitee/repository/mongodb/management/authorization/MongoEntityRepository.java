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
package io.gravitee.repository.mongodb.management.authorization;

import io.gravitee.apim.authorization.api.EntityRepository;
import io.gravitee.apim.authorization.domain.Entity;
import io.gravitee.apim.authorization.domain.EntityKind;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MongoEntityRepository implements EntityRepository {

    private final SpringEntityMongoRepository springRepository;

    public MongoEntityRepository(SpringEntityMongoRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Entity save(Entity entity) {
        return springRepository.save(EntityDocument.fromDomain(entity)).toDomain();
    }

    @Override
    public Optional<Entity> findById(String environmentId, String id) {
        return springRepository.findByEnvironmentIdAndId(environmentId, id).map(EntityDocument::toDomain);
    }

    @Override
    public Optional<Entity> findByEntityId(String environmentId, String entityId) {
        Objects.requireNonNull(entityId, "entityId");
        return springRepository.findByEnvironmentIdAndEntityId(environmentId, entityId).map(EntityDocument::toDomain);
    }

    @Override
    public List<Entity> findAll(String environmentId) {
        return springRepository.findAllByEnvironmentId(environmentId).stream().map(EntityDocument::toDomain).toList();
    }

    @Override
    public List<Entity> findByKind(String environmentId, EntityKind kind) {
        return springRepository.findAllByEnvironmentIdAndKind(environmentId, kind).stream().map(EntityDocument::toDomain).toList();
    }

    @Override
    public List<Entity> findByEntityIdPrefix(String environmentId, String prefix) {
        return springRepository
            .findAllByEnvironmentIdAndEntityIdStartingWith(environmentId, prefix)
            .stream()
            .map(EntityDocument::toDomain)
            .toList();
    }

    @Override
    public boolean deleteById(String environmentId, String id) {
        return springRepository.deleteByEnvironmentIdAndId(environmentId, id) > 0L;
    }

    @Override
    public boolean deleteByEntityId(String environmentId, String entityId) {
        return springRepository.deleteByEnvironmentIdAndEntityId(environmentId, entityId) > 0L;
    }
}
