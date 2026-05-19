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

import io.gravitee.gamma.repository.authorization.api.AuthorizationPolicyRepository;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyStatus;
import io.gravitee.gamma.repository.exceptions.TechnicalException;
import io.gravitee.gamma.repository.mongodb.internal.authorization.AuthorizationPolicyMongoRepository;
import io.gravitee.gamma.repository.mongodb.internal.model.AuthorizationPolicyMongo;
import io.gravitee.gamma.repository.mongodb.mapper.AuthorizationMapper;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
public class MongoAuthorizationPolicyRepository implements AuthorizationPolicyRepository {

    @Autowired
    private AuthorizationPolicyMongoRepository internalRepository;

    @Autowired
    private AuthorizationMapper mapper;

    @Autowired
    @Qualifier("mongoTemplate")
    private MongoOperations mongoOperations;

    @Override
    public Optional<AuthorizationPolicy> findById(String id) throws TechnicalException {
        log.debug("Find authorization policy by id [{}]", id);
        return internalRepository.findById(id).map(mapper::map);
    }

    @Override
    public AuthorizationPolicy create(AuthorizationPolicy policy) throws TechnicalException {
        log.debug("Create authorization policy [{}]", policy.getId());
        return mapper.map(internalRepository.insert(mapper.map(policy)));
    }

    @Override
    public AuthorizationPolicy update(AuthorizationPolicy policy) throws TechnicalException {
        if (policy == null) {
            throw new IllegalStateException("AuthorizationPolicy must not be null");
        }
        var existing = internalRepository.findById(policy.getId()).orElse(null);
        if (existing == null) {
            throw new IllegalStateException(String.format("No authorization policy found with id [%s]", policy.getId()));
        }
        log.debug("Update authorization policy [{}]", policy.getId());
        return mapper.map(internalRepository.save(mapper.map(policy)));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        log.debug("Delete authorization policy [{}]", id);
        internalRepository.deleteById(id);
    }

    @Override
    public Set<AuthorizationPolicy> findAll() throws TechnicalException {
        return internalRepository.findAll().stream().map(mapper::map).collect(Collectors.toSet());
    }

    @Override
    public Optional<AuthorizationPolicy> findByEnvironmentIdAndId(String environmentId, String id) throws TechnicalException {
        return internalRepository.findByEnvironmentIdAndId(environmentId, id).map(mapper::map);
    }

    @Override
    public List<AuthorizationPolicy> findAllByEnvironmentId(String environmentId) throws TechnicalException {
        return internalRepository.findAllByEnvironmentId(environmentId).stream().map(mapper::map).toList();
    }

    @Override
    public List<AuthorizationPolicy> findAllByEnvironmentIdAndKind(String environmentId, AuthorizationPolicyKind kind)
        throws TechnicalException {
        return internalRepository.findAllByEnvironmentIdAndKind(environmentId, kind).stream().map(mapper::map).toList();
    }

    @Override
    public List<AuthorizationPolicy> findAllByEnvironmentIdAndEntityId(String environmentId, String entityId) throws TechnicalException {
        return internalRepository.findAllByEnvironmentIdAndEntityId(environmentId, entityId).stream().map(mapper::map).toList();
    }

    @Override
    public long deleteByEnvironmentIdAndId(String environmentId, String id) throws TechnicalException {
        return internalRepository.deleteByEnvironmentIdAndId(environmentId, id);
    }

    @Override
    public PagedResult<AuthorizationPolicy> findPage(
        String environmentId,
        AuthorizationPolicyKind kind,
        String entityId,
        AuthorizationPolicyStatus status,
        Pageable pageable
    ) throws TechnicalException {
        Query query = new Query(Criteria.where("environmentId").is(environmentId));
        if (kind != null) {
            query.addCriteria(Criteria.where("kind").is(kind));
        }
        if (entityId != null) {
            query.addCriteria(Criteria.where("entityId").is(entityId));
        }
        if (status != null) {
            query.addCriteria(Criteria.where("status").is(status));
        }
        long total = mongoOperations.count(query, AuthorizationPolicyMongo.class);
        query.skip(pageable.skip()).limit(pageable.perPage());
        List<AuthorizationPolicy> data = mongoOperations.find(query, AuthorizationPolicyMongo.class).stream().map(mapper::map).toList();
        return new PagedResult<>(data, total, pageable.page(), pageable.perPage());
    }
}
