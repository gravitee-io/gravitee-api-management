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
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Use case for getting the subscription form for an environment.
 *
 * <p>When {@link Input#apiId()} is non-null, EL expressions in option-bearing fields are resolved
 * against the target API's metadata and returned in {@link Output#resolvedOptions()}.
 * When null (e.g. Console Form Builder), only environment-level metadata is used for resolution.</p>
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
public class GetSubscriptionFormForEnvironmentUseCase {

    private final SubscriptionFormQueryService subscriptionFormQueryService;
    private final SubscriptionFormSchemaGenerator schemaGenerator;
    private final SubscriptionFormElResolverDomainService elResolver;

    public Output execute(Input input) {
        var subscriptionForm = subscriptionFormQueryService
            .findDefaultForEnvironmentId(input.environmentId())
            .orElseThrow(() -> new SubscriptionFormNotFoundException(input.environmentId()));

        if (input.onlyEnabled() && !subscriptionForm.isEnabled()) {
            throw new SubscriptionFormNotFoundException(input.environmentId());
        }

        var schema = schemaGenerator.generate(subscriptionForm.getGmdContent());
        var resolvedOptions = elResolver.resolveSchemaOptions(schema, input.environmentId(), input.apiId());

        return new Output(subscriptionForm, resolvedOptions);
    }

    @Builder
    public record Input(String environmentId, boolean onlyEnabled, String apiId) {}

    public record Output(SubscriptionForm subscriptionForm, Map<String, List<String>> resolvedOptions) {}
}
