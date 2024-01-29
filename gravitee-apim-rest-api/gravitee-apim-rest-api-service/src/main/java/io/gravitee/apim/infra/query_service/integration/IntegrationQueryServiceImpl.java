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

import io.gravitee.apim.core.integration.model.IntegrationEntity;
import io.gravitee.apim.core.integration.query_service.IntegrationQueryService;
import io.gravitee.apim.infra.adapter.IntegrationAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Service
public class IntegrationQueryServiceImpl implements IntegrationQueryService {

    private final IntegrationRepository integrationRepository;

    public IntegrationQueryServiceImpl(@Lazy final IntegrationRepository integrationRepository) {
        this.integrationRepository = integrationRepository;
    }

    @Override
    public Optional<IntegrationEntity> findByEnvironmentIdAndRemoteId(final String environmentId, final String remoteId) {
        try {
            return integrationRepository.findByEnvironmentAndRemoteId(environmentId, remoteId).map(IntegrationAdapter.INSTANCE::toEntity);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }
}
