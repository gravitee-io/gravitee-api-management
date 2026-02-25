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
import io.gravitee.apim.core.api.crud_service.ApiCrudService;
import io.gravitee.apim.core.api_product.crud_service.ApiProductCrudService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.CreateSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.rest.api.model.v4.plan.GenericPlanEntity;
import io.gravitee.rest.api.service.exceptions.PlanNotFoundException;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@CustomLog
@RequiredArgsConstructor
public class CreateSubscriptionUseCase {

    private final CreateSubscriptionDomainService createSubscriptionDomainService;
    private final SubscriptionCrudService subscriptionCrudService;
    private final PlanCrudService planCrudService;
    private final ApiCrudService apiCrudService;
    private final ApiProductCrudService apiProductCrudService;

    public Output execute(Input input) {
        log.debug(
            "Creating subscription for {} {} with plan {} and application {}",
            input.referenceType,
            input.referenceId,
            input.planId,
            input.applicationId
        );

        verifyReferenceExists(input.referenceId, input.referenceType);

        var plan = planCrudService.getById(input.planId);
        validatePlanBelongsToReference(plan, input.referenceId, input.referenceType);

        if (plan.getPlanStatus() == PlanStatus.DEPRECATED) {
            throw new PlanNotFoundException(input.planId);
        }

        io.gravitee.rest.api.model.SubscriptionEntity createdSubscriptionEntity = createSubscriptionDomainService.create(
            input.auditInfo,
            input.planId,
            input.applicationId,
            input.requestMessage,
            input.customApiKey,
            input.configuration,
            input.metadata,
            input.apiKeyMode,
            input.generalConditionsAccepted,
            input.generalConditionsContentRevision
        );

        SubscriptionEntity createdSubscription = subscriptionCrudService.get(createdSubscriptionEntity.getId());

        log.debug("Created subscription {} for {} {}", createdSubscription.getId(), input.referenceType, input.referenceId);
        return new Output(createdSubscription);
    }

    private void verifyReferenceExists(String referenceId, SubscriptionReferenceType referenceType) {
        if (referenceType == SubscriptionReferenceType.API) {
            apiCrudService.get(referenceId);
        } else if (referenceType == SubscriptionReferenceType.API_PRODUCT) {
            apiProductCrudService.get(referenceId);
        }
    }

    private void validatePlanBelongsToReference(Plan plan, String referenceId, SubscriptionReferenceType referenceType) {
        GenericPlanEntity.ReferenceType expectedReferenceType = referenceType == SubscriptionReferenceType.API
            ? GenericPlanEntity.ReferenceType.API
            : GenericPlanEntity.ReferenceType.API_PRODUCT;

        if (
            plan.getReferenceType() == null ||
            !plan.getReferenceType().equals(expectedReferenceType) ||
            !plan.getReferenceId().equals(referenceId)
        ) {
            throw new PlanNotFoundException(plan.getId());
        }
    }

    @Builder
    public record Input(
        String referenceId,
        SubscriptionReferenceType referenceType,
        String planId,
        String applicationId,
        String requestMessage,
        String customApiKey,
        io.gravitee.apim.core.subscription.model.SubscriptionConfiguration configuration,
        java.util.Map<String, String> metadata,
        io.gravitee.rest.api.model.ApiKeyMode apiKeyMode,
        Boolean generalConditionsAccepted,
        io.gravitee.rest.api.model.PageEntity.PageRevisionId generalConditionsContentRevision,
        AuditInfo auditInfo
    ) {}

    public record Output(SubscriptionEntity subscription) {}
}
