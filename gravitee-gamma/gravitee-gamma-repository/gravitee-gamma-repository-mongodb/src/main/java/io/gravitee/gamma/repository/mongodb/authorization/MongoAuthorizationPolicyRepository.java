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
import io.gravitee.gamma.repository.exceptions.TechnicalException;
import io.gravitee.gamma.repository.mongodb.internal.authorization.AuthorizationPolicyMongoRepository;
import io.gravitee.gamma.repository.mongodb.mapper.AuthorizationMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class MongoAuthorizationPolicyRepository implements AuthorizationPolicyRepository {

    @Autowired
    private AuthorizationPolicyMongoRepository internalRepository;

    @Autowired
    private AuthorizationMapper mapper;

    @Override
    public Optional<AuthorizationPolicy> findById(String id) throws TechnicalException {
        log.debug("Find authorization policy by id [{}]", id);
        return internalRepository.findById(id).map(mapper::map);
    }

    @Override
    public AuthorizationPolicy create(AuthorizationPolicy policy) throws TechnicalException {
        log.debug("Create authorization policy [{}]", policy.id());
        return mapper.map(internalRepository.insert(mapper.map(policy)));
    }

    @Override
    public AuthorizationPolicy update(AuthorizationPolicy policy) throws TechnicalException {
        if (policy == null) {
            throw new IllegalStateException("AuthorizationPolicy must not be null");
        }
        var existing = internalRepository.findById(policy.id()).orElse(null);
        if (existing == null) {
            throw new IllegalStateException(String.format("No authorization policy found with id [%s]", policy.id()));
        }
        log.debug("Update authorization policy [{}]", policy.id());
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
}
