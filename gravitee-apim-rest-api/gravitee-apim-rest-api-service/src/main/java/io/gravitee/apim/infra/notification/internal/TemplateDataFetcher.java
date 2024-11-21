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
package io.gravitee.apim.infra.notification.internal;

import static io.gravitee.rest.api.service.notification.ApiHook.SUBSCRIPTION_ACCEPTED;
import static io.gravitee.rest.api.service.notification.ApiHook.SUBSCRIPTION_NEW;
import static io.gravitee.rest.api.service.notification.ApiHook.SUBSCRIPTION_REJECTED;

import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService;
import io.gravitee.apim.core.api.domain_service.ApiMetadataDecoderDomainService.ApiMetadataDecodeContext;
import io.gravitee.apim.core.documentation.model.PrimaryOwnerApiTemplateData;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.exception.ApiPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.notification.model.ApiNotificationTemplateData;
import io.gravitee.apim.core.notification.model.ApplicationNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PlanNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PrimaryOwnerNotificationTemplateData;
import io.gravitee.apim.core.notification.model.SubscriptionNotificationTemplateData;
import io.gravitee.apim.core.notification.model.hook.HookContext;
import io.gravitee.apim.core.notification.model.hook.HookContextEntry;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.api.SubscriptionRepository;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TemplateDataFetcher {

    private final ApiRepository apiRepository;
    private final ApplicationRepository applicationRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ApplicationPrimaryOwnerDomainService applicationPrimaryOwnerDomainService;
    private final ApiMetadataDecoderDomainService apiMetadataDecoderDomainService;
    private final UserCrudService userCrudService;

    public TemplateDataFetcher(
        @Lazy ApiRepository apiRepository,
        @Lazy ApplicationRepository applicationRepository,
        @Lazy PlanRepository planRepository,
        @Lazy SubscriptionRepository subscriptionRepository,
        ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService,
        ApplicationPrimaryOwnerDomainService applicationPrimaryOwnerDomainService,
        ApiMetadataDecoderDomainService apiMetadataDecoderDomainService,
        UserCrudService userCrudService
    ) {
        this.apiRepository = apiRepository;
        this.applicationRepository = applicationRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.apiPrimaryOwnerDomainService = apiPrimaryOwnerDomainService;
        this.applicationPrimaryOwnerDomainService = applicationPrimaryOwnerDomainService;
        this.apiMetadataDecoderDomainService = apiMetadataDecoderDomainService;
        this.userCrudService = userCrudService;
    }

    public Map<String, Object> fetchData(String organizationId, HookContext hookContext) {
        return hookContext
            .getProperties()
            .entrySet()
            .stream()
            .map(entry ->
                Map.entry(
                    keyFor(entry.getKey()),
                    switch (entry.getKey()) {
                        case API_ID -> buildApiNotificationTemplateData(organizationId, entry.getValue());
                        case APPLICATION_ID -> buildApplicationNotificationTemplateData(organizationId, entry.getValue());
                        case PLAN_ID -> buildPlanNotificationTemplateData(entry.getValue());
                        case SUBSCRIPTION_ID -> buildSubscriptionNotificationTemplateData(entry.getValue());
                        case API_KEY -> Optional.of(entry.getValue());
                        case OWNER -> buildOwnerNotificationTemplateData(hookContext.getHook().name(), entry.getValue());
                    }
                )
            )
            .filter(entry -> entry.getValue().isPresent())
            .map(entry -> Map.entry(entry.getKey(), entry.getValue().get()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Optional<PrimaryOwnerNotificationTemplateData> buildOwnerNotificationTemplateData(String hook, String userId) {
        if (List.of(SUBSCRIPTION_NEW.name(), SUBSCRIPTION_ACCEPTED.name(), SUBSCRIPTION_REJECTED.name()).contains(hook)) {
            try {
                var user = userCrudService.getBaseUser(userId);
                return Optional.of(
                    PrimaryOwnerNotificationTemplateData
                        .builder()
                        .id(user.getId())
                        .displayName(user.displayName())
                        .email(user.getEmail())
                        .build()
                );
            } catch (ApiPrimaryOwnerNotFoundException e) {
                throw new TechnicalManagementException(e);
            }
        }
        return Optional.empty();
    }

    private String keyFor(HookContextEntry entry) {
        return switch (entry) {
            case API_ID -> "api";
            case APPLICATION_ID -> "application";
            case PLAN_ID -> "plan";
            case SUBSCRIPTION_ID -> "subscription";
            case API_KEY -> "apiKey";
            case OWNER -> "owner";
        };
    }

    private Optional<ApiNotificationTemplateData> buildApiNotificationTemplateData(String organizationId, String apiId) {
        try {
            return apiRepository
                .findById(apiId)
                .map(api -> {
                    var apiPrimaryOwner = apiPrimaryOwnerDomainService.getApiPrimaryOwner(organizationId, apiId);
                    var metadata = apiMetadataDecoderDomainService.decodeMetadata(
                        api.getEnvironmentId(),
                        apiId,
                        ApiMetadataDecodeContext
                            .builder()
                            .name(api.getName())
                            .description(api.getDescription())
                            .createdAt(api.getCreatedAt())
                            .updatedAt(api.getUpdatedAt())
                            .primaryOwner(
                                new PrimaryOwnerApiTemplateData(
                                    apiPrimaryOwner.id(),
                                    apiPrimaryOwner.displayName(),
                                    apiPrimaryOwner.email(),
                                    apiPrimaryOwner.type().name()
                                )
                            )
                            .build()
                    );

                    return ApiNotificationTemplateData
                        .builder()
                        .id(api.getId())
                        .name(api.getName())
                        .apiVersion(api.getVersion())
                        .description(api.getDescription())
                        .definitionVersion(api.getDefinitionVersion())
                        .createdAt(api.getCreatedAt())
                        .updatedAt(api.getUpdatedAt())
                        .deployedAt(api.getDeployedAt())
                        .primaryOwner(PrimaryOwnerNotificationTemplateData.from(apiPrimaryOwner))
                        .metadata(metadata)
                        .build();
                });
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private Optional<ApplicationNotificationTemplateData> buildApplicationNotificationTemplateData(
        String organizationId,
        String applicationId
    ) {
        try {
            return applicationRepository
                .findById(applicationId)
                .map(application -> {
                    var primaryOwner = applicationPrimaryOwnerDomainService.getApplicationPrimaryOwner(organizationId, applicationId);
                    return ApplicationNotificationTemplateData
                        .builder()
                        .id(application.getId())
                        .name(application.getName())
                        .type(application.getType().name())
                        .description(application.getDescription())
                        .status(application.getStatus().name())
                        .primaryOwner(PrimaryOwnerNotificationTemplateData.from(primaryOwner))
                        .createdAt(application.getCreatedAt())
                        .updatedAt(application.getUpdatedAt())
                        .apiKeyMode(application.getApiKeyMode().name())
                        .build();
                });
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private Optional<PlanNotificationTemplateData> buildPlanNotificationTemplateData(String planId) {
        try {
            return planRepository
                .findById(planId)
                .map(plan ->
                    PlanNotificationTemplateData
                        .builder()
                        .id(plan.getId())
                        .name(plan.getName())
                        .description(plan.getDescription())
                        .order(plan.getOrder())
                        .createdAt(plan.getCreatedAt())
                        .updatedAt(plan.getUpdatedAt())
                        .publishedAt(plan.getPublishedAt())
                        .closedAt(plan.getClosedAt())
                        .commentMessage(plan.getCommentMessage())
                        .security(plan.getSecurity() != null ? plan.getSecurity().name() : null)
                        .validation(plan.getValidation().name())
                        .build()
                );
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }

    private Optional<SubscriptionNotificationTemplateData> buildSubscriptionNotificationTemplateData(String subscriptionId) {
        try {
            return subscriptionRepository
                .findById(subscriptionId)
                .map(subscription ->
                    SubscriptionNotificationTemplateData
                        .builder()
                        .id(subscription.getId())
                        .reason(subscription.getReason())
                        .request(subscription.getRequest())
                        .status(subscription.getStatus().name())
                        .build()
                );
        } catch (TechnicalException e) {
            throw new TechnicalManagementException(e);
        }
    }
}
