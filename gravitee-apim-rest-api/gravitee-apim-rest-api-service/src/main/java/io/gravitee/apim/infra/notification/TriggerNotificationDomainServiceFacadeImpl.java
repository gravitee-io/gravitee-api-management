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
package io.gravitee.apim.infra.notification;

import io.gravitee.apim.core.api.ApiMetadataQueryService;
import io.gravitee.apim.core.api.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.notification.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.ApiNotificationTemplateData;
import io.gravitee.apim.core.notification.model.ApplicationNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PlanNotificationTemplateData;
import io.gravitee.apim.core.notification.model.PrimaryOwnerNotificationTemplateData;
import io.gravitee.apim.core.notification.model.hook.ApiHookContext;
import io.gravitee.apim.core.notification.model.hook.ApplicationHookContext;
import io.gravitee.apim.core.notification.model.hook.HookContext;
import io.gravitee.apim.core.notification.model.hook.HookContextEntry;
import io.gravitee.apim.infra.template.TemplateProcessor;
import io.gravitee.apim.infra.template.TemplateProcessorException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.ApplicationRepository;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.rest.api.service.NotifierService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Implementation of the TriggerNotificationDomainService interface using APIM "legacy" notification system.
 */
@Service
@Slf4j
public class TriggerNotificationDomainServiceFacadeImpl implements TriggerNotificationDomainService {

    private final NotifierService notifierService;
    private final ApiRepository apiRepository;
    private final ApplicationRepository applicationRepository;
    private final PlanRepository planRepository;
    private final ApiPrimaryOwnerDomainService primaryOwnerDomainService;
    private final ApiMetadataQueryService metadataQueryService;
    private final TemplateProcessor templateProcessor;

    public TriggerNotificationDomainServiceFacadeImpl(
        NotifierService notifierService,
        @Lazy ApiRepository apiRepository,
        @Lazy ApplicationRepository applicationRepository,
        @Lazy PlanRepository planRepository,
        ApiPrimaryOwnerDomainService primaryOwnerDomainService,
        ApiMetadataQueryService metadataQueryService,
        TemplateProcessor templateProcessor
    ) {
        this.notifierService = notifierService;
        this.apiRepository = apiRepository;
        this.applicationRepository = applicationRepository;
        this.planRepository = planRepository;
        this.primaryOwnerDomainService = primaryOwnerDomainService;
        this.metadataQueryService = metadataQueryService;
        this.templateProcessor = templateProcessor;
    }

    @Override
    public void triggerApiNotification(ExecutionContext executionContext, ApiHookContext context) {
        var props = buildParams(executionContext, context);

        notifierService.trigger(executionContext, context.getHook(), context.getApiId(), props);
    }

    @Override
    public void triggerApplicationNotification(ExecutionContext executionContext, ApplicationHookContext context) {
        var props = buildParams(executionContext, context);

        notifierService.trigger(executionContext, context.getHook(), context.getApplicationId(), props);
    }

    private Map<String, Object> buildParams(ExecutionContext executionContext, HookContext context) {
        var apiId = context.getProperties().get(HookContextEntry.API_ID);
        var applicationId = context.getProperties().get(HookContextEntry.APPLICATION_ID);
        var planId = context.getProperties().get(HookContextEntry.PLAN_ID);
        Optional<ApiNotificationTemplateData> apiData = Optional.empty();
        Optional<ApplicationNotificationTemplateData> applicationData = Optional.empty();
        Optional<PlanNotificationTemplateData> planData = Optional.empty();

        if (apiId != null) {
            try {
                apiData =
                    apiRepository
                        .findById(apiId)
                        .map(api -> {
                            var apiPrimaryOwner = primaryOwnerDomainService.getApiPrimaryOwner(executionContext, apiId);
                            var metadata = metadataQueryService
                                .findApiMetadata(apiId)
                                .entrySet()
                                .stream()
                                .map(entry -> Map.entry(entry.getKey(), entry.getValue().getValue()))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

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
                                .metadata(decodeMetadata(metadata, api, apiPrimaryOwner))
                                .build();
                        });
            } catch (TechnicalException e) {
                throw new TechnicalManagementException(e);
            }
        }

        if (applicationId != null) {
            try {
                applicationData =
                    applicationRepository
                        .findById(applicationId)
                        .map(application -> {
                            var apiPrimaryOwner = primaryOwnerDomainService.getApiPrimaryOwner(executionContext, applicationId);
                            return ApplicationNotificationTemplateData
                                .builder()
                                .name(application.getName())
                                .type(application.getType().name())
                                .description(application.getDescription())
                                .status(application.getStatus().name())
                                .primaryOwner(PrimaryOwnerNotificationTemplateData.from(apiPrimaryOwner))
                                .createdAt(application.getCreatedAt())
                                .updatedAt(application.getUpdatedAt())
                                .build();
                        });
            } catch (TechnicalException e) {
                throw new TechnicalManagementException(e);
            }
        }

        if (planId != null) {
            try {
                var optPlan = planRepository.findById(planId);
                planData =
                    optPlan.map(plan ->
                        PlanNotificationTemplateData
                            .builder()
                            .name(plan.getName())
                            .description(plan.getDescription())
                            .order(plan.getOrder())
                            .createdAt(plan.getCreatedAt())
                            .updatedAt(plan.getUpdatedAt())
                            .publishedAt(plan.getPublishedAt())
                            .closedAt(plan.getClosedAt())
                            .build()
                    );
            } catch (TechnicalException e) {
                throw new TechnicalManagementException(e);
            }
        }

        var params = new HashMap<String, Object>();
        apiData.ifPresent(apiNotificationTemplateData -> params.put("api", apiNotificationTemplateData));
        applicationData.ifPresent(data -> params.put("application", data));
        planData.ifPresent(data -> params.put("plan", data));

        return params;
    }

    private Map<String, String> decodeMetadata(Map<String, String> metadata, Api api, PrimaryOwnerEntity primaryOwner) {
        if (metadata.isEmpty()) {
            return metadata;
        }

        try {
            var decodedValue = templateProcessor.processInlineTemplate(
                metadata.toString(),
                Collections.singletonMap(
                    "api",
                    ApiMetadataDecodeContext
                        .builder()
                        .name(api.getName())
                        .description(api.getDescription())
                        .createdAt(api.getCreatedAt())
                        .updatedAt(api.getUpdatedAt())
                        .primaryOwner(
                            new PrimaryOwnerMetadataDecodeContext(
                                primaryOwner.id(),
                                primaryOwner.displayName(),
                                primaryOwner.email(),
                                primaryOwner.type()
                            )
                        )
                        .build()
                )
            );

            return Arrays
                .stream(decodedValue.substring(1, decodedValue.length() - 1).split(", "))
                .map(entry -> entry.split("=", 2))
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry.length > 1 ? entry[1] : ""));
        } catch (TemplateProcessorException e) {
            log.warn("Error while creating template from reader:\n{}", e.getMessage());
            return metadata;
        } catch (Exception ex) {
            throw new TechnicalManagementException("An error occurs while evaluating API metadata", ex);
        }
    }

    @Builder
    @AllArgsConstructor
    @Data
    public static class ApiMetadataDecodeContext {

        private String name;
        private String description;
        private Date createdAt;
        private Date updatedAt;
        private PrimaryOwnerMetadataDecodeContext primaryOwner;
    }

    @Builder
    @AllArgsConstructor
    @Data
    public static class PrimaryOwnerMetadataDecodeContext {

        private String id;
        private String displayName;
        private String email;
        private String type;
    }
}
