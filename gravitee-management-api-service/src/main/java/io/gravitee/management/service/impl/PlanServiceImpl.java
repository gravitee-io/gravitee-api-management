/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.Path;
import io.gravitee.management.model.*;
import io.gravitee.management.service.PlanService;
import io.gravitee.management.service.exceptions.PlanNotFoundException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PlanServiceImpl extends TransactionalService implements PlanService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(PlanServiceImpl.class);

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PlanEntity findById(String plan) {
        try {
            LOGGER.debug("Find plan by id : {}", plan);

            Optional<Plan> optPlan = planRepository.findById(plan);

            if (! optPlan.isPresent()) {
                throw new PlanNotFoundException(plan);
            }

            return convert(optPlan.get());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a plan by id: {}", plan, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to find a plan by id: %s", plan), ex);
        }
    }

    @Override
    public Set<PlanEntity> findByApi(String api) {
        try {
            LOGGER.debug("Find plan by api : {}", api);

            Set<Plan> plans = planRepository.findByApi(api);

            return plans
                    .stream()
                    .map(this::convert)
                    .sorted((o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()))
                    .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to find a plan by api: {}", api, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to find a plan by api: %s", api), ex);
        }
    }

    @Override
    public PlanEntity create(NewPlanEntity newPlan) {
        try {
            LOGGER.debug("Create a new plan {} for API {}", newPlan.getName(), newPlan.getApi());
            Plan plan = new Plan();

            plan.setId(UUID.toString(UUID.random()));
            plan.setApis(Collections.singleton(newPlan.getApi()));
            plan.setName(newPlan.getName());
            plan.setDescription(newPlan.getDescription());
            plan.setCreatedAt(new Date());
            plan.setUpdatedAt(plan.getCreatedAt());
            plan.setType(Plan.PlanType.valueOf(newPlan.getType().name()));

            String planPolicies = objectMapper.writeValueAsString(newPlan.getPaths());
            plan.setDefinition(planPolicies);

            plan.setValidation(Plan.PlanValidationType.valueOf(newPlan.getValidation().name()));
            plan.setCharacteristics(newPlan.getCharacteristics());
            
            plan = planRepository.create(plan);
            return convert(plan);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to create a plan {} for API {}", newPlan.getName(), newPlan.getApi(), ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to create a plan %s for API %s", newPlan.getName(), newPlan.getApi()), ex);
        } catch (JsonProcessingException jse) {
            LOGGER.error("Unexpected error while generating plan definition", jse);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to create a plan %s for API %s", newPlan.getName(), newPlan.getApi()), jse);
        }
    }

    @Override
    public PlanEntity update(UpdatePlanEntity updatePlan) {
        try {
            LOGGER.debug("Update plan {}", updatePlan.getName());

            Optional<Plan> optPlan = planRepository.findById(updatePlan.getId());
            if (! optPlan.isPresent()) {
                throw new PlanNotFoundException(updatePlan.getId());
            }

            Plan plan = optPlan.get();
            plan.setName(updatePlan.getName());
            plan.setDescription(updatePlan.getDescription());
            plan.setUpdatedAt(new Date());

            String planPolicies = objectMapper.writeValueAsString(updatePlan.getPaths());
            plan.setDefinition(planPolicies);

            plan.setValidation(Plan.PlanValidationType.valueOf(updatePlan.getValidation().name()));
            plan.setCharacteristics(updatePlan.getCharacteristics());

            // if order change, reorder all pages
            if (plan.getOrder() != updatePlan.getOrder()) {
                plan.setOrder(updatePlan.getOrder());
                reorderAndSavePlans(plan);
                return null;
            } else {
                plan = planRepository.update(plan);
                return convert(plan);
            }

        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update plan {}", updatePlan.getName(), ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to update plan %s", updatePlan.getName()), ex);
        } catch (JsonProcessingException jse) {
            LOGGER.error("Unexpected error while generating plan definition", jse);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to update a plan %s", updatePlan.getName()), jse);
        }
    }

    private void reorderAndSavePlans(final Plan planToReorder) throws TechnicalException {
        final Collection<Plan> plans = planRepository.findByApi(planToReorder.getApis().iterator().next());
        final List<Boolean> increment = asList(true);
        plans.stream()
                .sorted((o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder()))
                .forEachOrdered(page -> {
                    try {
                        if (page.equals(planToReorder)) {
                            increment.set(0, false);
                            page.setOrder(planToReorder.getOrder());
                        } else {
                            final int newOrder;
                            final Boolean isIncrement = increment.get(0);
                            if (page.getOrder() < planToReorder.getOrder()) {
                                newOrder = page.getOrder() - (isIncrement ? 0 : 1);
                            } else if (page.getOrder() > planToReorder.getOrder())  {
                                newOrder = page.getOrder() + (isIncrement? 1 : 0);
                            } else {
                                newOrder = page.getOrder() + (isIncrement? 1 : -1);
                            }
                            page.setOrder(newOrder);
                        }
                        planRepository.update(page);
                    } catch (final TechnicalException ex) {
                        LOGGER.error("An error occurs while trying to update plan {}", planToReorder.getId(), ex);
                        throw new TechnicalManagementException("An error occurs while trying to update plan " + planToReorder.getId(), ex);
                    }
                });
    }

    private PlanEntity convert(Plan plan) {
        PlanEntity entity = new PlanEntity();

        entity.setId(plan.getId());
        entity.setName(plan.getName());
        entity.setDescription(plan.getDescription());
        entity.setApis(plan.getApis());
        entity.setCreatedAt(plan.getCreatedAt());
        entity.setUpdatedAt(plan.getUpdatedAt());
        entity.setOrder(plan.getOrder());

        if (plan.getDefinition() != null && ! plan.getDefinition().isEmpty()) {
            try {
                HashMap<String, Path> rules = objectMapper.readValue(plan.getDefinition(),
                        new TypeReference<HashMap<String, Path>>() {});

                entity.setPaths(rules);
            } catch (IOException ioe) {
                LOGGER.error("Unexpected error while generating policy definition", ioe);
            }
        }

        entity.setType(PlanType.valueOf(plan.getType().name()));
        entity.setValidation(PlanValidationType.valueOf(plan.getValidation().name()));
        entity.setCharacteristics(plan.getCharacteristics());

        return entity;
    }
}
