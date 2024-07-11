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
package io.gravitee.apim.core.subscription.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.AcceptSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.rest.api.service.exceptions.PlanAlreadyClosedException;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import java.time.ZonedDateTime;
import lombok.Builder;

@UseCase
public class AcceptSubscriptionUseCase {

    private final SubscriptionCrudService subscriptionCrudService;
    private final PlanCrudService planCrudService;
    private final AcceptSubscriptionDomainService acceptSubscriptionDomainService;

    public AcceptSubscriptionUseCase(
        SubscriptionCrudService subscriptionCrudService,
        PlanCrudService planCrudService,
        AcceptSubscriptionDomainService acceptSubscriptionDomainService
    ) {
        this.subscriptionCrudService = subscriptionCrudService;
        this.planCrudService = planCrudService;
        this.acceptSubscriptionDomainService = acceptSubscriptionDomainService;
    }

    public Output execute(Input input) {
        var subscription = subscriptionCrudService.get(input.subscriptionId);
        if (!input.apiId.equalsIgnoreCase(subscription.getApiId())) {
            throw new SubscriptionNotFoundException(input.subscriptionId);
        }

        checkSubscriptionStatus(subscription);
        var plan = checkPlanStatus(subscription);

        return new Output(
            acceptSubscriptionDomainService.accept(
                subscription,
                plan,
                input.startingAt,
                input.endingAt,
                input.reasonMessage,
                input.customKey,
                input.auditInfo
            )
        );
    }

    private void checkSubscriptionStatus(SubscriptionEntity subscriptionEntity) {
        if (!subscriptionEntity.isPending()) {
            throw new IllegalStateException("Cannot accept subscription");
        }
    }

    private Plan checkPlanStatus(SubscriptionEntity subscriptionEntity) {
        var plan = planCrudService.getById(subscriptionEntity.getPlanId());
        if (plan.isClosed()) {
            throw new PlanAlreadyClosedException(plan.getId());
        }
        return plan;
    }

    @Builder
    public record Input(
        String apiId,
        String subscriptionId,
        ZonedDateTime startingAt,
        ZonedDateTime endingAt,
        String reasonMessage,
        String customKey,
        AuditInfo auditInfo
    ) {}

    public record Output(SubscriptionEntity subscription) {}
}
