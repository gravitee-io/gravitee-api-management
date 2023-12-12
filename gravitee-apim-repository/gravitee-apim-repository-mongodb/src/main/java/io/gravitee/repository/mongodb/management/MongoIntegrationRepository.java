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
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.mongodb.management.internal.integration.IntegrationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.IntegrationMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoIntegrationRepository implements IntegrationRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private IntegrationMongoRepository internalRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Integration> findById(String s) throws TechnicalException {
        logger.debug("Find integration by id [{}]", s);
        Integration integration = map(internalRepository.findById(s).orElse(null));
        logger.debug("Find integration by id [{}] - DONE", s);
        return Optional.ofNullable(integration);
    }

    @Override
    public Integration create(Integration integration) throws TechnicalException {
        logger.debug("Create integration [{}]", integration.getId());
        Integration createdIntegration = map(internalRepository.insert(map(integration)));
        logger.debug("Create integration [{}] - Done", createdIntegration.getId());
        return createdIntegration;
    }

    @Override
    public Integration update(Integration integration) throws TechnicalException {
        if (integration == null) {
            throw new IllegalStateException("Integration must not be null");
        }

        final IntegrationMongo integrationMongo = internalRepository.findById(integration.getId()).orElse(null);
        if (integrationMongo == null) {
            throw new IllegalStateException(String.format("No integration found with id [%s]", integration.getId()));
        }

        logger.debug("Update integration [{}]", integration.getId());
        Integration updatedIntegration = map(internalRepository.save(map(integration)));
        logger.debug("Update integration [{}] - Done", updatedIntegration.getId());
        return updatedIntegration;
    }

    @Override
    public void delete(String id) throws TechnicalException {
        logger.debug("Delete integration [{}]", id);
        internalRepository.deleteById(id);
        logger.debug("Delete integration [{}] - Done", id);
    }

    @Override
    public Set<Integration> findAll() throws TechnicalException {
        return internalRepository.findAll().stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public Set<Integration> findAllByEnvironment(String environmentId) throws TechnicalException {
        return internalRepository.findByEnvironmentId(environmentId).stream().map(this::map).collect(Collectors.toSet());
    }

    @Override
    public Optional<Integration> findByEnvironmentAndRemoteId(final String environmentId, final String remoteId) throws TechnicalException {
        return internalRepository.findByEnvironmentIdAndRemoteId(environmentId, remoteId).map(this::map);
    }

    private Integration map(IntegrationMongo integrationMongo) {
        return mapper.map(integrationMongo);
    }

    private IntegrationMongo map(Integration integration) {
        return mapper.map(integration);
    }
}
