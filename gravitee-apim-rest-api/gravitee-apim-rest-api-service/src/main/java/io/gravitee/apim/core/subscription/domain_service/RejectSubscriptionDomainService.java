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

import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApplicationHookContext;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RejectSubscriptionDomainService {

    private final AuditDomainService auditDomainService;
    private final SubscriptionCrudService subscriptionRepository;
    private final PlanCrudService planCrudService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final UserCrudService userCrudService;

    public RejectSubscriptionDomainService(
        SubscriptionCrudService subscriptionRepository,
        PlanCrudService planCrudService,
        AuditDomainService auditDomainService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        UserCrudService userCrudService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planCrudService = planCrudService;
        this.triggerNotificationDomainService = triggerNotificationDomainService;
        this.auditDomainService = auditDomainService;
        this.userCrudService = userCrudService;
    }

    public SubscriptionEntity rejectSubscription(String subscriptionId, AuditInfo auditInfo) {
        log.debug("Close subscription {}", subscriptionId);

        final SubscriptionEntity subscriptionEntity = subscriptionRepository.get(subscriptionId);
        return rejectSubscription(subscriptionEntity, auditInfo);
    }

    public SubscriptionEntity rejectSubscription(SubscriptionEntity subscriptionEntity, AuditInfo auditInfo) {
        if (subscriptionEntity == null) {
            throw new IllegalArgumentException("Subscription should not be null");
        }
        checkSubscriptionStatus(subscriptionEntity);
        checkPlanStatus(subscriptionEntity);
        var rejectedSubscriptionEntity = subscriptionEntity.rejectBy(auditInfo.actor().userId());

        subscriptionRepository.update(rejectedSubscriptionEntity);
        triggerNotifications(auditInfo.organizationId(), rejectedSubscriptionEntity);
        createAudit(subscriptionEntity, rejectedSubscriptionEntity, auditInfo);

        return rejectedSubscriptionEntity;
    }

    private void triggerNotifications(String organizationId, SubscriptionEntity rejectedSubscriptionEntity) {
        var subscriberEmail = userCrudService
            .findBaseUserById(rejectedSubscriptionEntity.getSubscribedBy())
            .map(BaseUserEntity::getEmail)
            .filter(email -> !email.isEmpty())
            .map(email -> new Recipient("EMAIL", email));

        var additionalRecipients = subscriberEmail.map(List::of).orElse(Collections.emptyList());

        triggerNotificationDomainService.triggerApiNotification(
            organizationId,
            new SubscriptionRejectedApiHookContext(
                rejectedSubscriptionEntity.getApiId(),
                rejectedSubscriptionEntity.getApplicationId(),
                rejectedSubscriptionEntity.getPlanId(),
                rejectedSubscriptionEntity.getId()
            )
        );
        triggerNotificationDomainService.triggerApplicationNotification(
            organizationId,
            new SubscriptionRejectedApplicationHookContext(
                rejectedSubscriptionEntity.getApplicationId(),
                rejectedSubscriptionEntity.getApiId(),
                rejectedSubscriptionEntity.getPlanId(),
                rejectedSubscriptionEntity.getId()
            ),
            additionalRecipients
        );
    }

    private static void checkSubscriptionStatus(SubscriptionEntity subscriptionEntity) {
        if (!subscriptionEntity.isPending()) {
            throw new IllegalStateException("Cannot reject subscription");
        }
    }

    private void createAudit(SubscriptionEntity subscriptionEntity, SubscriptionEntity rejectedSubscriptionEntity, AuditInfo auditInfo) {
        auditDomainService.createApiAuditLog(
            ApiAuditLogEntity
                .builder()
                .actor(auditInfo.actor())
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(subscriptionEntity.getApiId())
                .event(SubscriptionAuditEvent.SUBSCRIPTION_UPDATED)
                .oldValue(subscriptionEntity)
                .newValue(rejectedSubscriptionEntity)
                .createdAt(rejectedSubscriptionEntity.getClosedAt())
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
                .newValue(rejectedSubscriptionEntity)
                .createdAt(rejectedSubscriptionEntity.getClosedAt())
                .properties(Collections.singletonMap(AuditProperties.API, subscriptionEntity.getApiId()))
                .build()
        );
    }

    private void checkPlanStatus(SubscriptionEntity subscriptionEntity) {
        final GenericPlanEntity plan = planCrudService.findById(subscriptionEntity.getPlanId());
        if (plan.isClosed()) {
            throw new PlanAlreadyClosedException(plan.getId());
        }
    }
}
