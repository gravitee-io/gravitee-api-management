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
package io.gravitee.apim.infra.query_service.application;

import io.gravitee.apim.core.application.query_service.ApplicationQueryService;
import io.gravitee.apim.infra.adapter.ApplicationAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ApplicationQueryServiceImpl extends AbstractService implements ApplicationQueryService {

    private final ApplicationRepository applicationRepository;

    public ApplicationQueryServiceImpl(@Lazy ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Override
    public Set<BaseApplicationEntity> findByEnvironment(String environmentId) {
        try {
            return applicationRepository
                .findAllByEnvironment(environmentId, ApplicationStatus.values())
                .stream()
                .map(ApplicationAdapter.INSTANCE::toEntity)
                .collect(Collectors.toSet());
        } catch (TechnicalException e) {
            log.error("An error occurred while finding applications by environment", e);
            throw new TechnicalManagementException("An error occurred while finding applications by environment id: " + environmentId, e);
        }
    }
}
