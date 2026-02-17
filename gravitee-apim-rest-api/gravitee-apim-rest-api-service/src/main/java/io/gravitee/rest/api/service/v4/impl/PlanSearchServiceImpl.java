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
package io.gravitee.rest.api.service.v4.impl;

import static java.util.Collections.emptyList;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiProductsRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiProduct;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanQuery;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.TransactionalService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericPlanMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component("PlanSearchServiceImplV4")
public class PlanSearchServiceImpl extends TransactionalService implements PlanSearchService {

    private final PlanRepository planRepository;
    private final ApiRepository apiRepository;
    private final ApiProductsRepository apiProductsRepository;
    private final GroupService groupService;
    private final ApiSearchService apiSearchService;
    private final ObjectMapper objectMapper;
    private final GenericPlanMapper genericPlanMapper;

    private final GenericApiMapper genericApiMapper;

    public PlanSearchServiceImpl(
        @Lazy final PlanRepository planRepository,
        @Lazy final ApiRepository apiRepository,
        @Lazy final ApiProductsRepository apiProductsRepository,
        @Lazy final GroupService groupService,
        @Lazy final ApiSearchService apiSearchService,
        final ObjectMapper objectMapper,
        final GenericPlanMapper genericPlanMapper,
        GenericApiMapper genericApiMapper
    ) {
        this.planRepository = planRepository;
        this.apiRepository = apiRepository;
        this.apiProductsRepository = apiProductsRepository;
        this.groupService = groupService;
        this.apiSearchService = apiSearchService;
        this.objectMapper = objectMapper;
        this.genericPlanMapper = genericPlanMapper;
        this.genericApiMapper = genericApiMapper;
    }

    @Override
    public GenericPlanEntity findById(final ExecutionContext executionContext, final String plan) {
        try {
            log.debug("Find plan by id : {}", plan);
            return planRepository
                .findById(plan)
                .map(this::mapToGeneric)
                .orElseThrow(() -> new PlanNotFoundException(plan));
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(String.format("An error occurs while trying to find a plan by id: %s", plan), ex);
        }
    }

    @Override
    public Set<GenericPlanEntity> findByIdIn(final ExecutionContext executionContext, final Set<String> ids) {
        try {
            return planRepository.findByIdIn(ids).stream().map(this::mapToGeneric).collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error has occurred retrieving plans by ids", e);
        }
    }

    @Override
    public Set<GenericPlanEntity> findByApi(final ExecutionContext executionContext, final String apiId, boolean withFlow) {
        try {
            log.debug("Find plan by api : {}", apiId);
            Optional<Api> apiOptional = apiRepository.findById(apiId);
            if (apiOptional.isPresent()) {
                final Api api = apiOptional.get();
                final var plans = planRepository.findByReferenceIdAndReferenceType(apiId, Plan.PlanReferenceType.API);
                if (plans == null || plans.isEmpty()) {
                    return Set.of();
                }

                return withFlow
                    ? genericPlanMapper.toGenericPlansWithFlow(api, plans)
                    : genericPlanMapper.toGenericPlansWithoutFlow(api, plans);
            } else {
                return Set.of();
            }
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(String.format("An error occurs while trying to find a plan by api: %s", apiId), ex);
        }
    }

    @Override
    public Set<GenericPlanEntity> findByApi(final ExecutionContext executionContext, final GenericApiEntity genericApi, boolean withFlow) {
        try {
            log.debug("Find plan by api : {}", genericApi.getId());
            return planRepository
                .findByReferenceIdAndReferenceType(genericApi.getId(), Plan.PlanReferenceType.API)
                .stream()
                .map(plan ->
                    withFlow
                        ? genericPlanMapper.toGenericPlanWithFlow(genericApi, plan)
                        : genericPlanMapper.toGenericPlanWithoutFlow(genericApi, plan)
                )
                .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to find a plan by api: %s", genericApi.getId()),
                ex
            );
        }
    }

    @Override
    public List<GenericPlanEntity> search(
        final ExecutionContext executionContext,
        final PlanQuery query,
        String user,
        boolean isAdmin,
        boolean withFlow
    ) {
        final String actualApiId = query.getReferenceType() == GenericPlanEntity.ReferenceType.API ? query.getReferenceId() : null;

        if (actualApiId == null) {
            return emptyList();
        }

        final var actualReferenceId = query.getReferenceId() != null ? query.getReferenceId() : actualApiId;

        try {
            Optional<Api> apiOptional = apiRepository.findById(actualApiId);
            final Api api = apiOptional.orElseThrow(() -> new ApiNotFoundException(actualApiId));
            final GenericApiEntity genericApiEntity = genericApiMapper.toGenericApi(executionContext, api, null, false, false, false);

            return planRepository
                .findByReferenceIdAndReferenceType(actualReferenceId, Plan.PlanReferenceType.API)
                .stream()
                .map(plan ->
                    withFlow ? genericPlanMapper.toGenericPlanWithFlow(api, plan) : genericPlanMapper.toGenericPlanWithoutFlow(api, plan)
                )
                .filter(p -> {
                    boolean filtered = true;
                    if (query.getName() != null) {
                        filtered = query.getName().equals(p.getName());
                    }
                    if (filtered && !CollectionUtils.isEmpty(query.getSecurityType())) {
                        if (p.getPlanSecurity() == null || p.getPlanSecurity().getType() == null) {
                            return false;
                        }
                        PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(p.getPlanSecurity().getType());
                        filtered = query.getSecurityType().contains(planSecurityType);
                    }
                    if (filtered && !CollectionUtils.isEmpty(query.getStatus())) {
                        PlanStatus planStatus = PlanStatus.valueOfLabel(p.getPlanStatus().getLabel());
                        filtered = query.getStatus().contains(planStatus);
                    }
                    if (filtered && query.getMode() != null) {
                        filtered = query.getMode().equals(p.getPlanMode());
                    }
                    return filtered;
                })
                .filter(plan -> isAdmin || groupService.isUserAuthorizedToAccessApiData(genericApiEntity, plan.getExcludedGroups(), user))
                .collect(Collectors.toList());
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(String.format("An error occurs while trying search plans by api: %s", actualApiId), ex);
        }
    }

    @Override
    public boolean anyPlanMismatchWithApi(final List<String> planIds, final String apiId) {
        try {
            return planRepository
                .findByIdIn(planIds)
                .stream()
                .filter(plan -> plan.getReferenceType() == Plan.PlanReferenceType.API)
                .map(Plan::getReferenceId)
                .filter(Objects::nonNull)
                .anyMatch(id -> !id.equals(apiId));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error has occurred checking plans ownership", e);
        }
    }

    @Override
    public Map<String, Object> findByIdAsMap(final String id) throws TechnicalException {
        Plan plan = planRepository.findById(id).orElseThrow(() -> new PlanNotFoundException(id));
        return objectMapper.convertValue(plan, Map.class);
    }

    @Override
    public boolean exists(String planId) {
        try {
            return planRepository.exists(planId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to check if a plan exists", e);
        }
    }

    private GenericPlanEntity mapToGeneric(final Plan plan) {
        if (plan.getReferenceType() == Plan.PlanReferenceType.API_PRODUCT) {
            return mapToGenericForApiProduct(plan);
        }
        String apiId = plan.getReferenceType() == Plan.PlanReferenceType.API && plan.getReferenceId() != null
            ? plan.getReferenceId()
            : null;
        try {
            Optional<Api> apiOptional = apiRepository.findById(apiId);
            final Api api = apiOptional.orElseThrow(() -> new ApiNotFoundException(apiId));
            return genericPlanMapper.toGenericPlanWithFlow(api, plan);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, e);
        }
    }

    @Override
    public GenericPlanEntity findByPlanIdIdForApiProduct(
        final ExecutionContext executionContext,
        final String plan,
        final String apiProductId
    ) {
        try {
            log.debug("Find plan by id : {}", plan);
            return planRepository
                .findByIdAndReferenceIdAndReferenceType(plan, apiProductId, Plan.PlanReferenceType.API_PRODUCT)
                .map(this::mapToGenericForApiProduct)
                .orElseThrow(() -> new PlanNotFoundException(plan));
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(String.format("An error occurs while trying to find a plan by id: %s", plan), ex);
        }
    }

    @Override
    public Set<GenericPlanEntity> findByApiProduct(final ExecutionContext executionContext, final String apiProductId) {
        try {
            log.debug("Find plan by api product : {}", apiProductId);
            Optional<ApiProduct> apiOptional = apiProductsRepository.findById(apiProductId);
            if (apiOptional.isPresent()) {
                return planRepository
                    .findByReferenceIdAndReferenceType(apiProductId, Plan.PlanReferenceType.API_PRODUCT)
                    .stream()
                    .map(plan -> genericPlanMapper.toGenericApiProductPlan(plan))
                    .collect(Collectors.toSet());
            } else {
                return Set.of();
            }
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to find a plan by api product: %s", apiProductId),
                ex
            );
        }
    }

    @Override
    public List<GenericPlanEntity> searchForApiProductPlans(
        final ExecutionContext executionContext,
        final PlanQuery query,
        String user,
        boolean isAdmin
    ) {
        return findByApiProduct(executionContext, query.getReferenceId())
            .stream()
            .filter(p -> {
                boolean filtered = true;
                if (query.getName() != null) {
                    filtered = query.getName().equals(p.getName());
                }
                if (filtered && !CollectionUtils.isEmpty(query.getSecurityType())) {
                    if (p.getPlanSecurity() == null || p.getPlanSecurity().getType() == null) {
                        return false;
                    }
                    PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(p.getPlanSecurity().getType());
                    filtered = query.getSecurityType().contains(planSecurityType);
                }
                if (filtered && !CollectionUtils.isEmpty(query.getStatus())) {
                    PlanStatus planStatus = PlanStatus.valueOfLabel(p.getPlanStatus().getLabel());
                    filtered = query.getStatus().contains(planStatus);
                }
                if (filtered && query.getMode() != null) {
                    filtered = query.getMode().equals(p.getPlanMode());
                }
                return filtered;
            })
            .collect(Collectors.toList());
    }

    private GenericPlanEntity mapToGenericForApiProduct(final Plan plan) {
        return genericPlanMapper.toGenericApiProductPlan(plan);
    }
}
