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

import io.gravitee.apim.core.integration.crud_service.IntegrationCrudService;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Remi Baptiste (remi.baptiste at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
public class IntegrationCrudServiceImpl extends AbstractService implements IntegrationCrudService {

    private final IntegrationRepository integrationRepository;

    public IntegrationCrudServiceImpl(@Lazy IntegrationRepository integrationRepository) {
        this.integrationRepository = integrationRepository;
    }

    @Override
    public <T extends Integration> T create(T integration) {
        try {
            var specificAdapter = IntegrationAdapter.INSTANCE.specific(integration);
            var createdIntegration = integrationRepository.create(specificAdapter.toRepository(integration));
            return specificAdapter.toEntity(createdIntegration);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating Integration: " + integration.name(), e);
        }
    }

    @Override
    public Optional<Integration.ApiIntegration> findApiIntegrationById(String id) {
        return findById(id, IntegrationAdapter.SPECIFIC_API_INTEGRATION_ADAPTER::toEntity);
    }

    @Override
    public Optional<Integration.A2aIntegration> findA2aIntegrationById(String id) {
        return findById(id, IntegrationAdapter.SPECIFIC_A2A_INTEGRATION_ADAPTER::toEntity);
    }

    @Override
    public Optional<Integration> findById(String id) {
        return findById(id, IntegrationAdapter.INSTANCE::toEntity);
    }

    public <T extends Integration> Optional<T> findById(
        String id,
        Function<io.gravitee.repository.management.model.Integration, T> mapper
    ) {
        try {
            return integrationRepository.findByIntegrationId(id).map(mapper);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find the integration: " + id, e);
        }
    }

    @Override
    public <T extends Integration> T update(T integration) {
        try {
            var specificAdapter = IntegrationAdapter.INSTANCE.specific(integration);
            var updatedIntegration = integrationRepository.update(specificAdapter.toRepository(integration));
            return specificAdapter.toEntity(updatedIntegration);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurred when updating integration: " + integration.id(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            integrationRepository.delete(id);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when deleting Integration: " + id, e);
        }
    }
}
