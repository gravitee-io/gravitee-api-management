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

import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.Origin;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.NewApiMetadataEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.WorkflowReferenceType;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.alert.AlertTriggerEntity;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.model.notification.GenericNotificationConfigEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.settings.ApiPrimaryOwnerMode;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.ApiNotDeletableException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApiRunningStateException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TagNotAllowedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.impl.NotifierServiceImpl;
import io.gravitee.rest.api.service.impl.upgrade.initializer.DefaultMetadataInitializer;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiAuthorizationService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.PropertiesService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
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
    private final GenericApiMapper genericApiMapper;
    private final PrimaryOwnerService primaryOwnerService;
    private final ApiValidationService apiValidationService;
    private final ParameterService parameterService;
    private final WorkflowService workflowService;
    private final AuditService auditService;
    private final MembershipService membershipService;
    private final GenericNotificationConfigService genericNotificationConfigService;
    private final ApiMetadataService apiMetadataService;
    private final FlowCrudService flowCrudService;
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
    private final TagsValidationService tagsValidationService;
    private final ApiAuthorizationService apiAuthorizationService;
    private final GroupService groupService;
    private final ApiCategoryService apiCategoryService;

    private static final String EMAIL_METADATA_VALUE = "${(api.primaryOwner.email)!''}";
    private static final String EXPAND_PRIMARY_OWNER = "primaryOwner";

    public ApiServiceImpl(
        @Lazy final ApiRepository apiRepository,
        final ApiMapper apiMapper,
        final GenericApiMapper genericApiMapper,
        final PrimaryOwnerService primaryOwnerService,
        final ApiValidationService apiValidationService,
        final ParameterService parameterService,
        final WorkflowService workflowService,
        final AuditService auditService,
        final MembershipService membershipService,
        final GenericNotificationConfigService genericNotificationConfigService,
        @Lazy final ApiMetadataService apiMetadataService,
        final FlowCrudService flowCrudService,
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
        final ApiNotificationService apiNotificationService,
        final TagsValidationService tagsValidationService,
        final ApiAuthorizationService apiAuthorizationService,
        final GroupService groupService,
        ApiCategoryService apiCategoryService
    ) {
        this.apiRepository = apiRepository;
        this.apiMapper = apiMapper;
        this.genericApiMapper = genericApiMapper;
        this.primaryOwnerService = primaryOwnerService;
        this.apiValidationService = apiValidationService;
        this.parameterService = parameterService;
        this.workflowService = workflowService;
        this.auditService = auditService;
        this.membershipService = membershipService;
        this.genericNotificationConfigService = genericNotificationConfigService;
        this.apiMetadataService = apiMetadataService;
        this.flowCrudService = flowCrudService;
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
        this.tagsValidationService = tagsValidationService;
        this.apiAuthorizationService = apiAuthorizationService;
        this.groupService = groupService;
        this.apiCategoryService = apiCategoryService;
    }

    @Override
    public ApiEntity createWithImport(final ExecutionContext executionContext, final ApiEntity apiEntity, final String userId) {
        String id = apiEntity.getId() != null && !apiEntity.getId().isEmpty() ? apiEntity.getId() : UuidString.generateRandom();
        apiEntity.setId(id);

        log.debug("Importing API {}", id);
        try {
            apiRepository
                .findById(id)
                .ifPresent(action -> {
                    throw new ApiAlreadyExistsException(id);
                });
        } catch (TechnicalException ex) {
            String errorMsg = String.format("An error occurs while trying to check if 'id' %s is already used", id);
            log.error(errorMsg, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }

        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext, userId, apiEntity.getPrimaryOwner());
        apiValidationService.validateAndSanitizeImportApiForCreation(executionContext, apiEntity, primaryOwner);

        Api repositoryApi = apiMapper.toRepository(executionContext, apiEntity);
        repositoryApi.setEnvironmentId(executionContext.getEnvironmentId());
        // Set date fields
        repositoryApi.setCreatedAt(new Date());
        repositoryApi.setUpdatedAt(repositoryApi.getCreatedAt());

        repositoryApi.setApiLifecycleState(ApiLifecycleState.CREATED);
        if (apiEntity.getOriginContext() instanceof OriginContext.Management) {
            repositoryApi.setLifecycleState(LifecycleState.STOPPED);
        } else {
            repositoryApi.setLifecycleState(LifecycleState.valueOf(apiEntity.getState().name()));
        }
        // Make sure visibility is PRIVATE by default if not set.
        repositoryApi.setVisibility(
            apiEntity.getVisibility() == null ? Visibility.PRIVATE : Visibility.valueOf(apiEntity.getVisibility().toString())
        );

        // Add Default groups
        Set<String> defaultGroups = groupService
            .findByEvent(executionContext.getEnvironmentId(), GroupEvent.API_CREATE)
            .stream()
            .map(GroupEntity::getId)
            .collect(toSet());
        if (repositoryApi.getGroups() == null) {
            repositoryApi.setGroups(defaultGroups.isEmpty() ? null : defaultGroups);
        } else {
            repositoryApi.getGroups().addAll(defaultGroups);
        }

        // if po is a group, add it as a member of the API
        if (ApiPrimaryOwnerMode.GROUP.name().equals(primaryOwner.getType())) {
            if (repositoryApi.getGroups() == null) {
                repositoryApi.setGroups(new HashSet<>());
            }
            repositoryApi.getGroups().add(primaryOwner.getId());
        }

        if (parameterService.findAsBoolean(executionContext, Key.API_REVIEW_ENABLED, ParameterReferenceType.ENVIRONMENT)) {
            workflowService.create(WorkflowReferenceType.API, id, REVIEW, userId, DRAFT, "");
        }

        Api createdApi;
        try {
            createdApi = apiRepository.create(repositoryApi);
            log.debug("API {} imported", createdApi.getId());
        } catch (TechnicalException ex) {
            String errorMsg = String.format("An error occurs while trying to create '%s' for user '%s'", apiEntity, userId);
            log.error(errorMsg, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }

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
        addPrimaryOwnerToCreatedApi(executionContext, primaryOwner, createdApi);

        // create the default mail notification
        createDefaultMailNotification(createdApi);

        // create the default mail support metadata
        createDefaultSupportEmailMetadata(executionContext, createdApi);

        // create the API flows
        flowCrudService.saveApiFlows(createdApi.getId(), apiEntity.getFlows());

        // create Api Category Order entries
        apiCategoryService.addApiToCategories(createdApi.getId(), createdApi.getCategories());

        ApiEntity createdApiEntity = apiMapper.toEntity(executionContext, createdApi, primaryOwner, true);
        GenericApiEntity apiWithMetadata = apiMetadataService.fetchMetadataForApi(executionContext, createdApiEntity);

        searchEngineService.index(executionContext, apiWithMetadata, false);
        return createdApiEntity;
    }

    private void createDefaultSupportEmailMetadata(ExecutionContext executionContext, Api createdApi) {
        NewApiMetadataEntity newApiMetadataEntity = new NewApiMetadataEntity();
        newApiMetadataEntity.setFormat(MetadataFormat.MAIL);
        newApiMetadataEntity.setName(DefaultMetadataInitializer.METADATA_EMAIL_SUPPORT_KEY);
        newApiMetadataEntity.setDefaultValue(EMAIL_METADATA_VALUE);
        newApiMetadataEntity.setValue(EMAIL_METADATA_VALUE);
        newApiMetadataEntity.setApiId(createdApi.getId());
        apiMetadataService.create(executionContext, newApiMetadataEntity);
    }

    private void createDefaultMailNotification(Api createdApi) {
        GenericNotificationConfigEntity notificationConfigEntity = new GenericNotificationConfigEntity();
        notificationConfigEntity.setName("Default Mail Notifications");
        notificationConfigEntity.setReferenceType(HookScope.API.name());
        notificationConfigEntity.setReferenceId(createdApi.getId());
        notificationConfigEntity.setHooks(Arrays.stream(ApiHook.values()).map(Enum::name).collect(toList()));
        notificationConfigEntity.setNotifier(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID);
        notificationConfigEntity.setConfig(EMAIL_METADATA_VALUE);
        genericNotificationConfigService.create(notificationConfigEntity);
    }

    private void addPrimaryOwnerToCreatedApi(ExecutionContext executionContext, PrimaryOwnerEntity primaryOwner, Api createdApi) {
        membershipService.addRoleToMemberOnReference(
            executionContext,
            new MembershipService.MembershipReference(MembershipReferenceType.API, createdApi.getId()),
            new MembershipService.MembershipMember(primaryOwner.getId(), null, MembershipMemberType.valueOf(primaryOwner.getType())),
            new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
        );
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
            final ApiEntity existingApiEntity = apiMapper.toEntity(executionContext, apiToUpdate, primaryOwner, false);

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

                        try {
                            tagsValidationService.validatePlanTagsAgainstApiTags(planToUpdate.getTags(), updateApiEntity.getTags());
                        } catch (TagNotAllowedException e) {
                            final var missingTags = planToUpdate
                                .getTags()
                                .stream()
                                .filter(tag -> !updateApiEntity.getTags().contains(tag))
                                .toList();
                            throw new InvalidDataException(
                                "Sharding tags " + missingTags + " used by plan '" + planToUpdate.getName() + "'"
                            );
                        }
                    });
            }

            // encrypt API properties
            if (updateApiEntity.getProperties() != null) {
                updateApiEntity.setProperties(this.propertiesService.encryptProperties(updateApiEntity.getProperties()));
            }

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
            api.setOrigin(apiToUpdate.getOrigin());
            api.setMode(apiToUpdate.getMode());

            if (updateApiEntity.getCrossId() == null) {
                api.setCrossId(apiToUpdate.getCrossId());
            }

            // Keep existing picture as picture update has dedicated service
            api.setPicture(apiToUpdate.getPicture());
            api.setBackground(apiToUpdate.getBackground());

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
            flowCrudService.saveApiFlows(api.getId(), updateApiEntity.getFlows());

            // update API plans
            updateApiEntity
                .getPlans()
                .forEach(plan -> {
                    plan.setApiId(api.getId());
                    planService.createOrUpdatePlan(executionContext, plan);
                });

            // update Api Category Order entities
            apiCategoryService.updateApiCategories(updatedApi.getId(), updatedApi.getCategories());

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
                Logging existingApiLogging = null;
                if (existingApiEntity.getAnalytics() != null) {
                    existingApiLogging = existingApiEntity.getAnalytics().getLogging();
                }
                Logging updateApiLogging = null;
                if (updateApiEntity.getAnalytics() != null) {
                    updateApiLogging = updateApiEntity.getAnalytics().getLogging();
                }
                // Audit API logging if option is enabled
                auditApiLogging(executionContext, updateApiEntity.getId(), existingApiLogging, updateApiLogging);
            }

            ApiEntity apiEntity = apiMapper.toEntity(executionContext, updatedApi, primaryOwner, true);
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

    /**
     * Delete an Api regardless of version
     *
     * @param executionContext
     * @param apiId
     * @param closePlans
     */
    @Override
    public void delete(ExecutionContext executionContext, String apiId, boolean closePlans) {
        try {
            log.debug("Delete API {}", apiId);

            Api api = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));
            if (DefinitionContext.isManagement(api.getOrigin()) && api.getLifecycleState() == LifecycleState.STARTED) {
                throw new ApiRunningStateException(apiId);
            }

            Set<GenericPlanEntity> plans = planSearchService.findByApi(executionContext, apiId);
            if (closePlans) {
                plans =
                    plans
                        .stream()
                        .filter(plan -> plan.getPlanStatus() != PlanStatus.CLOSED)
                        .map(plan -> planService.close(executionContext, plan.getId()))
                        .collect(Collectors.toSet());
            }

            Set<String> plansNotClosed = plans
                .stream()
                .filter(plan -> plan.getPlanStatus() == PlanStatus.PUBLISHED)
                .map(GenericPlanEntity::getName)
                .collect(toSet());

            if (!plansNotClosed.isEmpty()) {
                throw new ApiNotDeletableException(plansNotClosed);
            }

            Collection<SubscriptionEntity> subscriptions = subscriptionService.findByApi(executionContext, apiId);
            subscriptions.forEach(sub -> subscriptionService.delete(executionContext, sub.getId()));

            // Delete plans
            plans.forEach(plan -> planService.delete(executionContext, plan.getId()));

            // Delete flows
            flowCrudService.saveApiFlows(apiId, null);

            // Delete events
            eventService.deleteApiEvents(apiId);

            // https://github.com/gravitee-io/issues/issues/4130
            // Ensure we are sending a last UNPUBLISH_API event because the gateway couldn't be aware that the API (and
            // all its relative events) have been deleted.
            Map<String, String> properties = new HashMap<>(2);
            if (getAuthenticatedUser() != null) {
                properties.put(Event.EventProperties.USER.getValue(), getAuthenticatedUser().getUsername());
            }

            if (!Origin.KUBERNETES.name().equalsIgnoreCase(api.getSyncFrom())) {
                eventService.createApiEvent(
                    executionContext,
                    singleton(executionContext.getEnvironmentId()),
                    executionContext.getOrganizationId(),
                    EventType.UNPUBLISH_API,
                    apiId,
                    properties
                );
            }

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

            // Delete Api Category Order entries
            apiCategoryService.deleteApiFromCategories(apiId);

            // Delete alerts
            final List<AlertTriggerEntity> alerts = alertService.findByReferenceWithEventCounts(AlertReferenceType.API, apiId);
            alerts.forEach(alert -> alertService.delete(alert.getId(), alert.getReferenceId()));
            // delete all reference on api quality rule
            apiQualityRuleRepository.deleteByApi(apiId);
            // Audit
            auditService.createApiAuditLog(executionContext, apiId, Collections.emptyMap(), API_DELETED, new Date(), api, null);
            // remove from search engine
            searchEngineService.delete(executionContext, apiMapper.toEntity(executionContext, api, null, false));

            mediaService.deleteAllByApi(apiId);

            apiMetadataService.deleteAllByApi(executionContext, apiId);
        } catch (TechnicalException ex) {
            throw new TechnicalManagementException("An error occurs while trying to delete API " + apiId, ex);
        }
    }

    @Override
    public Page<GenericApiEntity> findAll(
        final ExecutionContext executionContext,
        final String userId,
        final boolean isAdmin,
        final Set<String> expands,
        final Pageable pageable
    ) {
        ApiCriteria.Builder criteria = new ApiCriteria.Builder().environmentId(executionContext.getEnvironmentId());

        // If user is not admin, get list of apiIds in their scope and add it to the criteria
        if (!isAdmin) {
            Set<String> userApiIds = apiAuthorizationService.findApiIdsByUserId(executionContext, userId, null, true);

            // User has no associated apis
            if (userApiIds.isEmpty()) {
                return new Page<>(List.of(), 0, 0, 0);
            }
            criteria.ids(userApiIds);
        }

        Page<Api> apis = apiRepository.search(
            criteria.build(),
            null,
            convert(pageable),
            new ApiFieldFilter.Builder().excludePicture().build()
        );

        return apis
            .getContent()
            .stream()
            .map(api -> {
                PrimaryOwnerEntity primaryOwner = null;

                if (expands != null && expands.contains(EXPAND_PRIMARY_OWNER)) {
                    primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext.getOrganizationId(), api.getId());
                }

                return genericApiMapper.toGenericApi(api, primaryOwner);
            })
            .collect(
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    apiEntityList -> new Page<>(apiEntityList, apis.getPageNumber(), (int) apis.getPageElements(), apis.getTotalElements())
                )
            );
    }

    @Override
    public Page<GenericApiEntity> findAll(
        final ExecutionContext executionContext,
        final String userId,
        final boolean isAdmin,
        final Pageable pageable
    ) {
        return this.findAll(executionContext, userId, isAdmin, Collections.emptySet(), pageable);
    }

    @Override
    public Optional<ApiEntity> findByEnvironmentIdAndCrossId(String environment, String crossId) {
        try {
            return apiRepository.findByEnvironmentIdAndCrossId(environment, crossId).map(api -> apiMapper.toEntity(api, null));
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurred while finding API by environment " + environment + " and crossId " + crossId,
                e
            );
        }
    }

    private void auditApiLogging(ExecutionContext executionContext, String apiId, Logging existingLogging, Logging updatedLogging) {
        try {
            // no changes for logging configuration, continue
            if (
                existingLogging == updatedLogging ||
                (
                    existingLogging != null &&
                    updatedLogging != null &&
                    Objects.equals(existingLogging.getMode(), updatedLogging.getMode()) &&
                    Objects.equals(existingLogging.getContent(), updatedLogging.getContent()) &&
                    Objects.equals(existingLogging.getPhase(), updatedLogging.getPhase()) &&
                    Objects.equals(existingLogging.getCondition(), updatedLogging.getCondition())
                )
            ) {
                return;
            }

            // determine the audit event type
            Api.AuditEvent auditEvent;
            if (
                (existingLogging == null || !existingLogging.getMode().isEnabled()) &&
                (updatedLogging != null && updatedLogging.getMode().isEnabled())
            ) {
                auditEvent = Api.AuditEvent.API_LOGGING_ENABLED;
            } else if (
                (existingLogging != null && existingLogging.getMode().isEnabled()) &&
                (updatedLogging == null || !updatedLogging.getMode().isEnabled())
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
                existingLogging,
                updatedLogging
            );
        } catch (Exception ex) {
            String errorMsg = String.format("An error occurs while auditing API logging configuration for API:  %s", apiId);
            log.error(errorMsg, apiId, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }
    }
}
