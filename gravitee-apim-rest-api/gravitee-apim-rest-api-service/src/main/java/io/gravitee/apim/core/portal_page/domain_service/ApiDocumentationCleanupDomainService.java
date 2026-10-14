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
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContent;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import io.gravitee.apim.core.portal_page.query_service.PortalPageContentQueryService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class ApiDocumentationCleanupDomainService {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalPageContentQueryService pageContentQueryService;
    private final PortalPageContentCrudService pageContentCrudService;

    public void cleanupForApi(String environmentId, String apiId) {
        var owner = new NavigationItemReference.ApiReference(apiId);
        var documentation = navigationItemsQueryService
            .search(PortalNavigationItemQueryCriteria.builder().environmentId(environmentId).build())
            .stream()
            .filter(item -> owner.equals(item.getReference()))
            .filter(ApiDocumentationCleanupDomainService::isDocumentation)
            .toList();
        var navigationIds = documentation.stream().map(PortalNavigationItem::getId).collect(Collectors.toSet());
        var contentIds = documentation
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .map(PortalNavigationPage::getPortalPageContentId)
            .collect(Collectors.toSet());

        // Automation can persist content before its navigation item is materialized.
        pageContentQueryService
            .findByReference(environmentId, AutomationMetadata.ReferenceType.API, apiId)
            .stream()
            .map(PortalPageContent::getId)
            .forEach(contentIds::add);

        var contentToDelete = findExclusiveContentIds(environmentId, navigationIds, contentIds);
        // Keep navigation references until content deletion succeeds, so cleanup can be retried.
        contentToDelete.forEach(pageContentCrudService::delete);
        if (!navigationIds.isEmpty()) {
            navigationItemCrudService.deleteByIds(List.copyOf(navigationIds));
        }
    }

    private List<PortalPageContentId> findExclusiveContentIds(
        String environmentId,
        Set<PortalNavigationItemId> navigationIds,
        Set<PortalPageContentId> contentIds
    ) {
        if (contentIds.isEmpty()) {
            return List.of();
        }

        // Retained pages may reference the same content across environments.
        var sharedContentIds = navigationItemsQueryService
            .search(PortalNavigationItemQueryCriteria.builder().type(PortalNavigationItemType.PAGE).build())
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .filter(page -> !navigationIds.contains(page.getId()))
            .map(PortalNavigationPage::getPortalPageContentId)
            .filter(contentIds::contains)
            .collect(Collectors.toSet());

        return contentIds
            .stream()
            .filter(id -> !sharedContentIds.contains(id))
            .map(pageContentQueryService::findById)
            .flatMap(Optional::stream)
            .filter(content -> environmentId.equals(content.getEnvironmentId()))
            .map(PortalPageContent::getId)
            .toList();
    }

    private static boolean isDocumentation(PortalNavigationItem item) {
        return switch (item.getType()) {
            case PAGE, FOLDER, LINK -> true;
            default -> false;
        };
    }
}
