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

import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.model.OrganizationKey;
import io.gravitee.repository.mongodb.management.internal.model.OrganizationKeyMongo;
import io.gravitee.repository.mongodb.management.internal.organizationkey.OrganizationKeyMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class MongoOrganizationKeyRepository implements OrganizationKeyRepository {

    @Autowired
    private OrganizationKeyMongoRepository internalRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public OrganizationKey create(OrganizationKey organizationKey) throws TechnicalException {
        log.debug("MongoOrganizationKeyRepository.create({})", organizationKey.getId());
        try {
            return mapper.map(internalRepo.insert(mapper.map(organizationKey)));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new DuplicateKeyException("Organization [" + organizationKey.getOrganizationId() + "] already has a key", ex);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create the key of organization " + organizationKey.getOrganizationId(), ex);
        }
    }

    @Override
    public Optional<OrganizationKey> findByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("MongoOrganizationKeyRepository.findByOrganizationId({})", organizationId);
        try {
            return internalRepo.findByOrganizationId(organizationId).map(mapper::map);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find the key of organization " + organizationId, ex);
        }
    }

    @Override
    public Set<String> findKekIds() throws TechnicalException {
        log.debug("MongoOrganizationKeyRepository.findKekIds()");
        try {
            return mongoTemplate
                .getCollection(mongoTemplate.getCollectionName(OrganizationKeyMongo.class))
                .distinct("kekId", String.class)
                .into(new HashSet<>());
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find the key encryption key ids", ex);
        }
    }

    @Override
    public void deleteByOrganizationId(String organizationId) throws TechnicalException {
        log.debug("MongoOrganizationKeyRepository.deleteByOrganizationId({})", organizationId);
        try {
            internalRepo.deleteByOrganizationId(organizationId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete the key of organization " + organizationId, ex);
        }
    }
}
