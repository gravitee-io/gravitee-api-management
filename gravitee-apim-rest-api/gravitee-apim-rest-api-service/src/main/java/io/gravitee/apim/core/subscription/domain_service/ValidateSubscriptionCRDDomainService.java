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
package io.gravitee.apim.core.subscription.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.plan.crud_service.PlanCrudService;
import io.gravitee.apim.core.subscription.model.crd.SubscriptionCRDSpec;
import io.gravitee.apim.core.utils.StringUtils;
import io.gravitee.apim.core.validation.Validator;
import lombok.RequiredArgsConstructor;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
@RequiredArgsConstructor
public class ValidateSubscriptionCRDDomainService implements Validator<ValidateSubscriptionCRDDomainService.Input> {

    public record Input(AuditInfo auditInfo, SubscriptionCRDSpec spec) implements Validator.Input {}

    private final PlanCrudService planCrudService;

    @Override
    public Result<Input> validateAndSanitize(Input input) {
        if (StringUtils.isEmpty(input.spec().getCustomApiKey())) {
            return Result.ofValue(input);
        }

        var plan = planCrudService.getById(input.spec().getPlanId());
        if (!plan.isApiKey()) {
            return Result.withError(
                Error.severe("Subscription customApiKey is only allowed for API_KEY plans (plan: %s)", input.spec().getPlanId())
            );
        }

        return Result.ofValue(input);
    }
}
