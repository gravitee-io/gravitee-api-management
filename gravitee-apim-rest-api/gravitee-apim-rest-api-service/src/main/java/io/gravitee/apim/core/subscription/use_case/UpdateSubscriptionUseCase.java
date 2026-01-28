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
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.UpdateSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@CustomLog
@RequiredArgsConstructor
public class UpdateSubscriptionUseCase {

    private final UpdateSubscriptionDomainService updateSubscriptionDomainService;
    private final SubscriptionCrudService subscriptionCrudService;
    private final ApiCrudService apiCrudService;
    private final ApiProductCrudService apiProductCrudService;

    public Output execute(Input input) {
        log.debug("Updating subscription {} for {} {}", input.subscriptionId, input.referenceType, input.referenceId);

        verifyReferenceExists(input.referenceId, input.referenceType);

        var subscription = subscriptionCrudService.get(input.subscriptionId);
        validateSubscription(subscription, input.referenceId, input.referenceType);

        updateSubscriptionDomainService.update(
            input.auditInfo,
            input.subscriptionId,
            input.configuration,
            input.metadata,
            input.startingAt,
            input.endingAt
        );

        SubscriptionEntity updatedSubscription = subscriptionCrudService.get(input.subscriptionId);

        log.debug("Updated subscription {} for {} {}", input.subscriptionId, input.referenceType, input.referenceId);
        return new Output(updatedSubscription);
    }

    private void verifyReferenceExists(String referenceId, SubscriptionReferenceType referenceType) {
        if (referenceType == SubscriptionReferenceType.API) {
            apiCrudService.get(referenceId);
        } else if (referenceType == SubscriptionReferenceType.API_PRODUCT) {
            apiProductCrudService.get(referenceId);
        }
    }

    private void validateSubscription(SubscriptionEntity subscription, String referenceId, SubscriptionReferenceType referenceType) {
        if (!referenceType.equals(subscription.getReferenceType()) || !referenceId.equals(subscription.getReferenceId())) {
            throw new SubscriptionNotFoundException(subscription.getId());
        }
    }

    @Builder
    public record Input(
        String referenceId,
        SubscriptionReferenceType referenceType,
        String subscriptionId,
        io.gravitee.rest.api.model.SubscriptionConfigurationEntity configuration,
        java.util.Map<String, String> metadata,
        java.time.ZonedDateTime startingAt,
        java.time.ZonedDateTime endingAt,
        AuditInfo auditInfo
    ) {}

    public record Output(SubscriptionEntity subscription) {}
}
