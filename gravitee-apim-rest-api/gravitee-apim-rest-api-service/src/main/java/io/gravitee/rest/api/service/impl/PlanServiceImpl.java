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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.definition.model.DefinitionVersion.V2;
import static io.gravitee.repository.management.model.ApiLifecycleState.DEPRECATED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_CLOSED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_CREATED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_DELETED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_DEPRECATED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_PUBLISHED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_UPDATED;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Group;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.BasePlanEntity;
import io.gravitee.rest.api.model.NewPlanEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PlansConfigurationEntity;
import io.gravitee.rest.api.model.UpdatePlanEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.ApiDeprecatedException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import io.gravitee.rest.api.service.exceptions.KeylessPlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyDeprecatedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanFlowRequiredException;
import io.gravitee.rest.api.service.exceptions.PlanGeneralConditionStatusException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotYetPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanWithSubscriptionsException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotClosableException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedPlanSecurityTypeException;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@Component
public class PlanServiceImpl extends AbstractService implements PlanService {

    private static final List<PlanSecurityEntity> DEFAULT_SECURITY_LIST = List.of(
        new PlanSecurityEntity("oauth2", "OAuth2", "oauth2"),
        new PlanSecurityEntity("jwt", "JWT", "'jwt'"),
        new PlanSecurityEntity("api_key", "API Key", "api-key"),
        new PlanSecurityEntity("key_less", "Keyless (public)", "")
    );

    @Lazy
    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private PlanSearchService planSearchService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private CloseSubscriptionDomainService closeSubscriptionDomainService;

    @Autowired
    private PageService pageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ParameterService parameterService;

    @Autowired
    private SynchronizationService synchronizationService;

    @Lazy
    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private PlanConverter planConverter;

    @Autowired
    private FlowService flowService;

    @Autowired
    private TagsValidationService tagsValidationService;

    @Autowired
    private GroupService groupService;

    @Override
    public PlanEntity findById(final ExecutionContext executionContext, final String plan) {
        return PlanAdapter.INSTANCE.map(planSearchService.findById(executionContext, plan));
    }

    @Override
    public Set<PlanEntity> findByApi(final ExecutionContext executionContext, final String api) {
        return planSearchService.findByApi(executionContext, api, true).stream().flatMap(this::map).collect(Collectors.toSet());
    }

    @Override
    public PlanEntity create(final ExecutionContext executionContext, NewPlanEntity newPlan) {
        try {
            log.debug("Create a new plan {} for API {}", newPlan.getName(), newPlan.getReferenceId());

            assertPlanSecurityIsAllowed(executionContext, newPlan.getSecurity());

            Api api = apiRepository
                .findById(newPlan.getReferenceId())
                .orElseThrow(() -> new ApiNotFoundException(newPlan.getReferenceId()));

            if (api.getApiLifecycleState() == DEPRECATED) {
                throw new ApiDeprecatedException(api.getName());
            }

            validateTags(newPlan.getTags(), api);

            String id = newPlan.getId() != null && UUID.fromString(newPlan.getReferenceId()) != null
                ? newPlan.getId()
                : UuidString.generateRandom();

            newPlan.setId(id);
            Plan plan = planConverter.toPlan(newPlan, getApiDefinitionVersion(api));
            plan.setEnvironmentId(executionContext.getEnvironmentId());
            plan = planRepository.create(plan);

            flowService.save(FlowReferenceType.PLAN, plan.getId(), newPlan.getFlows());

            auditService.createApiAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()))
                    .event(PLAN_CREATED)
                    .createdAt(plan.getCreatedAt())
                    .oldValue(null)
                    .newValue(plan)
                    .build(),
                newPlan.getReferenceId()
            );
            return convert(plan);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to create a plan {} for API {}", newPlan.getName(), newPlan.getReferenceId(), ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to create a plan %s for API %s", newPlan.getName(), newPlan.getReferenceId()),
                ex
            );
        } catch (JsonProcessingException jse) {
            log.error("Unexpected error while generating plan definition", jse);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to create a plan %s for API %s", newPlan.getName(), newPlan.getReferenceId()),
                jse
            );
        }
    }

    @Override
    public PlanEntity createOrUpdatePlan(final ExecutionContext executionContext, PlanEntity planEntity) {
        PlanEntity resultPlanEntity;
        if (planEntity.getId() == null || !planSearchService.exists(planEntity.getId())) {
            resultPlanEntity = create(executionContext, planConverter.toNewPlanEntity(planEntity));
        } else {
            resultPlanEntity = update(executionContext, planConverter.toUpdatePlanEntity(planEntity));
        }
        return resultPlanEntity;
    }

    @Override
    public PlanEntity update(final ExecutionContext executionContext, UpdatePlanEntity updatePlan) {
        return update(executionContext, updatePlan, false);
    }

    public PlanEntity update(final ExecutionContext executionContext, UpdatePlanEntity updatePlan, boolean fromImport) {
        try {
            log.debug("Update plan {}", updatePlan.getName());

            Plan oldPlan = planRepository.findById(updatePlan.getId()).orElseThrow(() -> new PlanNotFoundException(updatePlan.getId()));
            assertPlanSecurityIsAllowed(executionContext, PlanSecurityType.valueOf(oldPlan.getSecurity().name()));

            Api api = apiRepository
                .findById(oldPlan.getReferenceId())
                .orElseThrow(() -> new ApiNotFoundException(oldPlan.getReferenceId()));
            if (getApiDefinitionVersion(api) == V2 && updatePlan.getFlows() == null) {
                throw new PlanFlowRequiredException(updatePlan.getId());
            }

            Plan newPlan = new Plan();
            //copy immutable values
            newPlan.setId(oldPlan.getId());
            newPlan.setHrid(oldPlan.getHrid());
            newPlan.setSecurity(oldPlan.getSecurity());
            newPlan.setType(oldPlan.getType());
            newPlan.setStatus(oldPlan.getStatus());
            newPlan.setOrder(oldPlan.getOrder());
            newPlan.setReferenceId(oldPlan.getReferenceId());
            newPlan.setReferenceType(oldPlan.getReferenceType());
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

            if (Plan.Status.PUBLISHED.equals(newPlan.getStatus()) || Plan.Status.DEPRECATED.equals(newPlan.getStatus())) {
                checkStatusOfGeneralConditions(newPlan);
            }

            String planPolicies = objectMapper.writeValueAsString(updatePlan.getPaths());
            newPlan.setDefinition(planPolicies);

            validateExcludedGroups(updatePlan.getExcludedGroups(), api.getEnvironmentId());
            newPlan.setExcludedGroups(updatePlan.getExcludedGroups());

            if (newPlan.getSecurity() == Plan.PlanSecurityType.KEY_LESS) {
                // There is no need for a validation when authentication is KEY_LESS, force to AUTO
                newPlan.setValidation(Plan.PlanValidationType.AUTO);
            } else {
                newPlan.setValidation(Plan.PlanValidationType.valueOf(updatePlan.getValidation().name()));
            }

            newPlan.setCharacteristics(updatePlan.getCharacteristics());

            validateTags(newPlan.getTags(), api);

            PlanEntity oldPlanEntity = convert(oldPlan);

            flowService.save(FlowReferenceType.PLAN, updatePlan.getId(), updatePlan.getFlows());
            PlanEntity newPlanEntity = convert(newPlan);

            if (
                !synchronizationService.checkSynchronization(PlanEntity.class, oldPlanEntity, newPlanEntity) ||
                !synchronizationService.checkSynchronization(BasePlanEntity.class, oldPlanEntity, newPlanEntity)
            ) {
                newPlan.setNeedRedeployAt(newPlan.getUpdatedAt());
            }

            // if order change, reorder all plans
            if (newPlan.getOrder() != updatePlan.getOrder()) {
                newPlan.setOrder(updatePlan.getOrder());
                newPlan = reorderAndSavePlans(newPlan);
            } else {
                newPlan = planRepository.update(newPlan);
            }

            auditService.createApiAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(Collections.singletonMap(Audit.AuditProperties.PLAN, newPlan.getId()))
                    .event(PLAN_UPDATED)
                    .createdAt(newPlan.getUpdatedAt())
                    .oldValue(oldPlan)
                    .newValue(newPlan)
                    .build(),
                newPlan.getReferenceId()
            );

            return convert(newPlan);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to update plan {}", updatePlan.getName(), ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to update plan %s", updatePlan.getName()),
                ex
            );
        } catch (JsonProcessingException jse) {
            log.error("Unexpected error while generating plan definition", jse);
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

    private void validateExcludedGroups(List<String> excludedGroups, String environmentId) throws TechnicalException {
        var envGroupsIds = groupService.findAllByEnvironment(environmentId).stream().map(Group::getId).collect(Collectors.toSet());
        if (excludedGroups != null && !excludedGroups.isEmpty()) {
            excludedGroups.forEach(excludedGroupId -> {
                if (!envGroupsIds.contains(excludedGroupId)) {
                    throw new GroupNotFoundException(excludedGroupId);
                }
            });
        }
    }

    @Override
    public PlanEntity close(final ExecutionContext executionContext, String planId) {
        try {
            log.debug("Close plan {}", planId);

            Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

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
                var auditInfo = AuditInfo.builder()
                    .organizationId(executionContext.getOrganizationId())
                    .environmentId(executionContext.getEnvironmentId())
                    .actor(getAuthenticatedUserAsAuditActor())
                    .build();

                subscriptionService
                    .findByPlan(executionContext, planId)
                    .forEach(subscription -> {
                        try {
                            closeSubscriptionDomainService.closeSubscription(subscription.getId(), auditInfo);
                        } catch (SubscriptionNotClosableException snce) {
                            // subscription status could not be closed (already closed or rejected)
                            // ignore it
                        }
                    });
            }

            // Save plan
            plan = planRepository.update(plan);

            // Audit
            String referenceId = plan.getReferenceId();
            Plan.PlanReferenceType referenceType = plan.getReferenceType();
            var auditLogData = AuditService.AuditLogData.builder()
                .properties(Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()))
                .event(PLAN_CLOSED)
                .createdAt(plan.getUpdatedAt())
                .oldValue(previousPlan)
                .newValue(plan)
                .build();

            if (referenceType == Plan.PlanReferenceType.API_PRODUCT) {
                auditService.createApiProductAuditLog(executionContext, auditLogData, referenceId);
            } else {
                String apiId = plan.getApi() != null ? plan.getApi() : referenceId;
                auditService.createApiAuditLog(executionContext, auditLogData, apiId);
            }

            //reorder plan
            reorderedAndSavePlansAfterRemove(plan);

            return convert(plan);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to close plan: {}", planId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to close plan: %s", planId), ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String planId) {
        try {
            log.debug("Delete plan {}", planId);

            Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

            if (plan.getSecurity() != Plan.PlanSecurityType.KEY_LESS) {
                int subscriptions = subscriptionService.findByPlan(executionContext, planId).size();
                if ((plan.getStatus() == Plan.Status.PUBLISHED || plan.getStatus() == Plan.Status.DEPRECATED) && subscriptions > 0) {
                    throw new PlanWithSubscriptionsException(planId);
                }
            }

            // Delete plan and his flows
            flowService.save(FlowReferenceType.PLAN, planId, null);
            planRepository.delete(planId);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()))
                    .event(PLAN_DELETED)
                    .createdAt(new Date())
                    .oldValue(plan)
                    .newValue(null)
                    .build(),
                plan.getReferenceId()
            );

            //reorder plan
            reorderedAndSavePlansAfterRemove(plan);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to delete plan: {}", planId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to delete plan: %s", planId), ex);
        }
    }

    @Override
    public PlanEntity publish(final ExecutionContext executionContext, String planId) {
        try {
            log.debug("Publish plan {}", planId);

            Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
            Plan previousPlan = new Plan(plan);
            if (plan.getStatus() == Plan.Status.CLOSED) {
                throw new PlanAlreadyClosedException(planId);
            } else if (plan.getStatus() == Plan.Status.PUBLISHED) {
                throw new PlanAlreadyPublishedException(planId);
            } else if (plan.getStatus() == Plan.Status.DEPRECATED) {
                throw new PlanAlreadyDeprecatedException(planId);
            }

            checkStatusOfGeneralConditions(plan);

            Set<Plan> plans = planRepository.findByApi(plan.getReferenceId());
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
            var orderedPublishedPlans = plans
                .stream()
                .filter(plan1 -> Plan.Status.PUBLISHED.equals(plan1.getStatus()))
                .sorted(Comparator.comparingInt(Plan::getOrder))
                .toList();
            plan.setOrder(orderedPublishedPlans.isEmpty() ? 1 : (orderedPublishedPlans.getLast().getOrder() + 1));

            plan.setPublishedAt(new Date());
            plan.setUpdatedAt(plan.getPublishedAt());
            plan.setNeedRedeployAt(plan.getPublishedAt());

            // Save plan
            plan = planRepository.update(plan);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                AuditService.AuditLogData.builder()
                    .properties(Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()))
                    .event(PLAN_PUBLISHED)
                    .createdAt(plan.getUpdatedAt())
                    .oldValue(previousPlan)
                    .newValue(plan)
                    .build(),
                plan.getReferenceId()
            );

            return convert(plan);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to publish plan: {}", planId, ex);
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
            log.debug("Deprecate plan {}", planId);

            Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));
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
                AuditService.AuditLogData.builder()
                    .properties(Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()))
                    .event(PLAN_DEPRECATED)
                    .createdAt(plan.getUpdatedAt())
                    .oldValue(previousPlan)
                    .newValue(plan)
                    .build(),
                plan.getReferenceId()
            );

            return convert(plan);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to deprecate plan: {}", planId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to deprecate plan: %s", planId), ex);
        }
    }

    @Override
    public PlansConfigurationEntity getConfiguration() {
        PlansConfigurationEntity config = new PlansConfigurationEntity();
        config.setSecurity(DEFAULT_SECURITY_LIST);
        return config;
    }

    private Plan reorderAndSavePlans(final Plan planToReorder) throws TechnicalException {
        final Collection<Plan> plans = planRepository.findByApi(planToReorder.getReferenceId());
        Plan[] plansToReorder = plans
            .stream()
            .filter(p -> Plan.Status.PUBLISHED.equals(p.getStatus()) && !Objects.equals(p.getId(), planToReorder.getId()))
            .sorted(Comparator.comparingInt(Plan::getOrder))
            .toArray(Plan[]::new);

        // the new plan order must be between 1 && numbers of published apis
        if (planToReorder.getOrder() < 1) {
            planToReorder.setOrder(1);
        } else if (planToReorder.getOrder() > plansToReorder.length + 1) {
            // -1 because we have filtered the plan itself
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
            return planRepository.update(planToReorder);
        } catch (final TechnicalException ex) {
            log.error("An error occurs while trying to update plan {}", planToReorder.getId(), ex);
            throw new TechnicalManagementException("An error occurs while trying to update plan " + planToReorder.getId(), ex);
        }
    }

    private void reorderedAndSavePlansAfterRemove(final Plan planRemoved) throws TechnicalException {
        final Collection<Plan> plans = planRepository.findByApi(planRemoved.getReferenceId());
        plans
            .stream()
            .filter(p -> Plan.Status.PUBLISHED.equals(p.getStatus()))
            .sorted(Comparator.comparingInt(Plan::getOrder))
            .forEachOrdered(plan -> {
                try {
                    if (plan.getOrder() > planRemoved.getOrder()) {
                        planRepository.updateOrder(plan.getId(), plan.getOrder() - 1);
                    }
                } catch (final TechnicalException ex) {
                    log.error("An error occurs while trying to reorder plan {}", plan.getId(), ex);
                    throw new TechnicalManagementException("An error occurs while trying to update plan " + plan.getId(), ex);
                }
            });
    }

    private PlanEntity convert(Plan plan) {
        List<Flow> flows = flowService.findByReference(FlowReferenceType.PLAN, plan.getId());
        return planConverter.toPlanEntity(plan, flows);
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
        return planSearchService.anyPlanMismatchWithApi(planIds, apiId);
    }

    @Override
    public Map<String, Object> findByIdAsMap(String id) throws TechnicalException {
        return planSearchService.findByIdAsMap(id);
    }

    private DefinitionVersion getApiDefinitionVersion(Api api) {
        try {
            return objectMapper.readValue(api.getDefinition(), io.gravitee.definition.model.Api.class).getDefinitionVersion();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error("Unexpected error while reading API definition", e);
            return V2;
        }
    }

    private void validateTags(Set<String> tags, Api api) {
        this.tagsValidationService.validatePlanTagsAgainstApiTags(tags, api);
    }

    private Stream<PlanEntity> map(GenericPlanEntity entity) {
        return Stream.ofNullable(PlanAdapter.INSTANCE.map(entity));
    }
}
