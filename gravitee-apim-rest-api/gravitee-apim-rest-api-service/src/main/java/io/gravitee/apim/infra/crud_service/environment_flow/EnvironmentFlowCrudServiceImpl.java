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
package io.gravitee.apim.infra.crud_service.environment_flow;

import io.gravitee.apim.core.environment_flow.crud_service.EnvironmentFlowCrudService;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.definition.model.v4.environmentflow.EnvironmentFlow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentFlowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EnvironmentFlowCrudServiceImpl implements EnvironmentFlowCrudService {

    private final EnvironmentFlowRepository environmentFlowRepository;

    public EnvironmentFlowCrudServiceImpl(@Lazy final EnvironmentFlowRepository environmentFlowRepository) {
        this.environmentFlowRepository = environmentFlowRepository;
    }

    @Override
    public EnvironmentFlow create(EnvironmentFlow flow) {
        try {
            return environmentFlowRepository.create(flow);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to create the environment flow", e);
        }
    }
}
