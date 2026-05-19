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
package io.gravitee.apim.core.portal_page.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.api.service_provider.ApiTemplateModelProvider;
import io.gravitee.apim.core.environment.service_provider.EnvironmentTemplateModelProvider;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdownValidator;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.UpdatePortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@DomainService
public class GraviteePortalPageContentValidatorService implements PortalPageContentValidator {

    private final GraviteeMarkdownValidator graviteeMarkdownValidator;
    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationEnclosingApiDomainService portalNavigationEnclosingApiDomainService;
    private final PortalNavigationTemplatingService portalNavigationTemplatingService;
    private final ApiTemplateModelProvider apiTemplateModelProvider;
    private final EnvironmentTemplateModelProvider environmentTemplateModelProvider;

    @Override
    public boolean appliesTo(PortalPageContent<?> existingContent) {
        return existingContent instanceof GraviteeMarkdownPageContent;
    }

    @Override
    public void validate(PortalPageContent<?> existingContent, UpdatePortalPageContent updateContent) {
        graviteeMarkdownValidator.validateNotEmpty(GraviteeMarkdown.of(updateContent.getContent()));

        final var environmentId = existingContent.getEnvironmentId();
        final var portalPageContentId = existingContent.getId();
        final var navigationPage = portalNavigationItemsQueryService.findNavigationPageByPortalPageContentId(
            environmentId,
            portalPageContentId
        );
        if (navigationPage.isEmpty()) {
            return;
        }

        final var markdown = updateContent.getContent();
        final var organizationId = existingContent.getOrganizationId();
        final var enclosingApiId = portalNavigationEnclosingApiDomainService.findEnclosingApiId(environmentId, navigationPage.get());

        tryDryRender(markdown, organizationId, environmentId, enclosingApiId);
    }

    private void tryDryRender(String markdown, String organizationId, String environmentId, Optional<String> enclosingApiId) {
        final Map<String, Object> model = enclosingApiId
            .<Map<String, Object>>map(id -> Map.of("api", apiTemplateModelProvider.getApiTemplateModel(organizationId, environmentId, id)))
            .orElseGet(() -> Map.of("metadata", environmentTemplateModelProvider.getEnvironmentMetadata(environmentId)));
        try {
            portalNavigationTemplatingService.renderGraviteeMarkdown(
                new RenderPortalNavigationMarkdownInput(GraviteeMarkdown.of(markdown), model)
            );
        } catch (PortalPageContentTemplateException e) {
            throw new InvalidPortalPageContentTemplateException(e.getMessage(), e);
        }
    }
}
