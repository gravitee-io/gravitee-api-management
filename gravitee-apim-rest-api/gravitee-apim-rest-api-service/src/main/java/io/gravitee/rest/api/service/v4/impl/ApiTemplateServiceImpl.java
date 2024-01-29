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

import static io.gravitee.rest.api.service.impl.upgrade.initializer.DefaultMetadataInitializer.*;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.ApiModel;
import io.gravitee.rest.api.model.ProxyModelEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.federation.FederatedApiEntity;
import io.gravitee.rest.api.model.federation.FederatedApiModel;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiTemplateServiceImpl implements ApiTemplateService {

    private final ApiSearchService apiSearchService;
    private final ApiMetadataService apiMetadataService;
    private final PrimaryOwnerService primaryOwnerService;
    private final NotificationTemplateService notificationTemplateService;

    public ApiTemplateServiceImpl(
        @Lazy final ApiSearchService apiSearchService,
        final ApiMetadataService apiMetadataService,
        final PrimaryOwnerService primaryOwnerService,
        final NotificationTemplateService notificationTemplateService
    ) {
        this.apiSearchService = apiSearchService;
        this.apiMetadataService = apiMetadataService;
        this.primaryOwnerService = primaryOwnerService;
        this.notificationTemplateService = notificationTemplateService;
    }

    @Override
    public GenericApiModel findByIdForTemplates(ExecutionContext executionContext, String apiId, boolean decodeTemplate) {
        final GenericApiEntity genericApiEntity = apiSearchService.findGenericById(executionContext, apiId);

        if (genericApiEntity.getDefinitionVersion() == DefinitionVersion.FEDERATED) {
            FederatedApiEntity federatedApiEntity = (FederatedApiEntity) genericApiEntity;
            final FederatedApiModel federatedApiModel = new FederatedApiModel();

            federatedApiModel.setId(federatedApiEntity.getId());
            federatedApiModel.setDefinitionVersion(federatedApiEntity.getDefinitionVersion());
            federatedApiModel.setName(federatedApiEntity.getName());
            federatedApiModel.setDescription(federatedApiEntity.getDescription());
            federatedApiModel.setCreatedAt(federatedApiEntity.getCreatedAt());
            federatedApiModel.setDeployedAt(federatedApiEntity.getDeployedAt());
            federatedApiModel.setUpdatedAt(federatedApiEntity.getUpdatedAt());
            federatedApiModel.setGroups(federatedApiEntity.getGroups());
            federatedApiModel.setVisibility(federatedApiEntity.getVisibility());
            federatedApiModel.setCategories(federatedApiEntity.getCategories());
            federatedApiModel.setApiVersion(federatedApiEntity.getApiVersion());
            federatedApiModel.setState(federatedApiEntity.getState());
            federatedApiModel.setTags(federatedApiEntity.getTags());
            federatedApiModel.setPicture(federatedApiEntity.getPicture());
            federatedApiModel.setPrimaryOwner(federatedApiEntity.getPrimaryOwner());
            federatedApiModel.setLifecycleState(federatedApiEntity.getLifecycleState());
            federatedApiModel.setDisableMembershipNotifications(federatedApiEntity.isDisableMembershipNotifications());
            federatedApiModel.setAccessPoint(federatedApiEntity.getAccessPoint());

            federatedApiModel.setMetadata(getApiMetadata(executionContext, apiId, decodeTemplate, federatedApiModel));
            return federatedApiModel;
        } else if (genericApiEntity.getDefinitionVersion() != DefinitionVersion.V4) {
            ApiEntity apiEntity = (ApiEntity) genericApiEntity;
            final ApiModel apiModelEntity = new ApiModel();

            apiModelEntity.setId(apiEntity.getId());
            apiModelEntity.setDefinitionVersion(apiEntity.getDefinitionVersion());
            apiModelEntity.setName(apiEntity.getName());
            apiModelEntity.setDescription(apiEntity.getDescription());
            apiModelEntity.setCreatedAt(apiEntity.getCreatedAt());
            apiModelEntity.setDeployedAt(apiEntity.getDeployedAt());
            apiModelEntity.setUpdatedAt(apiEntity.getUpdatedAt());
            apiModelEntity.setGroups(apiEntity.getGroups());
            apiModelEntity.setVisibility(apiEntity.getVisibility());
            apiModelEntity.setCategories(apiEntity.getCategories());
            apiModelEntity.setVersion(apiEntity.getVersion());
            apiModelEntity.setState(apiEntity.getState());
            apiModelEntity.setTags(apiEntity.getTags());
            apiModelEntity.setPicture(apiEntity.getPicture());
            apiModelEntity.setPrimaryOwner(apiEntity.getPrimaryOwner());
            apiModelEntity.setProperties(apiEntity.getProperties());
            apiModelEntity.setLifecycleState(apiEntity.getLifecycleState());
            apiModelEntity.setDisableMembershipNotifications(apiEntity.isDisableMembershipNotifications());

            apiModelEntity.setServices(apiEntity.getServices());
            apiModelEntity.setExecutionMode(apiEntity.getExecutionMode());
            apiModelEntity.setPaths(apiEntity.getPaths());
            apiModelEntity.setProxy(convert(apiEntity.getProxy()));

            apiModelEntity.setMetadata(getApiMetadata(executionContext, apiId, decodeTemplate, apiModelEntity));
            return apiModelEntity;
        } else {
            io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity = (io.gravitee.rest.api.model.v4.api.ApiEntity) genericApiEntity;
            final io.gravitee.rest.api.model.v4.api.ApiModel apiModelEntity = new io.gravitee.rest.api.model.v4.api.ApiModel();

            apiModelEntity.setId(apiEntity.getId());
            apiModelEntity.setDefinitionVersion(apiEntity.getDefinitionVersion());
            apiModelEntity.setName(apiEntity.getName());
            apiModelEntity.setDescription(apiEntity.getDescription());
            apiModelEntity.setCreatedAt(apiEntity.getCreatedAt());
            apiModelEntity.setDeployedAt(apiEntity.getDeployedAt());
            apiModelEntity.setUpdatedAt(apiEntity.getUpdatedAt());
            apiModelEntity.setGroups(apiEntity.getGroups());
            apiModelEntity.setVisibility(apiEntity.getVisibility());
            apiModelEntity.setCategories(apiEntity.getCategories());
            apiModelEntity.setApiVersion(apiEntity.getApiVersion());
            apiModelEntity.setState(apiEntity.getState());
            apiModelEntity.setTags(apiEntity.getTags());
            apiModelEntity.setPicture(apiEntity.getPicture());
            apiModelEntity.setPrimaryOwner(apiEntity.getPrimaryOwner());
            apiModelEntity.setProperties(apiEntity.getProperties());
            apiModelEntity.setLifecycleState(apiEntity.getLifecycleState());
            apiModelEntity.setDisableMembershipNotifications(apiEntity.isDisableMembershipNotifications());

            apiModelEntity.setServices(apiEntity.getServices());
            apiModelEntity.setListeners(apiEntity.getListeners());
            apiModelEntity.setEndpointGroups(apiEntity.getEndpointGroups());

            apiModelEntity.setMetadata(getApiMetadata(executionContext, apiId, decodeTemplate, apiModelEntity));
            return apiModelEntity;
        }
    }

    private Map<String, String> getApiMetadata(
        final ExecutionContext executionContext,
        final String apiId,
        final boolean decodeTemplate,
        final GenericApiModel genericApiModel
    ) {
        final List<ApiMetadataEntity> metadataList = apiMetadataService.findAllByApi(apiId);

        if (metadataList == null) {
            return Map.of();
        }

        final Map<String, String> mapMetadata = new HashMap<>(metadataList.size());
        metadataList.forEach(metadata ->
            mapMetadata.put(metadata.getKey(), metadata.getValue() == null ? metadata.getDefaultValue() : metadata.getValue())
        );

        try {
            return decodeTemplate ? decodeMetadata(executionContext, genericApiModel, mapMetadata) : mapMetadata;
        } catch (Exception ex) {
            throw new TechnicalManagementException("An error occurs while evaluating API metadata", ex);
        }
    }

    private Map<String, String> decodeMetadata(
        ExecutionContext executionContext,
        GenericApiModel genericApiModel,
        Map<String, String> metadata
    ) {
        try {
            String decodedValue =
                this.notificationTemplateService.resolveInlineTemplateWithParam(
                        executionContext.getOrganizationId(),
                        genericApiModel.getId(),
                        new StringReader(metadata.toString()),
                        Collections.singletonMap("api", genericApiModel)
                    );

            Map<String, String> decodedMetadata = Arrays
                .stream(decodedValue.substring(1, decodedValue.length() - 1).split(", "))
                .map(entry -> entry.split("=", 2))
                .collect(Collectors.toMap(entry -> entry[0], entry -> entry.length > 1 ? entry[1] : ""));

            String supportEmail = decodedMetadata.getOrDefault(METADATA_EMAIL_SUPPORT_KEY, "");
            if (supportEmail.isBlank()) {
                decodedMetadata.put(
                    METADATA_EMAIL_SUPPORT_KEY,
                    primaryOwnerService.getPrimaryOwnerEmail(executionContext.getOrganizationId(), genericApiModel.getId())
                );
            }

            return decodedMetadata;
        } catch (Exception ex) {
            throw new TechnicalManagementException("An error occurs while evaluating API metadata", ex);
        }
    }

    private ProxyModelEntity convert(Proxy proxy) {
        ProxyModelEntity proxyModelEntity = new ProxyModelEntity();
        if (proxy != null) {
            proxyModelEntity.setCors(proxy.getCors());
            proxyModelEntity.setFailover(proxy.getFailover());
            proxyModelEntity.setGroups(proxy.getGroups());
            proxyModelEntity.setLogging(proxy.getLogging());
            proxyModelEntity.setPreserveHost(proxy.isPreserveHost());
            proxyModelEntity.setStripContextPath(proxy.isStripContextPath());
            proxyModelEntity.setVirtualHosts(proxy.getVirtualHosts());

            //add a default context-path to preserve compatibility on old templates
            if (proxy.getVirtualHosts() != null && !proxy.getVirtualHosts().isEmpty()) {
                proxyModelEntity.setContextPath(proxy.getVirtualHosts().get(0).getPath());
            }
        }

        return proxyModelEntity;
    }
}
