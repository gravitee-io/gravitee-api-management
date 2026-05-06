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
package io.gravitee.apim.core.portal_page.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationEnclosingApiDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalPageContentTemplateException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import io.gravitee.apim.core.portal_page.service_provider.PortalNavigationTemplatingService;
import java.util.Optional;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class GetPortalPageContentByNavigationIdUseCase {

    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalPageContentQueryService portalPageContentQueryService;
    private final PortalNavigationApiVisibilityDomainService apiVisibilityDomainService;
    private final PortalNavigationEnclosingApiDomainService enclosingApiDomainService;
    private final PortalNavigationTemplatingService portalNavigationTemplatingService;

    public Output execute(Input input) {
        // Get the portal navigation item by id and env id
        final var portalNavigationItem = Optional.ofNullable(
            portalNavigationItemsQueryService.findByIdAndEnvironmentId(
                input.environmentId(),
                PortalNavigationItemId.of(input.portalNavigationItemId())
            )
        ).orElseThrow(() -> new PortalNavigationItemNotFoundException(input.portalNavigationItemId()));

        input.viewerContext().validateAccess(portalNavigationItem);

        if (
            portalNavigationItem instanceof PortalNavigationApi api &&
            apiVisibilityDomainService.isApiItemHidden(api, input.viewerContext())
        ) {
            throw new PortalNavigationItemNotFoundException(portalNavigationItem.getId().json());
        }

        if (apiVisibilityDomainService.hasHiddenApiAncestor(input.environmentId(), portalNavigationItem, input.viewerContext())) {
            throw new PortalNavigationItemNotFoundException(portalNavigationItem.getId().json());
        }

        // If the nav item is not a page, throw exception
        if (!(portalNavigationItem instanceof PortalNavigationPage page)) {
            throw InvalidPortalNavigationItemDataException.typeMismatch(
                PortalNavigationItemType.PAGE.name(),
                portalNavigationItem.getType().name()
            );
        }

        // Then get the portal page content by the content id from the navigation item
        var portalPageContent = portalPageContentQueryService
            .findById(page.getPortalPageContentId())
            .orElseThrow(() -> new PageContentNotFoundException(page.getPortalPageContentId().toString()));

        if (portalPageContent instanceof GraviteeMarkdownPageContent markdownPage) {
            final var markdownValue = markdownPage.getContent().value();
            if (markdownValue != null) {
                try {
                    final var renderedMarkdown = portalNavigationTemplatingService.renderGraviteeMarkdown(
                        new PortalNavigationTemplatingService.RenderPortalNavigationMarkdownInput(
                            markdownValue,
                            portalPageContent.getId().json(),
                            page.getOrganizationId(),
                            input.environmentId(),
                            enclosingApiDomainService.findEnclosingApiId(input.environmentId(), page)
                        )
                    );
                    portalPageContent = new GraviteeMarkdownPageContent(
                        markdownPage.getId(),
                        markdownPage.getOrganizationId(),
                        markdownPage.getEnvironmentId(),
                        GraviteeMarkdown.of(renderedMarkdown)
                    );
                } catch (PortalPageContentTemplateException e) {
                    log.warn(
                        "Skipping Gravitee markdown templating for portal navigation page [{}] in environment [{}]: {}",
                        page.getId().json(),
                        input.environmentId(),
                        e.getMessage()
                    );
                }
            }
        }

        return new Output(portalPageContent, portalNavigationItem);
    }

    public record Input(String portalNavigationItemId, String environmentId, PortalNavigationItemViewerContext viewerContext) {}

    public record Output(PortalPageContent<?> portalPageContent, PortalNavigationItem portalNavigationItem) {}
}
