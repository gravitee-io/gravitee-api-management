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
package io.gravitee.apim.core.subscription.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api.model.Api;
import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApplicationHookContext;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
public class CloseSubscriptionDomainService {

    private final SubscriptionCrudService subscriptionCrudService;
    private final RejectSubscriptionDomainService rejectSubscriptionDomainService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final AuditDomainService auditDomainService;
    private final ApplicationCrudService applicationCrudService;
    private final RevokeApiKeyDomainService revokeApiKeyDomainService;
    private final ApiCrudService apiCrudService;
    private final IntegrationAgent integrationAgent;

    public CloseSubscriptionDomainService(
        SubscriptionCrudService subscriptionCrudService,
        ApplicationCrudService applicationCrudService,
        AuditDomainService auditDomainService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        RejectSubscriptionDomainService rejectSubscriptionDomainService,
        RevokeApiKeyDomainService revokeApiKeyDomainService,
        ApiCrudService apiCrudService,
        IntegrationAgent integrationAgent
    ) {
        this.subscriptionCrudService = subscriptionCrudService;
        this.rejectSubscriptionDomainService = rejectSubscriptionDomainService;
        this.triggerNotificationDomainService = triggerNotificationDomainService;
        this.auditDomainService = auditDomainService;
        this.applicationCrudService = applicationCrudService;
        this.revokeApiKeyDomainService = revokeApiKeyDomainService;
        this.apiCrudService = apiCrudService;
        this.integrationAgent = integrationAgent;
    }

    public SubscriptionEntity closeSubscription(String subscriptionId, AuditInfo auditInfo) {
        var subscription = subscriptionCrudService.get(subscriptionId);
        var api = apiCrudService.get(subscription.getApiId());

        return closeSubscription(subscription, api, auditInfo);
    }

    public SubscriptionEntity closeSubscription(String subscriptionId, Api api, AuditInfo auditInfo) {
        var subscription = subscriptionCrudService.get(subscriptionId);

        return closeSubscription(subscription, api, auditInfo);
    }

    public SubscriptionEntity closeSubscription(SubscriptionEntity subscription, Api api, AuditInfo auditInfo) {
        log.debug("Close subscription {}", subscription.getId());

        return switch (subscription.getStatus()) {
            case ACCEPTED, PAUSED -> closeAcceptedOrPausedSubscription(subscription, api, auditInfo);
            case PENDING -> rejectSubscriptionDomainService.reject(subscription, "Subscription has been closed.", auditInfo);
            case CLOSED, REJECTED -> subscription;
        };
    }

    private SubscriptionEntity closeAcceptedOrPausedSubscription(SubscriptionEntity subscriptionEntity, Api api, AuditInfo auditInfo) {
        if (api.getDefinitionVersion() == DefinitionVersion.FEDERATED && api.getOriginContext() instanceof OriginContext.Integration inte) {
            var federatedApi = api.getFederatedApiDefinition();
            integrationAgent.unsubscribe(inte.integrationId(), federatedApi, subscriptionEntity).blockingAwait();
        }

        var closedSubscriptionEntity = subscriptionCrudService.update(subscriptionEntity.close());

        triggerNotifications(closedSubscriptionEntity, auditInfo);

        createAuditLog(subscriptionEntity, closedSubscriptionEntity, auditInfo);

        revokeApiKeys(subscriptionEntity, auditInfo);

        return closedSubscriptionEntity;
    }

    private void triggerNotifications(SubscriptionEntity closedSubscriptionEntity, AuditInfo auditInfo) {
        triggerNotificationDomainService.triggerApiNotification(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            new SubscriptionClosedApiHookContext(
                closedSubscriptionEntity.getApiId(),
                closedSubscriptionEntity.getApplicationId(),
                closedSubscriptionEntity.getPlanId()
            )
        );
        triggerNotificationDomainService.triggerApplicationNotification(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            new SubscriptionClosedApplicationHookContext(
                closedSubscriptionEntity.getApplicationId(),
                closedSubscriptionEntity.getApiId(),
                closedSubscriptionEntity.getPlanId()
            )
        );
    }

    private void createAuditLog(SubscriptionEntity originalSubscription, SubscriptionEntity closedSubscription, AuditInfo auditInfo) {
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(originalSubscription.getApiId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                .oldValue(originalSubscription)
                .newValue(closedSubscription)
                .actor(auditInfo.actor())
                .createdAt(closedSubscription.getUpdatedAt())
                .properties(Collections.singletonMap(AuditProperties.APPLICATION, originalSubscription.getApplicationId()))
                .build()
        );
        auditDomainService.createApplicationAuditLog(
            ApplicationAuditLogEntity
                .builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .applicationId(originalSubscription.getApplicationId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                .oldValue(originalSubscription)
                .newValue(closedSubscription)
                .actor(auditInfo.actor())
                .createdAt(closedSubscription.getUpdatedAt())
                .properties(Collections.singletonMap(AuditProperties.API, originalSubscription.getApiId()))
                .build()
        );
    }

    private void revokeApiKeys(SubscriptionEntity subscriptionEntity, AuditInfo auditInfo) {
        var application = applicationCrudService.findById(
            new ExecutionContext(auditInfo.organizationId(), auditInfo.environmentId()),
            subscriptionEntity.getApplicationId()
        );
        if (!application.hasApiKeySharedMode()) {
            revokeApiKeyDomainService.revokeAllSubscriptionsApiKeys(subscriptionEntity, auditInfo);
        }
    }
}
