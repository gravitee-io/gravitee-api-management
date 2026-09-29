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
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class HomepageReconciler {

    private final PortalNavigationItemsQueryService navigationItemsQueryService;
    private final PortalNavigationItemCrudService navigationItemCrudService;
    private final PortalPageContentCrudService pageContentCrudService;

    /** Homepages that {@link #dropStaleHomepages} would remove, without removing them. */
    public List<PortalNavigationItem> findStaleHomepages(String environmentId, String portalId, PortalNavigationItemId activeHomepageId) {
        return Stream.concat(
            staleHomepagesMatching(environmentId, new NavigationItemReference.PortalReference(PortalId.of(portalId)), activeHomepageId),
            staleHomepagesMatching(environmentId, NavigationItemReference.defaultReference(), activeHomepageId)
        ).toList();
    }

    public void dropStaleHomepages(String environmentId, String portalId, PortalNavigationItemId activeHomepageId) {
        findStaleHomepages(environmentId, portalId, activeHomepageId).forEach(this::deleteItemAndContent);
    }

    private Stream<PortalNavigationItem> staleHomepagesMatching(
        String environmentId,
        NavigationItemReference reference,
        PortalNavigationItemId activeHomepageId
    ) {
        return navigationItemsQueryService
            .findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(environmentId, PortalArea.HOMEPAGE, reference)
            .stream()
            .filter(item -> !item.getId().equals(activeHomepageId))
            .filter(item -> item.getAutomationMetadata() == null);
    }

    private void deleteItemAndContent(PortalNavigationItem item) {
        if (item instanceof PortalNavigationPage page) {
            pageContentCrudService.delete(page.getPortalPageContentId());
        }
        navigationItemCrudService.delete(item.getId());
    }
}
