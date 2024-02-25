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

import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api.model.ApiWithFlows;
import io.gravitee.apim.core.api.model.crd.ApiCRD;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.apim.core.datetime.TimeProvider;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerDomainService;
import io.gravitee.apim.core.membership.domain_service.ApiPrimaryOwnerFactory;
import io.gravitee.apim.core.notification.crud_service.NotificationConfigCrudService;
import io.gravitee.apim.core.notification.model.config.NotificationConfig;
import io.gravitee.apim.core.parameters.model.ParameterContext;
import io.gravitee.apim.core.parameters.query_service.ParametersQueryService;
import io.gravitee.apim.core.search.Indexer;
import io.gravitee.apim.core.workflow.crud_service.WorkflowCrudService;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Collections;

public class CreateApiDomainService {

    private final ValidateApiDomainService validateApiDomainService;
    private final ApiCrudService apiCrudService;
    private final AuditDomainService auditService;
    private final ApiIndexerDomainService apiIndexerDomainService;
    private final ApiPrimaryOwnerFactory apiPrimaryOwnerFactory;
    private final ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService;
    private final ApiMetadataDomainService apiMetadataDomainService;
    private final FlowCrudService flowCrudService;
    private final NotificationConfigCrudService notificationConfigCrudService;
    private final ParametersQueryService parametersQueryService;
    private final WorkflowCrudService workflowCrudService;

    public CreateApiDomainService(
        ValidateApiDomainService validateApiDomainService,
        ApiCrudService apiCrudService,
        AuditDomainService auditService,
        ApiIndexerDomainService apiIndexerDomainService,
        ApiMetadataDomainService apiMetadataDomainService,
        ApiPrimaryOwnerFactory apiPrimaryOwnerFactory,
        ApiPrimaryOwnerDomainService apiPrimaryOwnerDomainService,
        FlowCrudService flowCrudService,
        NotificationConfigCrudService notificationConfigCrudService,
        ParametersQueryService parametersQueryService,
        WorkflowCrudService workflowCrudService
    ) {
        this.validateApiDomainService = validateApiDomainService;
        this.apiCrudService = apiCrudService;
        this.auditService = auditService;
        this.apiIndexerDomainService = apiIndexerDomainService;
        this.apiPrimaryOwnerFactory = apiPrimaryOwnerFactory;
        this.apiPrimaryOwnerDomainService = apiPrimaryOwnerDomainService;
        this.apiMetadataDomainService = apiMetadataDomainService;
        this.flowCrudService = flowCrudService;
        this.notificationConfigCrudService = notificationConfigCrudService;
        this.parametersQueryService = parametersQueryService;
        this.workflowCrudService = workflowCrudService;
    }

    public ApiWithFlows create(Api api, AuditInfo auditInfo) {
        var primaryOwner = apiPrimaryOwnerFactory.createForNewApi(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            auditInfo.actor().userId()
        );

        var sanitized = validateApiDomainService.validateAndSanitizeForCreation(
            api,
            primaryOwner,
            auditInfo.environmentId(),
            auditInfo.organizationId()
        );

        if (sanitized.getId() == null) {
            sanitized.setId(UuidString.generateRandom());
        }

        var created = apiCrudService.create(
            sanitized.setEnvironmentId(auditInfo.environmentId()).setCreatedAt(TimeProvider.now()).setUpdatedAt(TimeProvider.now())
        );

        createAuditLog(created, auditInfo);

        apiPrimaryOwnerDomainService.createApiPrimaryOwnerMembership(created.getId(), primaryOwner, auditInfo);

        createDefaultMailNotification(created.getId());

        apiMetadataDomainService.createDefaultApiMetadata(created.getId(), auditInfo);

        flowCrudService.saveApiFlows(api.getId(), api.getApiDefinitionV4().getFlows());

        if (isApiReviewEnabled(auditInfo.organizationId(), auditInfo.environmentId())) {
            workflowCrudService.create(newApiReviewWorkflow(api.getId(), auditInfo.actor().userId()));
        }

        apiIndexerDomainService.index(
            new Indexer.IndexationContext(auditInfo.organizationId(), auditInfo.environmentId()),
            created,
            primaryOwner
        );
        return new ApiWithFlows(created, api.getApiDefinitionV4().getFlows());
    }

    public Api create(ApiCRD crd, AuditInfo auditInfo) {
        return create(crd.toApi(), auditInfo).toApi();
    }

    private void createAuditLog(Api created, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
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

    private void createDefaultMailNotification(String apiId) {
        notificationConfigCrudService.create(NotificationConfig.defaultMailNotificationConfigFor(apiId));
    }

    private boolean isApiReviewEnabled(String organizationId, String environmentId) {
        return parametersQueryService.findAsBoolean(
            Key.API_REVIEW_ENABLED,
            new ParameterContext(environmentId, organizationId, ParameterReferenceType.ENVIRONMENT)
        );
    }
}
