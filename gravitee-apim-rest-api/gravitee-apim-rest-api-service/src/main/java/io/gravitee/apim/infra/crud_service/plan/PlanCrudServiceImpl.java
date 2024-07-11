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

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlanCrudServiceImpl implements PlanCrudService {

    private final PlanRepository planRepository;

    public PlanCrudServiceImpl(@Lazy PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public Plan getById(String planId) {
        try {
            log.debug("Get plan by id : {}", planId);
            return planRepository
                .findById(planId)
                .map(PlanAdapter.INSTANCE::fromRepository)
                .orElseThrow(() -> new PlanNotFoundException(planId));
        } catch (TechnicalException ex) {
            throw new TechnicalDomainException(String.format("An error occurs while trying to get a plan by id: %s", planId), ex);
        }
    }

    @Override
    public Optional<Plan> findById(String planId) {
        try {
            log.debug("Find a plan by id : {}", planId);
            return planRepository.findById(planId).map(PlanAdapter.INSTANCE::fromRepository);
        } catch (TechnicalException ex) {
            throw new TechnicalDomainException(String.format("An error occurs while trying to find a plan by id: %s", planId), ex);
        }
    }

    @Override
    public Plan create(Plan plan) {
        try {
            var result = planRepository.create(PlanAdapter.INSTANCE.toRepository(plan));
            return PlanAdapter.INSTANCE.fromRepository(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to create the plan: " + plan.getId(), e);
        }
    }

    @Override
    public Plan update(io.gravitee.apim.core.plan.model.Plan plan) {
        try {
            var result = planRepository.update(PlanAdapter.INSTANCE.toRepository(plan));
            return PlanAdapter.INSTANCE.fromRepository(result);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to update the plan: " + plan.getId(), e);
        }
    }

    @Override
    public void delete(String planId) {
        try {
            planRepository.delete(planId);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurs while trying to delete the plan: " + planId, e);
        }
    }
}
