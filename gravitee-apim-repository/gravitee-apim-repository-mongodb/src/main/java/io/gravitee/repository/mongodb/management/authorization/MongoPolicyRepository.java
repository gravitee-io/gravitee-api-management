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

import io.gravitee.apim.authorization.api.PolicyRepository;
import io.gravitee.apim.authorization.domain.Policy;
import io.gravitee.apim.authorization.domain.PolicyKind;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class MongoPolicyRepository implements PolicyRepository {

    private final SpringPolicyMongoRepository springRepository;

    public MongoPolicyRepository(SpringPolicyMongoRepository springRepository) {
        this.springRepository = springRepository;
    }

    @Override
    public Policy save(Policy policy) {
        return springRepository.save(PolicyDocument.fromDomain(policy)).toDomain();
    }

    @Override
    public Optional<Policy> findById(String environmentId, String id) {
        return springRepository.findByEnvironmentIdAndId(environmentId, id).map(PolicyDocument::toDomain);
    }

    @Override
    public List<Policy> findAll(String environmentId) {
        return springRepository.findAllByEnvironmentId(environmentId).stream().map(PolicyDocument::toDomain).toList();
    }

    @Override
    public List<Policy> findByKind(String environmentId, PolicyKind kind) {
        return springRepository.findAllByEnvironmentIdAndKind(environmentId, kind).stream().map(PolicyDocument::toDomain).toList();
    }

    @Override
    public List<Policy> findByEntityId(String environmentId, String entityId) {
        Objects.requireNonNull(entityId, "entityId");
        return springRepository.findAllByEnvironmentIdAndEntityId(environmentId, entityId).stream().map(PolicyDocument::toDomain).toList();
    }

    @Override
    public boolean deleteById(String environmentId, String id) {
        return springRepository.deleteByEnvironmentIdAndId(environmentId, id) > 0L;
    }
}
