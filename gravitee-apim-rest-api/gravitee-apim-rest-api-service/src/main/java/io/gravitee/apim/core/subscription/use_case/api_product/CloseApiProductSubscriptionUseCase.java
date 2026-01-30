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
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.apim.core.subscription.model.SubscriptionReferenceType;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import lombok.Builder;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@CustomLog
@RequiredArgsConstructor
public class CloseApiProductSubscriptionUseCase {

    private final SubscriptionCrudService subscriptionCrudService;
    private final CloseSubscriptionDomainService closeSubscriptionDomainService;
    private final ApiProductCrudService apiProductCrudService;

    public Output execute(Input input) {
        log.debug("Closing subscription {} for API Product {}", input.subscriptionId, input.apiProductId);
        // Verify API Product exists
        apiProductCrudService.get(input.apiProductId);

        var subscription = subscriptionCrudService.get(input.subscriptionId);
        if (
            !SubscriptionReferenceType.API_PRODUCT.equals(subscription.getReferenceType()) ||
            !input.apiProductId.equals(subscription.getReferenceId())
        ) {
            throw new SubscriptionNotFoundException(input.subscriptionId);
        }

        var closedSubscription = closeSubscriptionDomainService.closeSubscriptionForApiProduct(input.subscriptionId, input.auditInfo);

        log.debug("Closed subscription {} for API Product {}", input.subscriptionId, input.apiProductId);
        return new Output(closedSubscription);
    }

    @Builder
    public record Input(String apiProductId, String subscriptionId, AuditInfo auditInfo) {
        public Input(String apiProductId, String subscriptionId, AuditInfo auditInfo) {
            this.apiProductId = apiProductId;
            this.subscriptionId = subscriptionId;
            this.auditInfo = auditInfo;
        }
    }

    public record Output(SubscriptionEntity subscription) {}
}
