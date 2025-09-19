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
package io.gravitee.apim.core.api.domain_service;

import static io.gravitee.apim.core.workflow.model.Workflow.newApiReviewWorkflow;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.category.domain_service.CreateCategoryApiDomainService;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.notification.crud_service.NotificationConfigCrudService;
import io.gravitee.apim.core.notification.model.config.NotificationConfig;
import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.workflow.crud_service.WorkflowCrudService;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

@DomainService
public class CreateApiDomainService {

    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditService;
    private final ApiIndexerDomainService apiIndexerDomainService;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;
    private final FlowCrudService flowCrudService;
    private final NotificationConfigCrudService notificationConfigCrudService;
    private final ParametersQueryService parametersQueryService;
    private final WorkflowCrudService workflowCrudService;
    private final CreateCategoryApiDomainService createCategoryApiDomainService;

    public CreateApiDomainService(
        ApiCrudService apiCrudService,
        AuditDomainService auditService,
        ApiIndexerDomainService apiIndexerDomainService,
        ApiMetadataDomainService apiMetadataDomainService,
        ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService,
        FlowCrudService flowCrudService,
        NotificationConfigCrudService notificationConfigCrudService,
        ParametersQueryService parametersQueryService,
        WorkflowCrudService workflowCrudService,
        CreateCategoryApiDomainService createCategoryApiDomainService
    ) {
        this.apiCrudService = apiCrudService;
        this.auditService = auditService;
        this.apiIndexerDomainService = apiIndexerDomainService;
        this.apiPrimaryOwnerDomainService = apiPrimaryOwnerDomainService;
        this.apiMetadataDomainService = apiMetadataDomainService;
        this.flowCrudService = flowCrudService;
        this.notificationConfigCrudService = notificationConfigCrudService;
        this.parametersQueryService = parametersQueryService;
        this.workflowCrudService = workflowCrudService;
        this.createCategoryApiDomainService = createCategoryApiDomainService;
    }

    /**
     * Create a new API in the datastore.
     * <p>
     * This method will create the API, its primary owner, its default mail notification, its default metadata and its flows.
     * </p>
     * <p>
     * Once created, the API is indexed in the search engine.
     * </p>
     *
     * @param api          The API to create.
     * @param primaryOwner The primary owner of the API.
     * @param auditInfo    The audit information.
     * @param sanitizer    A sanitizer function to apply on the API before creating it. This will be used to apply additional validation and sanitization.
     * @return The created API.
     */
    public ApiWithFlows create(Api api, PrimaryOwnerEntity primaryOwner, AuditInfo auditInfo, UnaryOperator<Api> sanitizer) {
        var sanitized = sanitizer.apply(api);

        var created = apiCrudService.create(sanitized);

        createAuditLog(created, auditInfo);

        apiPrimaryOwnerDomainService.createApiPrimaryOwnerMembership(created.getId(), primaryOwner, auditInfo);

        createDefaultMailNotification(created);

        createDefaultMetadata(created, auditInfo);

        var createdFlows = saveApiFlows(api);

        // create Api Category Order entries
        createCategoryApiDomainService.addApiToCategories(api.getId(), api.getCategories());

        if (isApiReviewEnabled(created, auditInfo.organizationId(), auditInfo.environmentId())) {
            workflowCrudService.create(newApiReviewWorkflow(api.getId(), auditInfo.actor().userId()));
        }

        apiIndexerDomainService.index(
            new Indexer.IndexationContext(auditInfo.organizationId(), auditInfo.environmentId()),
            created,
            primaryOwner
        );
        return new ApiWithFlows(created, createdFlows);
    }

    private void createAuditLog(Api created, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(created.getId())
                .event(ApiAuditEvent.API_CREATED)
                .actor(auditInfo.actor())
                .newValue(created)
                .createdAt(created.getCreatedAt())
                .properties(Collections.emptyMap())
                .build()
        );
    }

    private void createDefaultMailNotification(Api api) {
        switch (api.getDefinitionVersion()) {
            case V4 -> notificationConfigCrudService.create(NotificationConfig.defaultMailNotificationConfigFor(api.getId()));
            case V1, V2, FEDERATED -> {
                // nothing to do
            }
        }
    }

    private void createDefaultMetadata(Api api, AuditInfo auditInfo) {
        switch (api.getDefinitionVersion()) {
            case V4 -> apiMetadataDomainService.createDefaultApiMetadata(api.getId(), auditInfo);
            case V1, V2, FEDERATED -> {
                // nothing to do
            }
        }
    }

    private List<Flow> saveApiFlows(Api api) {
        return switch (api.getDefinitionVersion()) {
            case V4 -> flowCrudService.saveApiFlows(api.getId(), api.getApiDefinitionV4().getFlows());
            case V1, V2, FEDERATED -> null;
        };
    }

    private boolean isApiReviewEnabled(Api api, String organizationId, String environmentId) {
        return switch (api.getDefinitionVersion()) {
            case V1, V2, V4 -> parametersQueryService.findAsBoolean(
                Key.API_REVIEW_ENABLED,
                new ParameterContext(environmentId, organizationId, ParameterReferenceType.ENVIRONMENT)
            );
            case FEDERATED -> false;
        };
    }
}
