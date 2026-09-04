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
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormElResolverDomainService;
import io.gravitee.apim.core.subscription_form.domain_service.SubscriptionFormSchemaGenerator;
import io.gravitee.apim.core.subscription_form.exception.SubscriptionFormNotFoundException;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * Portal use case: resolve dynamic option values for the environment's default subscription form
 * — the one currently published {@code SUBSCRIPTION_FORM} navigation item, see
 * {@link PortalNavigationItemsQueryService#findPublishedSubscriptionForm} — against a specific API,
 * after enforcing portal navigation visibility for that API
 * ({@link PortalNavigationApiVisibilityDomainService}, same rules as {@link io.gravitee.apim.core.api.use_case.GetApiForPortalUseCase}).
 *
 * @author Gravitee.io Team
 */
@RequiredArgsConstructor
@UseCase
public class ResolveSubscriptionFormOptionsUseCase {

    private final PortalNavigationApiVisibilityDomainService portalNavigationApiVisibilityDomainService;
    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalPageContentQueryService pageContentQueryService;
    private final SubscriptionFormSchemaGenerator schemaGenerator;
    private final SubscriptionFormElResolverDomainService elResolver;

    public Output execute(Input input) {
        if (!portalNavigationApiVisibilityDomainService.isApiVisibleToUser(input.environmentId(), input.apiId(), input.userId())) {
            throw new ApiNotFoundException(input.apiId());
        }

        var form = navigationItemsQueryService
            .findPublishedSubscriptionForm(input.environmentId())
            .orElseThrow(() -> new SubscriptionFormNotFoundException(input.environmentId()));

        var content = pageContentQueryService
            .findById(form.getPortalPageContentId())
            .orElseThrow(() -> new PageContentNotFoundException(form.getPortalPageContentId().toString()));
        if (!(content instanceof GraviteeMarkdownPageContent gmdContent)) {
            throw new TechnicalDomainException(
                "Subscription form navigation item [%s] references non-GRAVITEE_MARKDOWN content".formatted(form.getId())
            );
        }

        var schema = schemaGenerator.generate(gmdContent.getContent());
        var resolvedOptions = elResolver.resolveSchemaOptions(schema, input.environmentId(), input.apiId());

        return new Output(resolvedOptions);
    }

    @Builder
    public record Input(String environmentId, String apiId, @Nullable String userId) {}

    public record Output(Map<String, List<String>> resolvedOptions) {}
}
