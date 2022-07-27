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
package io.gravitee.rest.api.service.v4.impl;

import static io.gravitee.repository.management.model.Api.AuditEvent.API_CREATED;
import static io.gravitee.repository.management.model.Api.AuditEvent.API_DELETED;
import static io.gravitee.rest.api.model.WorkflowState.DRAFT;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import io.gravitee.definition.model.DefinitionVersion;
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
import io.gravitee.rest.api.model.v4.api.IndexableApi;
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
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.ApiNotDeletableException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApiRunningStateException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.AbstractService;
import io.gravitee.rest.api.service.impl.NotifierServiceImpl;
import io.gravitee.rest.api.service.impl.upgrade.DefaultMetadataUpgrader;
import io.gravitee.rest.api.service.notification.ApiHook;
import io.gravitee.rest.api.service.notification.HookScope;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.IndexableApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final IndexableApiMapper indexableApiMapper;
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
    private final SubscriptionService subscriptionService;
    private final EventService eventService;
    private final PageService pageService;
    private final TopApiService topApiService;
    private final GenericNotificationConfigService portalNotificationConfigService;
    private final AlertService alertService;
    private final ApiQualityRuleRepository apiQualityRuleRepository;
    private final MediaService mediaService;

    @Autowired
    public ApiServiceImpl(
        @Lazy final ApiRepository apiRepository,
        final ApiMapper apiMapper,
        final IndexableApiMapper indexableApiMapper,
        final PrimaryOwnerService primaryOwnerService,
        final ApiValidationService apiValidationService,
        final ParameterService parameterService,
        final WorkflowService workflowService,
        final AuditService auditService,
        final MembershipService membershipService,
        final GenericNotificationConfigService genericNotificationConfigService,
        final ApiMetadataService apiMetadataService,
        final FlowService flowService,
        final SearchEngineService searchEngineService,
        final PlanService planService,
        final SubscriptionService subscriptionService,
        final EventService eventService,
        final PageService pageService,
        final TopApiService topApiService,
        final GenericNotificationConfigService portalNotificationConfigService,
        final AlertService alertService,
        @Lazy final ApiQualityRuleRepository apiQualityRuleRepository,
        final MediaService mediaService
    ) {
        this.apiRepository = apiRepository;
        this.apiMapper = apiMapper;
        this.indexableApiMapper = indexableApiMapper;
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
        this.subscriptionService = subscriptionService;
        this.eventService = eventService;
        this.pageService = pageService;
        this.topApiService = topApiService;
        this.portalNotificationConfigService = portalNotificationConfigService;
        this.alertService = alertService;
        this.apiQualityRuleRepository = apiQualityRuleRepository;
        this.mediaService = mediaService;
    }

    @Override
    public ApiEntity findById(final ExecutionContext executionContext, final String apiId) {
        final Api api = this.findApiById(executionContext, apiId);
        if (api.getDefinitionVersion() != DefinitionVersion.V4) {
            throw new IllegalArgumentException(
                String.format("Api found doesn't support v%s definition model.", DefinitionVersion.V4.getLabel())
            );
        }

        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext, api.getId());

        return apiMapper.toEntity(executionContext, api, primaryOwner, null, true);
    }

    @Override
    public IndexableApi findIndexableApiById(final ExecutionContext executionContext, final String apiId) {
        final Api api = this.findApiById(executionContext, apiId);
        PrimaryOwnerEntity primaryOwner = primaryOwnerService.getPrimaryOwner(executionContext, api.getId());
        return indexableApiMapper.toGenericApi(api, primaryOwner);
    }

    @Override
    public Optional<String> findApiIdByEnvironmentIdAndCrossId(final String environment, final String crossId) {
        try {
            return apiRepository.findIdByEnvironmentIdAndCrossId(environment, crossId);
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(
                "An error occurred while finding API by environment " + environment + " and crossId " + crossId,
                e
            );
        }
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
            IndexableApi apiWithMetadata = apiMetadataService.fetchMetadataForApi(executionContext, apiEntity);

            searchEngineService.index(executionContext, apiWithMetadata, false);
            return apiEntity;
        } catch (TechnicalException | IllegalStateException ex) {
            String errorMsg = String.format("An error occurs while trying to create '%s' for user '%s'", newApiEntity, userId);
            log.error(errorMsg, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }
    }

    @Override
    public ApiEntity update(final ExecutionContext executionContext, final String apiId, final UpdateApiEntity api) {
        return null;
    }

    @Override
    public ApiEntity update(
        final ExecutionContext executionContext,
        final String apiId,
        final UpdateApiEntity api,
        final boolean checkPlans
    ) {
        return null;
    }

    @Override
    public void delete(final ExecutionContext executionContext, final String apiId) {
        try {
            log.debug("Delete API {}", apiId);

            Api api = apiRepository.findById(apiId).orElseThrow(() -> new ApiNotFoundException(apiId));

            if (api.getLifecycleState() == LifecycleState.STARTED) {
                throw new ApiRunningStateException(apiId);
            } else {
                // Delete plans
                Set<PlanEntity> plans = planService.findByApi(executionContext, apiId);
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

                plans.forEach(plan -> planService.delete(executionContext, plan.getId()));

                // Delete flows
                flowService.save(FlowReferenceType.API, apiId, null);

                // Delete events
                final EventQuery query = new EventQuery();
                query.setApi(apiId);
                eventService.search(executionContext, query).forEach(event -> eventService.delete(event.getId()));

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
            }
        } catch (TechnicalException ex) {
            String errorMsg = String.format("An error occurs while trying to delete API '%s'", apiId);
            log.error(errorMsg, apiId, ex);
            throw new TechnicalManagementException(errorMsg, ex);
        }
    }

    @Override
    public boolean exists(final String apiId) {
        try {
            return apiRepository.existById(apiId);
        } catch (final TechnicalException te) {
            final String msg = "An error occurs while checking if the API exists: " + apiId;
            log.error(msg, te);
            throw new TechnicalManagementException(msg, te);
        }
    }

    private Api findApiById(final ExecutionContext executionContext, final String apiId) {
        try {
            log.debug("Find API by ID: {}", apiId);

            Optional<Api> optApi = apiRepository.findById(apiId);

            if (executionContext.hasEnvironmentId()) {
                optApi = optApi.filter(result -> result.getEnvironmentId().equals(executionContext.getEnvironmentId()));
            }

            return optApi.orElseThrow(() -> new ApiNotFoundException(apiId));
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to find an API using its ID: {}", apiId, ex);
            throw new TechnicalManagementException("An error occurs while trying to find an API using its ID: " + apiId, ex);
        }
    }
}
