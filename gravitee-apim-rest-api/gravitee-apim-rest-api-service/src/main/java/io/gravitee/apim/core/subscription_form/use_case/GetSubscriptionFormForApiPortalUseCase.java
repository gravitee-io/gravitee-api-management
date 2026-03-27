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
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormElResolverDomainService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSchemaGenerator;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.query_service.SubscriptionFormQueryService;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Portal use case: load the environment default subscription form with dynamic options resolved
 * against a specific API, after enforcing portal navigation visibility for that API
 * ({@link PortalNavigationApiVisibilityDomainService}, same rules as {@link io.gravitee.apim.core.api.use_case.GetApiForPortalUseCase}).
 * Only an <em>enabled</em> form is returned; a disabled form yields {@link SubscriptionFormNotFoundException}.
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
public class GetSubscriptionFormForApiPortalUseCase {

    private final PortalNavigationApiVisibilityDomainService portalNavigationApiVisibilityDomainService;
    private final SubscriptionFormQueryService subscriptionFormQueryService;
    private final SubscriptionFormSchemaGenerator schemaGenerator;
    private final SubscriptionFormElResolverDomainService elResolver;

    public Output execute(Input input) {
        if (!portalNavigationApiVisibilityDomainService.isApiVisibleToUser(input.environmentId(), input.apiId(), input.userId())) {
            throw new ApiNotFoundException(input.apiId());
        }

        var subscriptionForm = subscriptionFormQueryService
            .findDefaultForEnvironmentId(input.environmentId())
            .orElseThrow(() -> new SubscriptionFormNotFoundException(input.environmentId()));

        if (!subscriptionForm.isEnabled()) {
            throw new SubscriptionFormNotFoundException(input.environmentId());
        }

        var schema = schemaGenerator.generate(subscriptionForm.getGmdContent());
        var resolvedOptions = elResolver.resolveSchemaOptions(schema, input.environmentId(), input.apiId());

        return new Output(subscriptionForm, resolvedOptions);
    }

    @Builder
    public record Input(String environmentId, String apiId, @Nullable String userId) {}

    public record Output(SubscriptionForm subscriptionForm, Map<String, List<String>> resolvedOptions) {}
}
