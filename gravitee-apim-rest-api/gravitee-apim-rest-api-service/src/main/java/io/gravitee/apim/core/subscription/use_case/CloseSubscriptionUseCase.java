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
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import lombok.Builder;

@UseCase
public class CloseSubscriptionUseCase {

    private final SubscriptionCrudService subscriptionCrudService;
    private final CloseSubscriptionDomainService closeSubscriptionDomainService;

    public CloseSubscriptionUseCase(
        SubscriptionCrudService subscriptionCrudService,
        CloseSubscriptionDomainService closeSubscriptionDomainService
    ) {
        this.subscriptionCrudService = subscriptionCrudService;
        this.closeSubscriptionDomainService = closeSubscriptionDomainService;
    }

    public Output execute(Input input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        var subscription = subscriptionCrudService.get(input.subscriptionId);

        if (input.referenceId != null && input.referenceType != null) {
            validateSubscription(subscription, input.referenceId, input.referenceType);
        }

        if (input.applicationId != null && !subscription.getApplicationId().equals(input.applicationId)) {
            throw new SubscriptionNotFoundException(input.subscriptionId);
        }

        SubscriptionEntity closedSubscription = closeSubscriptionDomainService.closeSubscription(input.subscriptionId, input.auditInfo);
        return new Output(closedSubscription);
    }

    private void validateSubscription(SubscriptionEntity subscription, String referenceId, SubscriptionReferenceType referenceType) {
        if (
            subscription.getReferenceType() == null ||
            !referenceType.equals(subscription.getReferenceType()) ||
            subscription.getReferenceId() == null ||
            !referenceId.equals(subscription.getReferenceId())
        ) {
            throw new SubscriptionNotFoundException(subscription.getId());
        }
    }

    @Builder
    public record Input(
        String subscriptionId,
        String referenceId,
        SubscriptionReferenceType referenceType,
        String applicationId,
        AuditInfo auditInfo
    ) {
        public Input(String subscriptionId, AuditInfo auditInfo) {
            this(subscriptionId, null, null, null, auditInfo);
        }
    }

    public record Output(SubscriptionEntity subscription) {}
}
