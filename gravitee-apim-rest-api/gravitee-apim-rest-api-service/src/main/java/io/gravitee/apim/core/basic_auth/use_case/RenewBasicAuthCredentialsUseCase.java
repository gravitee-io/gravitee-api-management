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
package io.gravitee.apim.core.basic_auth.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.basic_auth.domain_service.GenerateBasicAuthCredentialsDomainService;
import io.gravitee.apim.core.basic_auth.model.BasicAuthCredentialsEntity;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.subscription.crud_service.SubscriptionCrudService;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RenewBasicAuthCredentialsUseCase {

    private final SubscriptionCrudService subscriptionCrudService;
    private final PlanCrudService planCrudService;
    private final GenerateBasicAuthCredentialsDomainService generateBasicAuthCredentialsDomainService;

    public Output execute(Input input) {
        var subscription = subscriptionCrudService.get(input.subscriptionId());
        validateSubscriptionBelongsToApi(subscription, input.apiId());
        validateSubscriptionIsActive(subscription);
        validatePlanIsBasicAuth(subscription);

        var renewed = generateBasicAuthCredentialsDomainService.renew(subscription, input.auditInfo());
        return new Output(renewed);
    }

    private void validateSubscriptionBelongsToApi(SubscriptionEntity subscription, String apiId) {
        if (!subscription.getApiId().equals(apiId)) {
            throw new IllegalArgumentException("Subscription does not belong to API " + apiId);
        }
    }

    private void validateSubscriptionIsActive(SubscriptionEntity subscription) {
        if (!subscription.isAccepted()) {
            throw new IllegalStateException("Cannot renew credentials for a non-active subscription");
        }
    }

    private void validatePlanIsBasicAuth(SubscriptionEntity subscription) {
        Plan plan = planCrudService.getById(subscription.getPlanId());
        if (!plan.isBasicAuth()) {
            throw new IllegalStateException("Cannot renew Basic Auth credentials for a non-Basic Auth plan");
        }
    }

    public record Input(String apiId, String subscriptionId, AuditInfo auditInfo) {}

    public record Output(BasicAuthCredentialsEntity credentials) {}
}
