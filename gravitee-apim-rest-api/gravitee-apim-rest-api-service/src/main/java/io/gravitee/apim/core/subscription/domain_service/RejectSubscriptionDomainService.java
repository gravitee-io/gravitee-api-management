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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApplicationAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.SubscriptionAuditEvent;
import io.gravitee.apim.core.membership.domain_service.ApplicationPrimaryOwnerDomainService;
import io.gravitee.apim.core.notification.domain_service.TriggerNotificationDomainService;
import io.gravitee.apim.core.notification.model.Recipient;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApiHookContext;
import io.gravitee.apim.core.notification.model.hook.SubscriptionRejectedApplicationHookContext;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.user.crud_service.UserCrudService;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@DomainService
public class RejectSubscriptionDomainService {

    private final AuditDomainService auditDomainService;
    private final SubscriptionCrudService subscriptionRepository;
    private final PlanCrudService planCrudService;
    private final TriggerNotificationDomainService triggerNotificationDomainService;
    private final UserCrudService userCrudService;
    private final ApplicationPrimaryOwnerDomainService applicationPrimaryOwnerDomainService;

    public RejectSubscriptionDomainService(
        SubscriptionCrudService subscriptionRepository,
        PlanCrudService planCrudService,
        AuditDomainService auditDomainService,
        TriggerNotificationDomainService triggerNotificationDomainService,
        UserCrudService userCrudService,
        ApplicationPrimaryOwnerDomainService applicationPrimaryOwnerDomainService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planCrudService = planCrudService;
        this.triggerNotificationDomainService = triggerNotificationDomainService;
        this.auditDomainService = auditDomainService;
        this.userCrudService = userCrudService;
        this.applicationPrimaryOwnerDomainService = applicationPrimaryOwnerDomainService;
    }

    public SubscriptionEntity reject(String subscriptionId, String reason, AuditInfo auditInfo) {
        log.debug("Close subscription {}", subscriptionId);

        final SubscriptionEntity subscriptionEntity = subscriptionRepository.get(subscriptionId);
        return reject(subscriptionEntity, reason, auditInfo);
    }

    public SubscriptionEntity reject(SubscriptionEntity subscriptionEntity, String reason, AuditInfo auditInfo) {
        if (subscriptionEntity == null) {
            throw new IllegalArgumentException("Subscription should not be null");
        }
        var rejectedSubscriptionEntity = subscriptionEntity.rejectBy(auditInfo.actor().userId(), reason);

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

        var applicationPrimaryOwner = applicationPrimaryOwnerDomainService.getApplicationPrimaryOwner(
            organizationId,
            rejectedSubscriptionEntity.getApplicationId()
        );

        triggerNotificationDomainService.triggerApiNotification(
            organizationId,
            new SubscriptionRejectedApiHookContext(
                rejectedSubscriptionEntity.getApiId(),
                rejectedSubscriptionEntity.getApplicationId(),
                rejectedSubscriptionEntity.getPlanId(),
                rejectedSubscriptionEntity.getId(),
                applicationPrimaryOwner.id()
            )
        );
        triggerNotificationDomainService.triggerApplicationNotification(
            organizationId,
            new SubscriptionRejectedApplicationHookContext(
                rejectedSubscriptionEntity.getApplicationId(),
                rejectedSubscriptionEntity.getApiId(),
                rejectedSubscriptionEntity.getPlanId(),
                rejectedSubscriptionEntity.getId(),
                applicationPrimaryOwner.id()
            ),
            additionalRecipients
        );
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
}
