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
import io.gravitee.apim.core.portal_page.domain_service.ContentRenderer;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.exception.RendererException;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.RenderedPageContent;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetPortalPageContentByNavigationIdUseCase {

    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalPageContentQueryService portalPageContentQueryService;
    private final PortalNavigationApiVisibilityDomainService apiVisibilityDomainService;
    private final List<ContentRenderer> contentRenderers;

    public Output execute(Input input) {
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

        if (!(portalNavigationItem instanceof PortalNavigationPage page)) {
            throw InvalidPortalNavigationItemDataException.typeMismatch(
                PortalNavigationItemType.PAGE.name(),
                portalNavigationItem.getType().name()
            );
        }

        var portalPageContent = portalPageContentQueryService
            .findById(page.getPortalPageContentId())
            .orElseThrow(() -> new PageContentNotFoundException(page.getPortalPageContentId().toString()));

        var rendered = contentRenderers
            .stream()
            .filter(r -> r.appliesTo(portalPageContent))
            .findFirst()
            .orElseThrow(() -> new RendererException("No renderer found for content type: " + portalPageContent.getType()))
            .render(page, portalPageContent);

        return new Output(rendered, portalNavigationItem);
    }

    public record Input(
        String portalNavigationItemId,
        String organizationId,
        String environmentId,
        PortalNavigationItemViewerContext viewerContext
    ) {}

    public record Output(RenderedPageContent renderedContent, PortalNavigationItem portalNavigationItem) {}
}
