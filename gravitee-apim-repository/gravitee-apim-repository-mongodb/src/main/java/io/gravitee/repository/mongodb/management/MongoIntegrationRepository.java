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

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.mongodb.management.internal.integration.IntegrationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.IntegrationMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@RequiredArgsConstructor
public class MongoIntegrationRepository implements IntegrationRepository {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final IntegrationMongoRepository internalRepository;
    private final GraviteeMapper mapper;

    @Override
    public Optional<Integration> findByIntegrationId(String s) {
        logger.debug("Find integration by id [{}]", s);
        Optional<Integration> result = internalRepository.findById(s).map(this::map);
        logger.debug("Find integration by id [{}] - DONE", s);
        return result;
    }

    @Override
    public Integration create(Integration integration) throws TechnicalException {
        logger.debug("Create integration [{}]", integration.getId());
        Integration createdIntegration = map(internalRepository.insert(map(integration)));
        if (createdIntegration.getWellKnownUrls() != null && createdIntegration.getWellKnownUrls().isEmpty()) {
            createdIntegration.setWellKnownUrls(null);
        }
        logger.debug("Create integration [{}] - Done", createdIntegration.getId());
        return createdIntegration;
    }

    @Override
    public Integration update(Integration integration) throws TechnicalException {
        if (integration == null) {
            throw new IllegalStateException("Integration must not be null");
        }

        return internalRepository
            .findById(integration.getId())
            .map(found -> {
                logger.debug("Update integration [{}]", integration.getId());
                Integration updatedIntegration = map(internalRepository.save(map(integration)));
                logger.debug("Update integration [{}] - Done", updatedIntegration.getId());
                return updatedIntegration;
            })
            .orElseThrow(() -> new IllegalStateException(String.format("No integration found with id [%s]", integration.getId())));
    }

    @Override
    public void delete(String id) throws TechnicalException {
        logger.debug("Delete integration [{}]", id);
        internalRepository.deleteById(id);
        logger.debug("Delete integration [{}] - Done", id);
    }

    @Override
    public Page<Integration> findAllByEnvironment(String environmentId, Pageable pageable) {
        logger.debug("Search by environment ID [{}]", environmentId);

        Page<Integration> integrations = internalRepository.findAllByEnvironmentId(environmentId, pageable).map(mapper::map);

        logger.debug("Search by environment ID [{}] - Done", environmentId);
        return integrations;
    }

    @Override
    public Page<Integration> findAllByEnvironmentAndGroups(
        String environmentId,
        Collection<String> integrationIds,
        Collection<String> groups,
        Pageable pageable
    ) {
        logger.debug("Search by environment ID [{}] and groups [{}]", environmentId, groups);

        Page<Integration> integrations = internalRepository
            .findAllByEnvironmentIdAndGroups(environmentId, pageable, integrationIds, groups)
            .map(mapper::map);

        logger.debug("Search by environment ID [{}] and groups [{}] - Done", environmentId, groups);
        return integrations;
    }

    @Override
    public List<String> deleteByEnvironmentId(String environmentId) throws TechnicalException {
        logger.debug("Delete integration by environmentId: {}", environmentId);
        try {
            List<String> all = internalRepository.deleteByEnvironmentId(environmentId).stream().map(IntegrationMongo::getId).toList();
            logger.debug("Delete integration by environment - Done {}", all);
            return all;
        } catch (Exception ex) {
            logger.error("Failed to delete integration by environmentId: {}", environmentId, ex);
            throw new TechnicalException("Failed to delete integration by environmentId");
        }
    }

    private Integration map(IntegrationMongo integrationMongo) {
        Integration integration = mapper.map(integrationMongo);
        if (integration.getWellKnownUrls() != null && integration.getWellKnownUrls().isEmpty()) {
            integration.setWellKnownUrls(null);
        }
        return integration;
    }

    private IntegrationMongo map(Integration integration) {
        return mapper.map(integration);
    }
}
