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
package io.gravitee.apim.infra.query_service.integration;

import io.gravitee.apim.core.integration.model.IntegrationJob;
import io.gravitee.apim.core.integration.query_service.IntegrationJobQueryService;
import io.gravitee.apim.infra.adapter.IntegrationJobAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationJobRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IntegrationJobQueryServiceImpl extends AbstractService implements IntegrationJobQueryService {

    private final IntegrationJobRepository integrationJobRepository;

    public IntegrationJobQueryServiceImpl(@Lazy IntegrationJobRepository integrationJobRepository) {
        this.integrationJobRepository = integrationJobRepository;
    }

    @Override
    public Optional<IntegrationJob> findPendingJobFor(String integrationId) {
        try {
            return integrationJobRepository.findPendingJobFor(integrationId).map(IntegrationJobAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurred while finding pending IntegrationJob for: " + integrationId, e);
        }
    }
}
