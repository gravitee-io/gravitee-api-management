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
package io.gravitee.gamma.repository.mongodb.authorization;

import io.gravitee.gamma.repository.authorization.api.AuthorizationEntityRepository;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntityKind;
import io.gravitee.gamma.repository.exceptions.TechnicalException;
import io.gravitee.gamma.repository.mongodb.internal.authorization.AuthorizationEntityMongoRepository;
import io.gravitee.gamma.repository.mongodb.internal.model.AuthorizationEntityMongo;
import io.gravitee.gamma.repository.mongodb.mapper.AuthorizationMapper;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class MongoAuthorizationEntityRepository implements AuthorizationEntityRepository {

    @Autowired
    private AuthorizationEntityMongoRepository internalRepository;

    @Autowired
    private AuthorizationMapper mapper;

    @Autowired
    @Qualifier("managementMongoTemplate")
    private MongoOperations mongoOperations;

    @Override
    public Optional<AuthorizationEntity> findById(String id) throws TechnicalException {
        log.debug("Find authorization entity by id [{}]", id);
        return internalRepository.findById(id).map(mapper::map);
    }

    @Override
    public AuthorizationEntity create(AuthorizationEntity entity) throws TechnicalException {
        log.debug("Create authorization entity [{}]", entity.getId());
        return mapper.map(internalRepository.insert(mapper.map(entity)));
    }

    @Override
    public AuthorizationEntity update(AuthorizationEntity entity) throws TechnicalException {
        if (entity == null) {
            throw new IllegalStateException("AuthorizationEntity must not be null");
        }
        var existing = internalRepository.findById(entity.getId()).orElse(null);
        if (existing == null) {
            throw new IllegalStateException(String.format("No authorization entity found with id [%s]", entity.getId()));
        }
        log.debug("Update authorization entity [{}]", entity.getId());
        return mapper.map(internalRepository.save(mapper.map(entity)));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete authorization entity [{}]", id);
        internalRepository.deleteById(id);
    }

    @Override
    public Set<AuthorizationEntity> findAll() throws TechnicalException {
        return internalRepository.findAll().stream().map(mapper::map).collect(Collectors.toSet());
    }

    @Override
    public Optional<AuthorizationEntity> findByEnvironmentIdAndId(String environmentId, String id) throws TechnicalException {
        return internalRepository.findByEnvironmentIdAndId(environmentId, id).map(mapper::map);
    }

    @Override
    public Optional<AuthorizationEntity> findByEnvironmentIdAndEntityId(String environmentId, String entityId) throws TechnicalException {
        return internalRepository.findByEnvironmentIdAndEntityId(environmentId, entityId).map(mapper::map);
    }

    @Override
    public List<AuthorizationEntity> findAllByEnvironmentId(String environmentId) throws TechnicalException {
        return internalRepository.findAllByEnvironmentId(environmentId).stream().map(mapper::map).toList();
    }

    @Override
    public List<AuthorizationEntity> findAllByEnvironmentIdAndKind(String environmentId, AuthorizationEntityKind kind)
        throws TechnicalException {
        return internalRepository.findAllByEnvironmentIdAndKind(environmentId, kind).stream().map(mapper::map).toList();
    }

    @Override
    public List<AuthorizationEntity> findAllByEnvironmentIdAndEntityIdStartingWith(String environmentId, String entityIdPrefix)
        throws TechnicalException {
        return internalRepository
            .findAllByEnvironmentIdAndEntityIdStartingWith(environmentId, entityIdPrefix)
            .stream()
            .map(mapper::map)
            .toList();
    }

    @Override
    public long deleteByEnvironmentIdAndId(String environmentId, String id) throws TechnicalException {
        return internalRepository.deleteByEnvironmentIdAndId(environmentId, id);
    }

    @Override
    public long deleteByEnvironmentIdAndEntityId(String environmentId, String entityId) throws TechnicalException {
        return internalRepository.deleteByEnvironmentIdAndEntityId(environmentId, entityId);
    }

    @Override
    public PagedResult<AuthorizationEntity> findPage(
        String environmentId,
        AuthorizationEntityKind kind,
        String source,
        String entityIdPrefix,
        Pageable pageable
    ) throws TechnicalException {
        Query query = new Query(Criteria.where("environmentId").is(environmentId));
        if (kind != null) {
            query.addCriteria(Criteria.where("kind").is(kind));
        }
        if (source != null) {
            query.addCriteria(Criteria.where("source").is(source));
        }
        if (entityIdPrefix != null) {
            query.addCriteria(Criteria.where("entityId").regex("^" + Pattern.quote(entityIdPrefix)));
        }
        long total = mongoOperations.count(query, AuthorizationEntityMongo.class);
        query.skip(pageable.skip()).limit(pageable.perPage());
        List<AuthorizationEntity> data = mongoOperations.find(query, AuthorizationEntityMongo.class).stream().map(mapper::map).toList();
        return new PagedResult<>(data, total, pageable.page(), pageable.perPage());
    }
}
