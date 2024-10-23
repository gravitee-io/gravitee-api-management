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
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.Service;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
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
        final List<Property> previousProperties = getCurrentProperties(api);
        final Api apiForUpdate = api.toBuilder().build();
        final boolean needToBeUpdated = apiForUpdate.updateDynamicProperties(input.dynamicProperties());

        if (!needToBeUpdated) {
            return;
        }

        apiForUpdate.setCategories(categoryDomainService.toCategoryId(apiForUpdate, apiForUpdate.getEnvironmentId()));

        final Api updated = apiCrudService.update(apiForUpdate);

        final AuditInfo auditInfo = buildAuditInfo(input, apiForUpdate);

        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .apiId(updated.getId())
                .environmentId(auditInfo.environmentId())
                .organizationId(auditInfo.organizationId())
                .event(ApiAuditEvent.API_UPDATED)
                .actor(auditInfo.actor())
                .oldValue(api)
                .newValue(updated)
                .createdAt(ZonedDateTime.ofInstant(apiForUpdate.getUpdatedAt().toInstant(), ZoneId.systemDefault()))
                .properties(Map.of(AuditProperties.API, apiForUpdate.getId()))
                .build()
        );

        final boolean isApiSynchronized = apiStateDomainService.isSynchronized(updated, auditInfo);

        // Deploy only if
        // - API is not synchronized: no manual changes
        // - properties have changed
        if (!isApiSynchronized && needRedployment(api.getApiDefinitionHttpV4().getProperties(), previousProperties)) {
            // Get the api from latest deployment event of the api to deploy the api with the same dynamic properties configuration
            // It avoids to deploy changes on the configuration that has not been explicitly deployed by the user
            apiEventQueryService
                .findLastPublishedApi(auditInfo.organizationId(), auditInfo.environmentId(), api.getId())
                .ifPresent(deployedApi -> {
                    if (
                        deployedApi.getApiDefinitionHttpV4().getServices() == null ||
                        deployedApi.getApiDefinitionHttpV4().getServices().getDynamicProperty() == null
                    ) {
                        return;
                    }
                    final Service deployedDynamicPropertiesService = deployedApi
                        .getApiDefinitionHttpV4()
                        .getServices()
                        .getDynamicProperty();
                    // If the deployed api has the service enabled, then redeploy with the service enabled.
                    if (deployedDynamicPropertiesService.isEnabled()) {
                        updated.getApiDefinitionHttpV4().getServices().setDynamicProperty(deployedDynamicPropertiesService);
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
        return Optional.ofNullable(api.getApiDefinitionHttpV4().getProperties()).orElse(Collections.emptyList()).stream().toList();
    }

    private AuditInfo buildAuditInfo(Input input, Api apiForUpdate) {
        return AuditInfo
            .builder()
            .environmentId(apiForUpdate.getEnvironmentId())
            .organizationId(environmentCrudService.get(apiForUpdate.getEnvironmentId()).getOrganizationId())
            .actor(AuditActor.builder().userId(String.format("%s-management-api-service", input.pluginId())).build())
            .build();
    }
}
