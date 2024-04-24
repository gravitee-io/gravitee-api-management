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
import io.gravitee.apim.core.api_key.domain_service.GenerateApiKeyDomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.SubscriptionAcceptedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionAcceptedApplicationHookContext;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@DomainService
@Slf4j
public class AcceptSubscriptionDomainService {

    private final SubscriptionCrudService subscriptionCrudService;
    private final AuditDomainService auditDomainService;
    private final PlanCrudService planCrudService;
    private final GenerateApiKeyDomainService generateApiKeyDomainService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final UserCrudService userCrudService;

    public AcceptSubscriptionDomainService(
        SubscriptionCrudService subscriptionCrudService,
        AuditDomainService auditDomainService,
        PlanCrudService planCrudService,
        GenerateApiKeyDomainService generateApiKeyDomainService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        UserCrudService userCrudService
    ) {
        this.subscriptionCrudService = subscriptionCrudService;
        this.auditDomainService = auditDomainService;
        this.planCrudService = planCrudService;
        this.generateApiKeyDomainService = generateApiKeyDomainService;
        this.triggerNotificationDomainService = triggerNotificationDomainService;
        this.userCrudService = userCrudService;
    }

    /**
     * Auto accept a subscription when Plan is configured with AUTO validation.
     * @param subscriptionId The subscription to accept.
     * @param startingAt The starting date of the subscription.
     * @param endingAt The ending date of the subscription. Can be null for non expiring subscription.
     * @param reason The optional reason of accepting the subscription.
     * @param customKey The optional custom key to use for API Key subscription.
     * @param auditInfo Audit information about whom accepting the subscription.
     * @return The accepted subscription.
     */
    public SubscriptionEntity autoAccept(
        String subscriptionId,
        ZonedDateTime startingAt,
        ZonedDateTime endingAt,
        String reason,
        String customKey,
        AuditInfo auditInfo
    ) {
        log.debug("Auto accepting subscription {}", subscriptionId);

        var subscription = subscriptionCrudService.get(subscriptionId);
        var plan = planCrudService.findById(subscription.getPlanId());
        return accept(subscription, plan, startingAt, endingAt, reason, customKey, auditInfo);
    }

    /**
     * Accept a subscription.
     * @param subscription The subscription to accept.
     * @param plan The subscribed plan.
     * @param startingAt The starting date of the subscription.
     * @param endingAt The ending date of the subscription. Can be null for non expiring subscription.
     * @param reason The optional reason of accepting the subscription.
     * @param customKey The optional custom key to use for API Key subscription.
     * @param auditInfo Audit information about whom accepting the subscription.
     * @return The accepted subscription.
     */
    public SubscriptionEntity accept(
        SubscriptionEntity subscription,
        Plan plan,
        ZonedDateTime startingAt,
        ZonedDateTime endingAt,
        String reason,
        String customKey,
        AuditInfo auditInfo
    ) {
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription should not be null");
        }

        var acceptedSubscription = subscription.acceptBy(auditInfo.actor().userId(), startingAt, endingAt, reason);

        if (plan.isApiKey()) {
            generateApiKeyDomainService.generate(acceptedSubscription, auditInfo, customKey);
        }
        subscriptionCrudService.update(acceptedSubscription);

        createAudit(subscription, acceptedSubscription, auditInfo);

        triggerNotifications(auditInfo.organizationId(), acceptedSubscription);

        return acceptedSubscription;
    }

    private void createAudit(SubscriptionEntity subscriptionEntity, SubscriptionEntity acceptedSubscriptionEntity, AuditInfo auditInfo) {
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .actor(auditInfo.actor())
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(subscriptionEntity.getApiId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .oldValue(subscriptionEntity)
                .newValue(acceptedSubscriptionEntity)
                .createdAt(acceptedSubscriptionEntity.getProcessedAt())
                .properties(Collections.singletonMap(AuditProperties.APPLICATION, subscriptionEntity.getApplicationId()))
                .build()
        );
        auditDomainService.createApplicationAuditLog(
            ApplicationAuditLogEntity
                .builder()
                .actor(auditInfo.actor())
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .applicationId(subscriptionEntity.getApplicationId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .oldValue(subscriptionEntity)
                .newValue(acceptedSubscriptionEntity)
                .createdAt(acceptedSubscriptionEntity.getProcessedAt())
                .properties(Collections.singletonMap(AuditProperties.API, subscriptionEntity.getApiId()))
                .build()
        );
    }

    private void triggerNotifications(String organizationId, SubscriptionEntity acceptedSubscription) {
        var subscriberEmail = userCrudService
            .findBaseUserById(acceptedSubscription.getSubscribedBy())
            .map(BaseUserEntity::getEmail)
            .filter(email -> !email.isEmpty())
            .map(email -> new Recipient("EMAIL", email));

        var additionalRecipients = subscriberEmail.map(List::of).orElse(Collections.emptyList());

        triggerNotificationDomainService.triggerApiNotification(
            organizationId,
            new SubscriptionAcceptedApiHookContext(
                acceptedSubscription.getApiId(),
                acceptedSubscription.getApplicationId(),
                acceptedSubscription.getPlanId(),
                acceptedSubscription.getId()
            )
        );
        triggerNotificationDomainService.triggerApplicationNotification(
            organizationId,
            new SubscriptionAcceptedApplicationHookContext(
                acceptedSubscription.getApplicationId(),
                acceptedSubscription.getApiId(),
                acceptedSubscription.getPlanId(),
                acceptedSubscription.getId()
            ),
            additionalRecipients
        );
    }
}
