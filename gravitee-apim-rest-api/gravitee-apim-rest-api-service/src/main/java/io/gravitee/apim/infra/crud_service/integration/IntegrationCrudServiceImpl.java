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
package io.gravitee.apim.infra.crud_service.integration;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.exception.IntegrationNotFoundException;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.integration.api.model.Integration;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class IntegrationCrudServiceImpl implements IntegrationCrudService {

    private static final Logger logger = LoggerFactory.getLogger(IntegrationCrudServiceImpl.class);

    private final IntegrationRepository integrationRepository;

    public IntegrationCrudServiceImpl(@Lazy IntegrationRepository integrationRepository) {
        this.integrationRepository = integrationRepository;
    }

    @Override
    public Integration createIntegration(Integration integration) {
        try {
            var createdIntegration = integrationRepository.create(IntegrationAdapter.INSTANCE.toRepository(integration));
            return IntegrationAdapter.INSTANCE.toEntity(createdIntegration);
        } catch (TechnicalException e) {
            logger.error("An error occurred while creating {}", integration, e);
            throw new TechnicalDomainException("Error when creating Integration", e);
        }
    }

    @Override
    public Integration get(String id) {
        try {
            var foundIntegration = integrationRepository.findById(id);
            if (foundIntegration.isPresent()) {
                return IntegrationAdapter.INSTANCE.toEntity(foundIntegration.get());
            }
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding Integration by id {}", id, e);
        }
        throw new IntegrationNotFoundException(id);
    }

    @Override
    public Set<Integration> findAll() {
        try {
            var integrations = integrationRepository.findAll();
            if (integrations != null) {
                return integrations.stream().map(IntegrationAdapter.INSTANCE::toEntity).collect(Collectors.toSet());
            }
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding all Integrations", e);
        }
        return Collections.emptySet();
    }

    @Override
    public Set<Integration> findByEnvironment(String environmentId) {
        try {
            var integrations = integrationRepository.findAllByEnvironment(environmentId);
            if (integrations != null) {
                return integrations.stream().map(IntegrationAdapter.INSTANCE::toEntity).collect(Collectors.toSet());
            }
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding Integrations by environment", e);
        }
        return Collections.emptySet();
    }

    @Override
    public Integration deleteIntegration(String id) {
        try {
            var foundIntegration = integrationRepository.findById(id);
            if (foundIntegration.isPresent()) {
                integrationRepository.delete(id);
                return IntegrationAdapter.INSTANCE.toEntity(foundIntegration.get());
            }
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding Integration by id {}", id, e);
        }
        throw new IntegrationNotFoundException(id);
    }
}
