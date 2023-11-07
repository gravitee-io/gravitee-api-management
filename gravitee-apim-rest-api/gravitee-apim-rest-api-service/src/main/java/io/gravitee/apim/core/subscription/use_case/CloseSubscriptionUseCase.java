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

import io.gravitee.apim.core.TransactionalUseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.domain_service.CloseSubscriptionDomainService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.rest.api.service.exceptions.SubscriptionNotFoundException;
import lombok.Builder;

@TransactionalUseCase
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
        var subscription = subscriptionCrudService.get(input.subscriptionId);
        if (input.apiId != null && !subscription.getApiId().equals(input.apiId)) {
            throw new SubscriptionNotFoundException(input.subscriptionId);
        }

        if (input.applicationId != null && !subscription.getApplicationId().equals(input.applicationId)) {
            throw new SubscriptionNotFoundException(input.subscriptionId);
        }

        var closedSubscription = closeSubscriptionDomainService.closeSubscription(input.subscriptionId, input.auditInfo);
        return new Output(closedSubscription);
    }

    @Builder
    public record Input(String subscriptionId, String apiId, String applicationId, AuditInfo auditInfo) {
        public Input(String subscriptionId, AuditInfo auditInfo) {
            this(subscriptionId, null, null, auditInfo);
        }
    }

    public record Output(SubscriptionEntity subscription) {}
}
