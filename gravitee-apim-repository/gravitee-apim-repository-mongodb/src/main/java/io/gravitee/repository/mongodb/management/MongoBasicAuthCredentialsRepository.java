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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.mongodb.client.result.UpdateResult;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.BasicAuthCredentialsRepository;
import io.gravitee.repository.management.api.search.BasicAuthCredentialsCriteria;
import io.gravitee.repository.management.model.BasicAuthCredentials;
import io.gravitee.repository.mongodb.management.internal.basic_auth.BasicAuthCredentialsMongoInternalRepository;
import io.gravitee.repository.mongodb.management.internal.model.BasicAuthCredentialsMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class MongoBasicAuthCredentialsRepository implements BasicAuthCredentialsRepository {

    @Autowired
    private GraviteeMapper mapper;

    @Autowired
    private BasicAuthCredentialsMongoInternalRepository internalRepository;

    @Override
    public Optional<BasicAuthCredentials> findById(String id) throws TechnicalException {
        return internalRepository.findById(id).map(this::toBasicAuthCredentials);
    }

    @Override
    public Optional<BasicAuthCredentials> findByUsername(String username) throws TechnicalException {
        return Optional.ofNullable(internalRepository.findByUsername(username)).map(this::toBasicAuthCredentials);
    }

    @Override
    public Optional<BasicAuthCredentials> findByUsernameAndEnvironmentId(String username, String environmentId) throws TechnicalException {
        return Optional.ofNullable(internalRepository.findByUsernameAndEnvironmentId(username, environmentId)).map(
            this::toBasicAuthCredentials
        );
    }

    @Override
    public BasicAuthCredentials create(BasicAuthCredentials credentials) throws TechnicalException {
        BasicAuthCredentialsMongo mongo = mapper.map(credentials);
        mongo = internalRepository.insert(mongo);
        return toBasicAuthCredentials(mongo);
    }

    @Override
    public BasicAuthCredentials update(BasicAuthCredentials credentials) throws TechnicalException {
        if (credentials == null || credentials.getId() == null) {
            throw new IllegalStateException("BasicAuthCredentials to update must have an id");
        }

        BasicAuthCredentialsMongo existing = internalRepository.findById(credentials.getId()).orElse(null);
        if (existing == null) {
            throw new IllegalStateException(String.format("No basic auth credentials found with id [%s]", credentials.getId()));
        }

        BasicAuthCredentialsMongo mongo = internalRepository.save(mapper.map(credentials));
        return toBasicAuthCredentials(mongo);
    }

    @Override
    public Set<BasicAuthCredentials> findBySubscription(String subscription) throws TechnicalException {
        return internalRepository.findBySubscription(subscription).stream().map(this::toBasicAuthCredentials).collect(toSet());
    }

    @Override
    public List<BasicAuthCredentials> findByApplication(String applicationId) throws TechnicalException {
        return internalRepository.findByApplication(applicationId).stream().map(this::toBasicAuthCredentials).collect(toList());
    }

    @Override
    public List<BasicAuthCredentials> findByCriteria(BasicAuthCredentialsCriteria criteria) throws TechnicalException {
        return mapper.mapBasicAuthCredentials(internalRepository.search(criteria));
    }

    @Override
    public Optional<BasicAuthCredentials> addSubscription(String id, String subscriptionId) throws TechnicalException {
        UpdateResult result = internalRepository.addSubscription(id, subscriptionId);
        if (result.getMatchedCount() == 0) {
            return Optional.empty();
        }
        return findById(id);
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete basic auth credentials by environmentId [{}]", environmentId);
        try {
            List<String> deletedIds = internalRepository
                .deleteByEnvironmentId(environmentId)
                .stream()
                .map(BasicAuthCredentialsMongo::getId)
                .toList();
            log.debug("Delete basic auth credentials by environmentId [{}] - Done", environmentId);
            return deletedIds;
        } catch (Exception ex) {
            log.error("Failed to delete basic auth credentials by environmentId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete basic auth credentials by environmentId", ex);
        }
    }

    private BasicAuthCredentials toBasicAuthCredentials(BasicAuthCredentialsMongo mongo) {
        return mapper.map(mongo);
    }
}
