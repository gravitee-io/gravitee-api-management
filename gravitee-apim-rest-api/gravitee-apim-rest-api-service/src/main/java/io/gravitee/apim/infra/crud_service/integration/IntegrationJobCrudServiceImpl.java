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

import io.gravitee.apim.core.integration.crud_service.IntegrationJobCrudService;
import io.gravitee.apim.core.integration.model.IntegrationJob;
import io.gravitee.apim.infra.adapter.IntegrationJobAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationJobRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class IntegrationJobCrudServiceImpl implements IntegrationJobCrudService {

    private final IntegrationJobRepository integrationRepository;

    public IntegrationJobCrudServiceImpl(@Lazy IntegrationJobRepository integrationJobRepository) {
        this.integrationRepository = integrationJobRepository;
    }

    @Override
    public IntegrationJob create(IntegrationJob job) {
        try {
            var created = integrationRepository.create(IntegrationJobAdapter.INSTANCE.toRepository(job));
            return IntegrationJobAdapter.INSTANCE.toEntity(created);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when creating IntegrationJob for integration: " + job.getSourceId(), e);
        }
    }

    @Override
    public Optional<IntegrationJob> findById(String id) {
        try {
            return integrationRepository.findById(id).map(IntegrationJobAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find the IntegrationJob: " + id, e);
        }
    }

    @Override
    public IntegrationJob update(IntegrationJob job) {
        try {
            var updated = integrationRepository.update(IntegrationJobAdapter.INSTANCE.toRepository(job));
            return IntegrationJobAdapter.INSTANCE.toEntity(updated);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurred when updating IntegrationJob: " + job.getId(), e);
        }
    }

    @Override
    public void delete(String id) {
        try {
            integrationRepository.delete(id);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("Error when deleting IntegrationJob: " + id, e);
        }
    }
}
