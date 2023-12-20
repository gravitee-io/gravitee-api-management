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
import io.gravitee.rest.api.model.integrations.IntegrationEntity;
import io.gravitee.rest.api.model.integrations.NewIntegrationEntity;
import io.gravitee.rest.api.service.IntegrationService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.event.IntegrationEvent;
import io.gravitee.rest.api.service.impl.AbstractService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Override
    public IntegrationEntity create(ExecutionContext executionContext, NewIntegrationEntity newIntegrationEntity) {
        //    try {
        LOGGER.debug("Create integration {}", newIntegrationEntity);

        String id = IdGenerator.generate(newIntegrationEntity.getName());

        IntegrationEntity integrationEntity = new IntegrationEntity();
        integrationEntity.setId(id);
        integrationEntity.setName(newIntegrationEntity.getName());
        integrationEntity.setDescription(newIntegrationEntity.getDescription());
        integrationEntity.setType(newIntegrationEntity.getType());
        integrationEntity.setConfiguration(newIntegrationEntity.getConfiguration());

        eventManager.publishEvent(IntegrationEvent.CREATED, integrationEntity);

        return integrationEntity;
        /*
            Optional<IdentityProvider> optIdentityProvider = identityProviderRepository.findById(
                    IdGenerator.generate(newIdentityProviderEntity.getName())
            );
            if (optIdentityProvider.isPresent()) {
                throw new IdentityProviderAlreadyExistsException(newIdentityProviderEntity.getName());
            }

            IdentityProvider identityProvider = convert(newIdentityProviderEntity);
            identityProvider.setOrganizationId(executionContext.getOrganizationId());

            // If provider is a social type, we must ensure required parameters
            if (identityProvider.getType() == IdentityProviderType.GOOGLE || identityProvider.getType() == IdentityProviderType.GITHUB) {
                checkSocialProvider(identityProvider);
            }

            // Set date fields
            identityProvider.setCreatedAt(new Date());
            identityProvider.setUpdatedAt(identityProvider.getCreatedAt());

            IdentityProvider createdIdentityProvider = identityProviderRepository.create(identityProvider);

            identityProviderActivationService.activateIdpOnTargets(
                    executionContext,
                    createdIdentityProvider.getId(),
                    new IdentityProviderActivationService.ActivationTarget(
                            executionContext.getOrganizationId(),
                            IdentityProviderActivationReferenceType.ORGANIZATION
                    )
            );

            auditService.createOrganizationAuditLog(
                    executionContext,
                    executionContext.getOrganizationId(),
                    singletonMap(IDENTITY_PROVIDER, createdIdentityProvider.getId()),
                    IdentityProvider.AuditEvent.IDENTITY_PROVIDER_CREATED,
                    createdIdentityProvider.getUpdatedAt(),
                    null,
                    createdIdentityProvider
            );

            return convert(createdIdentityProvider);
             */
        /*
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create integration {}", newIntegrationEntity, ex);
            throw new TechnicalManagementException("An error occurs while trying to create " + newIntegrationEntity, ex);
        }
         */
    }
}
