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
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemVisibilityEvaluator;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemVisibilityService;
import io.gravitee.apim.core.portal_page.exception.InvalidPortalNavigationItemDataException;
import io.gravitee.apim.core.portal_page.exception.PageContentNotFoundException;
import io.gravitee.apim.core.portal_page.exception.PortalNavigationItemNotFoundException;
import io.gravitee.apim.core.portal_page.exception.RendererException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalNavigationSubscriptionForm;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
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
    private final List<PortalNavigationItemVisibilityService> visibilityServices;
    private final List<ContentRenderer> contentRenderers;

    public Output execute(Input input) {
        final var portalNavigationItem = Optional.ofNullable(
            portalNavigationItemsQueryService.findByIdAndEnvironmentId(
                input.environmentId(),
                PortalNavigationItemId.of(input.portalNavigationItemId())
            )
        ).orElseThrow(() -> new PortalNavigationItemNotFoundException(input.portalNavigationItemId()));

        input.viewerContext().validateAccess(portalNavigationItem);

        var visibilityEvaluator = new PortalNavigationItemVisibilityEvaluator(
            input.environmentId(),
            input.viewerContext(),
            portalNavigationItemsQueryService,
            visibilityServices
        );
        if (!visibilityEvaluator.isVisible(portalNavigationItem) || visibilityEvaluator.hasHiddenAncestor(portalNavigationItem)) {
            throw new PortalNavigationItemNotFoundException(portalNavigationItem.getId().json());
        }

        PortalPageContentId portalPageContentId;
        if (portalNavigationItem instanceof PortalNavigationPage page) {
            portalPageContentId = page.getPortalPageContentId();
        } else if (portalNavigationItem instanceof PortalNavigationSubscriptionForm subscriptionForm) {
            portalPageContentId = subscriptionForm.getPortalPageContentId();
        } else {
            throw InvalidPortalNavigationItemDataException.typeMismatch(
                PortalNavigationItemType.PAGE.name(),
                portalNavigationItem.getType().name()
            );
        }

        var portalPageContent = portalPageContentQueryService
            .findById(portalPageContentId)
            .orElseThrow(() -> new PageContentNotFoundException(portalPageContentId.toString()));

        RenderedPageContent rendered;
        if (portalNavigationItem instanceof PortalNavigationSubscriptionForm) {
            // Subscription form content is served raw, with EL placeholders left unresolved: dynamic
            // option resolution against a specific API is handled separately (there is no "enclosing
            // API" for an unparented subscription-form item), matching this content's existing
            // pre-Phase-7 behavior of never being templated at the content-serving layer.
            var markdownContent = (GraviteeMarkdownPageContent) portalPageContent;
            rendered = RenderedPageContent.of(markdownContent.getContent().value(), markdownContent.getType());
        } else {
            rendered = contentRenderers
                .stream()
                .filter(r -> r.appliesTo(portalPageContent))
                .findFirst()
                .orElseThrow(() -> new RendererException("No renderer found for content type: " + portalPageContent.getType()))
                .render(portalNavigationItem, portalPageContent);
        }

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
