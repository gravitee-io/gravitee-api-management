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
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@CustomLog
@Service
public class PlanQueryServiceImpl implements PlanQueryService {

    private final PlanRepository planRepository;

    public PlanQueryServiceImpl(@Lazy final PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public List<Plan> findAllByApiIdAndGeneralConditionsAndIsActive(String apiId, DefinitionVersion definitionVersion, String pageId) {
        try {
            return planRepository
                .findByReferenceIdAndReferenceType(apiId, io.gravitee.repository.management.model.Plan.PlanReferenceType.API)
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
            log.error("An error occurred while finding plans by API ID {}", apiId, e);
            throw new TechnicalDomainException("An error occurred while trying to find plans by API ID: " + apiId, e);
        }
    }

    @Override
    public List<Plan> findAllByApiId(String apiId) {
        return findAllByReferenceIdAndReferenceType(apiId, GenericPlanEntity.ReferenceType.API);
    }

    @Override
    public List<Plan> findAllByApiIds(Set<String> apiIds, Set<String> environmentIds) {
        return findAllByReferenceIdsAndEnvironments(apiIds, environmentIds, GenericPlanEntity.ReferenceType.API);
    }

    @Override
    public List<Plan> findAllByReferenceIdAndReferenceType(String referenceId, GenericPlanEntity.ReferenceType referenceType) {
        try {
            return planRepository
                .findByReferenceIdAndReferenceType(
                    referenceId,
                    io.gravitee.repository.management.model.Plan.PlanReferenceType.valueOf(referenceType.name())
                )
                .stream()
                .map(PlanAdapter.INSTANCE::fromRepository)
                .collect(Collectors.toList());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to find plans by reference ID: " + referenceId, e);
        }
    }

    @Override
    public List<Plan> findAllByReferenceIdsAndEnvironments(
        Set<String> referenceIds,
        Set<String> environmentIds,
        GenericPlanEntity.ReferenceType referenceType
    ) {
        if (CollectionUtils.isEmpty(referenceIds) || CollectionUtils.isEmpty(environmentIds)) {
            return List.of();
        }
        try {
            return planRepository
                .findByReferenceIdsAndReferenceTypeAndEnvironment(
                    new ArrayList<>(referenceIds),
                    io.gravitee.repository.management.model.Plan.PlanReferenceType.valueOf(referenceType.name()),
                    environmentIds
                )
                .stream()
                .map(PlanAdapter.INSTANCE::fromRepository)
                .collect(Collectors.toList());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                "An error occurred while finding plans for reference IDs: " + referenceIds + " (" + referenceType + ")",
                e
            );
        }
    }
}
