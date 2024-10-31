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

import static io.gravitee.repository.management.model.ApiLifecycleState.DEPRECATED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_CLOSED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_CREATED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_DELETED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_DEPRECATED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_PUBLISHED;
import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_UPDATED;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.plan.PlanMode;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.Plan;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PlanSecurityEntity;
import io.gravitee.rest.api.model.PlansConfigurationEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.v4.nativeapi.NativePlanEntity;
import io.gravitee.rest.api.model.v4.plan.BasePlanEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.NewPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanSecurityType;
import io.gravitee.rest.api.model.v4.plan.UpdatePlanEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiDeprecatedException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.KeylessPlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyDeprecatedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanFlowRequiredException;
import io.gravitee.rest.api.service.exceptions.PlanGeneralConditionStatusException;
import io.gravitee.rest.api.service.exceptions.PlanInvalidException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.PlanNotYetPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanWithSubscriptionsException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotClosableException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedPlanSecurityTypeException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.mapper.GenericPlanMapper;
import io.gravitee.rest.api.service.v4.mapper.PlanMapper;
import io.gravitee.rest.api.service.v4.validation.FlowValidationService;
import io.gravitee.rest.api.service.v4.validation.PathParametersValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component("PlanServiceImplV4")
public class PlanServiceImpl extends AbstractService implements PlanService {

    private static final List<PlanSecurityEntity> DEFAULT_SECURITY_LIST = Collections.unmodifiableList(
        Arrays.asList(
            new PlanSecurityEntity("mtls", "mTLS", "mtls"),
            new PlanSecurityEntity("oauth2", "OAuth2", "oauth2"),
            new PlanSecurityEntity("jwt", "JWT", "'jwt'"),
            new PlanSecurityEntity("api_key", "API Key", "api-key"),
            new PlanSecurityEntity("key_less", "Keyless (public)", "")
        )
    );
    private final Logger logger = LoggerFactory.getLogger(PlanServiceImpl.class);

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
    private PlanMapper planMapper;

    @Autowired
    private GenericPlanMapper genericPlanMapper;

    @Autowired
    private FlowService flowService;

    @Autowired
    private FlowCrudService flowCrudService;

    @Autowired
    private TagsValidationService tagsValidationService;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private FlowValidationService flowValidationService;

    @Autowired
    private PathParametersValidationService pathParametersValidationService;

    @Override
    public PlanEntity findById(final ExecutionContext executionContext, final String planId) {
        return (PlanEntity) planSearchService.findById(executionContext, planId);
    }

    @Override
    public Set<PlanEntity> findByApi(final ExecutionContext executionContext, final String api) {
        return planSearchService.findByApi(executionContext, api).stream().map(PlanEntity.class::cast).collect(Collectors.toSet());
    }

    @Override
    public Set<NativePlanEntity> findNativePlansByApi(final ExecutionContext executionContext, final String api) {
        return planSearchService.findByApi(executionContext, api).stream().map(NativePlanEntity.class::cast).collect(Collectors.toSet());
    }

    private PlanEntity create(ExecutionContext executionContext, NewPlanEntity newPlan, boolean validatePathParams) {
        try {
            logger.debug("Create a new plan {} for API {}", newPlan.getName(), newPlan.getApiId());

            if (Optional.ofNullable(newPlan.getMode()).orElse(PlanMode.STANDARD) == PlanMode.STANDARD) {
                if (newPlan.getSecurity() == null) {
                    throw new PlanInvalidException("Security type is required for plan with 'STANDARD' mode");
                }
                assertPlanSecurityIsAllowed(executionContext, newPlan.getSecurity().getType());
                validatePlanSecurity(newPlan.getSecurity().getType(), newPlan.getSecurity().getConfiguration());
            } else if (newPlan.getSecurity() != null) {
                throw new PlanInvalidException("Security type is forbidden for plan with 'Push' mode");
            }

            Api api = apiRepository.findById(newPlan.getApiId()).orElseThrow(() -> new ApiNotFoundException(newPlan.getApiId()));

            newPlan.setFlows(flowValidationService.validateAndSanitize(api.getType(), newPlan.getFlows()));

            if (api.getApiLifecycleState() == DEPRECATED) {
                throw new ApiDeprecatedException(api.getName());
            }

            validateTags(newPlan.getTags(), api);
            if (validatePathParams) {
                validatePathParameters(api, newPlan.getFlows());
            }

            String id = newPlan.getId() != null && UUID.fromString(newPlan.getId()) != null ? newPlan.getId() : UuidString.generateRandom();
            newPlan.setId(id);

            Plan plan = planMapper.toRepository(newPlan, api);
            plan.setEnvironmentId(executionContext.getEnvironmentId());
            plan = planRepository.create(plan);

            flowCrudService.savePlanFlows(plan.getId(), newPlan.getFlows());

            auditService.createApiAuditLog(
                executionContext,
                newPlan.getApiId(),
                Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()),
                PLAN_CREATED,
                plan.getCreatedAt(),
                null,
                plan
            );
            return mapToEntity(plan);
        } catch (TechnicalException ex) {
            String errorMsg = String.format(
                "An error occurs while trying to create a plan %s for API %s",
                newPlan.getName(),
                newPlan.getApiId()
            );
            logger.error(errorMsg, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }
    }

    private void validatePathParameters(Api api, List<Flow> newPlanFlows) throws TechnicalException {
        final Set<Plan> plans = planRepository.findByApi(api.getId());
        final Stream<Flow> apiFlows = flowService.findByReference(FlowReferenceType.API, api.getId()).stream();

        Stream<Flow> planFlows = plans
            .stream()
            .map(plan -> flowService.findByReference(FlowReferenceType.PLAN, plan.getId()))
            .flatMap(Collection::stream);
        planFlows = Stream.concat(planFlows, newPlanFlows.stream());

        pathParametersValidationService.validate(api.getType(), apiFlows, planFlows);
    }

    private void validateTags(Set<String> tags, Api api) {
        this.tagsValidationService.validatePlanTagsAgainstApiTags(tags, api);
    }

    @Override
    public PlanEntity createOrUpdatePlan(final ExecutionContext executionContext, PlanEntity planEntity) {
        PlanEntity resultPlanEntity;
        try {
            if (planEntity.getId() == null) {
                // No need to validate again the path param in this case
                resultPlanEntity = create(executionContext, planMapper.toNewPlanEntity(planEntity), false);
            } else {
                planSearchService.findById(executionContext, planEntity.getId());
                // No need to validate again the path param in this case
                resultPlanEntity = update(executionContext, planMapper.toUpdatePlanEntity(planEntity), false);
            }
        } catch (PlanNotFoundException npe) {
            // No need to validate again the path param in this case
            resultPlanEntity = create(executionContext, planMapper.toNewPlanEntity(planEntity), false);
        }
        return resultPlanEntity;
    }

    @Override
    public PlanEntity update(final ExecutionContext executionContext, UpdatePlanEntity updatePlan) {
        return update(executionContext, updatePlan, true);
    }

    private PlanEntity update(final ExecutionContext executionContext, UpdatePlanEntity updatePlan, boolean validatePathParams) {
        try {
            logger.debug("Update plan {}", updatePlan.getName());

            Plan oldPlan = planRepository.findById(updatePlan.getId()).orElseThrow(() -> new PlanNotFoundException(updatePlan.getId()));
            if (Optional.ofNullable(oldPlan.getMode()).orElse(Plan.PlanMode.STANDARD) == Plan.PlanMode.STANDARD) {
                assertPlanSecurityIsAllowed(executionContext, PlanSecurityType.valueOf(oldPlan.getSecurity().name()).getLabel());
                validatePlanSecurity(
                    PlanSecurityType.valueOf(oldPlan.getSecurity().name()).getLabel(),
                    updatePlan.getSecurity().getConfiguration()
                );
            }

            if (updatePlan.getFlows() == null) {
                throw new PlanFlowRequiredException(updatePlan.getId());
            }

            Plan newPlan = new Plan();
            //copy immutable values
            newPlan.setDefinitionVersion(oldPlan.getDefinitionVersion());
            newPlan.setId(oldPlan.getId());
            newPlan.setDefinitionVersion(oldPlan.getDefinitionVersion());
            newPlan.setSecurity(oldPlan.getSecurity());
            newPlan.setType(oldPlan.getType());
            newPlan.setStatus(oldPlan.getStatus());
            newPlan.setOrder(oldPlan.getOrder());
            newPlan.setApi(oldPlan.getApi());
            newPlan.setCreatedAt(oldPlan.getCreatedAt());
            newPlan.setPublishedAt(oldPlan.getPublishedAt());
            newPlan.setClosedAt(oldPlan.getClosedAt());
            newPlan.setMode(oldPlan.getMode());
            newPlan.setApiType(oldPlan.getApiType());
            // for existing plans, needRedeployAt doesn't exist. We have to initialize it
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
            if (updatePlan.getSecurity() != null) {
                newPlan.setSecurityDefinition(updatePlan.getSecurity().getConfiguration());
            } else {
                newPlan.setSecurityDefinition(null);
            }
            newPlan.setCommentRequired(updatePlan.isCommentRequired());
            newPlan.setCommentMessage(updatePlan.getCommentMessage());
            newPlan.setTags(updatePlan.getTags());
            newPlan.setSelectionRule(updatePlan.getSelectionRule());
            newPlan.setGeneralConditions(updatePlan.getGeneralConditions());

            if (Plan.Status.PUBLISHED.equals(newPlan.getStatus()) || Plan.Status.DEPRECATED.equals(newPlan.getStatus())) {
                checkStatusOfGeneralConditions(newPlan);
            }

            newPlan.setExcludedGroups(updatePlan.getExcludedGroups());

            if (newPlan.getSecurity() == Plan.PlanSecurityType.KEY_LESS) {
                // There is no need for a validation when authentication is KEY_LESS, force to AUTO
                newPlan.setValidation(Plan.PlanValidationType.AUTO);
            } else {
                newPlan.setValidation(Plan.PlanValidationType.valueOf(updatePlan.getValidation().name()));
            }

            newPlan.setCharacteristics(updatePlan.getCharacteristics());

            final var apiId = newPlan.getApi();
            Api api = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
            validateTags(newPlan.getTags(), api);
            updatePlan.setFlows(flowValidationService.validateAndSanitize(api.getType(), updatePlan.getFlows()));

            if (validatePathParams) {
                validatePathParameters(api, updatePlan);
            }

            // if order change, reorder all pages
            if (newPlan.getOrder() != updatePlan.getOrder()) {
                newPlan.setOrder(updatePlan.getOrder());
                reorderAndSavePlans(newPlan);
                return null;
            } else {
                PlanEntity oldPlanEntity = mapToEntity(oldPlan);

                flowCrudService.savePlanFlows(updatePlan.getId(), updatePlan.getFlows());
                PlanEntity newPlanEntity = mapToEntity(newPlan);

                if (
                    !synchronizationService.checkSynchronization(PlanEntity.class, oldPlanEntity, newPlanEntity) ||
                    !synchronizationService.checkSynchronization(BasePlanEntity.class, oldPlanEntity, newPlanEntity)
                ) {
                    newPlan.setNeedRedeployAt(newPlan.getUpdatedAt());
                }
                newPlan = planRepository.update(newPlan);

                auditService.createApiAuditLog(
                    executionContext,
                    apiId,
                    Collections.singletonMap(Audit.AuditProperties.PLAN, newPlan.getId()),
                    PLAN_UPDATED,
                    newPlan.getUpdatedAt(),
                    oldPlan,
                    newPlan
                );

                return mapToEntity(newPlan);
            }
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to update plan {}", updatePlan.getName(), ex);
            throw new TechnicalManagementException(
                String.format("An error occurs while trying to update plan %s", updatePlan.getName()),
                ex
            );
        }
    }

    private void validatePathParameters(Api api, UpdatePlanEntity updatePlan) throws TechnicalException {
        final Set<Plan> plans = planRepository.findByApi(api.getId());
        final Stream<Flow> apiFlows = flowService.findByReference(FlowReferenceType.API, api.getId()).stream();

        Stream<Flow> planFlows = plans
            .stream()
            .map(plan -> {
                if (plan.getId().equals(updatePlan.getId())) {
                    return updatePlan.getFlows();
                } else {
                    return flowService.findByReference(FlowReferenceType.PLAN, plan.getId());
                }
            })
            .flatMap(Collection::stream);

        pathParametersValidationService.validate(api.getType(), apiFlows, planFlows);
    }

    private void checkStatusOfGeneralConditions(Plan plan) {
        if (plan.getGeneralConditions() != null && !plan.getGeneralConditions().isEmpty()) {
            PageEntity generalConditions = pageService.findById(plan.getGeneralConditions());
            if (!generalConditions.isPublished()) {
                throw new PlanGeneralConditionStatusException(plan.getName());
            }
        }
    }

    /**
     * Close a plan regardless of version
     *
     * @param executionContext
     * @param planId
     * @return
     */
    @Override
    public GenericPlanEntity close(final ExecutionContext executionContext, String planId) {
        try {
            logger.debug("Close plan {}", planId);

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
                var auditInfo = AuditInfo
                    .builder()
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
            auditService.createApiAuditLog(
                executionContext,
                plan.getApi(),
                Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()),
                PLAN_CLOSED,
                plan.getUpdatedAt(),
                previousPlan,
                plan
            );

            //reorder plan
            reorderedAndSavePlansAfterRemove(plan);

            String apiId = plan.getApi();
            Api api = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

            return genericPlanMapper.toGenericPlan(api, plan);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to close plan: {}", planId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to close plan: %s", planId), ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String planId) {
        try {
            logger.debug("Delete plan {}", planId);

            Plan plan = planRepository.findById(planId).orElseThrow(() -> new PlanNotFoundException(planId));

            if (plan.getSecurity() != Plan.PlanSecurityType.KEY_LESS) {
                int subscriptions = subscriptionService.findByPlan(executionContext, planId).size();
                if ((plan.getStatus() == Plan.Status.PUBLISHED || plan.getStatus() == Plan.Status.DEPRECATED) && subscriptions > 0) {
                    throw new PlanWithSubscriptionsException(planId);
                }
            }

            // Delete plan and his flows
            flowCrudService.savePlanFlows(planId, null);
            planRepository.delete(planId);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                plan.getApi(),
                Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()),
                PLAN_DELETED,
                new Date(),
                plan,
                null
            );

            //reorder plan
            reorderedAndSavePlansAfterRemove(plan);
        } catch (TechnicalException ex) {
            logger.error("An error occurs while trying to delete plan: {}", planId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to delete plan: %s", planId), ex);
        }
    }

    @Override
    public PlanEntity publish(final ExecutionContext executionContext, String planId) {
        try {
            logger.debug("Publish plan {}", planId);

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
                Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()),
                PLAN_PUBLISHED,
                plan.getUpdatedAt(),
                previousPlan,
                plan
            );

            return mapToEntity(plan);
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
                plan.getApi(),
                Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()),
                PLAN_DEPRECATED,
                plan.getUpdatedAt(),
                previousPlan,
                plan
            );

            return mapToEntity(plan);
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

    private PlanEntity mapToEntity(final Plan plan) {
        List<Flow> flows = flowService.findByReference(FlowReferenceType.PLAN, plan.getId());
        return planMapper.toEntity(plan, flows);
    }

    private void assertPlanSecurityIsAllowed(final ExecutionContext executionContext, String securityType) {
        Key securityKey;
        PlanSecurityType planSecurityType = PlanSecurityType.valueOfLabel(securityType);
        switch (planSecurityType) {
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
            throw new UnauthorizedPlanSecurityTypeException(io.gravitee.rest.api.model.PlanSecurityType.valueOf(planSecurityType.name()));
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

    private void validatePlanSecurity(String type, String configuration) {
        policyService.validatePolicyConfiguration(type, configuration);
    }
}
