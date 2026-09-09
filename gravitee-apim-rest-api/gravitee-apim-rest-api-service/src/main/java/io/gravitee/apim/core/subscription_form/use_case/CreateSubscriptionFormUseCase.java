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
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import jakarta.annotation.Nullable;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

/**
 * Adds a form to the environment catalog, optionally dedicated to a set of APIs. The new form starts
 * disabled and is not the environment default: it only reaches consumers once enabled.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
@CustomLog
public class CreateSubscriptionFormUseCase {

    private final SubscriptionFormCrudService subscriptionFormCrudService;
    private final SubscriptionFormDefinitionDomainService definitionDomainService;

    public Output execute(Input input) {
        definitionDomainService.validateName(input.environmentId(), input.name(), null);
        var apiIds = definitionDomainService.validateApiIds(input.environmentId(), input.apiIds(), null);
        var definition = definitionDomainService.compile(input.gmdContent());

        var created = subscriptionFormCrudService.create(
            SubscriptionForm.builder()
                .environmentId(input.environmentId())
                .name(input.name().trim())
                .gmdContent(definition.gmdContent())
                .validationConstraints(definition.constraints())
                .apiIds(apiIds)
                .enabled(false)
                .defaultForm(false)
                .build()
        );
        log.info("Created subscription form [{}] '{}' for environment [{}]", created.getId(), created.getName(), input.environmentId());
        return new Output(created);
    }

    public record Input(String environmentId, String name, String gmdContent, @Nullable List<String> apiIds) {
        public Input(String environmentId, String name, String gmdContent) {
            this(environmentId, name, gmdContent, null);
        }
    }

    public record Output(SubscriptionForm subscriptionForm) {}
}
