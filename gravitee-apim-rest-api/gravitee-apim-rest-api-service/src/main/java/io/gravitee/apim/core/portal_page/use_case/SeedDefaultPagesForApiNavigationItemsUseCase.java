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
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationItemDomainService;
import io.gravitee.apim.core.portal_page.model.CreatePortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationApi;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentType;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import java.util.ArrayList;
import java.util.List;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@CustomLog
public class SeedDefaultPagesForApiNavigationItemsUseCase {

    private static final String DEFAULT_PAGE_TITLE = "Overview";
    private static final String DEFAULT_OVERVIEW_TEMPLATE = "api-overview-page-content.md";

    private final PortalNavigationItemsQueryService portalNavigationItemsQueryService;
    private final PortalNavigationItemDomainService portalNavigationItemDomainService;
    private final PortalPageContentCrudService portalPageContentCrudService;

    public Output execute(Input input) {
        var seededNavigationItemIds = new ArrayList<PortalNavigationItemId>();

        for (var apiNavigationItemId : input.apiNavigationItemIds()) {
            try {
                if (seedDefaultPage(input.organizationId(), input.environmentId(), apiNavigationItemId)) {
                    seededNavigationItemIds.add(apiNavigationItemId);
                }
            } catch (Exception e) {
                log.warn(
                    "Skipping default page seed for portal navigation item {} in environment {}",
                    apiNavigationItemId,
                    input.environmentId(),
                    e
                );
            }
        }

        return new Output(List.copyOf(seededNavigationItemIds));
    }

    private boolean seedDefaultPage(String organizationId, String environmentId, PortalNavigationItemId apiNavigationItemId) {
        var navigationItem = portalNavigationItemsQueryService.findByIdAndEnvironmentId(environmentId, apiNavigationItemId);
        if (!(navigationItem instanceof PortalNavigationApi apiNavigationItem)) {
            return false;
        }

        var hasChildPage = portalNavigationItemsQueryService
            .findByParentIdAndEnvironmentId(environmentId, apiNavigationItemId)
            .stream()
            .anyMatch(PortalNavigationPage.class::isInstance);
        if (hasChildPage) {
            return false;
        }

        var content = portalPageContentCrudService.create(
            GraviteeMarkdownPageContent.create(organizationId, environmentId, loadContent(DEFAULT_OVERVIEW_TEMPLATE))
        );

        portalNavigationItemDomainService.create(
            organizationId,
            environmentId,
            CreatePortalNavigationItem.builder()
                .title(DEFAULT_PAGE_TITLE)
                .type(PortalNavigationItemType.PAGE)
                .area(PortalArea.TOP_NAVBAR)
                .parentId(apiNavigationItemId)
                .portalPageContentId(content.getId())
                .contentType(PortalPageContentType.GRAVITEE_MARKDOWN)
                .order(0)
                .published(false)
                .visibility(apiNavigationItem.getVisibility())
                .build()
        );

        return true;
    }

    private String loadContent(String contentPath) {
        try (
            var inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(String.format("templates/%s", contentPath))
        ) {
            if (inputStream == null) {
                throw new IllegalStateException(String.format("Could not load default portal page template for %s", contentPath));
            }
            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Could not load default portal page template", e);
        }
    }

    public record Input(String organizationId, String environmentId, List<PortalNavigationItemId> apiNavigationItemIds) {}

    public record Output(List<PortalNavigationItemId> seededNavigationItemIds) {}
}
