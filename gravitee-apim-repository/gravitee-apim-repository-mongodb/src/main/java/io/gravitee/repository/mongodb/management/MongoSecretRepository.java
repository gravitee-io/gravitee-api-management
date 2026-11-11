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
package io.gravitee.repository.mongodb.management;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Secret;
import io.gravitee.repository.mongodb.management.internal.model.SecretMongo;
import io.gravitee.repository.mongodb.management.internal.secret.SecretMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import lombok.CustomLog;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class MongoSecretRepository implements SecretRepository {

    @Autowired
    private SecretMongoRepository internalRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Secret create(Secret secret) throws TechnicalException {
        log.debug("MongoSecretRepository.create({})", secret.getId());
        try {
            return mapper.map(internalRepo.insert(mapper.map(secret)));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new DuplicateKeyException(
                "A secret with id [" +
                    secret.getId() +
                    "] or key [" +
                    secret.getKey() +
                    "] already exists in environment [" +
                    secret.getEnvironmentId() +
                    "]",
                ex
            );
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create secret " + secret.getId(), ex);
        }
    }

    @Override
    public Optional<Secret> findById(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("MongoSecretRepository.findById({}, {}, {})", organizationId, environmentId, id);
        try {
            return internalRepo.findById(organizationId, environmentId, id).map(mapper::map);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find secret " + id, ex);
        }
    }

    @Override
    public Optional<Secret> findByKey(String organizationId, String environmentId, String key) throws TechnicalException {
        log.debug("MongoSecretRepository.findByKey({}, {}, {})", organizationId, environmentId, key);
        try {
            return internalRepo.findByKey(organizationId, environmentId, key).map(mapper::map);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find secret by key " + key, ex);
        }
    }

    @Override
    public Secret update(Secret secret) throws TechnicalException {
        log.debug("MongoSecretRepository.update({})", secret.getId());
        var filter = Filters.and(
            Filters.eq("_id", mongoTemplate.getConverter().convertId(secret.getId(), ObjectId.class)),
            Filters.eq("organizationId", secret.getOrganizationId()),
            Filters.eq("environmentId", secret.getEnvironmentId())
        );
        var update = Updates.combine(
            Updates.set("encryptedValue", secret.getEncryptedValue()),
            Updates.set("updatedAt", secret.getUpdatedAt())
        );
        long matched;
        try {
            matched = mongoTemplate.execute(SecretMongo.class, collection -> collection.updateOne(filter, update)).getMatchedCount();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to update secret " + secret.getId(), ex);
        }
        if (matched == 0) {
            throw new TechnicalException("Secret " + secret.getId() + " not found in environment " + secret.getEnvironmentId());
        }
        return findById(secret.getOrganizationId(), secret.getEnvironmentId(), secret.getId()).orElseThrow(() ->
            new TechnicalException("Secret " + secret.getId() + " disappeared after its update")
        );
    }

    @Override
    public void delete(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("MongoSecretRepository.delete({}, {}, {})", organizationId, environmentId, id);
        try {
            internalRepo.delete(organizationId, environmentId, id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete secret " + id, ex);
        }
    }

    @Override
    public Page<Secret> search(String organizationId, String environmentId, Pageable pageable) throws TechnicalException {
        log.debug("MongoSecretRepository.search({}, {})", organizationId, environmentId);
        try {
            var result = internalRepo.search(
                organizationId,
                environmentId,
                PageRequest.of(pageable.pageNumber(), pageable.pageSize(), Sort.by("key"))
            );
            return new Page<>(
                result.getContent().stream().map(mapper::map).toList(),
                pageable.pageNumber(),
                result.getNumberOfElements(),
                result.getTotalElements()
            );
        } catch (Exception ex) {
            throw new TechnicalException("Failed to search secrets of environment " + environmentId, ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("MongoSecretRepository.deleteByEnvironmentId({})", environmentId);
        try {
            internalRepo.deleteByEnvironmentId(environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete secrets of environment " + environmentId, ex);
        }
    }

    @Override
    public boolean existsByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("MongoSecretRepository.existsByOrganizationId({})", organizationId);
        try {
            return internalRepo.existsByOrganizationId(organizationId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to check for secrets of organization " + organizationId, ex);
        }
    }
}
