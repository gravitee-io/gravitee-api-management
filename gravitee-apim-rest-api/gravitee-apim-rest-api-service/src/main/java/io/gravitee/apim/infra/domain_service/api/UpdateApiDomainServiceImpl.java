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
package io.gravitee.apim.infra.domain_service.api;

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.domain_service.UpdateApiDomainService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.crd.ApiCRDSpec;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.infra.adapter.ApiAdapter;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.rest.api.model.v4.api.properties.PropertyEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.v4.ApiService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class UpdateApiDomainServiceImpl implements UpdateApiDomainService {

    private final ApiService delegate;
    private final ApiCrudService apiCrudService;

    public UpdateApiDomainServiceImpl(ApiService delegate, ApiCrudService apiCrudService) {
        this.delegate = delegate;
        this.apiCrudService = apiCrudService;
    }

    @Override
    public Api update(String apiId, ApiCRDSpec crd, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var apiEntity = delegate.update(executionContext, apiId, ApiAdapter.INSTANCE.toUpdateApiEntity(crd), auditInfo.actor().userId());

        return apiCrudService.get(apiEntity.getId());
    }

    @Override
    public Api updateV4(Api api, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var apiDefinition = (io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue();

        var updateApiEntity = ApiAdapter.INSTANCE.toUpdateApiEntity(api, apiDefinition);

        delegate.update(executionContext, api.getId(), updateApiEntity, false, auditInfo.actor().userId());

        return apiCrudService.get(api.getId());
    }

    @Override
    public Api validateV4(Api api, AuditInfo auditInfo) {
        var executionContext = new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId());
        var apiDefinition = (io.gravitee.definition.model.v4.Api) api.getApiDefinitionValue();
        var updateApiEntity = ApiAdapter.INSTANCE.toUpdateApiEntity(api, apiDefinition);

        delegate.validate(executionContext, api.getId(), updateApiEntity, auditInfo.actor().userId());

        return applySanitizedValues(api, updateApiEntity, apiDefinition);
    }

    private Api applySanitizedValues(
        Api original,
        io.gravitee.rest.api.model.v4.api.UpdateApiEntity sanitized,
        io.gravitee.definition.model.v4.Api originalDefinition
    ) {
        var sanitizedLifecycle = sanitized.getLifecycleState() != null
            ? Api.ApiLifecycleState.valueOf(sanitized.getLifecycleState().name())
            : original.getApiLifecycleState();
        var sanitizedVisibility = sanitized.getVisibility() != null
            ? Api.Visibility.valueOf(sanitized.getVisibility().name())
            : original.getVisibility();
        var sanitizedDefinition = originalDefinition
            .toBuilder()
            .tags(coalesce(sanitized.getTags(), originalDefinition.getTags()))
            .endpointGroups(coalesce(sanitized.getEndpointGroups(), originalDefinition.getEndpointGroups()))
            .flows(coalesce(sanitized.getFlows(), originalDefinition.getFlows()))
            .analytics(coalesce(sanitized.getAnalytics(), originalDefinition.getAnalytics()))
            .failover(coalesce(sanitized.getFailover(), originalDefinition.getFailover()))
            .flowExecution(coalesce(sanitized.getFlowExecution(), originalDefinition.getFlowExecution()))
            .services(coalesce(sanitized.getServices(), originalDefinition.getServices()))
            .allowedInApiProducts(coalesce(sanitized.getAllowedInApiProducts(), originalDefinition.getAllowedInApiProducts()))
            .responseTemplates(coalesce(sanitized.getResponseTemplates(), originalDefinition.getResponseTemplates()))
            .resources(coalesce(sanitized.getResources(), originalDefinition.getResources()))
            .properties(coalesce(toProperties(sanitized.getProperties()), originalDefinition.getProperties()))
            .listeners(coalesce(sanitized.getListeners(), originalDefinition.getListeners()))
            .build();
        return original
            .toBuilder()
            .name(coalesce(sanitized.getName(), original.getName()))
            .description(coalesce(sanitized.getDescription(), original.getDescription()))
            .version(coalesce(sanitized.getApiVersion(), original.getVersion()))
            .visibility(sanitizedVisibility)
            .apiLifecycleState(sanitizedLifecycle)
            .labels(coalesce(sanitized.getLabels(), original.getLabels()))
            .categories(coalesce(sanitized.getCategories(), original.getCategories()))
            .groups(coalesce(sanitized.getGroups(), original.getGroups()))
            .allowMultiJwtOauth2Subscriptions(sanitized.isAllowMultiJwtOauth2Subscriptions())
            .disableMembershipNotifications(sanitized.isDisableMembershipNotifications())
            .apiDefinitionValue(sanitizedDefinition)
            .build();
    }

    private static <T> T coalesce(T sanitized, T original) {
        return sanitized != null ? sanitized : original;
    }

    private static List<Property> toProperties(List<PropertyEntity> properties) {
        if (properties == null) {
            return null;
        }
        return properties
            .stream()
            .map(pe -> new Property(pe.getKey(), pe.getValue(), pe.isEncrypted(), pe.isDynamic()))
            .toList();
    }
}
