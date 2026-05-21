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

import io.gravitee.gamma.authorization.api.PolicyRepository;
import io.gravitee.gamma.authorization.domain.Policy;
import io.gravitee.gamma.authorization.domain.PolicyKind;
import io.gravitee.gamma.authorization.infra.adapter.AuthorizationAdapter;
import io.gravitee.gamma.authorization.service.PolicyFilter;
import io.gravitee.gamma.repository.authorization.api.AuthorizationPolicyRepository;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PolicyRepositoryAdapter implements PolicyRepository {

    private final AuthorizationPolicyRepository storage;
    private final AuthorizationAdapter mapper;

    public PolicyRepositoryAdapter(AuthorizationPolicyRepository storage, AuthorizationAdapter mapper) {
        this.storage = storage;
        this.mapper = mapper;
    }

    @Override
    public Policy save(Policy policy) {
        var stored = mapper.toStorage(policy);
        var existing = storage.findById(stored.getId());
        var saved = existing.isPresent() ? storage.update(stored) : storage.create(stored);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Policy> findById(String environmentId, String id) {
        return storage.findByEnvironmentIdAndId(environmentId, id).map(mapper::toDomain);
    }

    @Override
    public List<Policy> findAll(String environmentId) {
        return storage.findAllByEnvironmentId(environmentId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Policy> findByKind(String environmentId, PolicyKind kind) {
        return storage.findAllByEnvironmentIdAndKind(environmentId, mapper.toStorage(kind)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Policy> findByEntityId(String environmentId, String entityId) {
        return storage.findAllByEnvironmentIdAndEntityId(environmentId, entityId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean deleteById(String environmentId, String id) {
        return storage.deleteByEnvironmentIdAndId(environmentId, id) > 0;
    }

    @Override
    public PagedResult<Policy> findPage(String environmentId, PolicyFilter filter, Pageable pageable) {
        PolicyFilter f = filter == null ? PolicyFilter.none() : filter;
        var storedKind = f.kind() == null ? null : mapper.toStorage(f.kind());
        var storedStatus = f.status() == null ? null : mapper.toStorage(f.status());
        return storage.findPage(environmentId, storedKind, f.entityId(), storedStatus, pageable).map(mapper::toDomain);
    }
}
