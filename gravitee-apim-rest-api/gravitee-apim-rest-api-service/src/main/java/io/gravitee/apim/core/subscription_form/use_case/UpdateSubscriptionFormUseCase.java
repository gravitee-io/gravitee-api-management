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
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Use case for updating the subscription form for an environment.
 * The subscription form must already exist (created by the DefaultSubscriptionFormUpgrader on system startup).
 * This operation does NOT change the enabled state - use Enable/Disable use cases for that.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
@CustomLog
public class UpdateSubscriptionFormUseCase {

    private final SubscriptionFormCrudService subscriptionFormCrudService;
    private final SubscriptionFormQueryService subscriptionFormQueryService;
    private final GraviteeMarkdownValidator graviteeMarkdownValidator;

    public Output execute(Input input) {
        graviteeMarkdownValidator.validateNotEmpty(input::gmdContent);

        var existingForm = subscriptionFormQueryService
            .findByIdAndEnvironmentId(input.environmentId(), input.subscriptionFormId())
            .orElseThrow(() ->
                new SubscriptionFormNotFoundException(
                    "Subscription form not found with id [ " + input.subscriptionFormId() + " ]",
                    input.subscriptionFormId().toString()
                )
            );

        existingForm.update(input.gmdContent());
        var savedForm = subscriptionFormCrudService.update(existingForm);

        log.info("Updated subscription form [{}] for environment [{}]", input.subscriptionFormId(), input.environmentId());

        return new Output(savedForm);
    }

    public record Input(String environmentId, SubscriptionFormId subscriptionFormId, String gmdContent) {}

    public record Output(SubscriptionForm subscriptionForm) {}
}
