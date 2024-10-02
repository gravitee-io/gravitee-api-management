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
package io.gravitee.rest.api.service.cockpit.command.handler;

import io.gravitee.apim.core.access_point.crud_service.AccessPointCrudService;
import io.gravitee.apim.core.access_point.model.AccessPoint;
import io.gravitee.apim.core.scoring.model.ScoringRuleset;
import io.gravitee.apim.core.workflow.model.Workflow;
import io.gravitee.cockpit.api.command.v1.CockpitCommandType;
import io.gravitee.cockpit.api.command.v1.environment.DeleteEnvironmentCommand;
import io.gravitee.cockpit.api.command.v1.environment.DeleteEnvironmentReply;
import io.gravitee.exchange.api.command.CommandHandler;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.AccessPointRepository;
import io.gravitee.repository.management.api.ApiCategoryOrderRepository;
import io.gravitee.repository.management.api.ApiHeaderRepository;
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.AsyncJobRepository;
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.CategoryRepository;
import io.gravitee.repository.management.api.CommandRepository;
import io.gravitee.repository.management.api.CustomUserFieldsRepository;
import io.gravitee.repository.management.api.DashboardRepository;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.api.GenericNotificationConfigRepository;
import io.gravitee.repository.management.api.GroupRepository;
import io.gravitee.repository.management.api.IdentityProviderActivationRepository;
import io.gravitee.repository.management.api.IntegrationRepository;
import io.gravitee.repository.management.api.InvitationRepository;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.PageRevisionRepository;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.PortalMenuLinkRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.api.RatingAnswerRepository;
import io.gravitee.repository.management.api.RatingRepository;
import io.gravitee.repository.management.api.RoleRepository;
import io.gravitee.repository.management.api.ScoringReportRepository;
import io.gravitee.repository.management.api.ScoringRulesetRepository;
import io.gravitee.repository.management.api.SharedPolicyGroupHistoryRepository;
import io.gravitee.repository.management.api.SharedPolicyGroupRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.ThemeRepository;
import io.gravitee.repository.management.api.WorkflowRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import io.gravitee.repository.management.model.DashboardReferenceType;
import io.gravitee.repository.management.model.InvitationReferenceType;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.repository.management.model.RatingReferenceType;
import io.gravitee.repository.management.model.RoleReferenceType;
import io.gravitee.repository.management.model.ThemeReferenceType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApplicationAlertService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.exceptions.EnvironmentNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.reactivex.rxjava3.core.Single;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeleteEnvironmentCommandHandler implements CommandHandler<DeleteEnvironmentCommand, DeleteEnvironmentReply> {

    private final AccessPointCrudService accessPointService;
    private final AccessPointRepository accessPointRepository;
    private final AlertService alertService;
    private final ApiCategoryOrderRepository apiCategoryOrderRepository;
    private final ApiHeaderRepository apiHeaderRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiQualityRuleRepository apiQualityRuleRepository;
    private final ApiRepository apiRepository;
    private final ApiStateService apiStateService;
    private final ApplicationAlertService applicationAlertService;
    private final ApplicationRepository applicationRepository;
    private final AuditRepository auditRepository;
    private final CategoryRepository categoryRepository;
    private final CommandRepository commandRepository;
    private final CustomUserFieldsRepository customUserFieldsRepository;
    private final DashboardRepository dashboardRepository;
    private final DictionaryRepository dictionaryRepository;
    private final DictionaryService dictionaryService;
    private final EnvironmentService environmentService;
    private final EventService eventService;
    private final FlowRepository flowRepository;
    private final GenericNotificationConfigRepository genericNotificationConfigRepository;
    private final GroupRepository groupRepository;
    private final IdentityProviderActivationRepository identityProviderActivationRepository;
    private final IdentityProviderActivationService identityProviderActivationService;
    private final AsyncJobRepository asyncJobRepository;
    private final IntegrationRepository integrationRepository;
    private final InvitationRepository invitationRepository;
    private final MediaRepository mediaRepository;
    private final MembershipRepository membershipRepository;
    private final MetadataRepository metadataRepository;
    private final PageRepository pageRepository;
    private final PageRevisionRepository pageRevisionRepository;
    private final ParameterRepository parameterRepository;
    private final PlanRepository planRepository;
    private final PortalMenuLinkRepository portalMenuLinkRepository;
    private final PortalNotificationConfigRepository portalNotificationConfigRepository;
    private final RatingAnswerRepository ratingAnswerRepository;
    private final RatingRepository ratingRepository;
    private final RoleRepository roleRepository;
    private final ScoringReportRepository scoringReportRepository;
    private final ScoringRulesetRepository scoringRulesetRepository;
    private final SearchEngineService searchEngineService;
    private final SharedPolicyGroupRepository sharedPolicyGroupRepository;
    private final SharedPolicyGroupHistoryRepository sharedPolicyGroupHistoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ThemeRepository themeRepository;
    private final WorkflowRepository workflowRepository;

    public DeleteEnvironmentCommandHandler(
        @Lazy AccessPointRepository accessPointRepository,
        @Lazy ApiCategoryOrderRepository apiCategoryOrderRepository,
        @Lazy ApiHeaderRepository apiHeaderRepository,
        @Lazy ApiKeyRepository apiKeyRepository,
        @Lazy ApiQualityRuleRepository apiQualityRuleRepository,
        @Lazy ApiRepository apiRepository,
        @Lazy ApplicationRepository applicationRepository,
        @Lazy AsyncJobRepository asyncJobRepository,
        @Lazy AuditRepository auditRepository,
        @Lazy CategoryRepository categoryRepository,
        @Lazy CommandRepository commandRepository,
        @Lazy CustomUserFieldsRepository customUserFieldsRepository,
        @Lazy DashboardRepository dashboardRepository,
        @Lazy DictionaryRepository dictionaryRepository,
        @Lazy FlowRepository flowRepository,
        @Lazy GenericNotificationConfigRepository genericNotificationConfigRepository,
        @Lazy GroupRepository groupRepository,
        @Lazy IdentityProviderActivationRepository identityProviderActivationRepository,
        @Lazy IntegrationRepository integrationRepository,
        @Lazy InvitationRepository invitationRepository,
        @Lazy MediaRepository mediaRepository,
        @Lazy MembershipRepository membershipRepository,
        @Lazy MetadataRepository metadataRepository,
        @Lazy PageRepository pageRepository,
        @Lazy PageRevisionRepository pageRevisionRepository,
        @Lazy ParameterRepository parameterRepository,
        @Lazy PlanRepository planRepository,
        @Lazy PortalMenuLinkRepository portalMenuLinkRepository,
        @Lazy PortalNotificationConfigRepository portalNotificationConfigRepository,
        @Lazy RatingAnswerRepository ratingAnswerRepository,
        @Lazy RatingRepository ratingRepository,
        @Lazy RoleRepository roleRepository,
        @Lazy ScoringReportRepository scoringReportRepository,
        @Lazy ScoringRulesetRepository scoringRulesetRepository,
        @Lazy SharedPolicyGroupRepository sharedPolicyGroupRepository,
        @Lazy SharedPolicyGroupHistoryRepository sharedPolicyGroupHistoryRepository,
        @Lazy SubscriptionRepository subscriptionRepository,
        @Lazy ThemeRepository themeRepository,
        @Lazy WorkflowRepository workflowRepository,
        AccessPointCrudService accessPointService,
        AlertService alertService,
        ApiStateService apiStateService,
        ApplicationAlertService applicationAlertService,
        DictionaryService dictionaryService,
        EnvironmentService environmentService,
        EventService eventService,
        IdentityProviderActivationService identityProviderActivationService,
        SearchEngineService searchEngineService
    ) {
        this.accessPointRepository = accessPointRepository;
        this.accessPointService = accessPointService;
        this.alertService = alertService;
        this.apiCategoryOrderRepository = apiCategoryOrderRepository;
        this.apiHeaderRepository = apiHeaderRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiQualityRuleRepository = apiQualityRuleRepository;
        this.apiRepository = apiRepository;
        this.apiStateService = apiStateService;
        this.applicationAlertService = applicationAlertService;
        this.applicationRepository = applicationRepository;
        this.auditRepository = auditRepository;
        this.categoryRepository = categoryRepository;
        this.commandRepository = commandRepository;
        this.customUserFieldsRepository = customUserFieldsRepository;
        this.dashboardRepository = dashboardRepository;
        this.dictionaryRepository = dictionaryRepository;
        this.dictionaryService = dictionaryService;
        this.environmentService = environmentService;
        this.eventService = eventService;
        this.flowRepository = flowRepository;
        this.genericNotificationConfigRepository = genericNotificationConfigRepository;
        this.groupRepository = groupRepository;
        this.identityProviderActivationRepository = identityProviderActivationRepository;
        this.identityProviderActivationService = identityProviderActivationService;
        this.asyncJobRepository = asyncJobRepository;
        this.integrationRepository = integrationRepository;
        this.invitationRepository = invitationRepository;
        this.mediaRepository = mediaRepository;
        this.membershipRepository = membershipRepository;
        this.metadataRepository = metadataRepository;
        this.pageRepository = pageRepository;
        this.pageRevisionRepository = pageRevisionRepository;
        this.parameterRepository = parameterRepository;
        this.planRepository = planRepository;
        this.portalMenuLinkRepository = portalMenuLinkRepository;
        this.portalNotificationConfigRepository = portalNotificationConfigRepository;
        this.ratingAnswerRepository = ratingAnswerRepository;
        this.ratingRepository = ratingRepository;
        this.roleRepository = roleRepository;
        this.scoringReportRepository = scoringReportRepository;
        this.scoringRulesetRepository = scoringRulesetRepository;
        this.searchEngineService = searchEngineService;
        this.sharedPolicyGroupRepository = sharedPolicyGroupRepository;
        this.sharedPolicyGroupHistoryRepository = sharedPolicyGroupHistoryRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.themeRepository = themeRepository;
        this.workflowRepository = workflowRepository;
    }

    @Override
    public String supportType() {
        return CockpitCommandType.DELETE_ENVIRONMENT.name();
    }

    @Override
    public Single<DeleteEnvironmentReply> handle(DeleteEnvironmentCommand command) {
        var payload = command.getPayload();

        try {
            log.info("Delete environment with id [{}]", payload.cockpitId());
            var environment = environmentService.findByCockpitId(payload.cockpitId());
            var executionContext = new ExecutionContext(environment.getOrganizationId(), environment.getId());

            disableEnvironment(executionContext, payload.userId());
            deleteEnvironment(executionContext, environment);

            log.info("Environment [{}] with id [{}] has been deleted.", environment.getName(), environment.getId());
            return Single.just(new DeleteEnvironmentReply(command.getId()));
        } catch (EnvironmentNotFoundException e) {
            log.warn("Environment with cockpitId [{}] has not been found.", payload.cockpitId());
            return Single.just(new DeleteEnvironmentReply(command.getId()));
        } catch (Exception e) {
            String errorDetails = "Error occurred when deleting environment with id [%s].".formatted(payload.id());
            log.error(errorDetails, e);
            return Single.just(new DeleteEnvironmentReply(command.getId(), errorDetails));
        }
    }

    private void disableEnvironment(ExecutionContext executionContext, String userId) {
        // Stop all Environment APIs
        apiRepository
            .search(
                new ApiCriteria.Builder().state(LifecycleState.STARTED).environmentId(executionContext.getEnvironmentId()).build(),
                new ApiFieldFilter.Builder().excludeDefinition().excludePicture().build()
            )
            .forEach(api -> apiStateService.stop(executionContext, api.getId(), userId));

        // Delete related access points
        this.accessPointService.deleteAccessPoints(AccessPoint.ReferenceType.ENVIRONMENT, executionContext.getEnvironmentId());

        this.dictionaryService.findAll(executionContext)
            .forEach(dictionaryEntity -> dictionaryService.stop(executionContext, dictionaryEntity.getId()));

        // Deactivate all identity providers
        this.identityProviderActivationService.removeAllIdpsFromTarget(
                executionContext,
                new IdentityProviderActivationService.ActivationTarget(
                    executionContext.getEnvironmentId(),
                    IdentityProviderActivationReferenceType.ENVIRONMENT
                )
            );
    }

    private void deleteEnvironment(ExecutionContext executionContext, EnvironmentEntity environment) throws TechnicalException {
        deleteApis(executionContext);
        deleteApplications(executionContext);
        deletePages(executionContext, PageReferenceType.ENVIRONMENT, environment.getId());
        subscriptionRepository.deleteByEnvironmentId(environment.getId());
        apiKeyRepository.deleteByEnvironmentId(environment.getId());
        planRepository
            .deleteByEnvironmentId(executionContext.getEnvironmentId())
            .forEach(planId -> {
                try {
                    flowRepository.deleteByReferenceIdAndReferenceType(planId, FlowReferenceType.PLAN);
                } catch (TechnicalException e) {
                    throw new TechnicalManagementException(e);
                }
            });
        alertService
            .findByReference(AlertReferenceType.ENVIRONMENT, environment.getId())
            .forEach(alert -> alertService.delete(alert.getId(), environment.getId()));

        apiHeaderRepository.deleteByEnvironmentId(environment.getId());
        accessPointRepository.deleteByReferenceIdAndReferenceType(environment.getId(), AccessPointReferenceType.ENVIRONMENT);
        parameterRepository.deleteByReferenceIdAndReferenceType(environment.getId(), ParameterReferenceType.ENVIRONMENT);
        portalMenuLinkRepository.deleteByEnvironmentId(environment.getId());
        customUserFieldsRepository.deleteByReferenceIdAndReferenceType(environment.getId(), CustomUserFieldReferenceType.ENVIRONMENT);
        groupRepository
            .deleteByEnvironmentId(environment.getId())
            .forEach(groupId -> {
                try {
                    membershipRepository.deleteByReferenceIdAndReferenceType(groupId, MembershipReferenceType.GROUP);
                    invitationRepository.deleteByReferenceIdAndReferenceType(groupId, InvitationReferenceType.GROUP);
                } catch (TechnicalException e) {
                    throw new TechnicalManagementException(e);
                }
            });

        membershipRepository.deleteByReferenceIdAndReferenceType(environment.getId(), MembershipReferenceType.ENVIRONMENT);
        roleRepository.deleteByReferenceIdAndReferenceType(environment.getId(), RoleReferenceType.ENVIRONMENT);
        categoryRepository.deleteByEnvironmentId(environment.getId());
        dashboardRepository.deleteByReferenceIdAndReferenceType(environment.getId(), DashboardReferenceType.ENVIRONMENT);
        dictionaryRepository.deleteByEnvironmentId(environment.getId());
        eventService.deleteOrUpdateEventsByEnvironment(environment.getId());
        scoringRulesetRepository.deleteByReferenceId(environment.getId(), ScoringRuleset.ReferenceType.ENVIRONMENT.name());

        // Always default for environment
        portalNotificationConfigRepository.deleteByReferenceIdAndReferenceType(environment.getId(), NotificationReferenceType.ENVIRONMENT);
        genericNotificationConfigRepository.deleteByReferenceIdAndReferenceType(environment.getId(), NotificationReferenceType.ENVIRONMENT);
        sharedPolicyGroupRepository.deleteByEnvironmentId(environment.getId());
        sharedPolicyGroupHistoryRepository.deleteByEnvironmentId(environment.getId());
        themeRepository.deleteByReferenceIdAndReferenceType(environment.getId(), ThemeReferenceType.ENVIRONMENT);
        identityProviderActivationRepository.deleteByReferenceIdAndReferenceType(
            environment.getId(),
            io.gravitee.repository.management.model.IdentityProviderActivationReferenceType.ENVIRONMENT
        );
        commandRepository.deleteByEnvironmentId(environment.getId());
        integrationRepository.deleteByEnvironmentId(environment.getId());
        asyncJobRepository.deleteByEnvironmentId(environment.getId());
        mediaRepository.deleteByEnvironment(environment.getId());
        metadataRepository.deleteByReferenceIdAndReferenceType(environment.getId(), MetadataReferenceType.ENVIRONMENT);
        environmentService.delete(environment.getId());
        auditRepository.deleteByReferenceIdAndReferenceType(environment.getId(), Audit.AuditReferenceType.ENVIRONMENT);
    }

    private void deleteApis(ExecutionContext executionContext) throws TechnicalException {
        apiRepository
            .deleteByEnvironmentId(executionContext.getEnvironmentId())
            .forEach(apiId -> {
                alertService.findByReference(AlertReferenceType.API, apiId).forEach(alert -> alertService.delete(alert.getId(), apiId));
                eventService.deleteApiEvents(apiId);
                searchEngineService.delete(executionContext, ApiEntity.builder().id(apiId).build());
                deletePages(executionContext, PageReferenceType.API, apiId);

                try {
                    apiCategoryOrderRepository.deleteByApiId(apiId);
                    apiQualityRuleRepository.deleteByApi(apiId);
                    auditRepository.deleteByReferenceIdAndReferenceType(apiId, Audit.AuditReferenceType.API);
                    flowRepository.deleteByReferenceIdAndReferenceType(apiId, FlowReferenceType.API);
                    genericNotificationConfigRepository.deleteByReferenceIdAndReferenceType(apiId, NotificationReferenceType.API);
                    invitationRepository.deleteByReferenceIdAndReferenceType(apiId, InvitationReferenceType.API);
                    mediaRepository.deleteAllByApi(apiId);
                    membershipRepository.deleteByReferenceIdAndReferenceType(apiId, MembershipReferenceType.API);
                    metadataRepository.deleteByReferenceIdAndReferenceType(apiId, MetadataReferenceType.API);
                    portalNotificationConfigRepository.deleteByReferenceIdAndReferenceType(apiId, NotificationReferenceType.API);
                    scoringReportRepository.deleteByApi(apiId);
                    workflowRepository.deleteByReferenceIdAndReferenceType(apiId, Workflow.ReferenceType.API.name());
                    deleteRatings(apiId, RatingReferenceType.API);
                } catch (TechnicalException e) {
                    throw new TechnicalManagementException(e);
                }
            });
    }

    private void deleteRatings(String referenceId, RatingReferenceType referenceType) throws TechnicalException {
        ratingRepository
            .deleteByReferenceIdAndReferenceType(referenceId, referenceType)
            .forEach(ratingId -> {
                try {
                    ratingAnswerRepository.deleteByRating(ratingId);
                } catch (TechnicalException e) {
                    throw new TechnicalManagementException(e);
                }
            });
    }

    private void deletePages(ExecutionContext executionContext, PageReferenceType referenceType, String id) {
        try {
            pageRepository
                .deleteByReferenceIdAndReferenceType(id, referenceType)
                .forEach((pageId, mediaHash) -> {
                    searchEngineService.delete(executionContext, PageEntity.builder().id(pageId).build());
                    try {
                        pageRevisionRepository.deleteByPageId(pageId);
                    } catch (TechnicalException e) {
                        throw new TechnicalManagementException(e);
                    }
                });
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private void deleteApplications(ExecutionContext executionContext) throws TechnicalException {
        applicationRepository
            .deleteByEnvironmentId(executionContext.getEnvironmentId())
            .forEach(applicationId -> {
                applicationAlertService.deleteAll(applicationId);
                try {
                    genericNotificationConfigRepository.deleteByReferenceIdAndReferenceType(
                        applicationId,
                        NotificationReferenceType.APPLICATION
                    );
                    invitationRepository.deleteByReferenceIdAndReferenceType(applicationId, InvitationReferenceType.APPLICATION);
                    membershipRepository.deleteByReferenceIdAndReferenceType(applicationId, MembershipReferenceType.APPLICATION);
                    metadataRepository.deleteByReferenceIdAndReferenceType(applicationId, MetadataReferenceType.APPLICATION);
                    portalNotificationConfigRepository.deleteByReferenceIdAndReferenceType(
                        applicationId,
                        NotificationReferenceType.APPLICATION
                    );
                    workflowRepository.deleteByReferenceIdAndReferenceType(applicationId, Workflow.ReferenceType.APPLICATION.name());
                    auditRepository.deleteByReferenceIdAndReferenceType(applicationId, Audit.AuditReferenceType.APPLICATION);
                } catch (TechnicalException e) {
                    throw new TechnicalManagementException(e);
                }
            });
    }
}
