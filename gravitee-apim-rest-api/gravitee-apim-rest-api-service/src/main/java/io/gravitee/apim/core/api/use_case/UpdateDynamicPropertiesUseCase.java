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
package io.gravitee.apim.core.api.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.ApiStateDomainService;
import io.gravitee.apim.core.api.domain_service.CategoryDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.query_service.ApiEventQueryService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.environment.crud_service.EnvironmentCrudService;
import io.gravitee.definition.model.v4.AbstractApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApi;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.definition.model.v4.service.Service;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.CustomLog;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
@UseCase
public class UpdateDynamicPropertiesUseCase {

    private final ApiCrudService apiCrudService;
    private final ApiStateDomainService apiStateDomainService;
    private final EnvironmentCrudService environmentCrudService;
    private final AuditDomainService auditDomainService;
    private final ApiEventQueryService apiEventQueryService;
    private final CategoryDomainService categoryDomainService;

    public UpdateDynamicPropertiesUseCase(
        ApiCrudService apiCrudService,
        ApiStateDomainService apiStateDomainService,
        EnvironmentCrudService environmentCrudService,
        AuditDomainService auditDomainService,
        ApiEventQueryService apiEventQueryService,
        CategoryDomainService categoryDomainService
    ) {
        this.apiCrudService = apiCrudService;
        this.apiStateDomainService = apiStateDomainService;
        this.environmentCrudService = environmentCrudService;
        this.auditDomainService = auditDomainService;
        this.apiEventQueryService = apiEventQueryService;
        this.categoryDomainService = categoryDomainService;
    }

    public record Input(String apiId, String pluginId, List<Property> dynamicProperties) {}

    public void execute(Input input) {
        final Api api = apiCrudService.get(input.apiId());
        final AuditInfo auditInfo = buildAuditInfo(input, api);

        // determine the sync state before modifying the api with the new properties
        final boolean isApiSynchronized = apiStateDomainService.isSynchronized(api, auditInfo);

        final List<Property> previousProperties = getCurrentProperties(api);
        final boolean needToBeUpdated = api.updateDynamicProperties(input.dynamicProperties());

        if (!needToBeUpdated) {
            return;
        }

        api.setCategories(categoryDomainService.toCategoryId(api, api.getEnvironmentId()));

        final Api updated = apiCrudService.update(api);

        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .apiId(updated.getId())
                .environmentId(auditInfo.environmentId())
                .organizationId(auditInfo.organizationId())
                .event(ApiAuditEvent.API_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(api)
                .newValue(updated)
                .createdAt(ZonedDateTime.ofInstant(api.getUpdatedAt().toInstant(), ZoneId.systemDefault()))
                .properties(Map.of(AuditProperties.API, api.getId()))
                .build()
        );

        // We can force a redeployment only if:
        // - the API was synchronized before the properties are updated (i.e. no manual changes have been done by a user)
        // - and the properties have changed
        if (isApiSynchronized && needRedployment(getCurrentProperties(api), previousProperties)) {
            // Get the api from latest deployment event of the api to deploy the api with the same dynamic properties configuration
            // It avoids to deploy changes on the configuration that has not been explicitly deployed by the user
            apiEventQueryService
                .findLastPublishedApi(auditInfo.organizationId(), auditInfo.environmentId(), api.getId())
                .ifPresent(deployedApi -> {
                    final Service deployedDynamicPropertiesService = getDynamicPropertyService(deployedApi);
                    if (deployedDynamicPropertiesService == null) {
                        return;
                    }
                    // If the deployed api has the service enabled, then redeploy with the service enabled.
                    if (deployedDynamicPropertiesService.isEnabled()) {
                        setDynamicPropertyService(updated, deployedDynamicPropertiesService);
                    }
                });
            apiStateDomainService.deploy(updated, String.format("%s sync", input.pluginId()), auditInfo);
        }
    }

    /**
     * Needs a redeployment when new properties list differs from the original
     *
     * @param updatedProperties  is the new list of properties including the dynamic ones
     * @param previousProperties is the list of properties currently deployed
     * @return true if the API needs to be reployed
     */
    private static boolean needRedployment(List<Property> updatedProperties, List<Property> previousProperties) {
        return !new HashSet<>(updatedProperties).equals(new HashSet<>(previousProperties));
    }

    /**
     * Get api properties as an immutable list
     *
     * @param api to extract properties from
     * @return the copy of the list of properties
     */
    private static List<Property> getCurrentProperties(Api api) {
        return Optional.ofNullable(api.getApiDefinitionValue())
            .filter(AbstractApi.class::isInstance)
            .map(AbstractApi.class::cast)
            .map(AbstractApi::getProperties)
            .orElse(Collections.emptyList())
            .stream()
            .toList();
    }

    /**
     * Read the dynamic properties service whatever the V4 API type is (HTTP or Native).
     *
     * @param api to extract the service from
     * @return the dynamic properties service, or null when the API type has no services or none is configured
     */
    private static Service getDynamicPropertyService(Api api) {
        return switch (api.getApiDefinitionValue()) {
            case io.gravitee.definition.model.v4.Api httpV4 -> httpV4.getServices() == null
                ? null
                : httpV4.getServices().getDynamicProperty();
            case NativeApi nativeV4 -> nativeV4.getServices() == null ? null : nativeV4.getServices().getDynamicProperty();
            case null, default -> null;
        };
    }

    /**
     * Set the dynamic properties service whatever the V4 API type is (HTTP or Native). No-op for API types without services.
     *
     * @param api to set the service on
     * @param dynamicPropertyService the service to set
     */
    private static void setDynamicPropertyService(Api api, Service dynamicPropertyService) {
        switch (api.getApiDefinitionValue()) {
            case io.gravitee.definition.model.v4.Api httpV4 -> {
                if (httpV4.getServices() == null) {
                    httpV4.setServices(new ApiServices());
                }
                httpV4.getServices().setDynamicProperty(dynamicPropertyService);
            }
            case NativeApi nativeV4 -> {
                if (nativeV4.getServices() == null) {
                    nativeV4.setServices(new NativeApiServices());
                }
                nativeV4.getServices().setDynamicProperty(dynamicPropertyService);
            }
            // Only HTTP and Native V4 APIs hold services; getDynamicPropertyService() already filtered the others out
            case null, default -> {}
        }
    }

    private AuditInfo buildAuditInfo(Input input, Api apiForUpdate) {
        return AuditInfo.builder()
            .environmentId(apiForUpdate.getEnvironmentId())
            .organizationId(environmentCrudService.get(apiForUpdate.getEnvironmentId()).getOrganizationId())
            .actor(AuditActor.builder().userId(String.format("%s-management-api-service", input.pluginId())).build())
            .build();
    }
}
