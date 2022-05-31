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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Audit.AuditProperties.PLAN;
import static io.gravitee.repository.management.model.Plan.AuditEvent.*;
import static java.util.Collections.emptySet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.plan.PlanQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.processor.PlanSynchronizationProcessor;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PlanServiceImpl extends TransactionalService implements PlanService {

    private final Logger logger = LoggerFactory.getLogger(PlanServiceImpl.class);

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PageService pageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private PlanSynchronizationProcessor planSynchronizationProcessor;

    @Autowired
    private ApiService apiService;

    @Autowired
    private PlanConverter planConverter;

    private static final List<PlanSecurityEntity> DEFAULT_SECURITY_LIST = Collections.unmodifiableList(
        Arrays.asList(
            new PlanSecurityEntity("oauth2", "OAuth2", "oauth2"),
            new PlanSecurityEntity("jwt", "JWT", "'jwt'"),
            new PlanSecurityEntity("api_key", "API Key", "api-key"),
            new PlanSecurityEntity("key_less", "Keyless (public)", "")
        )
    );

    @Override
    public PlanEntity findById(final ExecutionContext executionContext, String plan) {
        try {
            logger.debug("Find plan by id : {}", plan);

            return planRepository.findById(plan).map(planConverter::toPlanEntity).orElseThrow(() -> new PlanNotFoundException(plan));
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find a plan by id: {}", plan, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to find a plan by id: %s", plan), ex);
        }
    }

    @Override
    public Set<PlanEntity> findByIdIn(final ExecutionContext executionContext, Set<String> ids) {
        try {
            return planRepository.findByIdIn(ids).stream().map(planConverter::toPlanEntity).collect(Collectors.toSet());
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error has occurred retrieving plans by ids", e);
        }
    }

    @Override
    public Set<PlanEntity> findByApi(final ExecutionContext executionContext, String api) {
        try {
            logger.debug("Find plan by api : {}", api);

            return planRepository.findByApi(api).stream().map(planConverter::toPlanEntity).collect(Collectors.toSet());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to find a plan by api: {}", api, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to find a plan by api: %s", api), ex);
        }
    }

    @Override
    public List<PlanEntity> search(final ExecutionContext executionContext, final PlanQuery query) {
        Set<PlanEntity> planEntities;
        if (query.getApi() != null) {
            planEntities = findByApi(executionContext, query.getApi());
        } else {
            planEntities = emptySet();
        }

        return planEntities
            .stream()
            .filter(
                p -> {
                    boolean filtered = true;
                    if (query.getName() != null) {
                        filtered = p.getName().equals(query.getName());
                    }
                    if (filtered && query.getSecurity() != null) {
                        filtered = p.getSecurity().equals(query.getSecurity());
                    }
                    return filtered;
                }
            )
            .collect(Collectors.toList());
    }

    @Override
    public PlanEntity create(final ExecutionContext executionContext, NewPlanEntity newPlan) {
        try {
            logger.debug("Create a new plan {} for API {}", newPlan.getName(), newPlan.getApi());

            assertPlanSecurityIsAllowed(executionContext, newPlan.getSecurity());

            final ApiEntity api = apiService.findById(executionContext, newPlan.getApi());
            if (ApiLifecycleState.DEPRECATED.equals(api.getLifecycleState())) {
                throw new ApiDeprecatedException(api.getName());
            }

            String id = newPlan.getId() != null && UUID.fromString(newPlan.getId()) != null ? newPlan.getId() : UuidString.generateRandom();

            newPlan.setId(id);
            Plan plan = planConverter.toPlan(newPlan, DefinitionVersion.valueOfLabel(api.getGraviteeDefinitionVersion()));
            plan = planRepository.create(plan);

            auditService.createApiAuditLog(
                executionContext,
                newPlan.getApi(),
                Collections.singletonMap(PLAN, plan.getId()),
                PLAN_CREATED,
                plan.getCreatedAt(),
                null,
                plan
            );
            return planConverter.toPlanEntity(plan);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to create a plan {} for API {}", newPlan.getName(), newPlan.getApi(), ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to create a plan %s for API %s", newPlan.getName(), newPlan.getApi()),
                ex
            );
        } catch (JsonProcessingException jse) {
            logger.error("Unexpected error while generating plan definition", jse);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to create a plan %s for API %s", newPlan.getName(), newPlan.getApi()),
                jse
            );
        }
    }

    @Override
    public PlanEntity createOrUpdatePlan(final ExecutionContext executionContext, PlanEntity planEntity) {
        PlanEntity resultPlanEntity;
        try {
            findById(executionContext, planEntity.getId());
            resultPlanEntity = update(executionContext, planConverter.toUpdatePlanEntity(planEntity));
        } catch (PlanNotFoundException npe) {
            resultPlanEntity = create(executionContext, planConverter.toNewPlanEntity(planEntity));
        }
        return resultPlanEntity;
    }

    @Override
    public PlanEntity update(final ExecutionContext executionContext, UpdatePlanEntity updatePlan) {
        return update(executionContext, updatePlan, false);
    }

    public PlanEntity update(final ExecutionContext executionContext, UpdatePlanEntity updatePlan, boolean fromImport) {
        try {
            logger.debug("Update plan {}", updatePlan.getName());
            Optional<Plan> optPlan = planRepository.findById(updatePlan.getId());
            if (!optPlan.isPresent()) {
                throw new PlanNotFoundException(updatePlan.getId());
            }

            Plan oldPlan = optPlan.get();
            assertPlanSecurityIsAllowed(executionContext, PlanSecurityType.valueOf(oldPlan.getSecurity().name()));

            ApiEntity api = apiService.findById(executionContext, oldPlan.getApi());
            if (
                DefinitionVersion.V2.equals(DefinitionVersion.valueOfLabel(api.getGraviteeDefinitionVersion())) &&
                updatePlan.getFlows() == null
            ) {
                throw new PlanInvalidException(updatePlan.getId());
            }

            Plan newPlan = new Plan();
            //copy immutable values
            newPlan.setId(oldPlan.getId());
            newPlan.setSecurity(oldPlan.getSecurity());
            newPlan.setType(oldPlan.getType());
            newPlan.setStatus(oldPlan.getStatus());
            newPlan.setOrder(oldPlan.getOrder());
            newPlan.setApi(oldPlan.getApi());
            newPlan.setCreatedAt(oldPlan.getCreatedAt());
            newPlan.setPublishedAt(oldPlan.getPublishedAt());
            newPlan.setClosedAt(oldPlan.getClosedAt());
            // for existing plans, needRedeployAt doesn't exist. We have to initalize it
            if (oldPlan.getNeedRedeployAt() == null) {
                newPlan.setNeedRedeployAt(oldPlan.getUpdatedAt());
            } else {
                newPlan.setNeedRedeployAt(oldPlan.getNeedRedeployAt());
            }

            // update data
            newPlan.setName(updatePlan.getName());
            newPlan.setCrossId(updatePlan.getCrossId() != null ? updatePlan.getCrossId() : oldPlan.getCrossId());
            newPlan.setDescription(updatePlan.getDescription());
            newPlan.setUpdatedAt(new Date());
            newPlan.setSecurityDefinition(updatePlan.getSecurityDefinition());
            newPlan.setCommentRequired(updatePlan.isCommentRequired());
            newPlan.setCommentMessage(updatePlan.getCommentMessage());
            newPlan.setTags(updatePlan.getTags());
            newPlan.setSelectionRule(updatePlan.getSelectionRule());
            newPlan.setGeneralConditions(updatePlan.getGeneralConditions());
            newPlan.setFlows(objectMapper.writeValueAsString(updatePlan.getFlows()));

            if (Plan.Status.PUBLISHED.equals(newPlan.getStatus()) || Plan.Status.DEPRECATED.equals(newPlan.getStatus())) {
                checkStatusOfGeneralConditions(newPlan);
            }

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
                if (
                    !planSynchronizationProcessor.processCheckSynchronization(
                        planConverter.toPlanEntity(oldPlan),
                        planConverter.toPlanEntity(newPlan)
                    )
                ) {
                    newPlan.setNeedRedeployAt(newPlan.getUpdatedAt());
                }
                newPlan = planRepository.update(newPlan);
                auditService.createApiAuditLog(
                    executionContext,
                    newPlan.getApi(),
                    Collections.singletonMap(PLAN, newPlan.getId()),
                    PLAN_UPDATED,
                    newPlan.getUpdatedAt(),
                    oldPlan,
                    newPlan
                );

                return planConverter.toPlanEntity(newPlan);
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to update plan {}", updatePlan.getName(), ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to update plan %s", updatePlan.getName()),
                ex
            );
        } catch (JsonProcessingException jse) {
            logger.error("Unexpected error while generating plan definition", jse);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to update a plan %s", updatePlan.getName()),
                jse
            );
        }
    }

    private void checkStatusOfGeneralConditions(Plan plan) {
        if (plan.getGeneralConditions() != null && !plan.getGeneralConditions().isEmpty()) {
            PageEntity generalConditions = pageService.findById(plan.getGeneralConditions());
            if (!generalConditions.isPublished()) {
                throw new PlanGeneralConditionStatusException(plan.getName());
            }
        }
    }

    @Override
    public PlanEntity close(final ExecutionContext executionContext, String planId, String userId) {
        try {
            logger.debug("Close plan {}", planId);

            Optional<Plan> optPlan = planRepository.findById(planId);
            if (!optPlan.isPresent()) {
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
            plan.setNeedRedeployAt(plan.getClosedAt());

            // Close subscriptions
            if (plan.getSecurity() != Plan.PlanSecurityType.KEY_LESS) {
                subscriptionService
                    .findByPlan(executionContext, planId)
                    .stream()
                    .forEach(
                        subscription -> {
                            try {
                                subscriptionService.close(executionContext, subscription.getId());
                            } catch (SubscriptionNotClosableException snce) {
                                // subscription status could not be closed (already closed or rejected)
                                // ignore it
                            }
                        }
                    );
            }

            // Save plan
            plan = planRepository.update(plan);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                plan.getApi(),
                Collections.singletonMap(PLAN, plan.getId()),
                PLAN_CLOSED,
                plan.getUpdatedAt(),
                previousPlan,
                plan
            );

            //reorder plan
            reorderedAndSavePlansAfterRemove(optPlan.get());

            return planConverter.toPlanEntity(plan);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete plan: {}", planId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to delete plan: %s", planId), ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String plan) {
        try {
            logger.debug("Delete plan {}", plan);

            Optional<Plan> optPlan = planRepository.findById(plan);
            if (!optPlan.isPresent()) {
                throw new PlanNotFoundException(plan);
            }

            if (optPlan.get().getSecurity() != Plan.PlanSecurityType.KEY_LESS) {
                int subscriptions = subscriptionService.findByPlan(executionContext, plan).size();
                if (
                    (optPlan.get().getStatus() == Plan.Status.PUBLISHED || optPlan.get().getStatus() == Plan.Status.DEPRECATED) &&
                    subscriptions > 0
                ) {
                    throw new PlanWithSubscriptionsException(plan);
                }
            }

            // Delete plan
            planRepository.delete(plan);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                optPlan.get().getApi(),
                Collections.singletonMap(PLAN, optPlan.get().getId()),
                PLAN_DELETED,
                new Date(),
                optPlan.get(),
                null
            );

            //reorder plan
            reorderedAndSavePlansAfterRemove(optPlan.get());
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete plan: {}", plan, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to delete plan: %s", plan), ex);
        }
    }

    @Override
    public PlanEntity publish(final ExecutionContext executionContext, String planId) {
        try {
            logger.debug("Publish plan {}", planId);

            Optional<Plan> optPlan = planRepository.findById(planId);
            if (!optPlan.isPresent()) {
                throw new PlanNotFoundException(planId);
            }

            Plan plan = optPlan.get();
            Plan previousPlan = new Plan(plan);
            if (plan.getStatus() == Plan.Status.CLOSED) {
                throw new PlanAlreadyClosedException(planId);
            } else if (plan.getStatus() == Plan.Status.PUBLISHED) {
                throw new PlanAlreadyPublishedException(planId);
            } else if (plan.getStatus() == Plan.Status.DEPRECATED) {
                throw new PlanAlreadyDeprecatedException(planId);
            }

            checkStatusOfGeneralConditions(plan);

            Set<Plan> plans = planRepository.findByApi(plan.getApi());
            if (plan.getSecurity() == Plan.PlanSecurityType.KEY_LESS) {
                // Look to other plans if there is already a keyless-published plan
                long count = plans
                    .stream()
                    .filter(plan1 -> plan1.getStatus() == Plan.Status.PUBLISHED || plan1.getStatus() == Plan.Status.DEPRECATED)
                    .filter(plan1 -> plan1.getSecurity() == Plan.PlanSecurityType.KEY_LESS)
                    .count();

                if (count > 0) {
                    throw new KeylessPlanAlreadyPublishedException(planId);
                }
            }

            // Update plan status
            plan.setStatus(Plan.Status.PUBLISHED);
            // Update plan order
            List<Plan> orderedPublishedPlans = plans
                .stream()
                .filter(plan1 -> Plan.Status.PUBLISHED.equals(plan1.getStatus()))
                .sorted(Comparator.comparingInt(Plan::getOrder))
                .collect(Collectors.toList());
            plan.setOrder(
                orderedPublishedPlans.isEmpty() ? 1 : (orderedPublishedPlans.get(orderedPublishedPlans.size() - 1).getOrder() + 1)
            );

            plan.setPublishedAt(new Date());
            plan.setUpdatedAt(plan.getPublishedAt());
            plan.setNeedRedeployAt(plan.getPublishedAt());

            // Save plan
            plan = planRepository.update(plan);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                plan.getApi(),
                Collections.singletonMap(PLAN, plan.getId()),
                PLAN_PUBLISHED,
                plan.getUpdatedAt(),
                previousPlan,
                plan
            );

            return planConverter.toPlanEntity(plan);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to publish plan: {}", planId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to publish plan: %s", planId), ex);
        }
    }

    @Override
    public PlanEntity deprecate(final ExecutionContext executionContext, String planId) {
        return deprecate(executionContext, planId, false);
    }

    @Override
    public PlanEntity deprecate(final ExecutionContext executionContext, String planId, boolean allowStaging) {
        try {
            logger.debug("Deprecate plan {}", planId);

            Optional<Plan> optPlan = planRepository.findById(planId);
            if (!optPlan.isPresent()) {
                throw new PlanNotFoundException(planId);
            }

            Plan plan = optPlan.get();
            Plan previousPlan = new Plan(plan);
            if (plan.getStatus() == Plan.Status.DEPRECATED) {
                throw new PlanAlreadyDeprecatedException(planId);
            } else if (plan.getStatus() == Plan.Status.CLOSED) {
                throw new PlanAlreadyClosedException(planId);
            } else if (!allowStaging && plan.getStatus() == Plan.Status.STAGING) {
                throw new PlanNotYetPublishedException(planId);
            }

            // Update plan status
            plan.setStatus(Plan.Status.DEPRECATED);
            plan.setUpdatedAt(new Date());

            // Save plan
            plan = planRepository.update(plan);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                plan.getApi(),
                Collections.singletonMap(PLAN, plan.getId()),
                PLAN_DEPRECATED,
                plan.getUpdatedAt(),
                previousPlan,
                plan
            );

            return planConverter.toPlanEntity(plan);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to deprecate plan: {}", planId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to deprecate plan: %s", planId), ex);
        }
    }

    @Override
    public PlansConfigurationEntity getConfiguration() {
        PlansConfigurationEntity config = new PlansConfigurationEntity();
        config.setSecurity(DEFAULT_SECURITY_LIST);
        return config;
    }

    private void reorderAndSavePlans(final Plan planToReorder) throws TechnicalException {
        final Collection<Plan> plans = planRepository.findByApi(planToReorder.getApi());
        Plan[] plansToReorder = plans
            .stream()
            .filter(p -> Plan.Status.PUBLISHED.equals(p.getStatus()) && !Objects.equals(p.getId(), planToReorder.getId()))
            .sorted(Comparator.comparingInt(Plan::getOrder))
            .toArray(Plan[]::new);

        // the new plan order must be between 1 && numbers of published apis
        if (planToReorder.getOrder() < 1) {
            planToReorder.setOrder(1);
        } else if (planToReorder.getOrder() > plansToReorder.length + 1) { // -1 because we have filtered the plan itself
            planToReorder.setOrder(plansToReorder.length + 1);
        }
        try {
            // reorder plans before and after the reordered plan
            // we update the first range only because of https://github.com/gravitee-io/issues/issues/751
            // if orders are good in the repository, we should not update the first range
            for (int i = 0; i < plansToReorder.length; i++) {
                int newOrder = (i + 1) < planToReorder.getOrder() ? (i + 1) : (i + 2);
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
        final Collection<Plan> plans = planRepository.findByApi(planRemoved.getApi());
        plans
            .stream()
            .filter(p -> Plan.Status.PUBLISHED.equals(p.getStatus()))
            .sorted(Comparator.comparingInt(Plan::getOrder))
            .forEachOrdered(
                plan -> {
                    try {
                        if (plan.getOrder() > planRemoved.getOrder()) {
                            plan.setOrder(plan.getOrder() - 1);
                            planRepository.update(plan);
                        }
                    } catch (final TechnicalException ex) {
                        logger.error("An error occurs while trying to reorder plan {}", plan.getId(), ex);
                        throw new TechnicalManagementException("An error occurs while trying to update plan " + plan.getId(), ex);
                    }
                }
            );
    }

    private void assertPlanSecurityIsAllowed(final ExecutionContext executionContext, PlanSecurityType securityType) {
        Key securityKey;
        switch (securityType) {
            case API_KEY:
                securityKey = Key.PLAN_SECURITY_APIKEY_ENABLED;
                break;
            case OAUTH2:
                securityKey = Key.PLAN_SECURITY_OAUTH2_ENABLED;
                break;
            case JWT:
                securityKey = Key.PLAN_SECURITY_JWT_ENABLED;
                break;
            case KEY_LESS:
                securityKey = Key.PLAN_SECURITY_KEYLESS_ENABLED;
                break;
            default:
                return;
        }
        if (!parameterService.findAsBoolean(executionContext, securityKey, ParameterReferenceType.ENVIRONMENT)) {
            throw new UnauthorizedPlanSecurityTypeException(securityType);
        }
    }

    @Override
    public boolean anyPlanMismatchWithApi(List<String> planIds, String apiId) {
        try {
            return planRepository.findByIdIn(planIds).stream().map(Plan::getApi).filter(Objects::nonNull).anyMatch(id -> !id.equals(apiId));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException("An error has occurred checking plans ownership", e);
        }
    }

    @Override
    public Map<String, Object> findByIdAsMap(String id) throws TechnicalException {
        Plan plan = planRepository.findById(id).orElseThrow(() -> new PlanNotFoundException(id));
        return objectMapper.convertValue(plan, Map.class);
    }
}
