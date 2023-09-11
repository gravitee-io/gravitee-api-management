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
package io.gravitee.apim.infra.crud_service.plan;

import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlanCrudServiceImpl implements PlanCrudService {

    private final PlanRepository planRepository;
    private final ApiRepository apiRepository;

    public PlanCrudServiceImpl(@Lazy PlanRepository planRepository, @Lazy ApiRepository apiRepository) {
        this.planRepository = planRepository;
        this.apiRepository = apiRepository;
    }

    @Override
    public GenericPlanEntity findById(String planId) {
        try {
            log.debug("Find plan by id : {}", planId);
            return planRepository.findById(planId).map(this::mapToGeneric).orElseThrow(() -> new PlanNotFoundException(planId));
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(String.format("An error occurs while trying to find a plan by id: %s", planId), ex);
        }
    }

    private GenericPlanEntity mapToGeneric(final Plan plan) {
        try {
            Optional<Api> apiOptional = apiRepository.findById(plan.getApi());
            final Api api = apiOptional.orElseThrow(() -> new ApiNotFoundException(plan.getApi()));
            return api.getDefinitionVersion() == DefinitionVersion.V4
                ? PlanAdapter.INSTANCE.toEntityV4(plan)
                : PlanAdapter.INSTANCE.toEntityV2(plan);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + plan.getApi(), e);
        }
    }
}
