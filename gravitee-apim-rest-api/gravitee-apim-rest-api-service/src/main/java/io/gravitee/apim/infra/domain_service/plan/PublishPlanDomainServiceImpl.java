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
package io.gravitee.apim.infra.domain_service.plan;

import static io.gravitee.repository.management.model.Plan.AuditEvent.PLAN_PUBLISHED;
import static java.util.Map.entry;

import io.gravitee.apim.core.membership.domain_service.PublishPlanDomainService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.infra.adapter.PlanAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.PlanRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.KeylessPlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyDeprecatedException;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyPublishedException;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
public class PublishPlanDomainServiceImpl implements PublishPlanDomainService {

    private final PlanRepository planRepository;
    private final AuditService auditService;

    @Autowired
    public PublishPlanDomainServiceImpl(@Lazy PlanRepository planRepository, @Lazy AuditService auditService) {
        this.planRepository = planRepository;
        this.auditService = auditService;
    }

    @Override
    public Plan publish(final ExecutionContext executionContext, String planId) {
        try {
            log.debug("Publish plan {}", planId);

            io.gravitee.repository.management.model.Plan plan = planRepository
                .findById(planId)
                .orElseThrow(() -> new PlanNotFoundException(planId));
            io.gravitee.repository.management.model.Plan previousPlan = new io.gravitee.repository.management.model.Plan(plan);
            if (plan.getStatus() == io.gravitee.repository.management.model.Plan.Status.CLOSED) {
                throw new PlanAlreadyClosedException(planId);
            } else if (plan.getStatus() == io.gravitee.repository.management.model.Plan.Status.PUBLISHED) {
                throw new PlanAlreadyPublishedException(planId);
            } else if (plan.getStatus() == io.gravitee.repository.management.model.Plan.Status.DEPRECATED) {
                throw new PlanAlreadyDeprecatedException(planId);
            }

            Set<io.gravitee.repository.management.model.Plan> plans = planRepository.findByReferenceIdAndReferenceType(
                plan.getReferenceId(),
                plan.getReferenceType()
            );
            if (plan.getSecurity() == io.gravitee.repository.management.model.Plan.PlanSecurityType.KEY_LESS) {
                // Look to other plans if there is already a keyless-published plan
                long count = plans
                    .stream()
                    .filter(
                        plan1 ->
                            plan1.getStatus() == io.gravitee.repository.management.model.Plan.Status.PUBLISHED ||
                            plan1.getStatus() == io.gravitee.repository.management.model.Plan.Status.DEPRECATED
                    )
                    .filter(plan1 -> plan1.getSecurity() == io.gravitee.repository.management.model.Plan.PlanSecurityType.KEY_LESS)
                    .count();

                if (count > 0) {
                    throw new KeylessPlanAlreadyPublishedException(planId);
                }
            }

            // Update plan status
            plan.setStatus(io.gravitee.repository.management.model.Plan.Status.PUBLISHED);
            // Update plan order
            List<io.gravitee.repository.management.model.Plan> orderedPublishedPlans = plans
                .stream()
                .filter(plan1 -> io.gravitee.repository.management.model.Plan.Status.PUBLISHED.equals(plan1.getStatus()))
                .sorted(Comparator.comparingInt(io.gravitee.repository.management.model.Plan::getOrder))
                .collect(Collectors.toList());
            plan.setOrder(
                orderedPublishedPlans.isEmpty() ? 1 : (orderedPublishedPlans.get(orderedPublishedPlans.size() - 1).getOrder() + 1)
            );

            plan.setPublishedAt(new Date());
            plan.setUpdatedAt(plan.getPublishedAt());
            plan.setNeedRedeployAt(plan.getPublishedAt());

            // Save plan
            plan = planRepository.update(plan);

            // Audit
            AuditService.AuditLogData auditLogData = AuditService.AuditLogData.builder()
                .properties(Collections.singletonMap(Audit.AuditProperties.PLAN, plan.getId()))
                .event(PLAN_PUBLISHED)
                .createdAt(plan.getUpdatedAt())
                .oldValue(previousPlan)
                .newValue(plan)
                .build();

            if (plan.getReferenceType() == io.gravitee.repository.management.model.Plan.PlanReferenceType.API_PRODUCT) {
                auditService.createApiProductAuditLog(executionContext, auditLogData, plan.getReferenceId());
            } else {
                auditService.createApiAuditLog(executionContext, auditLogData, plan.getReferenceId());
            }

            return PlanAdapter.INSTANCE.fromRepository(plan);
        } catch (TechnicalException ex) {
            log.error("An error occurs while trying to publish plan: {}", planId, ex);
            throw new TechnicalManagementException(String.format("An error occurs while trying to publish plan: %s", planId), ex);
        }
    }
}
