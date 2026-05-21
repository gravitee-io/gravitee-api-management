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

import io.gravitee.gamma.authorization.api.AuthzEntityRepository;
import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.infra.repository.document.AuthzEntityDocumentMapper;
import io.gravitee.gamma.authorization.infra.repository.document.AuthzEntityMongo;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.service.AuthzEntityFilter;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@RequiredArgsConstructor
public class MongoAuthzEntityRepository implements AuthzEntityRepository {

    private final MongoOperations mongo;

    @PostConstruct
    void ensureIndexes() {
        var indexOps = mongo.indexOps(AuthzEntityMongo.class);
        indexOps.ensureIndex(new Index().on("environmentId", Sort.Direction.ASC).on("entityId", Sort.Direction.ASC).named("e1e1").unique());
        indexOps.ensureIndex(new Index().on("environmentId", Sort.Direction.ASC).on("kind", Sort.Direction.ASC).named("e1k1"));
    }

    @Override
    public AuthzEntity save(AuthzEntity entity) {
        mongo.save(AuthzEntityDocumentMapper.toDocument(entity));
        return entity;
    }

    @Override
    public Optional<AuthzEntity> findById(String environmentId, String id) {
        var query = new Query(Criteria.where("_id").is(id).and("environmentId").is(environmentId));
        return Optional.ofNullable(mongo.findOne(query, AuthzEntityMongo.class)).map(AuthzEntityDocumentMapper::toDomain);
    }

    @Override
    public Optional<AuthzEntity> findByEntityId(String environmentId, String entityId) {
        var query = new Query(Criteria.where("environmentId").is(environmentId).and("entityId").is(entityId));
        return Optional.ofNullable(mongo.findOne(query, AuthzEntityMongo.class)).map(AuthzEntityDocumentMapper::toDomain);
    }

    @Override
    public List<AuthzEntity> findAll(String environmentId) {
        var query = new Query(Criteria.where("environmentId").is(environmentId));
        return mongo.find(query, AuthzEntityMongo.class).stream().map(AuthzEntityDocumentMapper::toDomain).toList();
    }

    @Override
    public List<AuthzEntity> findByKind(String environmentId, AuthzEntityKind kind) {
        var query = new Query(Criteria.where("environmentId").is(environmentId).and("kind").is(kind.name()));
        return mongo.find(query, AuthzEntityMongo.class).stream().map(AuthzEntityDocumentMapper::toDomain).toList();
    }

    @Override
    public List<AuthzEntity> findByEntityIdPrefix(String environmentId, String prefix) {
        var query = new Query(Criteria.where("environmentId").is(environmentId).and("entityId").regex("^" + Pattern.quote(prefix)));
        return mongo.find(query, AuthzEntityMongo.class).stream().map(AuthzEntityDocumentMapper::toDomain).toList();
    }

    @Override
    public List<AuthzEntity> findByEntityIds(String environmentId, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return List.of();
        }
        var query = new Query(Criteria.where("environmentId").is(environmentId).and("entityId").in(entityIds));
        return mongo.find(query, AuthzEntityMongo.class).stream().map(AuthzEntityDocumentMapper::toDomain).toList();
    }

    @Override
    public List<AuthzEntity> findByAnyEntityIdPrefix(String environmentId, Collection<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return List.of();
        }
        List<Criteria> orCriteria = prefixes
            .stream()
            .map(p -> Criteria.where("entityId").regex("^" + Pattern.quote(p)))
            .toList();
        var query = new Query(new Criteria("environmentId").is(environmentId).orOperator(orCriteria));
        return mongo.find(query, AuthzEntityMongo.class).stream().map(AuthzEntityDocumentMapper::toDomain).toList();
    }

    @Override
    public boolean deleteById(String environmentId, String id) {
        var query = new Query(Criteria.where("_id").is(id).and("environmentId").is(environmentId));
        return mongo.remove(query, AuthzEntityMongo.class).getDeletedCount() > 0;
    }

    @Override
    public boolean deleteByEntityId(String environmentId, String entityId) {
        var query = new Query(Criteria.where("environmentId").is(environmentId).and("entityId").is(entityId));
        return mongo.remove(query, AuthzEntityMongo.class).getDeletedCount() > 0;
    }

    @Override
    public long deleteByEntityIds(String environmentId, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return 0L;
        }
        var query = new Query(Criteria.where("environmentId").is(environmentId).and("entityId").in(entityIds));
        return mongo.remove(query, AuthzEntityMongo.class).getDeletedCount();
    }

    @Override
    public PagedResult<AuthzEntity> findPage(String environmentId, AuthzEntityFilter filter, Pageable pageable) {
        AuthzEntityFilter f = filter == null ? AuthzEntityFilter.none() : filter;
        Query query = new Query(Criteria.where("environmentId").is(environmentId));
        if (f.kind() != null) {
            query.addCriteria(Criteria.where("kind").is(f.kind().name()));
        }
        if (f.source() != null) {
            query.addCriteria(Criteria.where("source").is(f.source()));
        }
        if (f.entityIdPrefix() != null) {
            query.addCriteria(Criteria.where("entityId").regex("^" + Pattern.quote(f.entityIdPrefix())));
        }
        long total = mongo.count(query, AuthzEntityMongo.class);
        query.skip(pageable.skip()).limit(pageable.perPage());
        var data = mongo.find(query, AuthzEntityMongo.class).stream().map(AuthzEntityDocumentMapper::toDomain).toList();
        return new PagedResult<>(data, total, pageable.page(), pageable.perPage());
    }
}
