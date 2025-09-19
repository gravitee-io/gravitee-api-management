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
package io.gravitee.apim.infra.query_service.plan;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class PlanQueryServiceImpl implements PlanQueryService {

    private final PlanRepository planRepository;
    private static final Logger logger = LoggerFactory.getLogger(PlanQueryServiceImpl.class);

    public PlanQueryServiceImpl(@Lazy final PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public List<Plan> findAllByApiIdAndGeneralConditionsAndIsActive(String apiId, DefinitionVersion definitionVersion, String pageId) {
        try {
            return planRepository
                .findByApi(apiId)
                .stream()
                .filter(
                    plan ->
                        Objects.equals(plan.getGeneralConditions(), pageId) &&
                        !(io.gravitee.repository.management.model.Plan.Status.CLOSED == plan.getStatus() ||
                            io.gravitee.repository.management.model.Plan.Status.STAGING == plan.getStatus())
                )
                .map(PlanAdapter.INSTANCE::fromRepository)
                .collect(Collectors.toList());
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding plans by API ID {}", apiId, e);
            throw new TechnicalDomainException("An error occurred while trying to find plans by API ID: " + apiId, e);
        }
    }

    @Override
    public List<Plan> findAllByApiId(String apiId) {
        try {
            return planRepository.findByApi(apiId).stream().map(PlanAdapter.INSTANCE::fromRepository).collect(Collectors.toList());
        } catch (TechnicalException e) {
            logger.error("An error occurred while finding plans by API ID {}", apiId, e);
            throw new TechnicalDomainException("An error occurred while trying to find plans by API ID: " + apiId, e);
        }
    }
}
