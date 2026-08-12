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
package io.gravitee.apim.core.portal_page.domain_service.reconciliation;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal.model.PortalId;
import io.gravitee.apim.core.portal_page.crud_service.PortalNavigationItemCrudService;
import io.gravitee.apim.core.portal_page.crud_service.PortalPageContentCrudService;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.query_service.PortalNavigationItemsQueryService;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class HomepageReconciler {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalPageContentCrudService pageContentCrudService;

    public void dropStaleHomepages(String environmentId, String portalId, PortalNavigationItemId activeHomepageId) {
        dropHomepagesMatching(environmentId, new NavigationItemReference.PortalReference(PortalId.of(portalId)), activeHomepageId);
        dropHomepagesMatching(environmentId, NavigationItemReference.DEFAULT, activeHomepageId);
    }

    private void dropHomepagesMatching(String environmentId, NavigationItemReference reference, PortalNavigationItemId activeHomepageId) {
        var stale = navigationItemsQueryService.findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
            environmentId,
            PortalArea.HOMEPAGE,
            reference
        );
        var itemsToDelete = stale
            .stream()
            .filter(item -> !item.getId().equals(activeHomepageId))
            .toList();
        itemsToDelete.forEach(this::deleteItemAndContent);
    }

    private void deleteItemAndContent(PortalNavigationItem item) {
        if (item instanceof PortalNavigationPage page) {
            pageContentCrudService.delete(page.getPortalPageContentId());
        }
        navigationItemCrudService.delete(item.getId());
    }
}
