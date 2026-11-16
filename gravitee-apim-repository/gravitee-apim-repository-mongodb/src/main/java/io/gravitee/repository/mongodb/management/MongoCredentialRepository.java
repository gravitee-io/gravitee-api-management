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

import static org.springframework.data.mongodb.core.query.Criteria.where;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CredentialRepository;
import io.gravitee.repository.management.api.search.CredentialCriteria;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Credential;
import io.gravitee.repository.mongodb.management.internal.credential.CredentialMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.CredentialMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.CustomLog;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class MongoCredentialRepository implements CredentialRepository {

    @Autowired
    private CredentialMongoRepository internalRepo;

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Credential create(Credential credential) throws TechnicalException {
        log.debug("MongoCredentialRepository.create({})", credential.getId());
        try {
            return mapper.map(internalRepo.insert(mapper.map(credential)));
        } catch (org.springframework.dao.DuplicateKeyException ex) {
            throw new DuplicateKeyException("A credential with id [" + credential.getId() + "] already exists", ex);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to create credential " + credential.getId(), ex);
        }
    }

    @Override
    public Optional<Credential> findById(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("MongoCredentialRepository.findById({}, {}, {})", organizationId, environmentId, id);
        try {
            return internalRepo.findById(organizationId, environmentId, id).map(mapper::map);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to find credential " + id, ex);
        }
    }

    @Override
    public Credential update(Credential credential) throws TechnicalException {
        log.debug("MongoCredentialRepository.update({})", credential.getId());
        var filter = Filters.and(
            Filters.eq("_id", mongoTemplate.getConverter().convertId(credential.getId(), ObjectId.class)),
            Filters.eq("organizationId", credential.getOrganizationId()),
            Filters.eq("environmentId", credential.getEnvironmentId())
        );
        var update = Updates.combine(
            Updates.set("name", credential.getName()),
            Updates.set("serviceId", credential.getServiceId()),
            Updates.set("ownerId", credential.getOwnerId()),
            Updates.set("expiresAt", credential.getExpiresAt()),
            Updates.set("configuration", credential.getConfiguration()),
            Updates.set("secretReferences", credential.getSecretReferences()),
            Updates.set("updatedAt", credential.getUpdatedAt()),
            Updates.set("rotatedAt", credential.getRotatedAt())
        );
        long matched;
        try {
            matched = mongoTemplate.execute(CredentialMongo.class, collection -> collection.updateOne(filter, update)).getMatchedCount();
        } catch (Exception ex) {
            throw new TechnicalException("Failed to update credential " + credential.getId(), ex);
        }
        if (matched == 0) {
            throw new TechnicalException("Credential " + credential.getId() + " not found in environment " + credential.getEnvironmentId());
        }
        return findById(credential.getOrganizationId(), credential.getEnvironmentId(), credential.getId()).orElseThrow(() ->
            new TechnicalException("Credential " + credential.getId() + " disappeared after its update")
        );
    }

    @Override
    public void delete(String organizationId, String environmentId, String id) throws TechnicalException {
        log.debug("MongoCredentialRepository.delete({}, {}, {})", organizationId, environmentId, id);
        try {
            internalRepo.delete(organizationId, environmentId, id);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete credential " + id, ex);
        }
    }

    @Override
    public Page<Credential> search(String organizationId, String environmentId, CredentialCriteria criteria, Pageable pageable)
        throws TechnicalException {
        log.debug("MongoCredentialRepository.search({}, {})", organizationId, environmentId);
        try {
            var query = new Query(where("organizationId").is(organizationId).and("environmentId").is(environmentId));
            if (criteria.getQuery() != null) {
                // Quoted: the JDBC side matches with an escaped LIKE, so the query is literal text on both.
                query.addCriteria(where("name").regex(Pattern.quote(criteria.getQuery()), "i"));
            }
            if (criteria.getStrategies() != null && !criteria.getStrategies().isEmpty()) {
                query.addCriteria(where("strategy").in(criteria.getStrategies()));
            }
            if (criteria.getServiceId() != null) {
                query.addCriteria(where("serviceId").is(criteria.getServiceId()));
            }
            long total = mongoTemplate.count(query, CredentialMongo.class);
            var content = mongoTemplate
                .find(query.with(PageRequest.of(pageable.pageNumber(), pageable.pageSize(), Sort.by("name", "id"))), CredentialMongo.class)
                .stream()
                .map(mapper::map)
                .toList();
            return new Page<>(content, pageable.pageNumber(), content.size(), total);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to search credentials of environment " + environmentId, ex);
        }
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("MongoCredentialRepository.deleteByEnvironmentId({})", environmentId);
        try {
            internalRepo.deleteByEnvironmentId(environmentId);
        } catch (Exception ex) {
            throw new TechnicalException("Failed to delete credentials of environment " + environmentId, ex);
        }
    }
}
