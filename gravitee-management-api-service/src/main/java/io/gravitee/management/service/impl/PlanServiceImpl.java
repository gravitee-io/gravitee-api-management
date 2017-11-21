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
import io.gravitee.management.service.AuditService;
import io.gravitee.management.service.PlanService;
import io.gravitee.management.service.SubscriptionService;
import io.gravitee.management.service.exceptions.*;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PLAN;
import static io.gravitee.repository.management.model.Plan.AuditEvent.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PlanServiceImpl extends TransactionalService implements PlanService {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(PlanServiceImpl.class);

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditService auditService;

    @Override
    public PlanEntity findById(String plan) {
        try {
            logger.debug("Find plan by id : {}", plan);

            Optional<Plan> optPlan = planRepository.findById(plan);

            if (! optPlan.isPresent()) {
                throw new PlanNotFoundException(plan);
            }

            return convert(optPlan.get());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find a plan by id: {}", plan, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to find a plan by id: %s", plan), ex);
        }
    }

    @Override
    public Set<PlanEntity> findByApi(String api) {
        try {
            logger.debug("Find plan by api : {}", api);

            Set<Plan> plans = planRepository.findByApi(api);

            return plans
                    .stream()
                    .map(this::convert)
                    .sorted(Comparator.comparingInt(PlanEntity::getOrder))
                    .collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find a plan by api: {}", api, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to find a plan by api: %s", api), ex);
        }
    }

    @Override
    public PlanEntity create(NewPlanEntity newPlan) {
        try {
            logger.debug("Create a new plan {} for API {}", newPlan.getName(), newPlan.getApi());
            Plan plan = new Plan();

            plan.setId(UUID.toString(UUID.random()));
            plan.setApis(Collections.singleton(newPlan.getApi()));
            plan.setName(newPlan.getName());
            plan.setDescription(newPlan.getDescription());
            plan.setCreatedAt(new Date());
            plan.setUpdatedAt(plan.getCreatedAt());
            plan.setType(Plan.PlanType.valueOf(newPlan.getType().name()));
            plan.setSecurity(Plan.PlanSecurityType.valueOf(newPlan.getSecurity().name()));
            plan.setStatus(Plan.Status.valueOf(newPlan.getStatus().name()));
            plan.setExcludedGroups(newPlan.getExcludedGroups());

            if (plan.getSecurity() == Plan.PlanSecurityType.KEY_LESS) {
                // There is no need for a validation when authentication is KEY_LESS, force to AUTO
                plan.setValidation(Plan.PlanValidationType.AUTO);
            } else {
                plan.setValidation(Plan.PlanValidationType.valueOf(newPlan.getValidation().name()));
            }

            plan.setCharacteristics(newPlan.getCharacteristics());

            String planPolicies = objectMapper.writeValueAsString(newPlan.getPaths());
            plan.setDefinition(planPolicies);

            plan = planRepository.create(plan);

            auditService.createApiAuditLog(
                    newPlan.getApi(),
                    Collections.singletonMap(PLAN, plan.getId()),
                    PLAN_CREATED,
                    plan.getCreatedAt(),
                    null,
                    plan);
            return convert(plan);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to create a plan {} for API {}", newPlan.getName(), newPlan.getApi(), ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to create a plan %s for API %s", newPlan.getName(), newPlan.getApi()), ex);
        } catch (JsonProcessingException jse) {
            logger.error("Unexpected error while generating plan definition", jse);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to create a plan %s for API %s", newPlan.getName(), newPlan.getApi()), jse);
        }
    }

    @Override
    public PlanEntity update(UpdatePlanEntity updatePlan) {
        try {
            logger.debug("Update plan {}", updatePlan.getName());

            Optional<Plan> optPlan = planRepository.findById(updatePlan.getId());
            if (! optPlan.isPresent()) {
                throw new PlanNotFoundException(updatePlan.getId());
            }
            Plan oldPlan = optPlan.get();

            Plan newPlan = new Plan();
            //copy immutable values
            newPlan.setId(oldPlan.getId());
            newPlan.setSecurity(oldPlan.getSecurity());
            newPlan.setType(oldPlan.getType());
            newPlan.setStatus(oldPlan.getStatus());
            newPlan.setOrder(oldPlan.getOrder());
            newPlan.setApis(oldPlan.getApis());
            newPlan.setCreatedAt(oldPlan.getCreatedAt());
            newPlan.setPublishedAt(oldPlan.getPublishedAt());
            newPlan.setClosedAt(oldPlan.getClosedAt());

            // update datas
            newPlan.setName(updatePlan.getName());
            newPlan.setDescription(updatePlan.getDescription());
            newPlan.setUpdatedAt(new Date());

            String planPolicies = objectMapper.writeValueAsString(updatePlan.getPaths());
            newPlan.setDefinition(planPolicies);

            newPlan.setExcludedGroups(updatePlan.getExcludedGroups());

            if (newPlan.getSecurity() == Plan.PlanSecurityType.KEY_LESS) {
                // There is no need for a validation when authentication is KEY_LESS, force to AUTO
                newPlan.setValidation(Plan.PlanValidationType.AUTO);
            } else {
                newPlan.setValidation(Plan.PlanValidationType.valueOf(updatePlan.getValidation().name()));
            }

            newPlan.setCharacteristics(updatePlan.getCharacteristics());

            // if order change, reorder all pages
            if (newPlan.getOrder() != updatePlan.getOrder()) {
                newPlan.setOrder(updatePlan.getOrder());
                reorderAndSavePlans(newPlan);
                return null;
            } else {
                newPlan = planRepository.update(newPlan);
                auditService.createApiAuditLog(
                        newPlan.getApis().iterator().next(),
                        Collections.singletonMap(PLAN, newPlan.getId()),
                        PLAN_UPDATED,
                        newPlan.getUpdatedAt(),
                        oldPlan,
                        newPlan);
                return convert(newPlan);
            }

        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to update plan {}", updatePlan.getName(), ex);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to update plan %s", updatePlan.getName()), ex);
        } catch (JsonProcessingException jse) {
            logger.error("Unexpected error while generating plan definition", jse);
            throw new TechnicalManagementException(String.format(
                    "An error occurs while trying to update a plan %s", updatePlan.getName()), jse);
        }
    }

    @Override
    public PlanEntity close(String planId, String user) {
        try {
            logger.debug("Close plan {}", planId);

            Optional<Plan> optPlan = planRepository.findById(planId);
            if (! optPlan.isPresent()) {
                throw new PlanNotFoundException(planId);
            }

            Plan plan = optPlan.get();
            Plan previousPlan = new Plan(plan);

            if (plan.getStatus() == Plan.Status.CLOSED) {
                throw new PlanAlreadyClosedException(planId);
            }

            // Update plan status
            plan.setStatus(Plan.Status.CLOSED);
            plan.setClosedAt(new Date());
            plan.setUpdatedAt(plan.getClosedAt());

            // Close active subscriptions and reject pending
            if (plan.getSecurity() != Plan.PlanSecurityType.KEY_LESS) {
                subscriptionService.findByPlan(planId)
                        .stream()
                        .filter(subscriptionEntity -> subscriptionEntity.getStatus() == SubscriptionStatus.ACCEPTED)
                        .forEach(subscription -> subscriptionService.close(subscription.getId()));

                final String planName = plan.getName();
                subscriptionService.findByPlan(planId)
                        .stream()
                        .filter(subscriptionEntity -> subscriptionEntity.getStatus() == SubscriptionStatus.PENDING)
                        .forEach(subscription -> {
                            ProcessSubscriptionEntity processSubscriptionEntity = new ProcessSubscriptionEntity();
                            processSubscriptionEntity.setId(subscription.getId());
                            processSubscriptionEntity.setAccepted(false);
                            processSubscriptionEntity.setReason("Plan " + planName + " has been closed.");
                            subscriptionService.process(processSubscriptionEntity, user);
                        });
            }

            // Save plan
            plan = planRepository.update(plan);

            // Audit
            auditService.createApiAuditLog(
                    plan.getApis().iterator().next(),
                    Collections.singletonMap(PLAN, plan.getId()),
                    PLAN_CLOSED,
                    plan.getUpdatedAt(),
                    previousPlan,
                    plan);

            //reorder plan
            reorderedAndSavePlansAfterRemove(optPlan.get());

            return convert(plan);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete plan: {}", planId, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to delete plan: %s", planId), ex);
        }
    }

    @Override
    public void delete(String plan) {
        try {
            logger.debug("Delete plan {}", plan);

            Optional<Plan> optPlan = planRepository.findById(plan);
            if (! optPlan.isPresent()) {
                throw new PlanNotFoundException(plan);
            }

            if (optPlan.get().getSecurity() != Plan.PlanSecurityType.KEY_LESS) {
                int subscriptions = subscriptionService.findByPlan(plan).size();
                if (optPlan.get().getStatus() == Plan.Status.PUBLISHED && subscriptions > 0) {
                    throw new PlanWithSubscriptionsException();
                }
            }

            // Delete plan
            planRepository.delete(plan);
            // Audit
            auditService.createApiAuditLog(
                    optPlan.get().getApis().iterator().next(),
                    Collections.singletonMap(PLAN, optPlan.get().getId()),
                    PLAN_DELETED,
                    new Date(),
                    optPlan.get(),
                    null);
            //reorder plan
            reorderedAndSavePlansAfterRemove(optPlan.get());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete plan: {}", plan, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to delete plan: %s", plan), ex);
        }
    }

    @Override
    public PlanEntity publish(String planId) {
        try {
            logger.debug("Publish plan {}", planId);

            Optional<Plan> optPlan = planRepository.findById(planId);
            if (! optPlan.isPresent()) {
                throw new PlanNotFoundException(planId);
            }

            Plan plan = optPlan.get();
            Plan previousPlan = new Plan(plan);
            if (plan.getStatus() == Plan.Status.CLOSED) {
                throw new PlanAlreadyClosedException(planId);
            } else if (plan.getStatus() == Plan.Status.PUBLISHED) {
                throw new PlanAlreadyPublishedException(planId);
            }

            Set<Plan> plans = planRepository.findByApi(plan.getApis().iterator().next());
            if (plan.getSecurity() == Plan.PlanSecurityType.KEY_LESS) {
                // Look to other plans if there is already a keyless-published plan
                long count = plans
                        .stream()
                        .filter(plan1 -> plan1.getStatus() == Plan.Status.PUBLISHED)
                        .filter(plan1 -> plan1.getSecurity() == Plan.PlanSecurityType.KEY_LESS)
                        .count();

                if (count > 0) {
                    throw new KeylessPlanAlreadyPublishedException(planId);
                }
            }

            // Update plan status
            plan.setStatus(Plan.Status.PUBLISHED);
            // Update plan order
            List<Plan> orderedPublishedPlans = plans.
                    stream().
                    filter(plan1 -> Plan.Status.PUBLISHED.equals(plan1.getStatus())).
                    sorted(Comparator.comparingInt(Plan::getOrder)).
                    collect(Collectors.toList());
            plan.setOrder(orderedPublishedPlans.isEmpty() ? 1 : (orderedPublishedPlans.get(orderedPublishedPlans.size()-1).getOrder() + 1));

            plan.setPublishedAt(new Date());
            plan.setUpdatedAt(plan.getPublishedAt());

            // Save plan
            plan = planRepository.update(plan);

            // Audit
            auditService.createApiAuditLog(
                    plan.getApis().iterator().next(),
                    Collections.singletonMap(PLAN, plan.getId()),
                    PLAN_PUBLISHED,
                    plan.getUpdatedAt(),
                    previousPlan,
                    plan);

            return convert(plan);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to publish plan: {}", planId, ex);
            throw new TechnicalManagementException(
                    String.format("An error occurs while trying to publish plan: %s", planId), ex);
        }
    }

    private void reorderAndSavePlans(final Plan planToReorder) throws TechnicalException {
        final Collection<Plan> plans = planRepository.findByApi(planToReorder.getApis().iterator().next());
        Plan[] plansToReorder = plans.stream()
                .filter(p -> Plan.Status.PUBLISHED.equals(p.getStatus()) && !Objects.equals(p.getId(), planToReorder.getId()))
                .sorted(Comparator.comparingInt(Plan::getOrder)).toArray(Plan[]::new);

        // the new plan order must be between 1 && numbers of published apis
        if(planToReorder.getOrder() < 1) {
            planToReorder.setOrder(1);
        } else if (planToReorder.getOrder() > plansToReorder.length + 1) { // -1 because we have filtered the plan itself
            planToReorder.setOrder(plansToReorder.length + 1);
        }
        try {
            // reorder plans before and after the reordered plan
            // we update the first range only because of https://github.com/gravitee-io/issues/issues/751
            // if orders are good in the repository, we should not update the first range
            for (int i = 0; i < plansToReorder.length; i++) {
                int newOrder = (i + 1) < planToReorder.getOrder() ? (i+1) : (i+2);
                if (plansToReorder[i].getOrder() != newOrder) {
                    plansToReorder[i].setOrder(newOrder);
                    planRepository.update(plansToReorder[i]);
                }
            }
            // update the modified plan
            planRepository.update(planToReorder);

        } catch (final TechnicalException ex) {
            logger.error("An error occurs while trying to update plan {}", planToReorder.getId(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update plan " + planToReorder.getId(), ex);
        }
    }

    private void reorderedAndSavePlansAfterRemove(final Plan planRemoved) throws TechnicalException {
        final Collection<Plan> plans = planRepository.findByApi(planRemoved.getApis().iterator().next());
        plans.stream()
                .filter(p -> Plan.Status.PUBLISHED.equals(p.getStatus()))
                .sorted(Comparator.comparingInt(Plan::getOrder))
                .forEachOrdered(plan -> {
                    try {
                        if (plan.getOrder() > planRemoved.getOrder()) {
                            plan.setOrder(plan.getOrder() - 1);
                            planRepository.update(plan);
                        }
                    } catch (final TechnicalException ex) {
                        logger.error("An error occurs while trying to reorder plan {}", plan.getId(), ex);
                        throw new TechnicalManagementException("An error occurs while trying to update plan " + plan.getId(), ex);
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
        entity.setExcludedGroups(plan.getExcludedGroups());

        if (plan.getDefinition() != null && ! plan.getDefinition().isEmpty()) {
            try {
                HashMap<String, Path> rules = objectMapper.readValue(plan.getDefinition(),
                        new TypeReference<HashMap<String, Path>>() {});

                entity.setPaths(rules);
            } catch (IOException ioe) {
                logger.error("Unexpected error while generating policy definition", ioe);
            }
        }

        entity.setType(PlanType.valueOf(plan.getType().name()));

        // Backward compatibility
        if (plan.getStatus() != null) {
            entity.setStatus(PlanStatus.valueOf(plan.getStatus().name()));
        } else {
            entity.setStatus(PlanStatus.PUBLISHED);
        }

        if (plan.getSecurity() != null) {
            entity.setSecurity(PlanSecurityType.valueOf(plan.getSecurity().name()));
        } else {
            entity.setSecurity(PlanSecurityType.API_KEY);
        }

        entity.setClosedAt(plan.getClosedAt());
        entity.setPublishedAt(plan.getPublishedAt());
        entity.setValidation(PlanValidationType.valueOf(plan.getValidation().name()));
        entity.setCharacteristics(plan.getCharacteristics());

        return entity;
    }
}
