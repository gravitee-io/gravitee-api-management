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
import io.gravitee.apim.core.subscription_form.crud_service.SubscriptionFormCrudService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormDefinitionDomainService;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Updates the name, definition and dedicated APIs of an existing subscription form.
 * This operation does NOT change the enabled or default state - use the dedicated use cases for that.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
@CustomLog
public class UpdateSubscriptionFormUseCase {

    private final SubscriptionFormCrudService subscriptionFormCrudService;
    private final SubscriptionFormQueryService subscriptionFormQueryService;
    private final SubscriptionFormDefinitionDomainService definitionDomainService;

    public Output execute(Input input) {
        var existingForm = subscriptionFormQueryService
            .findByIdAndEnvironmentId(input.environmentId(), input.subscriptionFormId())
            .orElseThrow(() ->
                new SubscriptionFormNotFoundException(
                    "Subscription form not found with id [ " + input.subscriptionFormId() + " ]",
                    input.subscriptionFormId().toString()
                )
            );
        definitionDomainService.validateName(input.environmentId(), input.name(), input.subscriptionFormId());
        var apiIds = definitionDomainService.validateApiIds(input.environmentId(), input.apiIds(), input.subscriptionFormId());
        var definition = definitionDomainService.compile(input.gmdContent());

        existingForm.rename(input.name().trim());
        existingForm.update(definition.gmdContent(), definition.constraints());
        existingForm.assignApis(apiIds);
        var savedForm = subscriptionFormCrudService.update(existingForm);
        log.info("Updated subscription form [{}] for environment [{}]", input.subscriptionFormId(), input.environmentId());
        return new Output(savedForm);
    }

    public record Input(
        String environmentId,
        SubscriptionFormId subscriptionFormId,
        String name,
        String gmdContent,
        @Nullable List<String> apiIds
    ) {
        public Input(String environmentId, SubscriptionFormId subscriptionFormId, String name, String gmdContent) {
            this(environmentId, subscriptionFormId, name, gmdContent, null);
        }
    }

    public record Output(SubscriptionForm subscriptionForm) {}
}
