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
package io.gravitee.rest.api.service.impl.integrations;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.utils.IdGenerator;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.model.Integration;
import io.gravitee.repository.management.model.IntegrationType;
import io.gravitee.rest.api.model.integrations.IntegrationEntity;
import io.gravitee.rest.api.model.integrations.NewIntegrationEntity;
import io.gravitee.rest.api.service.IntegrationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.event.IntegrationEvent;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class IntegrationServiceImpl extends AbstractService implements IntegrationService {

    private final Logger LOGGER = LoggerFactory.getLogger(IntegrationServiceImpl.class);

    @Autowired
    private EventManager eventManager;

    @Lazy
    @Autowired
    private IntegrationRepository integrationRepository;

    @Override
    public IntegrationEntity create(ExecutionContext executionContext, NewIntegrationEntity newIntegrationEntity) {
        try {
            LOGGER.debug("Create integration {}", newIntegrationEntity);

            Optional<Integration> optIntegration = integrationRepository.findById(IdGenerator.generate(newIntegrationEntity.getName()));
            if (optIntegration.isPresent()) {
                throw new IntegrationAlreadyExistsException(newIntegrationEntity.getName());
            }

            Integration integration = convert(newIntegrationEntity);
            integration.setEnvironmentId(executionContext.getEnvironmentId());

            // Set date fields
            integration.setCreatedAt(new Date());
            integration.setUpdatedAt(integration.getCreatedAt());

            Integration createdIntegration = integrationRepository.create(integration);

            IntegrationEntity integrationEntity = convert(createdIntegration);

            /*
            auditService.createOrganizationAuditLog(
                    executionContext,
                    executionContext.getOrganizationId(),
                    singletonMap(IDENTITY_PROVIDER, createdIdentityProvider.getId()),
                    IdentityProvider.AuditEvent.IDENTITY_PROVIDER_CREATED,
                    createdIdentityProvider.getUpdatedAt(),
                    null,
                    createdIdentityProvider
            );
             */

            eventManager.publishEvent(IntegrationEvent.CREATED, integrationEntity);

            return integrationEntity;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create integration {}", newIntegrationEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create " + newIntegrationEntity, ex);
        }
    }

    @Override
    public Set<IntegrationEntity> findAll(ExecutionContext executionContext) {
        try {
            return integrationRepository
                .findAllByEnvironmentId(executionContext.getEnvironmentId())
                .stream()
                .map(this::convert)
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to retrieve integrations", ex);
            throw new TechnicalManagementException("An error occurs while trying to retrieve integrations", ex);
        }
    }

    @Override
    public IntegrationEntity findById(String id) {
        try {
            LOGGER.debug("Find integration by ID: {}", id);

            Optional<Integration> integration = integrationRepository.findById(id);

            if (integration.isPresent()) {
                return convert(integration.get());
            }

            throw new IntegrationNotFoundException(id);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find an integration using its ID {}", id, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an integration using its ID " + id, ex);
        }
    }

    private IntegrationEntity convert(Integration integration) {
        IntegrationEntity integrationEntity = new IntegrationEntity();

        integrationEntity.setId(integration.getId());
        integrationEntity.setName(integration.getName());
        integrationEntity.setDescription(integration.getDescription());
        integrationEntity.setType(
            io.gravitee.rest.api.model.integrations.IntegrationType.valueOf(integration.getType().name().toUpperCase())
        );
        integrationEntity.setConfiguration(integration.getConfiguration());

        return integrationEntity;
    }

    private Integration convert(NewIntegrationEntity newIntegrationEntity) {
        Integration integration = new Integration();

        integration.setId(IdGenerator.generate(newIntegrationEntity.getName()));
        integration.setName(newIntegrationEntity.getName());
        integration.setDescription(newIntegrationEntity.getDescription());
        integration.setConfiguration(newIntegrationEntity.getConfiguration());
        integration.setType(IntegrationType.valueOf(newIntegrationEntity.getType().name().toUpperCase()));

        return integration;
    }
}
