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
package io.gravitee.apim.core.subscription.use_case.api_product;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.CreateSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@CustomLog
@RequiredArgsConstructor
public class CreateApiProductSubscriptionUseCase {

    private final CreateSubscriptionDomainService createSubscriptionDomainService;
    private final SubscriptionCrudService subscriptionCrudService;
    private final PlanCrudService planCrudService;
    private final ApiProductCrudService apiProductCrudService;

    public Output execute(Input input) {
        log.debug(
            "Creating subscription for API Product {} with plan {} and application {}",
            input.apiProductId,
            input.planId,
            input.applicationId
        );
        // Verify API Product exists
        apiProductCrudService.get(input.apiProductId);

        // Get and validate plan belongs to API Product
        var plan = planCrudService.getById(input.planId);
        if (
            plan.getReferenceType() == null ||
            !plan.getReferenceType().equals(GenericPlanEntity.ReferenceType.API_PRODUCT) ||
            !plan.getReferenceId().equals(input.apiProductId)
        ) {
            throw new PlanNotFoundException(input.planId);
        }

        // Create subscription using domain service
        io.gravitee.rest.api.model.SubscriptionEntity createdSubscriptionEntity = createSubscriptionDomainService.create(
            input.auditInfo,
            input.planId,
            input.applicationId,
            input.requestMessage,
            input.customApiKey,
            input.configuration,
            input.metadata,
            input.generalConditionsAccepted,
            input.generalConditionsContentRevision
        );

        // SubscriptionServiceImpl.create already sets referenceId and referenceType from the plan
        SubscriptionEntity createdSubscription = subscriptionCrudService.get(createdSubscriptionEntity.getId());

        log.debug("Created subscription {} for API Product {}", createdSubscription.getId(), input.apiProductId);
        return new Output(createdSubscription);
    }

    @Builder
    public record Input(
        String apiProductId,
        String planId,
        String applicationId,
        String requestMessage,
        String customApiKey,
        io.gravitee.apim.core.subscription.model.SubscriptionConfiguration configuration,
        java.util.Map<String, String> metadata,
        Boolean generalConditionsAccepted,
        io.gravitee.rest.api.model.PageEntity.PageRevisionId generalConditionsContentRevision,
        AuditInfo auditInfo
    ) {}

    public record Output(SubscriptionEntity subscription) {}
}
