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
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    public SubscriptionEntity accept(
        String subscriptionId,
        ZonedDateTime startingAt,
        ZonedDateTime endingAt,
        String reason,
        String customKey,
        AuditInfo auditInfo
    ) {
        log.debug("Accept subscription {}", subscriptionId);

        final SubscriptionEntity subscriptionEntity = subscriptionCrudService.get(subscriptionId);
        return accept(subscriptionEntity, startingAt, endingAt, reason, customKey, auditInfo);
    }

    public SubscriptionEntity accept(
        SubscriptionEntity subscriptionEntity,
        ZonedDateTime startingAt,
        ZonedDateTime endingAt,
        String reason,
        String customKey,
        AuditInfo auditInfo
    ) {
        if (subscriptionEntity == null) {
            throw new IllegalArgumentException("Subscription should not be null");
        }
        checkSubscriptionStatus(subscriptionEntity);
        var plan = checkPlanStatus(subscriptionEntity);

        var acceptedSubscription = subscriptionEntity.acceptBy(auditInfo.actor().userId(), startingAt, endingAt, reason);

        if (plan.isApiKey()) {
            generateApiKeyDomainService.generate(acceptedSubscription, auditInfo, customKey);
        }
        subscriptionCrudService.update(acceptedSubscription);

        createAudit(subscriptionEntity, acceptedSubscription, auditInfo);

        triggerNotifications(auditInfo.organizationId(), acceptedSubscription);

        return acceptedSubscription;
    }

    private void checkSubscriptionStatus(SubscriptionEntity subscriptionEntity) {
        if (!subscriptionEntity.isPending()) {
            throw new IllegalStateException("Cannot accept subscription");
        }
    }

    private Plan checkPlanStatus(SubscriptionEntity subscriptionEntity) {
        var plan = planCrudService.findById(subscriptionEntity.getPlanId());
        if (plan.isClosed()) {
            throw new PlanAlreadyClosedException(plan.getId());
        }
        return plan;
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
