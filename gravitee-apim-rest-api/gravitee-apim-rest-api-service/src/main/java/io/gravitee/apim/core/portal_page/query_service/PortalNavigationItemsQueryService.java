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
package io.gravitee.apim.core.portal_page.query_service;

import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.AutomationMetadata;
import io.gravitee.apim.core.portal_page.model.NavigationItemReference;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public interface PortalNavigationItemsQueryService {
    PortalNavigationItem findByIdAndEnvironmentId(String environmentId, PortalNavigationItemId id);

    List<PortalNavigationItem> findByParentIdAndEnvironmentId(String environmentId, PortalNavigationItemId id);

    List<PortalNavigationItem> findByAutomationReference(
        String environmentId,
        AutomationMetadata.ReferenceType referenceType,
        String referenceId
    );

    default Optional<PortalNavigationItem> findByParentIdAndSegment(String environmentId, PortalNavigationItemId parentId, String segment) {
        return findByParentIdAndEnvironmentId(environmentId, parentId)
            .stream()
            .filter(it -> segment.equals(it.getSegment()))
            .findFirst();
    }

    List<PortalNavigationItem> search(PortalNavigationItemQueryCriteria criteria);

    List<PortalNavigationItem> findTopLevelItemsByEnvironmentIdAndPortalArea(String environmentId, PortalArea portalArea);

    List<PortalNavigationItem> findTopLevelItemsByEnvironmentIdAndPortalAreaAndReference(
        String environmentId,
        PortalArea area,
        @Nonnull NavigationItemReference reference
    );

    List<PortalNavigationItem> findAllByRootId(String environmentId, PortalNavigationItemId rootId);

    /**
     * Every item whose source has auto-fetch enabled, across all environments: the auto-fetch scheduler
     * is a node-wide job and does not run inside an environment context.
     */
    default List<PortalNavigationItem> findAllWithAutoFetchEnabled() {
        // Restricted to PAGE in the query rather than in the caller: only a PAGE owns content that the
        // scheduler knows how to refresh.
        return search(PortalNavigationItemQueryCriteria.builder().useAutoFetch(true).type(PortalNavigationItemType.PAGE).build());
    }

    default Optional<PortalNavigationPage> findNavigationPageByPortalPageContentId(String environmentId, PortalPageContentId contentId) {
        final var criteria = PortalNavigationItemQueryCriteria.builder()
            .environmentId(environmentId)
            .type(PortalNavigationItemType.PAGE)
            .build();
        return search(criteria)
            .stream()
            .filter(PortalNavigationPage.class::isInstance)
            .map(PortalNavigationPage.class::cast)
            .filter(page -> page.getPortalPageContentId().equals(contentId))
            .findFirst();
    }
}
