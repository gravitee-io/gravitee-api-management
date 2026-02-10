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
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import lombok.RequiredArgsConstructor;

/**
 * Use case for getting the subscription form for an environment (the default form for that environment).
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
public class GetSubscriptionFormForEnvironmentUseCase {

    private final SubscriptionFormQueryService subscriptionFormQueryService;

    public Output execute(Input input) {
        var subscriptionForm = subscriptionFormQueryService
            .findDefaultForEnvironmentId(input.environmentId())
            .orElseThrow(() -> new SubscriptionFormNotFoundException(input.environmentId()));

        return new Output(subscriptionForm);
    }

    public record Input(String environmentId) {}

    public record Output(SubscriptionForm subscriptionForm) {}
}
