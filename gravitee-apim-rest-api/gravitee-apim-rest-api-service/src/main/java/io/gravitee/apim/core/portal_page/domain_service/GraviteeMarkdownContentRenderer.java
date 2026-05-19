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
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.RenderedPageContent;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class GraviteeMarkdownContentRenderer implements ContentRenderer {

    private final PortalNavigationEnclosingApiDomainService enclosingApiDomainService;
    private final PortalNavigationTemplatingService portalNavigationTemplatingService;
    private final ApiTemplateModelProvider apiTemplateModelProvider;
    private final EnvironmentTemplateModelProvider environmentTemplateModelProvider;

    @Override
    public boolean appliesTo(PortalPageContent<?> content) {
        return content instanceof GraviteeMarkdownPageContent;
    }

    @Override
    public RenderedPageContent render(PortalNavigationItem item, PortalPageContent<?> content) {
        var markdownPage = (GraviteeMarkdownPageContent) content;
        var page = (PortalNavigationPage) item;
        var enclosingApiId = enclosingApiDomainService.findEnclosingApiId(content.getEnvironmentId(), page);
        Map<String, Object> model = enclosingApiId
            .<Map<String, Object>>map(id ->
                Map.of("api", apiTemplateModelProvider.getApiTemplateModel(content.getOrganizationId(), content.getEnvironmentId(), id))
            )
            .orElseGet(() -> Map.of("metadata", environmentTemplateModelProvider.getEnvironmentMetadata(content.getEnvironmentId())));
        return portalNavigationTemplatingService.renderGraviteeMarkdown(
            new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(markdownPage.getContent(), model)
        );
    }
}
