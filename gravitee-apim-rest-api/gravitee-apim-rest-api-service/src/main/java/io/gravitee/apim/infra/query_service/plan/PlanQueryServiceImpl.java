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
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
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
            return findAllByReferenceIdAndReferenceType(apiId, PlanReferenceType.API.name())
                .stream()
                .filter(
                    plan ->
                        Objects.equals(plan.getGeneralConditions(), pageId) &&
                        plan.getPlanStatus() != PlanStatus.CLOSED &&
                        plan.getPlanStatus() != PlanStatus.STAGING
                )
                .collect(Collectors.toList());
        } catch (TechnicalDomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while finding plans by API ID {}", apiId, e);
            throw new TechnicalDomainException("An error occurred while trying to find plans by API ID: " + apiId, e);
        }
    }

    @Override
    public List<Plan> findAllByReferenceIdAndReferenceType(String referenceId, String referenceType) {
        try {
            PlanReferenceType refType = PlanReferenceType.valueOf(referenceType);
            return planRepository
                .findByReferenceIdAndReferenceType(referenceId, refType)
                .stream()
                .map(PlanAdapter.INSTANCE::fromRepository)
                .collect(Collectors.toList());
        } catch (TechnicalException e) {
            log.error("An error occurred while finding plans by reference {} {}", referenceId, referenceType, e);
            throw new TechnicalDomainException(
                "An error occurred while trying to find plans by reference: " + referenceId + ", " + referenceType,
                e
            );
        }
    }

    @Override
    public List<Plan> findAllByApiId(String apiId) {
        try {
            return planRepository.findByApi(apiId).stream().map(PlanAdapter.INSTANCE::fromRepository).collect(Collectors.toList());
        } catch (TechnicalException e) {
            log.error("An error occurred while finding plans by API ID {}", apiId, e);
            throw new TechnicalDomainException("An error occurred while trying to find plans by API ID: " + apiId, e);
    }

    @Override
    public List<Plan> findAllByApiIds(Set<String> apiIds, Set<String> environmentIds) {
        if (CollectionUtils.isEmpty(apiIds) || CollectionUtils.isEmpty(environmentIds)) {
            return List.of();
        }
        try {
            return planRepository
                .findByReferenceIdsAndReferenceTypeAndEnvironment(
                    new ArrayList<>(apiIds),
                    io.gravitee.repository.management.model.Plan.PlanReferenceType.API,
                    environmentIds
                )
                .stream()
                .map(PlanAdapter.INSTANCE::fromRepository)
                .collect(Collectors.toList());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while finding plans for API IDs: " + apiIds, e);
        }
    }

    @Override
    public List<Plan> findAllForApiProduct(String referenceId) {
        try {
            return planRepository
                .findByReferenceIdAndReferenceType(referenceId, io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT)
                .stream()
                .map(PlanAdapter.INSTANCE::fromRepository)
                .collect(Collectors.toList());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while trying to find plans by API Product ID: " + referenceId, e);
        }
    }

    @Override
        }
        try {
            return planRepository
                .findByReferenceIdsAndReferenceTypeAndEnvironment(
                    new ArrayList<>(apiProductIds),
                    io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT,
                    environmentIds
                )
                .stream()
                .map(PlanAdapter.INSTANCE::fromRepository)
                .collect(Collectors.toList());
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("An error occurred while finding plans for API Product IDs: " + apiProductIds, e);
        }
    }
}
