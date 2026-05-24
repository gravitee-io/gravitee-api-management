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

import io.gravitee.gamma.authorization.api.AuthzPolicyRepository;
import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.infra.repository.document.AuthzPolicyDocumentMapper;
import io.gravitee.gamma.authorization.infra.repository.document.AuthzPolicyMongo;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.service.AuthzPolicyFilter;
import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@RequiredArgsConstructor
public class MongoAuthzPolicyRepository implements AuthzPolicyRepository {

    private final MongoOperations mongo;

    @PostConstruct
    void ensureIndexes() {
        var indexOps = mongo.indexOps(AuthzPolicyMongo.class);
        indexOps.ensureIndex(new Index().on("environmentId", Sort.Direction.ASC).named("e1"));
        indexOps.ensureIndex(new Index().on("environmentId", Sort.Direction.ASC).on("entityId", Sort.Direction.ASC).named("e1ei1"));
    }

    @Override
    public AuthzPolicy save(AuthzPolicy policy) {
        mongo.save(AuthzPolicyDocumentMapper.toDocument(policy));
        return policy;
    }

    @Override
    public Optional<AuthzPolicy> findById(String environmentId, String id) {
        var query = new Query(Criteria.where("_id").is(id).and("environmentId").is(environmentId));
        return Optional.ofNullable(mongo.findOne(query, AuthzPolicyMongo.class)).map(AuthzPolicyDocumentMapper::toDomain);
    }

    @Override
    public List<AuthzPolicy> findAll(String environmentId) {
        var query = new Query(Criteria.where("environmentId").is(environmentId));
        return mongo.find(query, AuthzPolicyMongo.class).stream().map(AuthzPolicyDocumentMapper::toDomain).toList();
    }

    @Override
    public List<AuthzPolicy> findByKind(String environmentId, AuthzPolicyKind kind) {
        var query = new Query(Criteria.where("environmentId").is(environmentId).and("kind").is(kind.name()));
        return mongo.find(query, AuthzPolicyMongo.class).stream().map(AuthzPolicyDocumentMapper::toDomain).toList();
    }

    @Override
    public List<AuthzPolicy> findByEntityId(String environmentId, String entityId) {
        var query = new Query(Criteria.where("environmentId").is(environmentId).and("entityId").is(entityId));
        return mongo.find(query, AuthzPolicyMongo.class).stream().map(AuthzPolicyDocumentMapper::toDomain).toList();
    }

    @Override
    public List<AuthzPolicy> findByEntityIds(String environmentId, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return List.of();
        }
        var query = new Query(Criteria.where("environmentId").is(environmentId).and("entityId").in(entityIds));
        return mongo.find(query, AuthzPolicyMongo.class).stream().map(AuthzPolicyDocumentMapper::toDomain).toList();
    }

    @Override
    public boolean deleteById(String environmentId, String id) {
        var query = new Query(Criteria.where("_id").is(id).and("environmentId").is(environmentId));
        return mongo.remove(query, AuthzPolicyMongo.class).getDeletedCount() > 0;
    }

    @Override
    public long deleteByIds(String environmentId, Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        var query = new Query(Criteria.where("_id").in(ids).and("environmentId").is(environmentId));
        return mongo.remove(query, AuthzPolicyMongo.class).getDeletedCount();
    }

    @Override
    public PagedResult<AuthzPolicy> findPage(String environmentId, AuthzPolicyFilter filter, Pageable pageable) {
        AuthzPolicyFilter f = filter == null ? AuthzPolicyFilter.none() : filter;
        Query query = new Query(Criteria.where("environmentId").is(environmentId));
        if (f.kind() != null) {
            query.addCriteria(Criteria.where("kind").is(f.kind().name()));
        }
        if (f.entityId() != null) {
            query.addCriteria(Criteria.where("entityId").is(f.entityId()));
        }
        if (f.status() != null) {
            query.addCriteria(Criteria.where("status").is(f.status().name()));
        }
        long total = mongo.count(query, AuthzPolicyMongo.class);
        query.skip(pageable.skip()).limit(pageable.perPage());
        var data = mongo.find(query, AuthzPolicyMongo.class).stream().map(AuthzPolicyDocumentMapper::toDomain).toList();
        return new PagedResult<>(data, total, pageable.page(), pageable.perPage());
    }
}
