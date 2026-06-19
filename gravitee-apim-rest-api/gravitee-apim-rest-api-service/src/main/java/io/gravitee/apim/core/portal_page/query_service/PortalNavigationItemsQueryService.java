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

import io.gravitee.apim.core.portal_page.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemQueryCriteria;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemType;
import io.gravitee.apim.core.portal_page.model.PortalNavigationPage;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import java.util.List;
import java.util.Optional;

public interface PortalNavigationItemsQueryService {
    PortalNavigationItem findByIdAndEnvironmentId(String environmentId, PortalNavigationItemId id);

    List<PortalNavigationItem> findByParentIdAndEnvironmentId(String environmentId, PortalNavigationItemId id);

    default Optional<PortalNavigationItem> findByParentIdAndSegment(String environmentId, PortalNavigationItemId parentId, String segment) {
        return findByParentIdAndEnvironmentId(environmentId, parentId)
            .stream()
            .filter(it -> segment.equals(it.getSegment()))
            .findFirst();
    }

    List<PortalNavigationItem> search(PortalNavigationItemQueryCriteria criteria);

    List<PortalNavigationItem> findTopLevelItemsByEnvironmentIdAndPortalArea(String environmentId, PortalArea portalArea);

    List<PortalNavigationItem> findAllByRootId(String environmentId, PortalNavigationItemId rootId);

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
