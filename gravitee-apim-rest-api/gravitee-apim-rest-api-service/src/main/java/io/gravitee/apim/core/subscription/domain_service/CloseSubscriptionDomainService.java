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
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
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
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.definition.model.federation.FederatedApi;
import io.gravitee.rest.api.model.context.OriginContext;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.Collections;
import lombok.CustomLog;

@CustomLog
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
        Api api = null;
        if (!SubscriptionReferenceType.API_PRODUCT.equals(subscription.getReferenceType()) && subscription.getReferenceId() != null) {
            api = apiCrudService.get(subscription.getReferenceId());
        }
        return closeSubscription(subscription, api, auditInfo);
    }

    public SubscriptionEntity closeSubscription(String subscriptionId, Api api, AuditInfo auditInfo) {
        var subscription = subscriptionCrudService.get(subscriptionId);
        return closeSubscription(subscription, api, auditInfo);
    }

    public SubscriptionEntity closeSubscription(SubscriptionEntity subscription, AuditInfo auditInfo) {
        Api api = null;
        if (!SubscriptionReferenceType.API_PRODUCT.equals(subscription.getReferenceType()) && subscription.getReferenceId() != null) {
            api = apiCrudService.get(subscription.getReferenceId());
        }
        return closeSubscription(subscription, api, auditInfo);
    }

    public SubscriptionEntity closeSubscription(SubscriptionEntity subscription, Api api, AuditInfo auditInfo) {
        log.debug("Close subscription {}", subscription.getId());

        return switch (subscription.getStatus()) {
            case ACCEPTED, PAUSED -> closeAcceptedOrPausedSubscription(subscription, api, auditInfo);
            case PENDING -> rejectSubscription(subscription, auditInfo);
            case CLOSED, REJECTED -> subscription;
        };
    }

    private SubscriptionEntity closeAcceptedOrPausedSubscription(SubscriptionEntity subscriptionEntity, Api api, AuditInfo auditInfo) {
        boolean isApiProduct = SubscriptionReferenceType.API_PRODUCT.equals(subscriptionEntity.getReferenceType());

        // Handle federated API unsubscribe (only for API subscriptions)
        if (!isApiProduct && api != null) {
            var definition = api.getApiDefinitionValue();
            if (
                definition instanceof FederatedApi federatedApi &&
                api.getOriginContext() instanceof
                    OriginContext.Integration(Object ignored, String integrationId, Object ignored1, Object ignored2)
            ) {
                integrationAgent.unsubscribe(integrationId, federatedApi, subscriptionEntity).blockingAwait();
            }
        }

        var closedSubscriptionEntity = subscriptionCrudService.update(subscriptionEntity.close());

        if (!isApiProduct) {
            triggerNotifications(closedSubscriptionEntity, auditInfo);
        } else {
            // Notifications are not applicable for API Product subscriptions (TODO: implement notifications for API Product subscriptions)
        }

        if (isApiProduct) {
            createApiProductAuditLog(subscriptionEntity, closedSubscriptionEntity, auditInfo);
        } else {
            createAuditLog(subscriptionEntity, closedSubscriptionEntity, auditInfo);
        }

        revokeApiKeys(subscriptionEntity, auditInfo);

        return closedSubscriptionEntity;
    }

    private SubscriptionEntity rejectSubscription(SubscriptionEntity subscriptionEntity, AuditInfo auditInfo) {
        boolean isApiProduct = SubscriptionReferenceType.API_PRODUCT.equals(subscriptionEntity.getReferenceType());

        if (isApiProduct) {
            var rejectedSubscriptionEntity = subscriptionEntity.rejectBy(auditInfo.actor().userId(), "Subscription has been closed.");
            var updatedSubscriptionEntity = subscriptionCrudService.update(rejectedSubscriptionEntity);
            createApiProductRejectAuditLog(subscriptionEntity, updatedSubscriptionEntity, auditInfo);
            return updatedSubscriptionEntity;
        } else {
            return rejectSubscriptionDomainService.reject(subscriptionEntity, "Subscription has been closed.", auditInfo);
        }
    }

    private void triggerNotifications(SubscriptionEntity closedSubscriptionEntity, AuditInfo auditInfo) {
        triggerNotificationDomainService.triggerApiNotification(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            new SubscriptionClosedApiHookContext(
                closedSubscriptionEntity.getReferenceId(),
                closedSubscriptionEntity.getApplicationId(),
                closedSubscriptionEntity.getPlanId()
            )
        );
        triggerNotificationDomainService.triggerApplicationNotification(
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            new SubscriptionClosedApplicationHookContext(
                closedSubscriptionEntity.getApplicationId(),
                closedSubscriptionEntity.getReferenceId(),
                closedSubscriptionEntity.getPlanId()
            )
        );
    }

    private void createAuditLog(SubscriptionEntity originalSubscription, SubscriptionEntity closedSubscription, AuditInfo auditInfo) {
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(originalSubscription.getReferenceId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                .oldValue(originalSubscription)
                .newValue(closedSubscription)
                .actor(auditInfo.actor())
                .createdAt(closedSubscription.getUpdatedAt())
                .properties(Collections.singletonMap(AuditProperties.APPLICATION, originalSubscription.getApplicationId()))
                .build()
        );
        auditDomainService.createApplicationAuditLog(
            ApplicationAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .applicationId(originalSubscription.getApplicationId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                .oldValue(originalSubscription)
                .newValue(closedSubscription)
                .actor(auditInfo.actor())
                .createdAt(closedSubscription.getUpdatedAt())
                .properties(Collections.singletonMap(AuditProperties.API, originalSubscription.getReferenceId()))
                .build()
        );
    }

    private void createApiProductAuditLog(
        SubscriptionEntity originalSubscription,
        SubscriptionEntity closedSubscription,
        AuditInfo auditInfo
    ) {
        auditDomainService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiProductId(originalSubscription.getReferenceId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                .oldValue(originalSubscription)
                .newValue(closedSubscription)
                .actor(auditInfo.actor())
                .createdAt(closedSubscription.getUpdatedAt())
                .properties(Collections.singletonMap(AuditProperties.APPLICATION, originalSubscription.getApplicationId()))
                .build()
        );
        auditDomainService.createApplicationAuditLog(
            ApplicationAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .applicationId(originalSubscription.getApplicationId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_CLOSED)
                .oldValue(originalSubscription)
                .newValue(closedSubscription)
                .actor(auditInfo.actor())
                .createdAt(closedSubscription.getUpdatedAt())
                .properties(Collections.singletonMap(AuditProperties.API_PRODUCT, originalSubscription.getReferenceId()))
                .build()
        );
    }

    private void createApiProductRejectAuditLog(
        SubscriptionEntity originalSubscription,
        SubscriptionEntity rejectedSubscription,
        AuditInfo auditInfo
    ) {
        auditDomainService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiProductId(originalSubscription.getReferenceId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .oldValue(originalSubscription)
                .newValue(rejectedSubscription)
                .actor(auditInfo.actor())
                .createdAt(rejectedSubscription.getClosedAt())
                .properties(Collections.singletonMap(AuditProperties.APPLICATION, originalSubscription.getApplicationId()))
                .build()
        );
        auditDomainService.createApplicationAuditLog(
            ApplicationAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .applicationId(originalSubscription.getApplicationId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .oldValue(originalSubscription)
                .newValue(rejectedSubscription)
                .actor(auditInfo.actor())
                .createdAt(rejectedSubscription.getClosedAt())
                .properties(Collections.singletonMap(AuditProperties.API_PRODUCT, originalSubscription.getReferenceId()))
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
