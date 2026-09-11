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
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormElResolverDomainService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSchemaGenerator;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Loads one form of the catalog for the Console form builder, whether it is enabled or not.
 * EL expressions in option-bearing fields are resolved without API metadata and fall back to the
 * configured options when resolution fails.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
public class GetSubscriptionFormUseCase {

    private final SubscriptionFormQueryService subscriptionFormQueryService;
    private final SubscriptionFormSchemaGenerator schemaGenerator;
    private final SubscriptionFormElResolverDomainService elResolver;

    public Output execute(Input input) {
        var subscriptionForm = subscriptionFormQueryService
            .findByIdAndEnvironmentId(input.environmentId(), input.subscriptionFormId())
            .orElseThrow(() ->
                new SubscriptionFormNotFoundException(
                    "Subscription form not found with id [ " + input.subscriptionFormId() + " ]",
                    input.subscriptionFormId().toString()
                )
            );
        var schema = schemaGenerator.generate(subscriptionForm.getGmdContent());
        return new Output(subscriptionForm, elResolver.resolveSchemaOptions(schema));
    }

    public record Input(String environmentId, SubscriptionFormId subscriptionFormId) {}

    public record Output(SubscriptionForm subscriptionForm, Map<String, List<String>> resolvedOptions) {}
}
