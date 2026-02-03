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
package io.gravitee.apim.core.plan.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.ApiAuditLogEntity;
import io.gravitee.apim.core.audit.model.ApiProductAuditLogEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.event.PlanAuditEvent;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.query_service.SubscriptionQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import java.util.List;
import java.util.Map;

@DomainService
public class ClosePlanDomainService {

    private final PlanCrudService planCrudService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final AuditDomainService auditService;
    private final CloseSubscriptionDomainService closeSubscriptionDomainService;

    public ClosePlanDomainService(
        PlanCrudService planCrudService,
        SubscriptionQueryService subscriptionQueryService,
        AuditDomainService auditService,
        CloseSubscriptionDomainService closeSubscriptionDomainService
    ) {
        this.planCrudService = planCrudService;
        this.subscriptionQueryService = subscriptionQueryService;
        this.auditService = auditService;
        this.closeSubscriptionDomainService = closeSubscriptionDomainService;
    }

    public void close(String planId, AuditInfo auditInfo) {
        var planToClose = planCrudService.getById(planId);

        // For API Product plans, close all active subscriptions when closing the plan
        if (planToClose.getReferenceType() == GenericPlanEntity.ReferenceType.API_PRODUCT) {
            List<SubscriptionEntity> activeSubscriptions = subscriptionQueryService.findActiveSubscriptionsByPlan(planId);
            for (SubscriptionEntity subscription : activeSubscriptions) {
                try {
                    closeSubscriptionDomainService.closeSubscriptionForApiProduct(subscription.getId(), auditInfo);
                } catch (Exception e) {
                    // Log but continue closing other subscriptions
                    // Subscription status could not be closed (already closed or rejected)
                }
            }
        } else {
            // For API plans, throw exception if there are active subscriptions
            if (!subscriptionQueryService.findActiveSubscriptionsByPlan(planId).isEmpty()) {
                throw new ValidationDomainException("Impossible to close a plan with active subscriptions");
            }
        }

        final Plan closedPlan = planToClose.close();

        var planUpdated = planCrudService.update(closedPlan);
        createAuditLog(closedPlan, planUpdated, auditInfo);
    }

    private void createAuditLog(Plan originalPlan, Plan planUpdated, AuditInfo auditInfo) {
        if (GenericPlanEntity.ReferenceType.API_PRODUCT.equals(originalPlan.getReferenceType())) {
            createApiProductAuditLog(originalPlan, planUpdated, auditInfo);
        } else {
            createApiAuditLog(originalPlan, planUpdated, auditInfo);
        }
    }

    private void createApiProductAuditLog(Plan originalPlan, Plan planUpdated, AuditInfo auditInfo) {
        auditService.createApiProductAuditLog(
            ApiProductAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiProductId(originalPlan.getReferenceId())
                .event(PlanAuditEvent.PLAN_CLOSED)
                .actor(auditInfo.actor())
                .oldValue(originalPlan)
                .newValue(planUpdated)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.PLAN, originalPlan.getId()))
                .build()
        );
    }

    private void createApiAuditLog(Plan planToClose, Plan planUpdated, AuditInfo auditInfo) {
        auditService.createApiAuditLog(
            ApiAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .apiId(planToClose.getReferenceId())
                .event(PlanAuditEvent.PLAN_CLOSED)
                .actor(auditInfo.actor())
                .oldValue(planToClose)
                .newValue(planUpdated)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.PLAN, planToClose.getId()))
                .build()
        );
    }
}
