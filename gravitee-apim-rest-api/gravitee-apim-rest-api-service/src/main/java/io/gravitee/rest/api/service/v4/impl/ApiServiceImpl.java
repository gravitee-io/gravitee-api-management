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

import static io.gravitee.repository.management.model.Api.AuditEvent.API_CREATED;
import static io.gravitee.repository.management.model.Api.AuditEvent.API_DELETED;
import static io.gravitee.repository.management.model.Api.AuditEvent.API_UPDATED;
import static io.gravitee.rest.api.model.WorkflowState.DRAFT;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.Logging;
import io.gravitee.definition.model.LoggingMode;
import io.gravitee.definition.model.v4.listener.Listener;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.NewApiMetadataEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.WorkflowReferenceType;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiNotDeletableException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApiRunningStateException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.impl.NotifierServiceImpl;
import io.gravitee.rest.api.service.impl.upgrade.DefaultMetadataUpgrader;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.PropertiesService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Component("ApiServiceImplV4")
public class ApiServiceImpl extends AbstractService implements ApiService {

    private final ApiRepository apiRepository;
    private final ApiMapper apiMapper;
    private final PrimaryOwnerService primaryOwnerService;
    private final ApiValidationService apiValidationService;
    private final ParameterService parameterService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final MembershipService membershipService;
    private final GenericNotificationConfigService genericNotificationConfigService;
    private final ApiMetadataService apiMetadataService;
    private final FlowService flowService;
    private final SearchEngineService searchEngineService;
    private final PlanService planService;
    private final PlanSearchService planSearchService;
    private final SubscriptionService subscriptionService;
    private final EventService eventService;
    private final PageService pageService;
    private final TopApiService topApiService;
    private final PortalNotificationConfigService portalNotificationConfigService;
    private final AlertService alertService;
    private final ApiQualityRuleRepository apiQualityRuleRepository;
    private final MediaService mediaService;
    private final PropertiesService propertiesService;
    private final ApiNotificationService apiNotificationService;

    public ApiServiceImpl(
        @Lazy final ApiRepository apiRepository,
        final ApiMapper apiMapper,
        final PrimaryOwnerService primaryOwnerService,
        final ApiValidationService apiValidationService,
        final ParameterService parameterService,
        final WorkflowService workflowService,
        final AuditService auditService,
        final MembershipService membershipService,
        final GenericNotificationConfigService genericNotificationConfigService,
        @Lazy final ApiMetadataService apiMetadataService,
        final FlowService flowService,
        @Lazy final SearchEngineService searchEngineService,
        final PlanService planService,
        final PlanSearchService planSearchService,
        @Lazy final SubscriptionService subscriptionService,
        final EventService eventService,
        @Lazy final PageService pageService,
        @Lazy final TopApiService topApiService,
        final PortalNotificationConfigService portalNotificationConfigService,
        @Lazy final AlertService alertService,
        @Lazy final ApiQualityRuleRepository apiQualityRuleRepository,
        final MediaService mediaService,
        final PropertiesService propertiesService,
        final ApiNotificationService apiNotificationService
    ) {
        this.apiRepository = apiRepository;
        this.apiMapper = apiMapper;
        this.primaryOwnerService = primaryOwnerService;
        this.apiValidationService = apiValidationService;
        this.parameterService = parameterService;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.membershipService = membershipService;
        this.genericNotificationConfigService = genericNotificationConfigService;
        this.apiMetadataService = apiMetadataService;
        this.flowService = flowService;
        this.searchEngineService = searchEngineService;
        this.planService = planService;
        this.planSearchService = planSearchService;
        this.subscriptionService = subscriptionService;
        this.eventService = eventService;
        this.pageService = pageService;
        this.topApiService = topApiService;
        this.portalNotificationConfigService = portalNotificationConfigService;
        this.alertService = alertService;
        this.apiQualityRuleRepository = apiQualityRuleRepository;
        this.mediaService = mediaService;
        this.propertiesService = propertiesService;
        this.apiNotificationService = apiNotificationService;
    }

    @Override
    public ApiEntity create(final ExecutionContext executionContext, final NewApiEntity newApiEntity, final String userId) {
        try {
            PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext, userId, null);

            apiValidationService.validateAndSanitizeNewApi(executionContext, newApiEntity, primaryOwner);

            Api repositoryApi = apiMapper.toRepository(executionContext, newApiEntity);
            repositoryApi.setApiLifecycleState(ApiLifecycleState.CREATED);
            if (parameterService.findAsBoolean(executionContext, Key.API_REVIEW_ENABLED, ParameterReferenceType.ENVIRONMENT)) {
                workflowService.create(WorkflowReferenceType.API, repositoryApi.getId(), REVIEW, userId, DRAFT, "");
            }

            Api createdApi = apiRepository.create(repositoryApi);

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                createdApi.getId(),
                Collections.emptyMap(),
                API_CREATED,
                createdApi.getCreatedAt(),
                null,
                createdApi
            );

            // Add the primary owner of the newly created API
            membershipService.addRoleToMemberOnReference(
                executionContext,
                new MembershipService.MembershipReference(MembershipReferenceType.API, createdApi.getId()),
                new MembershipService.MembershipMember(primaryOwner.getId(), null, MembershipMemberType.valueOf(primaryOwner.getType())),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            );

            // create the default mail notification
            final String emailMetadataValue = "${(api.primaryOwner.email)!''}";

            GenericNotificationConfigEntity notificationConfigEntity = new GenericNotificationConfigEntity();
            notificationConfigEntity.setName("Default Mail Notifications");
            notificationConfigEntity.setReferenceType(HookScope.API.name());
            notificationConfigEntity.setReferenceId(createdApi.getId());
            notificationConfigEntity.setHooks(Arrays.stream(ApiHook.values()).map(Enum::name).collect(toList()));
            notificationConfigEntity.setNotifier(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID);
            notificationConfigEntity.setConfig(emailMetadataValue);
            genericNotificationConfigService.create(notificationConfigEntity);

            // create the default mail support metadata
            NewApiMetadataEntity newApiMetadataEntity = new NewApiMetadataEntity();
            newApiMetadataEntity.setFormat(MetadataFormat.MAIL);
            newApiMetadataEntity.setName(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY);
            newApiMetadataEntity.setDefaultValue(emailMetadataValue);
            newApiMetadataEntity.setValue(emailMetadataValue);
            newApiMetadataEntity.setApiId(createdApi.getId());
            apiMetadataService.create(executionContext, newApiMetadataEntity);

            // create the API flows
            flowService.save(FlowReferenceType.API, createdApi.getId(), newApiEntity.getFlows());

            //TODO add membership log
            ApiEntity apiEntity = apiMapper.toEntity(executionContext, createdApi, primaryOwner, null, true);
            GenericApiEntity apiWithMetadata = apiMetadataService.fetchMetadataForApi(executionContext, apiEntity);

            searchEngineService.index(executionContext, apiWithMetadata, false);
            return apiEntity;
        } catch (TechnicalException | IllegalStateException ex) {
            String errorMsg = String.format("An error occurs while trying to create '%s' for user '%s'", newApiEntity, userId);
            log.error(errorMsg, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }
    }

    @Override
    public ApiEntity update(final ExecutionContext executionContext, final String apiId, final UpdateApiEntity api, final String userId) {
        return update(executionContext, apiId, api, false, userId);
    }

    @Override
    public ApiEntity update(
        final ExecutionContext executionContext,
        final String apiId,
        final UpdateApiEntity updateApiEntity,
        final boolean checkPlans,
        final String userId
    ) {
        try {
            PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext, userId, null);

            Api apiToUpdate = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
            final ApiEntity existingApiEntity = apiMapper.toEntity(executionContext, apiToUpdate, primaryOwner, null, false);

            apiValidationService.validateAndSanitizeUpdateApi(executionContext, updateApiEntity, primaryOwner, existingApiEntity);

            // TODO FCY
            // check HC inheritance
            // checkHealthcheckInheritance(updateApiEntity);

            // TODO FCY
            // validate HC cron schedule
            // validateHealtcheckSchedule(updateApiEntity);

            // TODO FCY: To be discussed, plans should be updated separately
            if (updateApiEntity.getPlans() == null) {
                updateApiEntity.setPlans(new HashSet<>());
            } else if (checkPlans) {
                Set<PlanEntity> existingPlans = existingApiEntity.getPlans();
                Map<String, PlanStatus> planStatuses = new HashMap<>();
                if (existingPlans != null && !existingPlans.isEmpty()) {
                    planStatuses.putAll(existingPlans.stream().collect(toMap(PlanEntity::getId, PlanEntity::getStatus)));
                }

                updateApiEntity
                    .getPlans()
                    .forEach(planToUpdate -> {
                        if (
                            !planStatuses.containsKey(planToUpdate.getId()) ||
                            (
                                planStatuses.containsKey(planToUpdate.getId()) &&
                                planStatuses.get(planToUpdate.getId()) == PlanStatus.CLOSED &&
                                planStatuses.get(planToUpdate.getId()) != planToUpdate.getStatus()
                            )
                        ) {
                            throw new InvalidDataException("Invalid status for plan '" + planToUpdate.getName() + "'");
                        }
                    });
            }

            // encrypt API properties
            this.propertiesService.encryptProperties(updateApiEntity.getProperties());

            if (io.gravitee.rest.api.model.api.ApiLifecycleState.DEPRECATED == updateApiEntity.getLifecycleState()) {
                planSearchService
                    .findByApi(executionContext, apiId)
                    .forEach(plan -> {
                        if (PlanStatus.PUBLISHED == plan.getPlanStatus() || PlanStatus.STAGING == plan.getPlanStatus()) {
                            planService.deprecate(executionContext, plan.getId(), true);
                            updateApiEntity
                                .getPlans()
                                .stream()
                                .filter(p -> p.getId().equals(plan.getId()))
                                .forEach(p -> p.setStatus(PlanStatus.DEPRECATED));
                        }
                    });
            }

            Api api = apiMapper.toRepository(executionContext, updateApiEntity);

            // Copy fields from existing values
            api.setEnvironmentId(apiToUpdate.getEnvironmentId());
            api.setDeployedAt(apiToUpdate.getDeployedAt());
            api.setCreatedAt(apiToUpdate.getCreatedAt());
            api.setLifecycleState(apiToUpdate.getLifecycleState());
            if (updateApiEntity.getCrossId() == null) {
                api.setCrossId(apiToUpdate.getCrossId());
            }

            // If no new picture and the current picture url is not the default one, keep the current picture
            if (
                updateApiEntity.getPicture() == null &&
                updateApiEntity.getPictureUrl() != null &&
                updateApiEntity.getPictureUrl().indexOf("?hash") > 0
            ) {
                api.setPicture(apiToUpdate.getPicture());
            }
            if (
                updateApiEntity.getBackground() == null &&
                updateApiEntity.getBackgroundUrl() != null &&
                updateApiEntity.getBackgroundUrl().indexOf("?hash") > 0
            ) {
                api.setBackground(apiToUpdate.getBackground());
            }
            if (updateApiEntity.getGroups() == null) {
                api.setGroups(apiToUpdate.getGroups());
            }
            if (updateApiEntity.getLabels() == null && apiToUpdate.getLabels() != null) {
                api.setLabels(new ArrayList<>(new HashSet<>(apiToUpdate.getLabels())));
            }
            if (updateApiEntity.getCategories() == null) {
                api.setCategories(apiToUpdate.getCategories());
            }

            if (ApiLifecycleState.DEPRECATED.equals(api.getApiLifecycleState())) {
                GenericApiEntity apiWithMetadata = apiMetadataService.fetchMetadataForApi(executionContext, existingApiEntity);
                apiNotificationService.triggerDeprecatedNotification(executionContext, apiWithMetadata);
            }

            Api updatedApi = apiRepository.update(api);

            // update API flows
            flowService.save(FlowReferenceType.API, api.getId(), updateApiEntity.getFlows());

            // update API plans
            updateApiEntity
                .getPlans()
                .forEach(plan -> {
                    plan.setApiId(api.getId());
                    planService.createOrUpdatePlan(executionContext, plan);
                });

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                updatedApi.getId(),
                Collections.emptyMap(),
                API_UPDATED,
                updatedApi.getUpdatedAt(),
                apiToUpdate,
                updatedApi
            );

            if (parameterService.findAsBoolean(executionContext, Key.LOGGING_AUDIT_TRAIL_ENABLED, ParameterReferenceType.ENVIRONMENT)) {
                Optional<Listener> firstUpdateHttpListener = updateApiEntity
                    .getListeners()
                    .stream()
                    .filter(listener -> ListenerType.HTTP == listener.getType())
                    .findFirst();
                Optional<Listener> firstExistingHttpListener = existingApiEntity
                    .getListeners()
                    .stream()
                    .filter(listener -> ListenerType.HTTP == listener.getType())
                    .findFirst();
                if (firstUpdateHttpListener.isPresent() && firstExistingHttpListener.isPresent()) {
                    HttpListener updateHttpListener = (HttpListener) firstUpdateHttpListener.get();
                    HttpListener existingHttpListener = (HttpListener) firstExistingHttpListener.get();
                    // Audit API logging if option is enabled
                    auditApiLogging(
                        executionContext,
                        updateApiEntity.getId(),
                        existingHttpListener.getLogging(),
                        updateHttpListener.getLogging()
                    );
                }
            }

            ApiEntity apiEntity = apiMapper.toEntity(executionContext, updatedApi, primaryOwner, null, true);
            GenericApiEntity apiWithMetadata = apiMetadataService.fetchMetadataForApi(executionContext, apiEntity);
            apiNotificationService.triggerUpdateNotification(executionContext, apiWithMetadata);

            searchEngineService.index(executionContext, apiWithMetadata, false);

            return apiEntity;
        } catch (TechnicalException ex) {
            String errorMsg = String.format("An error occurs while trying to update API '%s'", apiId);
            log.error(errorMsg, apiId, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }
    }

    @Override
    public void delete(ExecutionContext executionContext, String apiId, boolean closePlans) {
        try {
            log.debug("Delete API {}", apiId);

            Api api = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
            if (DefinitionContext.isManagement(api.getOrigin()) && api.getLifecycleState() == LifecycleState.STARTED) {
                throw new ApiRunningStateException(apiId);
            }

            Set<PlanEntity> plans = planService.findByApi(executionContext, apiId);
            if (closePlans) {
                plans =
                    plans
                        .stream()
                        .filter(plan -> plan.getStatus() != PlanStatus.CLOSED)
                        .map(plan -> planService.close(executionContext, plan.getId()))
                        .collect(Collectors.toSet());
            }

            Set<String> plansNotClosed = plans
                .stream()
                .filter(plan -> plan.getStatus() == PlanStatus.PUBLISHED)
                .map(PlanEntity::getName)
                .collect(toSet());

            if (!plansNotClosed.isEmpty()) {
                throw new ApiNotDeletableException(plansNotClosed);
            }

            Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApi(executionContext, apiId);
            subscriptions.forEach(sub -> subscriptionService.delete(executionContext, sub.getId()));

            // Delete plans
            plans.forEach(plan -> planService.delete(executionContext, plan.getId()));

            // Delete flows
            flowService.save(FlowReferenceType.API, apiId, null);

            // Delete events
            eventService.deleteApiEvents(apiId);

            // https://github.com/gravitee-io/issues/issues/4130
            // Ensure we are sending a last UNPUBLISH_API event because the gateway couldn't be aware that the API (and
            // all its relative events) have been deleted.
            Map<String, String> properties = new HashMap<>(2);
            properties.put(Event.EventProperties.API_ID.getValue(), apiId);
            if (getAuthenticatedUser() != null) {
                properties.put(Event.EventProperties.USER.getValue(), getAuthenticatedUser().getUsername());
            }
            eventService.createApiEvent(
                executionContext,
                singleton(executionContext.getEnvironmentId()),
                EventType.UNPUBLISH_API,
                null,
                properties
            );

            // Delete pages
            pageService.deleteAllByApi(executionContext, apiId);

            // Delete top API
            topApiService.delete(executionContext, apiId);
            // Delete API
            apiRepository.delete(apiId);
            // Delete memberships
            membershipService.deleteReference(executionContext, MembershipReferenceType.API, apiId);
            // Delete notifications
            genericNotificationConfigService.deleteReference(NotificationReferenceType.API, apiId);
            portalNotificationConfigService.deleteReference(NotificationReferenceType.API, apiId);
            // Delete alerts
            final List<AlertTriggerEntity> alerts = alertService.findByReferenceWithEventCounts(AlertReferenceType.API, apiId);
            alerts.forEach(alert -> alertService.delete(alert.getId(), alert.getReferenceId()));
            // delete all reference on api quality rule
            apiQualityRuleRepository.deleteByApi(apiId);
            // Audit
            auditService.createApiAuditLog(executionContext, apiId, Collections.emptyMap(), API_DELETED, new Date(), api, null);
            // remove from search engine
            searchEngineService.delete(executionContext, apiMapper.toEntity(executionContext, api, null, null, false));

            mediaService.deleteAllByApi(apiId);

            apiMetadataService.deleteAllByApi(executionContext, apiId);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to delete API " + apiId, ex);
        }
    }

    private void auditApiLogging(ExecutionContext executionContext, String apiId, Logging loggingToUpdate, Logging loggingUpdated) {
        try {
            // no changes for logging configuration, continue
            if (
                loggingToUpdate == loggingUpdated ||
                (
                    loggingToUpdate != null &&
                    loggingUpdated != null &&
                    Objects.equals(loggingToUpdate.getMode(), loggingUpdated.getMode()) &&
                    Objects.equals(loggingToUpdate.getCondition(), loggingUpdated.getCondition())
                )
            ) {
                return;
            }

            // determine the audit event type
            Api.AuditEvent auditEvent;
            if (
                (loggingToUpdate == null || loggingToUpdate.getMode().equals(LoggingMode.NONE)) &&
                (loggingUpdated != null && !loggingUpdated.getMode().equals(LoggingMode.NONE))
            ) {
                auditEvent = Api.AuditEvent.API_LOGGING_ENABLED;
            } else if (
                (loggingToUpdate != null && !loggingToUpdate.getMode().equals(LoggingMode.NONE)) &&
                (loggingUpdated == null || loggingUpdated.getMode().equals(LoggingMode.NONE))
            ) {
                auditEvent = Api.AuditEvent.API_LOGGING_DISABLED;
            } else {
                auditEvent = Api.AuditEvent.API_LOGGING_UPDATED;
            }

            // Audit
            auditService.createApiAuditLog(
                executionContext,
                apiId,
                Collections.emptyMap(),
                auditEvent,
                new Date(),
                loggingToUpdate,
                loggingUpdated
            );
        } catch (Exception ex) {
            String errorMsg = String.format("An error occurs while auditing API logging configuration for API:  %s", apiId);
            log.error(errorMsg, apiId, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }
    }
}
