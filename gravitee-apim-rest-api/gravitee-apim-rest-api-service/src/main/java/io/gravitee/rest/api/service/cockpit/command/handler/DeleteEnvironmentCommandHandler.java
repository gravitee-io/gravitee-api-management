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
import io.gravitee.repository.management.api.AuditRepository;
import io.gravitee.repository.management.api.FlowRepository;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.api.PageRepository;
import io.gravitee.repository.management.api.ParameterRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.PortalNotificationConfigRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.AccessPointReferenceType;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.PageReferenceType;
import io.gravitee.repository.management.model.ParameterReferenceType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.repository.media.api.MediaRepository;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.configuration.identity.IdentityProviderActivationReferenceType;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApplicationAlertService;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.configuration.dictionary.DictionaryService;
import io.gravitee.rest.api.service.configuration.identity.IdentityProviderActivationService;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteEnvironmentCommandHandler implements CommandHandler<DeleteEnvironmentCommand, DeleteEnvironmentReply> {

    @Lazy
    private final AccessPointRepository accessPointRepository;

    @Lazy
    private final ApiRepository apiRepository;

    @Lazy
    private final ApiHeaderRepository apiHeaderRepository;

    @Lazy
    private final ApplicationRepository applicationRepository;

    @Lazy
    private final SubscriptionRepository subscriptionRepository;

    @Lazy
    private final ApiKeyRepository apiKeyRepository;

    @Lazy
    private final PlanRepository planRepository;

    @Lazy
    private final FlowRepository flowRepository;

    @Lazy
    private final PageRepository pageRepository;

    @Lazy
    private final ParameterRepository parameterRepository;

    @Lazy
    private final PortalNotificationConfigRepository portalNotificationConfigRepository;

    @Lazy
    private final ApiCategoryOrderRepository apiCategoryOrderRepository;

    @Lazy
    private final ApiQualityRuleRepository apiQualityRuleRepository;

    @Lazy
    private final AuditRepository auditRepository;

    @Lazy
    private final MediaRepository mediaRepository;

    @Lazy
    private final MetadataRepository metadataRepository;

    private final AlertService alertService;
    private final EnvironmentService environmentService;
    private final ApiStateService apiStateService;
    private final EventService eventService;
    private final AccessPointCrudService accessPointService;
    private final IdentityProviderActivationService identityProviderActivationService;
    private final DictionaryService dictionaryService;
    private final GenericNotificationConfigService genericNotificationConfigService;
    private final MembershipService membershipService;
    private final ApplicationAlertService applicationAlertService;
    private final SearchEngineService searchEngineService;

    @Override
    public String supportType() {
        return CockpitCommandType.DELETE_ENVIRONMENT.name();
    }

    @Override
    public Single<DeleteEnvironmentReply> handle(DeleteEnvironmentCommand command) {
        var payload = command.getPayload();

        try {
            var environment = environmentService.findByCockpitId(payload.cockpitId());
            var executionContext = new ExecutionContext(environment.getOrganizationId(), environment.getId());

            disableEnvironment(executionContext, payload.userId());
            deleteEnvironment(executionContext, environment);

            log.info("Environment [{}] with id [{}] has been deleted.", environment.getName(), environment.getId());
            return Single.just(new DeleteEnvironmentReply(command.getId()));
        } catch (Exception e) {
            String errorDetails = "Error occurred when deleting environment [%s] with id [%s].".formatted(payload.name(), payload.id());
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
        subscriptionRepository.deleteByEnvironment(environment.getId());
        apiKeyRepository.deleteByEnvironment(environment.getId());

        alertService
            .findByReference(AlertReferenceType.ENVIRONMENT, environment.getId())
            .forEach(alert -> alertService.delete(alert.getId(), environment.getId()));

        pageRepository.deleteByReferenceIdAndReferenceType(PageReferenceType.ENVIRONMENT, environment.getId());
        apiHeaderRepository.deleteByEnvironment(environment.getId());
        accessPointRepository.deleteByReference(AccessPointReferenceType.ENVIRONMENT, environment.getId());
        parameterRepository.deleteByReferenceIdAndReferenceType(ParameterReferenceType.ENVIRONMENT, environment.getId());
        auditRepository.deleteByReferenceIdAndReferenceType(Audit.AuditReferenceType.ENVIRONMENT, environment.getId());
        environmentService.delete(environment.getId());
        log.info("Environment [{}] with id [{}] has been deleted.", environment.getName(), environment.getId());
        //TODO: remove users
    }

    private void deleteApis(ExecutionContext executionContext) throws TechnicalException {
        apiRepository
            .deleteByEnvironment(executionContext.getEnvironmentId())
            .forEach(apiId -> {
                membershipService.deleteReference(executionContext, MembershipReferenceType.API, apiId);
                genericNotificationConfigService.deleteReference(NotificationReferenceType.API, apiId);
                eventService.deleteApiEvents(apiId);

                alertService.findByReference(AlertReferenceType.API, apiId).forEach(alert -> alertService.delete(alert.getId(), apiId));

                searchEngineService.delete(executionContext, ApiEntity.builder().id(apiId).build());

                deletePages(executionContext, PageReferenceType.API, apiId);

                try {
                    portalNotificationConfigRepository.deleteReference(NotificationReferenceType.API, apiId);
                    //TODO: NotificationReferenceType.PORTAL ?
                    apiCategoryOrderRepository.deleteByApiId(apiId);
                    apiQualityRuleRepository.deleteByApi(apiId);
                    mediaRepository.deleteAllByApi(apiId);
                    metadataRepository.deleteByReferenceTypeAndReferenceId(MetadataReferenceType.API, apiId);
                    auditRepository.deleteByReferenceIdAndReferenceType(Audit.AuditReferenceType.API, apiId);
                } catch (TechnicalException e) {
                    throw new TechnicalManagementException(e);
                }
            });

        planRepository
            .deleteByEnvironment(executionContext.getEnvironmentId())
            .forEach(planId -> {
                try {
                    flowRepository.deleteByReference(FlowReferenceType.PLAN, planId);
                } catch (TechnicalException e) {
                    throw new TechnicalManagementException(e);
                }
            });
    }

    private void deletePages(ExecutionContext executionContext, PageReferenceType referenceType, String id) {
        try {
            pageRepository
                .deleteByReferenceIdAndReferenceType(referenceType, id)
                .forEach(pageId -> {
                    searchEngineService.delete(executionContext, PageEntity.builder().id(pageId).build());
                });
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private void deleteApplications(ExecutionContext executionContext) throws TechnicalException {
        List<String> applicationIds = applicationRepository.deleteByEnvironment(executionContext.getEnvironmentId());
        applicationIds.forEach(applicationId -> {
            genericNotificationConfigService.deleteReference(NotificationReferenceType.APPLICATION, applicationId);
            membershipService.deleteReference(executionContext, MembershipReferenceType.APPLICATION, applicationId);

            try {
                portalNotificationConfigRepository.deleteReference(NotificationReferenceType.APPLICATION, applicationId);
                auditRepository.deleteByReferenceIdAndReferenceType(Audit.AuditReferenceType.APPLICATION, applicationId);
                metadataRepository.deleteByReferenceTypeAndReferenceId(MetadataReferenceType.APPLICATION, applicationId);
            } catch (TechnicalException e) {
                throw new TechnicalManagementException(e);
            }
            applicationAlertService.deleteAll(applicationId);
        });
    }
}
