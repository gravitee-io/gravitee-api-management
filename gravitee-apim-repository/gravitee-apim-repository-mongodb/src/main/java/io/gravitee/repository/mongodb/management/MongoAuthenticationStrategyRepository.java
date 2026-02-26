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

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AuthenticationStrategyRepository;
import io.gravitee.repository.management.model.AuthenticationStrategy;
import io.gravitee.repository.mongodb.management.internal.application.AuthenticationStrategyMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.AuthenticationStrategyMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@CustomLog
@Component
public class MongoAuthenticationStrategyRepository implements AuthenticationStrategyRepository {

    @Autowired
    private AuthenticationStrategyMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<AuthenticationStrategy> findById(String id) throws TechnicalException {
        log.debug("Find authentication strategy by ID [{}]", id);
        AuthenticationStrategyMongo mongo = internalRepository.findById(id).orElse(null);
        log.debug("Find authentication strategy by ID [{}] - Done", id);
        return Optional.ofNullable(map(mongo));
    }

    @Override
    public AuthenticationStrategy create(AuthenticationStrategy strategy) throws TechnicalException {
        log.debug("Create authentication strategy [{}]", strategy.getName());
        AuthenticationStrategyMongo mongo = map(strategy);
        AuthenticationStrategyMongo created = internalRepository.insert(mongo);
        log.debug("Create authentication strategy [{}] - Done", strategy.getName());
        return map(created);
    }

    @Override
    public AuthenticationStrategy update(AuthenticationStrategy strategy) throws TechnicalException {
        if (strategy == null) {
            throw new IllegalStateException("Authentication strategy must not be null");
        }

        AuthenticationStrategyMongo mongo = internalRepository.findById(strategy.getId()).orElse(null);
        if (mongo == null) {
            throw new IllegalStateException(
                String.format("No authentication strategy found with id [%s]", strategy.getId())
            );
        }

        try {
            mongo.setName(strategy.getName());
            mongo.setDisplayName(strategy.getDisplayName());
            mongo.setDescription(strategy.getDescription());
            mongo.setEnvironmentId(strategy.getEnvironmentId());
            mongo.setType(strategy.getType() != null ? strategy.getType().name() : null);
            mongo.setClientRegistrationProviderId(strategy.getClientRegistrationProviderId());
            mongo.setScopes(strategy.getScopes());
            mongo.setAuthMethods(strategy.getAuthMethods());
            mongo.setCredentialClaims(strategy.getCredentialClaims());
            mongo.setAutoApprove(strategy.isAutoApprove());
            mongo.setHideCredentials(strategy.isHideCredentials());
            mongo.setUpdatedAt(strategy.getUpdatedAt());

            AuthenticationStrategyMongo updated = internalRepository.save(mongo);
            return map(updated);
        } catch (Exception e) {
            log.error("An error occurs when updating authentication strategy", e);
            throw new TechnicalException("An error occurs when updating authentication strategy");
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            internalRepository.deleteById(id);
        } catch (Exception e) {
            log.error("An error occurs when deleting authentication strategy [{}]", id, e);
            throw new TechnicalException("An error occurs when deleting authentication strategy");
        }
    }

    @Override
    public Set<AuthenticationStrategy> findAll() throws TechnicalException {
        log.debug("Find all authentication strategies");
        List<AuthenticationStrategyMongo> all = internalRepository.findAll();
        Set<AuthenticationStrategy> result = mapper.mapAuthenticationStrategies(all);
        log.debug("Find all authentication strategies - Done");
        return result;
    }

    @Override
    public Set<AuthenticationStrategy> findAllByEnvironment(String environmentId) throws TechnicalException {
        log.debug("Find all authentication strategies by environment [{}]", environmentId);
        List<AuthenticationStrategyMongo> all = internalRepository.findByEnvironmentId(environmentId);
        Set<AuthenticationStrategy> result = mapper.mapAuthenticationStrategies(all);
        log.debug("Find all authentication strategies by environment - Done");
        return result;
    }

    @Override
    public Set<AuthenticationStrategy> findByClientRegistrationProviderId(String clientRegistrationProviderId) throws TechnicalException {
        log.debug("Find authentication strategies by provider [{}]", clientRegistrationProviderId);
        List<AuthenticationStrategyMongo> all = internalRepository.findByClientRegistrationProviderId(clientRegistrationProviderId);
        Set<AuthenticationStrategy> result = mapper.mapAuthenticationStrategies(all);
        log.debug("Find authentication strategies by provider - Done");
        return result;
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        log.debug("Delete authentication strategies by environment [{}]", environmentId);
        try {
            List<String> deleted = internalRepository
                .deleteByEnvironmentId(environmentId)
                .stream()
                .map(AuthenticationStrategyMongo::getId)
                .toList();
            log.debug("Delete authentication strategies by environment [{}] - Done", environmentId);
            return deleted;
        } catch (Exception e) {
            log.error("Failed to delete authentication strategies by environment [{}]", environmentId, e);
            throw new TechnicalException("Failed to delete authentication strategies by environment");
        }
    }

    private AuthenticationStrategy map(AuthenticationStrategyMongo mongo) {
        return (mongo == null) ? null : mapper.map(mongo);
    }

    private AuthenticationStrategyMongo map(AuthenticationStrategy strategy) {
        return (strategy == null) ? null : mapper.map(strategy);
    }
}
