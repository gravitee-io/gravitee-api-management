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
package io.gravitee.apim.infra.crud_service.environment;

import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.apim.core.environment.model.Environment;
import io.gravitee.apim.infra.adapter.EnvironmentAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EnvironmentCrudServiceImpl implements EnvironmentCrudService {

    private final EnvironmentRepository environmentRepository;

    public EnvironmentCrudServiceImpl(@Lazy final EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Override
    public Environment get(String environmentId) {
        try {
            log.debug("Find environment by id: {}", environmentId);

            return this.environmentRepository.findById(environmentId)
                .map(EnvironmentAdapter.INSTANCE::toModel)
                .orElseThrow(() -> new EnvironmentNotFoundException(environmentId));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                String.format("An error occurred while finding Environment with id %s", environmentId),
                e
            );
        }
    }
}
