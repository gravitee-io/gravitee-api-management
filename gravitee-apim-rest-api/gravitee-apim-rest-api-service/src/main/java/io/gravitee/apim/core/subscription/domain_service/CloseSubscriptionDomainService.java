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

import io.gravitee.apim.core.api_key.domain_service.RevokeApiKeyDomainService;
import io.gravitee.apim.core.application.crud_service.ApplicationCrudService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionClosedApplicationHookContext;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CloseSubscriptionDomainService {

    private final SubscriptionCrudService subscriptionCrudService;
    private final RejectSubscriptionDomainService rejectSubscriptionDomainService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final AuditDomainService auditDomainService;
    private final ApplicationCrudService applicationCrudService;
    private final RevokeApiKeyDomainService revokeApiKeyDomainService;

    public CloseSubscriptionDomainService(
        SubscriptionCrudService subscriptionCrudService,
        ApplicationCrudService applicationCrudService,
        AuditDomainService auditDomainService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        RejectSubscriptionDomainService rejectSubscriptionDomainService,
        RevokeApiKeyDomainService revokeApiKeyDomainService
    ) {
        this.subscriptionCrudService = subscriptionCrudService;
        this.rejectSubscriptionDomainService = rejectSubscriptionDomainService;
        this.triggerNotificationDomainService = triggerNotificationDomainService;
        this.auditDomainService = auditDomainService;
        this.applicationCrudService = applicationCrudService;
        this.revokeApiKeyDomainService = revokeApiKeyDomainService;
    }

    public SubscriptionEntity closeSubscription(String subscriptionId, AuditInfo auditInfo) {
        log.debug("Close subscription {}", subscriptionId);
        var subscription = subscriptionCrudService.get(subscriptionId);

        return switch (subscription.getStatus()) {
            case ACCEPTED, PAUSED -> closeAcceptedOrPausedSubscription(subscription, auditInfo);
            case PENDING -> rejectSubscriptionDomainService.rejectSubscription(subscription, auditInfo);
            case CLOSED, REJECTED -> subscription;
        };
    }

    private SubscriptionEntity closeAcceptedOrPausedSubscription(SubscriptionEntity subscriptionEntity, AuditInfo auditInfo) {
        var closedSubscriptionEntity = subscriptionCrudService.update(subscriptionEntity.close());

        triggerNotifications(closedSubscriptionEntity, auditInfo);

        createAuditLog(subscriptionEntity, closedSubscriptionEntity, auditInfo);

        revokeApiKeys(subscriptionEntity, auditInfo);

        return closedSubscriptionEntity;
    }

    private void triggerNotifications(SubscriptionEntity closedSubscriptionEntity, AuditInfo auditInfo) {
        triggerNotificationDomainService.triggerApiNotification(
            auditInfo.organizationId(),
            new SubscriptionClosedApiHookContext(
                closedSubscriptionEntity.getApiId(),
                closedSubscriptionEntity.getApplicationId(),
                closedSubscriptionEntity.getPlanId()
            )
        );
        triggerNotificationDomainService.triggerApplicationNotification(
            auditInfo.organizationId(),
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
