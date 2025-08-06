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
package io.gravitee.apim.infra.crud_service.application;

import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.infra.adapter.ApplicationAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.Application;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApplicationNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ApplicationCrudServiceImpl implements ApplicationCrudService {

    private final ApplicationRepository applicationRepository;

    public ApplicationCrudServiceImpl(@Lazy ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    public BaseApplicationEntity findById(final ExecutionContext executionContext, String applicationId) {
        return findById(applicationId, executionContext.getEnvironmentId());
    }

    @Override
    public BaseApplicationEntity findById(String applicationId, String environmentId) {
        try {
            log.debug("Find application by id: {}", applicationId);

            Optional<Application> applicationOptional = applicationRepository.findById(applicationId);

            if (environmentId != null) {
                applicationOptional = applicationOptional.filter(result -> result.getEnvironmentId().equals(environmentId));
            }

            return applicationOptional
                .map(ApplicationAdapter.INSTANCE::toEntity)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find an application using its ID {}", applicationId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an application using its ID " + applicationId, ex);
        }
    }

    @Override
    public List<BaseApplicationEntity> findByIds(List<String> appIds, String environmentId) {
        log.debug("Find all applications by ids : {}", appIds);

        try {
            return applicationRepository
                .findByIds(appIds)
                .stream()
                .filter(app -> app.getEnvironmentId().equals(environmentId))
                .map(ApplicationAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find all applications using its IDs", e);
        }
    }
}
