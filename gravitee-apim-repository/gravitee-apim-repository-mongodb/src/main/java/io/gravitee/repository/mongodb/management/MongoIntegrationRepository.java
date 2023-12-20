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
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoIntegrationRepository implements IntegrationRepository {

    private final Logger LOGGER = LoggerFactory.getLogger(MongoIntegrationRepository.class);

    @Autowired
    private IntegrationMongoRepository integrationMongoRepository;

    @Autowired
    private GraviteeMapper mapper;

    @Override
    public Optional<Integration> findById(String id) throws TechnicalException {
        LOGGER.debug("Find integration by ID [{}]", id);

        IntegrationMongo integration = integrationMongoRepository.findById(id).orElse(null);

        LOGGER.debug("Find integration by ID [{}] - Done", id);
        return Optional.ofNullable(map(integration));
    }

    @Override
    public Integration create(Integration integration) throws TechnicalException {
        LOGGER.debug("Create integration [{}]", integration.getName());

        IntegrationMongo integrationMongo = map(integration);
        IntegrationMongo createdIntegrationMongo = integrationMongoRepository.insert(integrationMongo);

        LOGGER.debug("Create integration [{}] - Done", integration.getName());

        return map(createdIntegrationMongo);
    }

    @Override
    public Integration update(Integration integration) throws TechnicalException {
        if (integration == null) {
            throw new IllegalStateException("Integration must not be null");
        }

        IntegrationMongo integrationMongo = integrationMongoRepository.findById(integration.getId()).orElse(null);
        if (integrationMongo == null) {
            throw new IllegalStateException(String.format("No integration found with id [%s]", integration.getId()));
        }

        try {
            integrationMongo.setName(integration.getName());
            integrationMongo.setDescription(integration.getDescription());
            integrationMongo.setUpdatedAt(integration.getUpdatedAt());
            integrationMongo.setConfiguration(integration.getConfiguration());

            IntegrationMongo integrationMongoUpdated = integrationMongoRepository.save(integrationMongo);
            return map(integrationMongoUpdated);
        } catch (Exception e) {
            LOGGER.error("An error occurs when updating integration", e);
            throw new TechnicalException("An error occurs when updating integration");
        }
    }

    @Override
    public void delete(String id) throws TechnicalException {
        try {
            integrationMongoRepository.deleteById(id);
        } catch (Exception e) {
            LOGGER.error("An error occurs when deleting integration [{}]", id, e);
            throw new TechnicalException("An error occurs when deleting integration");
        }
    }

    @Override
    public Set<Integration> findAll() throws TechnicalException {
        LOGGER.debug("Find all integrations");

        Set<Integration> res = integrationMongoRepository.findAll().stream().map(this::map).collect(Collectors.toSet());

        LOGGER.debug("Find all integrations - Done");
        return res;
    }

    private IntegrationMongo map(Integration integration) {
        if (integration == null) {
            return null;
        }

        return mapper.map(integration);
    }

    private Integration map(IntegrationMongo integrationMongo) {
        if (integrationMongo == null) {
            return null;
        }

        return mapper.map(integrationMongo);
    }

    @Override
    public Set<Integration> findAllByEnvironmentId(String environmentId) throws TechnicalException {
        LOGGER.debug("Find all integrations by environment");

        Set<Integration> res = integrationMongoRepository
            .findByEnvironmentId(environmentId)
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());

        LOGGER.debug("Find all integrations by environment - Done");
        return res;
    }
}
