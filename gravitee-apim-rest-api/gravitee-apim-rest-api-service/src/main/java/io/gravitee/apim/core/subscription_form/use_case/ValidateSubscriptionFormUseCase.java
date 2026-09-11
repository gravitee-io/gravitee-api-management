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
package io.gravitee.apim.core.subscription_form.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSpecDomainService;
import lombok.RequiredArgsConstructor;

/**
 * Dry-run twin of {@link CreateOrUpdateSubscriptionFormUseCase}: runs the same checks and reports
 * the findings, persisting nothing.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
public class ValidateSubscriptionFormUseCase {

    private final SubscriptionFormSpecDomainService specDomainService;

    public CreateOrUpdateSubscriptionFormUseCase.Output execute(SubscriptionFormSpecDomainService.Spec spec) {
        var validation = specDomainService.validate(spec);
        return new CreateOrUpdateSubscriptionFormUseCase.Output(validation.existing().orElse(null), validation.errors());
    }
}
